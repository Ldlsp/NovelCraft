package com.mozhou.novelcraft

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

private data class WritingContextInput(
    val project: NovelProject?,
    val chapter: Chapter?,
    val chapters: List<Chapter>,
    val items: List<StoryItem>,
    val anchors: List<StoryAnchor>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class NovelViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NovelRepository(NovelDatabase.create(application))
    private val modelPreferences = ModelPreferences(application)
    private val modelClient = OpenAiCompatibleClient()
    private val saveChapterJobs = mutableMapOf<Long, Job>()
    private var renameChapterJob: Job? = null
    private var savePlanJob: Job? = null
    private var saveBeatSheetJob: Job? = null
    private var saveStyleJob: Job? = null
    private val generationJobs = mutableMapOf<GenerationTask, Job>()
    private val generationRequests = mutableMapOf<GenerationTask, GenerationRequest>()
    private val pendingChapterContent = mutableMapOf<Long, String>()

    val projects = repository.projects().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val selectedProjectId = MutableStateFlow<Long?>(null)
    private val selectedChapterId = MutableStateFlow<Long?>(null)

    val selectedProject = selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.project(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val chapters = selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.chapters(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val storyItems = selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.storyItems(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val anchors = selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.anchors(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val edges = selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.edges(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedChapter = combine(chapters, selectedChapterId) { all, id ->
        all.firstOrNull { it.id == id } ?: all.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val writingContextInput = combine(selectedProject, selectedChapter, chapters, storyItems, anchors) { project, chapter, allChapters, items, projectAnchors ->
        WritingContextInput(project, chapter, allChapters, items, projectAnchors)
    }

    val contextPacket = combine(writingContextInput, edges) { input, graphEdges ->
        if (input.project == null || input.chapter == null) ContextPacket() else ContextEngine.build(input.project, input.chapter, input.chapters, input.items, input.anchors, graphEdges)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContextPacket())

    val qualityIssues = combine(selectedChapter, storyItems, anchors) { chapter, items, projectAnchors ->
        QualityGate.inspect(chapter, items, projectAnchors)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val modelConfig = MutableStateFlow(modelPreferences.load())
    val message = MutableStateFlow<String?>(null)
    val generationTasks = MutableStateFlow<Set<GenerationTask>>(emptySet())
    val repairPlan = MutableStateFlow<String?>(null)
    private val outlineCascadePending = MutableStateFlow(false)

    private fun beginGeneration(task: GenerationTask): GenerationRequest? {
        if (task in generationTasks.value) {
            message.value = "${task.label}正在生成"
            return null
        }
        return GenerationRequest().also { request ->
            generationRequests[task] = request
            generationTasks.value = generationTasks.value + task
        }
    }

    private fun finishGeneration(task: GenerationTask, request: GenerationRequest) {
        if (generationRequests[task] !== request) return
        generationRequests.remove(task)
        generationJobs.remove(task)
        generationTasks.value = generationTasks.value - task
    }

    fun selectProject(projectId: Long) {
        selectedProjectId.value = projectId
        selectedChapterId.value = null
    }

    fun selectChapter(chapterId: Long) {
        selectedChapterId.value = chapterId
    }

    fun createProject(title: String, genre: String, premise: String) {
        if (title.isBlank()) {
            message.value = "请填写书名"
            return
        }
        viewModelScope.launch {
            val projectId = repository.createProject(title, genre.ifBlank { "待分类" }, premise)
            selectProject(projectId)
            message.value = "已创建《" + title.trim() + "》"
        }
    }

    fun importDocument(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                repository.importProject(
                    getApplication(),
                    uri,
                    uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "导入作品",
                )
            }.onSuccess {
                selectProject(it)
                message.value = "导入完成，可从第一章继续写"
            }.onFailure {
                message.value = it.message ?: "导入失败"
            }
        }
    }

    fun exportDocument(uri: Uri) {
        val project = selectedProject.value ?: run {
            message.value = "请先选择要导出的作品"
            return
        }
        val chaptersToExport = chapters.value
        viewModelScope.launch {
            runCatching {
                val markdown = buildString {
                    appendLine("# ${project.title}")
                    if (project.genre.isNotBlank()) appendLine("题材：${project.genre}")
                    if (project.premise.isNotBlank()) appendLine("设定：${project.premise}")
                    appendLine()
                    chaptersToExport.forEach { chapter ->
                        appendLine("## 第${chapter.number}章 ${chapter.title}")
                        appendLine()
                        appendLine(chapter.content.trim())
                        appendLine()
                    }
                }
                getApplication<Application>().contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(markdown) }
                    ?: error("无法创建导出文件")
            }.onSuccess {
                message.value = "已导出《${project.title}》"
            }.onFailure {
                message.value = it.message ?: "导出失败"
            }
        }
    }

    fun saveChapter(content: String) {
        val chapter = selectedChapter.value ?: return
        pendingChapterContent[chapter.id] = content
        saveChapterJobs.remove(chapter.id)?.cancel()
        saveChapterJobs[chapter.id] = viewModelScope.launch {
            delay(500)
            val pending = pendingChapterContent[chapter.id] ?: return@launch
            repository.updateChapter(chapter, pending)
            if (pendingChapterContent[chapter.id] == pending) pendingChapterContent.remove(chapter.id)
            saveChapterJobs.remove(chapter.id)
        }
    }

    fun renameChapter(title: String) {
        val chapter = selectedChapter.value ?: return
        renameChapterJob?.cancel()
        renameChapterJob = viewModelScope.launch {
            delay(500)
            repository.renameChapter(chapter, title)
        }
    }

    fun addChapter() {
        val projectId = selectedProjectId.value ?: return
        viewModelScope.launch {
            selectedChapterId.value = repository.addChapter(projectId)
            message.value = "已新建章节"
        }
    }

    fun deleteCurrentChapter() {
        val chapter = selectedChapter.value ?: return
        viewModelScope.launch {
            runCatching { repository.deleteChapter(chapter) }
                .onSuccess {
                    selectedChapterId.value = null
                    message.value = "已删除第${chapter.number}章"
                }
                .onFailure { message.value = it.message ?: "删除章节失败" }
        }
    }

    fun deleteCurrentProject() {
        val project = selectedProject.value ?: return
        viewModelScope.launch {
            repository.deleteProject(project)
            selectedProjectId.value = null
            selectedChapterId.value = null
            message.value = "已删除《${project.title}》"
        }
    }

    fun saveChapterPlan(outline: String, targetWordCount: Int) {
        val chapter = selectedChapter.value ?: return
        savePlanJob?.cancel()
        savePlanJob = viewModelScope.launch {
            delay(500)
            repository.updateChapterPlan(chapter, outline, targetWordCount)
        }
    }

    fun saveBeatSheet(beatSheet: String) {
        val chapter = selectedChapter.value ?: return
        saveBeatSheetJob?.cancel()
        saveBeatSheetJob = viewModelScope.launch {
            delay(500)
            repository.updateChapterBeatSheet(chapter, beatSheet)
        }
    }

    fun saveStyleGuide(styleGuide: String) {
        val project = selectedProject.value ?: return
        saveStyleJob?.cancel()
        saveStyleJob = viewModelScope.launch {
            delay(500)
            repository.updateProjectStyle(project, styleGuide)
        }
    }

    fun saveProjectProfile(
        genre: String,
        premise: String,
        summary: String,
        tags: String,
        targetAudience: String,
        protagonistName: String,
    ) {
        val project = selectedProject.value ?: return
        viewModelScope.launch {
            repository.updateProjectProfile(project, genre, premise, summary, tags, targetAudience, protagonistName)
            message.value = "作品资料已保存"
        }
    }

    fun generateCover() {
        val project = selectedProject.value ?: return
        val request = beginGeneration(GenerationTask.COVER) ?: return
        message.value = "正在生成封面..."
        val prompt = buildString {
            append("Create a polished vertical Chinese web novel cover illustration. ")
            append("No text, no typography, no logo, no watermark. ")
            append("Title concept: ${project.title}. ")
            if (project.genre.isNotBlank()) append("Genre: ${project.genre}. ")
            if (project.summary.isNotBlank()) append("Synopsis: ${project.summary.take(600)}. ")
            if (project.tags.isNotBlank()) append("Tags: ${project.tags}. ")
            if (project.protagonistName.isNotBlank()) append("Protagonist: ${project.protagonistName}. ")
            append("Portrait 2:3 composition, a clear focal subject, cinematic lighting, detailed commercial illustration.")
        }
        generationJobs[GenerationTask.COVER] = viewModelScope.launch {
            try {
                modelClient.generateCover(modelConfig.value, prompt, request).fold(
                    onSuccess = { bytes ->
                        val folder = File(getApplication<Application>().filesDir, "covers").apply { mkdirs() }
                        val file = File(folder, "cover-${project.id}.png")
                        file.writeBytes(bytes)
                        repository.updateCover(project, file.absolutePath)
                        message.value = "封面已保存到本机书架"
                    },
                    onFailure = { message.value = it.message ?: "封面生成失败" },
                )
            } finally {
                finishGeneration(GenerationTask.COVER, request)
            }
        }
    }

    fun addStoryItem(kind: String, name: String, detail: String, status: String) {
        val projectId = selectedProjectId.value ?: return
        if (name.isBlank()) {
            message.value = "请填写资料名称"
            return
        }
        viewModelScope.launch {
            repository.addStoryItem(projectId, kind, name, detail, status)
            message.value = "已保存" + kind
        }
    }

    fun updateStoryItem(item: StoryItem, kind: String, name: String, detail: String, status: String) {
        if (name.isBlank()) {
            message.value = "请填写资料名称"
            return
        }
        viewModelScope.launch {
            repository.updateStoryItem(item, kind, name, detail, status)
            message.value = "已更新${item.name}"
        }
    }

    fun addAnchor(
        startChapter: Int,
        endChapter: Int,
        title: String,
        coreConflict: String,
        allowedPlot: String,
        forbiddenReveals: String,
        mandatoryTension: String,
    ) {
        val projectId = selectedProjectId.value ?: return
        if (title.isBlank() || coreConflict.isBlank()) {
            message.value = "请填写锚点标题与核心冲突"
            return
        }
        viewModelScope.launch {
            repository.addAnchor(projectId, startChapter, endChapter, title, coreConflict, allowedPlot, forbiddenReveals, mandatoryTension)
            message.value = "大纲锚点已保存"
        }
    }

    fun addEdge(sourceItemId: Long, targetItemId: Long, relation: String, description: String, sinceChapter: Int) {
        val projectId = selectedProjectId.value ?: return
        if (relation.isBlank()) {
            message.value = "请填写关系类型"
            return
        }
        viewModelScope.launch {
            runCatching { repository.addEdge(projectId, sourceItemId, targetItemId, relation, description, sinceChapter) }
                .onSuccess { message.value = "关系已加入知识图谱" }
                .onFailure { message.value = it.message ?: "关系保存失败" }
        }
    }

    fun extractMemoryFromCurrentChapter() {
        val chapter = selectedChapter.value ?: return
        val content = pendingChapterContent[chapter.id] ?: chapter.content
        if (content.isBlank()) {
            message.value = "本章还没有正文，无法提取记忆"
            return
        }
        val request = beginGeneration(GenerationTask.MEMORY_EXTRACTION) ?: return
        message.value = "正在从本章提取知识图谱..."
        val extractionInput = "第${chapter.number}章 ${chapter.title}\n$content"
        generationJobs[GenerationTask.MEMORY_EXTRACTION] = viewModelScope.launch {
            try {
                val result = modelClient.extractStoryMemory(modelConfig.value, extractionInput, request)
                if (!isActive) return@launch
                result.fold(
                    onSuccess = { raw ->
                        runCatching { MemoryExtractionParser.parse(raw) }.onSuccess { extraction ->
                            val knownByName = storyItems.value.associateBy { it.name }.toMutableMap()
                            var addedItems = 0
                            extraction.items.distinctBy { it.name }.forEach { candidate ->
                                val existing = knownByName[candidate.name]
                                if (existing == null) {
                                    val id = repository.addStoryItem(
                                        chapter.projectId,
                                        candidate.kind,
                                        candidate.name,
                                        candidate.detail,
                                        candidate.status,
                                    )
                                    knownByName[candidate.name] = StoryItem(
                                        id = id,
                                        projectId = chapter.projectId,
                                        kind = candidate.kind,
                                        name = candidate.name,
                                        detail = candidate.detail,
                                        status = candidate.status,
                                    )
                                    addedItems += 1
                                } else if (candidate.detail.isNotBlank() && (existing.detail != candidate.detail || existing.status != candidate.status)) {
                                    repository.updateStoryItem(existing, candidate.kind, candidate.name, candidate.detail, candidate.status)
                                }
                            }
                            val existingEdges = edges.value.map { Triple(it.sourceItemId, it.targetItemId, it.relation) }.toMutableSet()
                            var addedEdges = 0
                            extraction.edges.forEach { candidate ->
                                val source = knownByName[candidate.sourceName] ?: return@forEach
                                val target = knownByName[candidate.targetName] ?: return@forEach
                                val key = Triple(source.id, target.id, candidate.relation)
                                if (source.id != target.id && existingEdges.add(key)) {
                                    repository.addEdge(chapter.projectId, source.id, target.id, candidate.relation, candidate.description, chapter.number)
                                    addedEdges += 1
                                }
                            }
                            message.value = "记忆已更新：新增 $addedItems 条资料、$addedEdges 条关系"
                        }.onFailure {
                            message.value = "模型返回的图谱格式无效，请重试"
                        }
                    },
                    onFailure = { message.value = it.message ?: "提取记忆失败" },
                )
            } finally {
                finishGeneration(GenerationTask.MEMORY_EXTRACTION, request)
            }
        }
    }

    fun generateRepairPlan() {
        val project = selectedProject.value ?: return
        val chapter = selectedChapter.value ?: return
        val issues = QualityGate.inspect(chapter, storyItems.value, anchors.value)
        val request = beginGeneration(GenerationTask.REPAIR_PLAN) ?: return
        message.value = "正在生成最短修复计划..."
        val context = buildString {
            append(ContextEngine.build(project, chapter, chapters.value, storyItems.value, anchors.value).prompt)
            appendLine("\n门禁问题：")
            issues.forEach { appendLine("- ${it.title}：${it.detail}") }
        }
        generationJobs[GenerationTask.REPAIR_PLAN] = viewModelScope.launch {
            try {
                val result = modelClient.generateRepairPlan(modelConfig.value, context, request)
                if (!isActive) return@launch
                result.fold(
                    onSuccess = { plan ->
                        repairPlan.value = plan
                        message.value = "修复计划已生成，正文未自动修改"
                    },
                    onFailure = { message.value = it.message ?: "生成修复计划失败" },
                )
            } finally {
                finishGeneration(GenerationTask.REPAIR_PLAN, request)
            }
        }
    }

    fun generateChapterPlan() {
        val project = selectedProject.value ?: return
        val chapter = selectedChapter.value ?: return
        val request = beginGeneration(GenerationTask.CHAPTER_PLAN) ?: return
        message.value = "正在根据本地记忆生成本章计划..."
        val context = ContextEngine.build(project, chapter, chapters.value, storyItems.value, anchors.value).prompt +
            "\n请为当前章节生成大纲，覆盖冲突、转折和结尾钩子。"
        generationJobs[GenerationTask.CHAPTER_PLAN] = viewModelScope.launch {
            try {
                val result = modelClient.generateChapterPlan(modelConfig.value, context, request)
                if (!isActive) return@launch
                result.fold(
                    onSuccess = { outline ->
                        repository.updateChapterPlan(chapter, outline, chapter.targetWordCount)
                        message.value = "本章计划已保存，可继续手动调整"
                    },
                    onFailure = { message.value = it.message ?: "生成大纲失败" },
                )
            } finally {
                finishGeneration(GenerationTask.CHAPTER_PLAN, request)
            }
        }
    }

    fun generateBeatSheet() {
        val project = selectedProject.value ?: return
        val chapter = selectedChapter.value ?: return
        val request = beginGeneration(GenerationTask.BEAT_SHEET) ?: return
        message.value = "正在生成本章分镜..."
        val context = ContextEngine.build(project, chapter, chapters.value, storyItems.value, anchors.value).prompt +
            "\n请把本章计划拆成按顺序执行的场景分镜。"
        generationJobs[GenerationTask.BEAT_SHEET] = viewModelScope.launch {
            try {
                val result = modelClient.generateBeatSheet(modelConfig.value, context, request)
                if (!isActive) return@launch
                result.fold(
                    onSuccess = { beatSheet ->
                        repository.updateChapterBeatSheet(chapter, beatSheet)
                        message.value = "本章分镜已保存，可继续编辑"
                    },
                    onFailure = { message.value = it.message ?: "生成分镜失败" },
                )
            } finally {
                finishGeneration(GenerationTask.BEAT_SHEET, request)
            }
        }
    }

    fun extractStyleGuideFromCurrentChapter() {
        val project = selectedProject.value ?: return
        val chapter = selectedChapter.value ?: return
        val sample = pendingChapterContent[chapter.id] ?: chapter.content
        if (sample.length < 200) {
            message.value = "样章至少需要 200 字才能提取文风"
            return
        }
        val request = beginGeneration(GenerationTask.STYLE_GUIDE) ?: return
        message.value = "正在提取项目文风..."
        generationJobs[GenerationTask.STYLE_GUIDE] = viewModelScope.launch {
            try {
                val result = modelClient.extractStyleGuide(modelConfig.value, sample.take(8_000), request)
                if (!isActive) return@launch
                result.fold(
                    onSuccess = { styleGuide ->
                        repository.updateProjectStyle(project, styleGuide)
                        message.value = "项目文风档案已保存，可继续调整"
                    },
                    onFailure = { message.value = it.message ?: "提取文风失败" },
                )
            } finally {
                finishGeneration(GenerationTask.STYLE_GUIDE, request)
            }
        }
    }

    fun saveModelConfig(config: ModelConfig) {
        modelPreferences.save(config)
        modelConfig.value = config
        message.value = "模型配置已加密保存到本机"
    }

    fun testModelConfig(config: ModelConfig) {
        viewModelScope.launch {
            message.value = "正在测试连接..."
            modelClient.test(config).fold(
                onSuccess = { message.value = it },
                onFailure = { message.value = it.message ?: "连接失败" },
            )
        }
    }

    fun generateContinuation() {
        val project = selectedProject.value ?: return
        val chapter = selectedChapter.value ?: return
        if (hasPendingCascade()) { message.value = "改纲待审项尚未复核，暂不能继续写作"; return }
        val sourceContent = pendingChapterContent[chapter.id] ?: chapter.content
        val sourceChapter = chapter.copy(content = sourceContent)
        val request = beginGeneration(GenerationTask.CONTINUATION) ?: return
        message.value = "正在直接请求你的模型..."
        val writingContext = ContextEngine.build(project, sourceChapter, chapters.value, storyItems.value, anchors.value).prompt
        generationJobs[GenerationTask.CONTINUATION] = viewModelScope.launch {
            try {
                val result = modelClient.continueWriting(modelConfig.value, writingContext, request)
                if (!isActive) return@launch
                result.fold(
                    onSuccess = { generated ->
                        // Preserve text typed while the model was generating; AI output only appends.
                        saveChapterJobs.remove(chapter.id)?.cancel()
                        val currentChapter = chapters.value.firstOrNull { it.id == chapter.id } ?: chapter
                        val currentContent = pendingChapterContent.remove(chapter.id) ?: currentChapter.content
                        repository.updateChapter(currentChapter, currentContent.trimEnd() + "\n\n" + generated)
                        message.value = "已续写并保存到本机"
                    },
                    onFailure = { message.value = it.message ?: "续写失败" },
                )
            } finally {
                finishGeneration(GenerationTask.CONTINUATION, request)
            }
        }
    }

    fun autoWriteChapters(count: Int) {
        val project = selectedProject.value ?: return
        if (hasPendingCascade()) { message.value = "改纲待审项尚未复核，暂不能批量写作"; return }
        val total = count.coerceIn(1, 5)
        val request = beginGeneration(GenerationTask.AUTO_WRITE) ?: return
        message.value = "自动写作已启动：准备生成 $total 章"
        generationJobs[GenerationTask.AUTO_WRITE] = viewModelScope.launch {
            try {
                val workingChapters = chapters.value.toMutableList()
                repeat(total) { index ->
                    if (!isActive) return@launch
                    val nextNumber = (workingChapters.maxOfOrNull { it.number } ?: 0) + 1
                    val target = Chapter(projectId = project.id, number = nextNumber, title = "第${nextNumber}章")
                    val context = ContextEngine.build(project, target, workingChapters, storyItems.value, anchors.value, edges.value).prompt +
                        "\n这是批量写作的第${index + 1}/${total} 章。请完整写出本章。"
                    val result = modelClient.writeFullChapter(modelConfig.value, context, request)
                    if (!isActive) return@launch
                    val body = result.getOrElse {
                        message.value = "自动写作在第${nextNumber}章停止：${it.message ?: "模型请求失败"}"
                        return@launch
                    }
                    val generatedDraft = target.copy(content = body)
                    val warnings = QualityGate.inspect(generatedDraft, storyItems.value, anchors.value)
                        .filter { it.severity == QualitySeverity.WARNING }
                    val completed = repository.addCompletedChapter(
                        project.id,
                        body,
                        if (warnings.isEmpty()) ChapterQualityStatus.READY else ChapterQualityStatus.NEEDS_REPAIR,
                        warnings.joinToString("；") { it.title },
                    )
                    workingChapters += completed
                    selectedChapterId.value = completed.id
                    if (warnings.isNotEmpty()) {
                        message.value = "第${completed.number}章已保存为待修复草稿，门禁提示 ${warnings.size} 项，自动写作已停止"
                        return@launch
                    }
                    message.value = "已完成第${completed.number}章（${index + 1}/${total}）"
                }
                message.value = "批量写作完成，共生成 ${total} 章"
            } finally {
                finishGeneration(GenerationTask.AUTO_WRITE, request)
            }
        }
    }

    fun reviseOutline(fromChapter: Int, description: String) {
        val project = selectedProject.value ?: return
        outlineCascadePending.value = true
        val report = OutlineCascadeAnalyzer.analyze(fromChapter, chapters.value, storyItems.value, anchors.value, edges.value, description)
        viewModelScope.launch {
            repository.applyOutlineCascade(project, report, storyItems.value, anchors.value, edges.value)
            message.value = report.summary + " 已标记为待审，请在资料和大纲页逐项确认。"
        }
    }

    fun resolveOutlineCascade() {
        val project = selectedProject.value ?: return
        viewModelScope.launch {
            repository.resolveOutlineCascade(project, storyItems.value, anchors.value, edges.value)
            outlineCascadePending.value = false
            message.value = "已确认全部改纲待审项，可恢复 AI 写作"
        }
    }

    private fun hasPendingCascade(): Boolean = outlineCascadePending.value || selectedProject.value?.outlineRevisionReport?.isNotBlank() == true || storyItems.value.any { it.cascadePending } || anchors.value.any { it.cascadePending } || edges.value.any { it.cascadePending }

    fun markCurrentChapterQualityRepaired() {
        val chapter = selectedChapter.value ?: return
        viewModelScope.launch {
            repository.markChapterQualityRepaired(chapter)
            message.value = "已标记本章门禁问题为已处理"
        }
    }

    fun cancelGeneration(task: GenerationTask) {
        if (task !in generationTasks.value) return
        generationRequests[task]?.cancel()
        generationJobs[task]?.cancel()
        finishGeneration(task, generationRequests[task] ?: return)
        message.value = "已取消${task.label}，其他任务继续运行"
    }

    fun clearMessage() {
        message.value = null
    }
}
