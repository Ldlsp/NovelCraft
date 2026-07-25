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

@OptIn(ExperimentalCoroutinesApi::class)
class NovelViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NovelRepository(NovelDatabase.create(application))
    private val modelPreferences = ModelPreferences(application)
    private val modelClient = OpenAiCompatibleClient()
    private var saveChapterJob: Job? = null

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

    val selectedChapter = combine(chapters, selectedChapterId) { all, id ->
        all.firstOrNull { it.id == id } ?: all.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    fun saveChapter(content: String) {
        val chapter = selectedChapter.value ?: return
        saveChapterJob?.cancel()
        saveChapterJob = viewModelScope.launch {
            delay(500)
            repository.updateChapter(chapter, content)
        }
    }

    fun addStoryItem(kind: String, name: String, detail: String) {
        val projectId = selectedProjectId.value ?: return
        if (name.isBlank()) {
            message.value = "请填写资料名称"
            return
        }
        viewModelScope.launch {
            repository.addStoryItem(projectId, kind, name, detail)
            message.value = "已保存" + kind
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
        isGenerating.value = true
        message.value = "正在直接请求你的模型..."
        val writingContext = listOf(
            "作品：" + project.title,
            "题材：" + project.genre,
            "核心设定：" + project.premise,
            "当前章节：第" + chapter.number + "章 " + chapter.title,
            "正文末尾：",
            chapter.content.takeLast(2200),
            "请继续本章，先推进一个可见动作，保留未解决的冲突，并以一个具体钩子收尾。",
        ).joinToString("\\n")
        viewModelScope.launch {
            modelClient.continueWriting(modelConfig.value, writingContext).fold(
                onSuccess = { generated ->
                    repository.updateChapter(chapter, chapter.content.trimEnd() + "\\n\\n" + generated)
                    message.value = "已续写并保存到本机"
                },
                onFailure = { message.value = it.message ?: "续写失败" },
            )
            isGenerating.value = false
        }
    }

    fun clearMessage() {
        message.value = null
    }
}
