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

@OptIn(ExperimentalCoroutinesApi::class)
class NovelViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NovelRepository(NovelDatabase.create(application))
    private val modelPreferences = ModelPreferences(application)
    private val modelClient = OpenAiCompatibleClient()
    private val saveChapterJobs = mutableMapOf<Long, Job>()
    private var renameChapterJob: Job? = null
    private var savePlanJob: Job? = null
    private var generationJob: Job? = null
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

    val contextPacket = combine(selectedProject, selectedChapter, chapters, storyItems, anchors) { project, chapter, allChapters, items, projectAnchors ->
        if (project == null || chapter == null) ContextPacket() else ContextEngine.build(project, chapter, allChapters, items, projectAnchors)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContextPacket())

    val qualityIssues = combine(selectedChapter, storyItems, anchors) { chapter, items, projectAnchors ->
        QualityGate.inspect(chapter, items, projectAnchors)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val modelConfig = MutableStateFlow(modelPreferences.load())
    val message = MutableStateFlow<String?>(null)
    val isGenerating = MutableStateFlow(false)

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

    fun saveChapterPlan(outline: String, targetWordCount: Int) {
        val chapter = selectedChapter.value ?: return
        savePlanJob?.cancel()
        savePlanJob = viewModelScope.launch {
            delay(500)
            repository.updateChapterPlan(chapter, outline, targetWordCount)
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

    fun generateChapterPlan() {
        val project = selectedProject.value ?: return
        val chapter = selectedChapter.value ?: return
        if (isGenerating.value) return
        isGenerating.value = true
        message.value = "正在根据本地记忆生成本章计划..."
        val context = ContextEngine.build(project, chapter, chapters.value, storyItems.value, anchors.value).prompt +
            "\n请为当前章节生成大纲，覆盖冲突、转折和结尾钩子。"
        generationJob = viewModelScope.launch {
            try {
                val result = modelClient.generateChapterPlan(modelConfig.value, context)
                if (!isActive) return@launch
                result.fold(
                    onSuccess = { outline ->
                        repository.updateChapterPlan(chapter, outline, chapter.targetWordCount)
                        message.value = "本章计划已保存，可继续手动调整"
                    },
                    onFailure = { message.value = it.message ?: "生成大纲失败" },
                )
            } finally {
                isGenerating.value = false
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
        if (isGenerating.value) return
        val sourceContent = pendingChapterContent[chapter.id] ?: chapter.content
        val sourceChapter = chapter.copy(content = sourceContent)
        isGenerating.value = true
        message.value = "正在直接请求你的模型..."
        val writingContext = ContextEngine.build(project, sourceChapter, chapters.value, storyItems.value, anchors.value).prompt
        generationJob = viewModelScope.launch {
            try {
                val result = modelClient.continueWriting(modelConfig.value, writingContext)
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
                isGenerating.value = false
            }
        }
    }

    fun cancelGeneration() {
        if (!isGenerating.value) return
        modelClient.cancelActiveRequest()
        generationJob?.cancel()
        isGenerating.value = false
        message.value = "已取消模型请求，正文未改动"
    }

    fun clearMessage() {
        message.value = null
    }
}
