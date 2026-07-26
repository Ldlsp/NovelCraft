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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

private data class WritingContextInput(
    val project: NovelProject?,
    val chapter: Chapter?,
    val chapters: List<Chapter>,
    val items: List<StoryItem>,
    val anchors: List<StoryAnchor>,
)

data class ProjectProfileSuggestion(
    val genre: String,
    val premise: String,
    val summary: String,
    val tags: String,
    val targetAudience: String,
    val protagonistName: String,
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
    private var autoWritePreparing = false
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

    val chapterMentions = selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.chapterMentions(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val researchNotes = selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.researchNotes(id)
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

    val latestRevision = selectedChapter.flatMapLatest { chapter ->
        if (chapter == null) flowOf(null) else repository.latestRevision(chapter.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val latestEditorialReview = selectedChapter.flatMapLatest { chapter ->
        if (chapter == null) flowOf(null) else repository.latestEditorialReview(chapter.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val resumableAutoWriteRun = selectedProjectId.flatMapLatest { projectId ->
        if (projectId == null) flowOf(null) else repository.resumableAutoWriteRun(projectId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val writingContextInput = combine(selectedProject, selectedChapter, chapters, storyItems, anchors) { project, chapter, allChapters, items, projectAnchors ->
        WritingContextInput(project, chapter, allChapters, items, projectAnchors)
    }

    val contextPacket = combine(writingContextInput, edges, chapterMentions, researchNotes) { input, graphEdges, mentions, notes ->
        if (input.project == null || input.chapter == null) ContextPacket() else ContextEngine.build(input.project, input.chapter, input.chapters, input.items, input.anchors, graphEdges, mentions, notes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContextPacket())

    val qualityIssues = combine(selectedProject, selectedChapter, storyItems, anchors) { project, chapter, items, projectAnchors ->
        QualityGate.inspect(chapter, items, projectAnchors, project)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val modelConfig = MutableStateFlow(modelPreferences.load())
    val message = MutableStateFlow<String?>(null)
    val generationTasks = MutableStateFlow<Set<GenerationTask>>(emptySet())
    val repairPlan = MutableStateFlow<String?>(null)
    val projectProfileSuggestion = MutableStateFlow<ProjectProfileSuggestion?>(null)
    private val outlineCascadePending = MutableStateFlow(false)

    init {
        viewModelScope.launch { repository.recoverInterruptedWritingState() }
    }

    private fun beginGeneration(task: GenerationTask): GenerationRequest? {
        val activeTask = generationTasks.value.firstOrNull()
        if (activeTask != null) {
            message.value = "正在执行${activeTask.label}，请等待完成或先取消"
            return null
        }
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
        projectProfileSuggestion.value = null
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
        val itemsToExport = storyItems.value
        val edgesToExport = edges.value
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
                    appendLine("## 知识图谱")
                    appendLine()
                    appendLine("```mermaid")
                    appendLine(StoryGraphExport.asMermaid(itemsToExport, edgesToExport))
                    appendLine("```")
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
        title: String,
        genre: String,
        premise: String,
        summary: String,
        tags: String,
        targetAudience: String,
        protagonistName: String,
    ) {
        val project = selectedProject.value ?: return
        viewModelScope.launch {
            repository.updateProjectProfile(project, title, genre, premise, summary, tags, targetAudience, protagonistName)
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

    fun importCover(uri: Uri) {
        val project = selectedProject.value ?: return
        viewModelScope.launch {
            runCatching {
                val resolver = getApplication<Application>().contentResolver
                val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) }
                    ?: error("无法读取选择的图片")
                require(options.outWidth > 0 && options.outHeight > 0) { "请选择有效的图片文件" }
                val extension = when (resolver.getType(uri)) {
                    "image/jpeg" -> "jpg"
                    "image/webp" -> "webp"
                    else -> "png"
                }
                val folder = File(getApplication<Application>().filesDir, "covers").apply { mkdirs() }
                val destination = File(folder, "cover-${project.id}.$extension")
                resolver.openInputStream(uri)?.use { input ->
                    destination.outputStream().use(input::copyTo)
                } ?: error("无法复制选择的图片")
                repository.updateCover(project, destination.absolutePath)
            }.onSuccess {
                message.value = "已使用本地图片作为书架封面"
            }.onFailure {
                message.value = it.message ?: "上传封面失败"
            }
        }
    }

    fun testImageModelConfig(config: ModelConfig) {
        viewModelScope.launch {
            message.value = "正在测试封面 AI..."
            modelClient.testImage(config).fold(
                onSuccess = { message.value = it },
                onFailure = { message.value = it.message ?: "封面 AI 测试失败" },
            )
        }
    }

    fun generateProjectProfile() {
        val project = selectedProject.value ?: return
        val request = beginGeneration(GenerationTask.PROJECT_PROFILE) ?: return
        message.value = "正在补全作品设定..."
        val context = buildString {
            appendLine("书名：${project.title}")
            if (project.genre.isNotBlank()) appendLine("已有题材：${project.genre}")
            if (project.premise.isNotBlank()) appendLine("已有一句话设定：${project.premise}")
            if (project.summary.isNotBlank()) appendLine("已有简介：${project.summary}")
            if (project.tags.isNotBlank()) appendLine("已有标签：${project.tags}")
            if (project.targetAudience.isNotBlank()) appendLine("已有目标读者：${project.targetAudience}")
            if (project.protagonistName.isNotBlank()) appendLine("已有主角名：${project.protagonistName}")
        }
        generationJobs[GenerationTask.PROJECT_PROFILE] = viewModelScope.launch {
            try {
                modelClient.generateProjectProfile(modelConfig.value, context, request).fold(
                    onSuccess = { raw ->
                        val json = org.json.JSONObject(raw.trim().removePrefix("```json").removeSuffix("```").trim())
                        projectProfileSuggestion.value = ProjectProfileSuggestion(
                            genre = json.optString("genre").trim(),
                            premise = json.optString("premise").trim(),
                            summary = json.optString("summary").trim(),
                            tags = json.optString("tags").trim(),
                            targetAudience = json.optString("targetAudience").trim(),
                            protagonistName = json.optString("protagonistName").trim(),
                        )
                        message.value = "作品设定已生成，请确认后保存"
                    },
                    onFailure = { message.value = it.message ?: "生成作品设定失败" },
                )
            } catch (_: Exception) {
                message.value = "AI 返回的作品设定格式无效，请重试"
            } finally {
                finishGeneration(GenerationTask.PROJECT_PROFILE, request)
            }
        }
    }

    fun saveLongFormBlueprint(blueprint: String) {
        val project = selectedProject.value ?: return
        viewModelScope.launch { repository.updateLongFormBlueprint(project, blueprint) }
    }

    fun savePacing(targetChapters: Int, targetWords: Int, profile: String) {
        val project = selectedProject.value ?: return
        viewModelScope.launch { repository.updatePacing(project, targetChapters, targetWords, profile) }
    }

    fun addResearchNote(title: String, sourceUrl: String, tags: String, content: String, rightsConfirmed: Boolean) {
        val projectId = selectedProjectId.value ?: return
        if (title.isBlank() || content.isBlank()) {
            message.value = "请填写调研标题和内容"
            return
        }
        viewModelScope.launch {
            repository.addResearchNote(projectId, title, sourceUrl, tags, content, rightsConfirmed)
            message.value = "调研笔记已保存到本机"
        }
    }

    fun updateResearchNote(note: ResearchNote, title: String, sourceUrl: String, tags: String, content: String, rightsConfirmed: Boolean) {
        if (title.isBlank() || content.isBlank()) {
            message.value = "请填写调研标题和内容"
            return
        }
        viewModelScope.launch { repository.updateResearchNote(note, title, sourceUrl, tags, content, rightsConfirmed) }
    }

    fun deleteResearchNote(note: ResearchNote) {
        viewModelScope.launch { repository.deleteResearchNote(note.id) }
    }

    fun analyzeReferenceStructure(note: ResearchNote) {
        if (!note.rightsConfirmed) {
            message.value = "请先确认笔记为自写摘要或你拥有处理授权，不能分析受保护正文"
            return
        }
        if (note.content.length > 4_000) {
            message.value = "参考分析仅接受不超过 4000 字的自写摘要，不接受作品正文"
            return
        }
        val request = beginGeneration(GenerationTask.REFERENCE_ANALYSIS) ?: return
        message.value = "正在提炼参考结构..."
        val context = "标题：${note.title}\n标签：${note.tags}\n来源：${note.sourceUrl}\n作者摘要：\n${note.content}"
        generationJobs[GenerationTask.REFERENCE_ANALYSIS] = viewModelScope.launch {
            try {
                modelClient.analyzeReferenceStructure(modelConfig.value, context, request).fold(
                    onSuccess = { analysis ->
                        repository.appendResearchAnalysis(note.id, analysis)
                        message.value = "结构提炼已附加到调研笔记"
                    },
                    onFailure = { message.value = it.message ?: "结构提炼失败" },
                )
            } finally {
                finishGeneration(GenerationTask.REFERENCE_ANALYSIS, request)
            }
        }
    }

    fun generateEditorialReview() {
        val project = selectedProject.value ?: return
        val chapter = selectedChapter.value ?: return
        if (chapter.content.isBlank()) {
            message.value = "本章还没有正文，无法审稿"
            return
        }
        val reviewerValues = listOf(modelConfig.value.reviewerBaseUrl, modelConfig.value.reviewerApiKey, modelConfig.value.reviewerModel)
        if (reviewerValues.any(String::isBlank) && reviewerValues.any(String::isNotBlank)) {
            message.value = "独立审稿模型需同时填写 Base URL、API Key 和模型名称，或全部留空"
            return
        }
        val request = beginGeneration(GenerationTask.EDITORIAL_REVIEW) ?: return
        message.value = "正在进行编辑审稿..."
        val context = ContextEngine.build(project, chapter, chapters.value, storyItems.value, anchors.value, edges.value, chapterMentions.value, researchNotes.value).prompt + "\n完整正文：\n${chapter.content}"
        generationJobs[GenerationTask.EDITORIAL_REVIEW] = viewModelScope.launch {
            try {
                modelClient.editorialReview(reviewModelConfig(), context, request).fold(
                    onSuccess = { review ->
                        repository.addEditorialReview(project.id, chapter.id, review)
                        message.value = "编辑审稿已保存"
                    },
                    onFailure = { message.value = it.message ?: "编辑审稿失败" },
                )
            } finally {
                finishGeneration(GenerationTask.EDITORIAL_REVIEW, request)
            }
        }
    }

    private fun reviewModelConfig(): ModelConfig = modelConfig.value.let { config ->
        if (config.reviewerBaseUrl.isBlank() || config.reviewerApiKey.isBlank() || config.reviewerModel.isBlank()) config
        else config.copy(baseUrl = config.reviewerBaseUrl, apiKey = config.reviewerApiKey, model = config.reviewerModel)
    }

    fun generateLongFormBlueprint() {
        val project = selectedProject.value ?: return
        val request = beginGeneration(GenerationTask.LONG_FORM_BLUEPRINT) ?: return
        message.value = "正在生成长篇路线图..."
        val context = buildString {
            appendLine("书名：${project.title}")
            appendLine("题材：${project.genre}")
            appendLine("核心设定：${project.premise}")
            if (project.summary.isNotBlank()) appendLine("简介：${project.summary}")
            if (project.tags.isNotBlank()) appendLine("标签：${project.tags}")
            if (project.targetAudience.isNotBlank()) appendLine("读者：${project.targetAudience}")
            if (project.protagonistName.isNotBlank()) appendLine("主角：${project.protagonistName}")
            if (project.longFormBlueprint.isNotBlank()) appendLine("现有路线图：${project.longFormBlueprint}")
        }
        generationJobs[GenerationTask.LONG_FORM_BLUEPRINT] = viewModelScope.launch {
            try {
                modelClient.generateLongFormBlueprint(modelConfig.value, context, request).fold(
                    onSuccess = { blueprint ->
                        repository.updateLongFormBlueprint(project, blueprint)
                        message.value = "长篇路线图已生成，可在作品页继续编辑"
                    },
                    onFailure = { message.value = it.message ?: "生成长篇路线图失败" },
                )
            } finally {
                finishGeneration(GenerationTask.LONG_FORM_BLUEPRINT, request)
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

    private suspend fun applyMemoryExtraction(chapter: Chapter, extraction: MemoryExtraction): String {
        val knownByName = storyItems.value.associateBy { it.name }.toMutableMap()
        val mentionedItemIds = mutableSetOf<Long>()
        var addedItems = 0
        extraction.items.distinctBy { it.name }.forEach { candidate ->
            val existing = knownByName[candidate.name]
            if (existing == null) {
                val id = repository.addStoryItem(chapter.projectId, candidate.kind, candidate.name, candidate.detail, candidate.status)
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
            knownByName[candidate.name]?.let { mentionedItemIds += it.id }
        }
        val knownEdges = edges.value.map { Triple(it.sourceItemId, it.targetItemId, it.relation) }.toMutableSet()
        var addedEdges = 0
        extraction.edges.forEach { candidate ->
            val source = knownByName[candidate.sourceName] ?: return@forEach
            val target = knownByName[candidate.targetName] ?: return@forEach
            mentionedItemIds += source.id
            mentionedItemIds += target.id
            if (source.id != target.id && knownEdges.add(Triple(source.id, target.id, candidate.relation))) {
                repository.addEdge(chapter.projectId, source.id, target.id, candidate.relation, candidate.description, chapter.number)
                addedEdges += 1
            }
        }
        repository.replaceChapterMentions(chapter, mentionedItemIds)
        return "记忆已更新：新增 $addedItems 条资料、$addedEdges 条关系、${mentionedItemIds.size} 条章节引用"
    }

    private suspend fun runChapterLifecycle(chapter: Chapter, request: GenerationRequest): ChapterLifecycleResult {
        repository.updateChapterLifecycle(
            chapter = chapter,
            lifecycleStatus = ChapterLifecycleStatus.PROCESSING,
            lifecycleDetail = "正在提取本章已发生的角色、事件、伏笔和关系",
        )
        val input = "第${chapter.number}章 ${chapter.title}\n${chapter.content}"
        return modelClient.extractStoryMemory(modelConfig.value, input, request).fold(
            onSuccess = { raw ->
                if (!repository.hasUnchangedContent(chapter)) {
                    repository.updateChapterLifecycle(
                        chapter = chapter,
                        lifecycleStatus = ChapterLifecycleStatus.MEMORY_FAILED,
                        lifecycleDetail = "正文已在处理期间修改，请重新运行章节闭环",
                    )
                    return@fold ChapterLifecycleResult(false, "正文已修改，请重新运行章节闭环")
                }
                runCatching { MemoryExtractionParser.parse(raw) }.fold(
                    onSuccess = { extraction ->
                        val memoryMessage = applyMemoryExtraction(chapter, extraction)
                        val warnings = QualityGate.inspect(chapter, storyItems.value, anchors.value, selectedProject.value)
                            .filter { it.severity == QualitySeverity.WARNING }
                        if (warnings.isEmpty()) {
                            repository.updateChapterLifecycle(
                                chapter = chapter,
                                lifecycleStatus = ChapterLifecycleStatus.PASSED,
                                lifecycleDetail = memoryMessage,
                                memoryUpdatedAt = System.currentTimeMillis(),
                            )
                            ChapterLifecycleResult(true, "$memoryMessage；门禁已通过")
                        } else {
                            val summary = warnings.joinToString("；") { it.title }
                            repository.updateChapterLifecycle(
                                chapter = chapter,
                                lifecycleStatus = ChapterLifecycleStatus.WAITING_REVIEW,
                                lifecycleDetail = "$memoryMessage；请在审阅页处理后确认",
                                qualityStatus = ChapterQualityStatus.NEEDS_REPAIR,
                                qualityIssueSummary = summary,
                                memoryUpdatedAt = System.currentTimeMillis(),
                            )
                            ChapterLifecycleResult(false, "门禁发现 ${warnings.size} 项风险：$summary")
                        }
                    },
                    onFailure = {
                        repository.updateChapterLifecycle(
                            chapter = chapter,
                            lifecycleStatus = ChapterLifecycleStatus.MEMORY_FAILED,
                            lifecycleDetail = "记忆解析失败：${it.message ?: "模型返回格式无效"}",
                        )
                        ChapterLifecycleResult(false, "记忆同步失败，可在审阅页重试")
                    },
                )
            },
            onFailure = {
                repository.updateChapterLifecycle(
                    chapter = chapter,
                    lifecycleStatus = ChapterLifecycleStatus.MEMORY_FAILED,
                    lifecycleDetail = "记忆同步失败：${it.message ?: "模型请求失败"}",
                )
                ChapterLifecycleResult(false, "记忆同步失败，可在审阅页重试")
            },
        )
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
        val issues = QualityGate.inspect(chapter, storyItems.value, anchors.value, project)
        val request = beginGeneration(GenerationTask.REPAIR_PLAN) ?: return
        message.value = "正在生成最短修复计划..."
        val context = buildString {
            append(ContextEngine.build(project, chapter, chapters.value, storyItems.value, anchors.value, researchNotes = researchNotes.value).prompt)
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
        val context = ContextEngine.build(project, chapter, chapters.value, storyItems.value, anchors.value, researchNotes = researchNotes.value).prompt +
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
        val context = ContextEngine.build(project, chapter, chapters.value, storyItems.value, anchors.value, researchNotes = researchNotes.value).prompt +
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
        blockingLifecycleChapter()?.let {
            message.value = "第${it.number}章尚未完成写作闭环，请先到审阅页处理"
            return
        }
        if (hasPendingCascade()) { message.value = "改纲待审项尚未复核，暂不能继续写作"; return }
        val sourceContent = pendingChapterContent[chapter.id] ?: chapter.content
        val sourceChapter = chapter.copy(content = sourceContent)
        val request = beginGeneration(GenerationTask.CONTINUATION) ?: return
        message.value = "正在直接请求你的模型..."
        val writingContext = ContextEngine.build(project, sourceChapter, chapters.value, storyItems.value, anchors.value, researchNotes = researchNotes.value).prompt
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
                        val updatedChapter = currentChapter.copy(content = currentContent.trimEnd() + "\n\n" + generated)
                        repository.updateChapter(currentChapter, updatedChapter.content)
                        val lifecycle = runChapterLifecycle(updatedChapter, request)
                        message.value = if (lifecycle.passed) "已续写并完成闭环：${lifecycle.message}" else "已保存续写草稿：${lifecycle.message}"
                    },
                    onFailure = { message.value = it.message ?: "续写失败" },
                )
            } finally {
                finishGeneration(GenerationTask.CONTINUATION, request)
            }
        }
    }

    private fun startAutoWrite(project: NovelProject, run: AutoWriteRun) {
        blockingLifecycleChapter()?.let {
            message.value = "第${it.number}章尚未完成写作闭环，不能开始批量写作"
            return
        }
        if (hasPendingCascade()) {
            message.value = "改纲待审项尚未复核，暂不能批量写作"
            return
        }
        val request = beginGeneration(GenerationTask.AUTO_WRITE) ?: run {
            viewModelScope.launch {
                repository.updateAutoWriteRun(run, run.completedCount, AutoWriteRunStatus.PAUSED, "等待其他 AI 任务结束")
            }
            return
        }
        generationJobs[GenerationTask.AUTO_WRITE] = viewModelScope.launch {
            var currentRun = repository.updateAutoWriteRun(run, run.completedCount, AutoWriteRunStatus.RUNNING, "正在准备第${run.completedCount + 1}章")
            try {
                val workingChapters = chapters.value.toMutableList()
                repeat(currentRun.requestedCount - currentRun.completedCount) {
                    val nextNumber = (workingChapters.maxOfOrNull { it.number } ?: 0) + 1
                    val target = Chapter(projectId = project.id, number = nextNumber, title = "第${nextNumber}章")
                    val context = ContextEngine.build(project, target, workingChapters, storyItems.value, anchors.value, edges.value, researchNotes = researchNotes.value).prompt +
                        "\n这是可恢复批量写作的第${currentRun.completedCount + 1}/${currentRun.requestedCount}章。请完整写出本章。"
                    val body = modelClient.writeFullChapter(modelConfig.value, context, request).getOrElse {
                        currentRun = repository.updateAutoWriteRun(currentRun, currentRun.completedCount, AutoWriteRunStatus.PAUSED, it.message ?: "模型请求失败")
                        message.value = "批量写作已暂停：${currentRun.detail}"
                        return@launch
                    }
                    val completed = repository.addGeneratedDraftChapter(project.id, body, currentRun.id)
                    workingChapters += completed
                    selectedChapterId.value = completed.id
                    val lifecycle = runChapterLifecycle(completed, request)
                    if (!lifecycle.passed) {
                        currentRun = repository.updateAutoWriteRun(currentRun, currentRun.completedCount + 1, AutoWriteRunStatus.PAUSED, "第${completed.number}章：${lifecycle.message}")
                        message.value = "第${completed.number}章待处理，批量写作已暂停"
                        return@launch
                    }
                    currentRun = repository.updateAutoWriteRun(
                        currentRun,
                        currentRun.completedCount + 1,
                        AutoWriteRunStatus.RUNNING,
                        "第${completed.number}章已通过写作闭环",
                    )
                    message.value = "第${completed.number}章已通过写作闭环（${currentRun.completedCount}/${currentRun.requestedCount}）"
                }
                repository.updateAutoWriteRun(currentRun, currentRun.requestedCount, AutoWriteRunStatus.COMPLETED, "全部章节已通过写作闭环")
                message.value = "批量写作完成，共生成 ${currentRun.requestedCount} 章"
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable) {
                    repository.updateAutoWriteRun(currentRun, currentRun.completedCount, AutoWriteRunStatus.PAUSED, "作者已暂停，可在处理当前章节后继续")
                }
                throw cancelled
            } finally {
                finishGeneration(GenerationTask.AUTO_WRITE, request)
            }
        }
    }

    fun resumeAutoWrite() {
        val project = selectedProject.value ?: return
        val run = resumableAutoWriteRun.value ?: run {
            message.value = "没有可继续的批量写作计划"
            return
        }
        startAutoWrite(project, run)
    }

    fun autoWriteChapters(count: Int) {
        val project = selectedProject.value ?: return
        blockingLifecycleChapter()?.let {
            message.value = "第${it.number}章尚未完成写作闭环，不能开始批量写作"
            return
        }
        if (hasPendingCascade()) {
            message.value = "改纲待审项尚未复核，暂不能批量写作"
            return
        }
        if (autoWritePreparing) {
            message.value = "正在创建批量写作计划"
            return
        }
        val total = count.coerceIn(1, 5)
        autoWritePreparing = true
        viewModelScope.launch {
            try {
                val run = repository.createAutoWriteRun(project.id, total)
                startAutoWrite(project, run)
            } finally {
                autoWritePreparing = false
            }
        }
    }

    fun reviseOutline(fromChapter: Int, description: String) {
        val project = selectedProject.value ?: return
        outlineCascadePending.value = true
        generationTasks.value.toList().forEach(::cancelGeneration)
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

    private fun blockingLifecycleChapter(): Chapter? = chapters.value
        .sortedByDescending { it.number }
        .firstOrNull { ChapterLifecycleStatus.blocksAutomaticWriting(it.lifecycleStatus) }

    fun markCurrentChapterQualityRepaired() {
        retryCurrentChapterLifecycle()
    }

    fun retryCurrentChapterLifecycle() {
        val chapter = selectedChapter.value ?: return
        if (chapter.content.isBlank()) {
            message.value = "本章还没有正文，无法运行写作闭环"
            return
        }
        val request = beginGeneration(GenerationTask.CHAPTER_LIFECYCLE) ?: return
        message.value = "正在重试章节记忆与质量门禁..."
        generationJobs[GenerationTask.CHAPTER_LIFECYCLE] = viewModelScope.launch {
            try {
                val result = runChapterLifecycle(chapter, request)
                message.value = if (result.passed) "章节闭环完成：${result.message}" else result.message
            } finally {
                finishGeneration(GenerationTask.CHAPTER_LIFECYCLE, request)
            }
        }
    }

    private fun rewriteCurrentChapter(task: GenerationTask, humanize: Boolean) {
        val project = selectedProject.value ?: return
        val chapter = selectedChapter.value ?: return
        val content = pendingChapterContent[chapter.id] ?: chapter.content
        if (content.isBlank()) {
            message.value = "本章还没有正文，无法改写"
            return
        }
        val request = beginGeneration(task) ?: return
        message.value = if (humanize) "正在润色本章语言..." else "正在按门禁问题改写本章..."
        generationJobs[task] = viewModelScope.launch {
            try {
                saveChapterJobs.remove(chapter.id)?.cancel()
                pendingChapterContent.remove(chapter.id)
                val source = chapter.copy(content = content)
                if (source.content != chapter.content) repository.updateChapter(chapter, source.content)
                val context = buildString {
                    append(ContextEngine.build(project, source, chapters.value, storyItems.value, anchors.value, edges.value, researchNotes = researchNotes.value).prompt)
                    appendLine("\n当前完整正文：")
                    appendLine(source.content)
                    if (!humanize) {
                        appendLine("\n需要处理的门禁问题：")
                        QualityGate.inspect(source, storyItems.value, anchors.value, project).forEach { appendLine("- ${it.title}：${it.detail}") }
                    }
                }
                val result = if (humanize) modelClient.humanizeChapter(modelConfig.value, context, request)
                else modelClient.rewriteChapter(modelConfig.value, context, request)
                result.fold(
                    onSuccess = { replacement ->
                        val updated = repository.replaceChapterWithRevision(
                            source,
                            replacement,
                            if (humanize) "去 AI 味润色" else "按门禁问题 AI 改写",
                        )
                        val lifecycle = runChapterLifecycle(updated, request)
                        message.value = if (lifecycle.passed) "AI 改写已复检通过，可在审阅页撤回" else "AI 改写已保存：${lifecycle.message}"
                    },
                    onFailure = { message.value = it.message ?: "AI 改写失败" },
                )
            } finally {
                finishGeneration(task, request)
            }
        }
    }

    fun rewriteCurrentChapterForGate() = rewriteCurrentChapter(GenerationTask.CHAPTER_REWRITE, humanize = false)

    fun humanizeCurrentChapter() = rewriteCurrentChapter(GenerationTask.HUMANIZE, humanize = true)

    fun restoreLatestRevision() {
        val chapter = selectedChapter.value ?: return
        val revision = latestRevision.value ?: return
        viewModelScope.launch {
            repository.restoreRevision(chapter, revision)
            message.value = "已撤回 AI 改写，恢复此前正文"
        }
    }

    fun cancelGeneration(task: GenerationTask) {
        if (task !in generationTasks.value) return
        generationRequests[task]?.cancel()
        generationJobs[task]?.cancel()
        selectedChapter.value?.takeIf { it.lifecycleStatus == ChapterLifecycleStatus.PROCESSING }?.let { chapter ->
            viewModelScope.launch {
                repository.updateChapterLifecycle(
                    chapter = chapter,
                    lifecycleStatus = ChapterLifecycleStatus.MEMORY_FAILED,
                    lifecycleDetail = "作者已取消处理，可在审阅页重试",
                )
            }
        }
        finishGeneration(task, generationRequests[task] ?: return)
        message.value = "已取消${task.label}，其他任务继续运行"
    }

    fun clearMessage() {
        message.value = null
    }
}
