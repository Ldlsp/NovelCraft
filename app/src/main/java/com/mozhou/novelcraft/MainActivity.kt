package com.mozhou.novelcraft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private val Ink = Color(0xFF1C1C1E)
private val Paper = Color(0xFFF2F2F7)
private val SurfaceWhite = Color(0xFFFFFFFF)
private val Red = Color(0xFF007AFF)
private val Teal = Color(0xFF34C759)
private val Green = Color(0xFF30D158)
private val Gold = Color(0xFFFF9500)
private val SecondaryLabel = Color(0xFF6D6D72)
private val IosShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NovelCraftApp() }
    }
}

private enum class MainDestination { SHELF, WORKSPACE, SETTINGS }
private enum class WorkspaceTab(val label: String) { WRITE("写作"), OUTLINE("大纲"), RESOURCES("资料"), REVIEW("审核") }

@Composable
private fun NovelCraftApp(viewModel: NovelViewModel = viewModel()) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val project by viewModel.selectedProject.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val selectedChapter by viewModel.selectedChapter.collectAsStateWithLifecycle()
    val storyItems by viewModel.storyItems.collectAsStateWithLifecycle()
    val anchors by viewModel.anchors.collectAsStateWithLifecycle()
    val edges by viewModel.edges.collectAsStateWithLifecycle()
    val contextPacket by viewModel.contextPacket.collectAsStateWithLifecycle()
    val qualityIssues by viewModel.qualityIssues.collectAsStateWithLifecycle()
    val repairPlan by viewModel.repairPlan.collectAsStateWithLifecycle()
    val modelConfig by viewModel.modelConfig.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(MainDestination.SHELF) }
    var createProjectVisible by rememberSaveable { mutableStateOf(false) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importDocument(uri)
            destination = MainDestination.WORKSPACE
        }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) viewModel.exportDocument(uri)
    }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Red,
            secondary = Teal,
            tertiary = Gold,
            background = Paper,
            surface = SurfaceWhite,
            onBackground = Ink,
            onSurface = Ink,
        ),
        shapes = IosShapes,
    ) {
        Scaffold(
            topBar = {
                AppTopBar(
                    destination = destination,
                    projectTitle = project?.title,
                    onBack = { destination = MainDestination.SHELF },
                    onExport = { project?.let { exporter.launch("${it.title}.md") } },
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = destination == MainDestination.SHELF,
                        onClick = { destination = MainDestination.SHELF },
                        icon = { Icon(Icons.Outlined.AutoStories, null) },
                        label = { Text("书架") },
                    )
                    NavigationBarItem(
                        selected = destination == MainDestination.WORKSPACE,
                        onClick = {
                            if (project != null) {
                                destination = MainDestination.WORKSPACE
                            } else {
                                createProjectVisible = true
                            }
                        },
                        icon = { Icon(Icons.Outlined.MenuBook, null) },
                        label = { Text("创作") },
                    )
                    NavigationBarItem(
                        selected = destination == MainDestination.SETTINGS,
                        onClick = { destination = MainDestination.SETTINGS },
                        icon = { Icon(Icons.Outlined.Settings, null) },
                        label = { Text("我的") },
                    )
                }
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                message?.let {
                    StatusMessage(it, onDismiss = viewModel::clearMessage)
                }
                when (destination) {
                    MainDestination.SHELF -> ShelfScreen(
                        projects = projects,
                        onCreate = { createProjectVisible = true },
                        onImport = { importer.launch(arrayOf("text/plain", "text/markdown", "text/*", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                        onOpen = {
                            viewModel.selectProject(it.id)
                            destination = MainDestination.WORKSPACE
                        },
                    )
                    MainDestination.WORKSPACE -> {
                        if (project == null) {
                            EmptyWorkspace()
                        } else {
                            WorkspaceScreen(
                                project = project!!,
                                chapters = chapters,
                                selectedChapter = selectedChapter,
                                storyItems = storyItems,
                                anchors = anchors,
                                edges = edges,
                                contextPacket = contextPacket,
                                qualityIssues = qualityIssues,
                                repairPlan = repairPlan,
                                config = modelConfig,
                                activeTasks = viewModel.generationTasks.collectAsStateWithLifecycle().value,
                                onSelectChapter = viewModel::selectChapter,
                                onSaveChapter = viewModel::saveChapter,
                                onRenameChapter = viewModel::renameChapter,
                                onAddChapter = viewModel::addChapter,
                                onSaveChapterPlan = viewModel::saveChapterPlan,
                                onSaveBeatSheet = viewModel::saveBeatSheet,
                                onSaveStyleGuide = viewModel::saveStyleGuide,
                                onAddStoryItem = viewModel::addStoryItem,
                                onUpdateStoryItem = viewModel::updateStoryItem,
                                onAddAnchor = viewModel::addAnchor,
                                onAddEdge = viewModel::addEdge,
                                onExtractMemory = viewModel::extractMemoryFromCurrentChapter,
                                onGenerate = viewModel::generateContinuation,
                                onAutoWrite = viewModel::autoWriteChapters,
                                onGeneratePlan = viewModel::generateChapterPlan,
                                onGenerateBeatSheet = viewModel::generateBeatSheet,
                                onExtractStyleGuide = viewModel::extractStyleGuideFromCurrentChapter,
                                onCancelGeneration = viewModel::cancelGeneration,
                                onGenerateRepairPlan = viewModel::generateRepairPlan,
                                onMarkQualityRepaired = viewModel::markCurrentChapterQualityRepaired,
                                onReviseOutline = viewModel::reviseOutline,
                                onResolveOutlineCascade = viewModel::resolveOutlineCascade,
                            )
                        }
                    }
                    MainDestination.SETTINGS -> ModelSettingsScreen(
                        config = modelConfig,
                        onSave = viewModel::saveModelConfig,
                        onTest = viewModel::testModelConfig,
                    )
                }
            }
        }
        if (createProjectVisible) {
            CreateProjectDialog(
                onDismiss = { createProjectVisible = false },
                onCreate = { title, genre, premise ->
                    viewModel.createProject(title, genre, premise)
                    createProjectVisible = false
                    destination = MainDestination.WORKSPACE
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    destination: MainDestination,
    projectTitle: String?,
    onBack: () -> Unit,
    onExport: () -> Unit,
) {
    val title = when (destination) {
        MainDestination.SHELF -> "墨舟"
        MainDestination.WORKSPACE -> projectTitle ?: "创作"
        MainDestination.SETTINGS -> "模型与本机存储"
    }
    CenterAlignedTopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (destination == MainDestination.WORKSPACE) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.Book, "返回书架") }
            }
        },
        actions = {
            if (destination == MainDestination.WORKSPACE) {
                IconButton(onClick = onExport) { Icon(Icons.Outlined.FileDownload, "导出作品") }
                Icon(Icons.Outlined.CloudDone, "本机已保存", tint = Green, modifier = Modifier.padding(end = 16.dp))
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SurfaceWhite),
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
    )
}

@Composable
private fun StatusMessage(text: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F3FF)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.CheckCircle, null, tint = Teal)
            Spacer(Modifier.width(8.dp))
            Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    }
}

@Composable
private fun ShelfScreen(
    projects: List<NovelProject>,
    onCreate: () -> Unit,
    onImport: () -> Unit,
    onOpen: (NovelProject) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("本地优先", color = Red, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text("今天，写哪一章？", style = MaterialTheme.typography.headlineMedium)
            Text("作品保存在本机 SQLite，模型密钥仅存于 Android Keystore。", color = SecondaryLabel, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ActionCard("新建作品", "从灵感建立项目", Icons.Outlined.Add, Modifier.weight(1f), onCreate)
                ActionCard("导入续写", "TXT / Markdown / DOCX", Icons.Outlined.FileOpen, Modifier.weight(1f), onImport)
            }
        }
        item { Text("我的作品", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp)) }
        if (projects.isEmpty()) {
            item {
                Card {
                    Column(Modifier.padding(20.dp)) {
                        Icon(Icons.Outlined.Description, null, tint = Teal)
                        Spacer(Modifier.height(10.dp))
                        Text("还没有作品", style = MaterialTheme.typography.titleMedium)
                        Text("从一个灵感开始，或导入已有 TXT/Markdown 后继续写。", color = Color.Gray)
                    }
                }
            }
        } else {
            items(projects, key = { it.id }) { project ->
                ProjectRow(project, onOpen)
            }
        }
    }
}

@Composable
private fun ActionCard(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(124.dp)) {
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, null, tint = Red)
            Spacer(Modifier.height(6.dp))
            Text(title)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = SecondaryLabel, maxLines = 2)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectRow(project: NovelProject, onOpen: (NovelProject) -> Unit) {
    Card(onClick = { onOpen(project) }) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.MenuBook, null, tint = Teal)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(project.title, style = MaterialTheme.typography.titleMedium)
                Text(project.genre, color = SecondaryLabel, style = MaterialTheme.typography.bodySmall)
                if (project.premise.isNotBlank()) {
                    Text(project.premise, maxLines = 1, overflow = TextOverflow.Ellipsis, color = SecondaryLabel, style = MaterialTheme.typography.labelSmall)
                }
            }
            Icon(Icons.Outlined.Book, null, tint = Red)
        }
    }
}

@Composable
private fun EmptyWorkspace() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.MenuBook, null, tint = Teal)
        Spacer(Modifier.height(12.dp))
        Text("先从书架选择一本作品")
    }
}

@Composable
private fun WorkspaceScreen(
    project: NovelProject,
    chapters: List<Chapter>,
    selectedChapter: Chapter?,
    storyItems: List<StoryItem>,
    anchors: List<StoryAnchor>,
    edges: List<StoryEdge>,
    contextPacket: ContextPacket,
    qualityIssues: List<QualityIssue>,
    repairPlan: String?,
    config: ModelConfig,
    activeTasks: Set<GenerationTask>,
    onSelectChapter: (Long) -> Unit,
    onSaveChapter: (String) -> Unit,
    onRenameChapter: (String) -> Unit,
    onAddChapter: () -> Unit,
    onSaveChapterPlan: (String, Int) -> Unit,
    onSaveBeatSheet: (String) -> Unit,
    onSaveStyleGuide: (String) -> Unit,
    onAddStoryItem: (String, String, String, String) -> Unit,
    onUpdateStoryItem: (StoryItem, String, String, String, String) -> Unit,
    onAddAnchor: (Int, Int, String, String, String, String, String) -> Unit,
    onAddEdge: (Long, Long, String, String, Int) -> Unit,
    onExtractMemory: () -> Unit,
    onGenerate: () -> Unit,
    onAutoWrite: (Int) -> Unit,
    onGeneratePlan: () -> Unit,
    onGenerateBeatSheet: () -> Unit,
    onExtractStyleGuide: () -> Unit,
    onCancelGeneration: (GenerationTask) -> Unit,
    onGenerateRepairPlan: () -> Unit,
    onMarkQualityRepaired: () -> Unit,
    onReviseOutline: (Int, String) -> Unit,
    onResolveOutlineCascade: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        GenerationTaskBar(activeTasks, onCancelGeneration)
        TabRow(selectedTabIndex = selectedTab) {
            WorkspaceTab.entries.forEachIndexed { index, tab ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(tab.label) })
            }
        }
        when (WorkspaceTab.entries[selectedTab]) {
            WorkspaceTab.WRITE -> WriteTab(
                chapters, selectedChapter, contextPacket, config, activeTasks,
                onSelectChapter, onSaveChapter, onRenameChapter, onAddChapter, onGenerate, onAutoWrite, onCancelGeneration,
            )
            WorkspaceTab.OUTLINE -> OutlineTab(project, chapters, selectedChapter, anchors, config, activeTasks, onSaveChapterPlan, onSaveBeatSheet, onSaveStyleGuide, onGeneratePlan, onGenerateBeatSheet, onExtractStyleGuide, onCancelGeneration, onAddAnchor, onReviseOutline, onResolveOutlineCascade)
            WorkspaceTab.RESOURCES -> ResourcesTab(storyItems, edges, GenerationTask.MEMORY_EXTRACTION in activeTasks, onAddStoryItem, onUpdateStoryItem, onAddEdge, onExtractMemory, { onCancelGeneration(GenerationTask.MEMORY_EXTRACTION) })
            WorkspaceTab.REVIEW -> ReviewTab(selectedChapter, qualityIssues, repairPlan, config, GenerationTask.REPAIR_PLAN in activeTasks, onGenerateRepairPlan, onMarkQualityRepaired, { onCancelGeneration(GenerationTask.REPAIR_PLAN) })
        }
    }
}

@Composable
private fun GenerationTaskBar(activeTasks: Set<GenerationTask>, onCancel: (GenerationTask) -> Unit) {
    if (activeTasks.isEmpty()) return
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(activeTasks.sortedBy { it.ordinal }, key = { it.name }) { task ->
            OutlinedButton(onClick = { onCancel(task) }) {
                Icon(Icons.Outlined.Close, null, modifier = Modifier.width(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("${task.label}中")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WriteTab(
    chapters: List<Chapter>,
    chapter: Chapter?,
    contextPacket: ContextPacket,
    config: ModelConfig,
    activeTasks: Set<GenerationTask>,
    onSelectChapter: (Long) -> Unit,
    onSave: (String) -> Unit,
    onRename: (String) -> Unit,
    onAddChapter: () -> Unit,
    onGenerate: () -> Unit,
    onAutoWrite: (Int) -> Unit,
    onCancel: (GenerationTask) -> Unit,
) {
    if (chapter == null) {
        EmptyWorkspace()
        return
    }
    var draft by remember(chapter.id, chapter.content) { mutableStateOf(chapter.content) }
    var title by remember(chapter.id, chapter.title) { mutableStateOf(chapter.title) }
    var continueDialogVisible by rememberSaveable { mutableStateOf(false) }
    var autoWriteDialogVisible by rememberSaveable { mutableStateOf(false) }
    val isContinuing = GenerationTask.CONTINUATION in activeTasks
    val isAutoWriting = GenerationTask.AUTO_WRITE in activeTasks
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ChapterRail(chapters, chapter.id, onSelectChapter, onAddChapter, enabled = true)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it; onRename(it) },
            label = { Text("第${chapter.number}章标题") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Text(draft.count { !it.isWhitespace() }.toString() + " 字 · 自动保存到 SQLite", color = Color.Gray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
        if (chapter.qualityStatus == ChapterQualityStatus.NEEDS_REPAIR) {
            Text("待修复：${chapter.qualityIssueSummary}", color = Red, style = MaterialTheme.typography.labelSmall)
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it; onSave(it) },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            label = { Text("小说正文") },
            minLines = 1,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (isContinuing) {
                Button(onClick = { onCancel(GenerationTask.CONTINUATION) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Close, null)
                    Spacer(Modifier.width(6.dp))
                    Text("取消")
                }
            } else {
                Button(
                    onClick = { continueDialogVisible = true },
                    modifier = Modifier.weight(1f),
                    enabled = config.baseUrl.isNotBlank() && config.apiKey.isNotBlank() && config.model.isNotBlank(),
                ) {
                    Icon(Icons.Outlined.Lightbulb, null)
                    Spacer(Modifier.width(6.dp))
                    Text("AI 续写")
                }
            }
            IconButton(onClick = { onSave(draft) }, modifier = Modifier.width(56.dp).height(48.dp)) {
                Icon(Icons.Outlined.Save, "保存", tint = Red)
            }
            OutlinedButton(
                onClick = { if (isAutoWriting) onCancel(GenerationTask.AUTO_WRITE) else autoWriteDialogVisible = true },
                modifier = Modifier.width(86.dp),
                enabled = isAutoWriting || (config.baseUrl.isNotBlank() && config.apiKey.isNotBlank() && config.model.isNotBlank()),
            ) { Text(if (isAutoWriting) "取消批量" else "批量") }
        }
        ActionHint("AI 续写会接在当前章节末尾，不会新建下一章；批量会依次新建后续章节；保存会立即写入本机。")
        if (config.baseUrl.isBlank() || config.apiKey.isBlank() || config.model.isBlank()) {
            Text("请先在“我的”填写 Base URL、API Key 与模型名称。", color = Gold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
    if (continueDialogVisible) {
        ContinueWritingDialog(
            packet = contextPacket,
            onDismiss = { continueDialogVisible = false },
            onStart = { onGenerate(); continueDialogVisible = false },
        )
    }
    if (autoWriteDialogVisible) {
        AutoWriteDialog(
            packet = contextPacket,
            onDismiss = { autoWriteDialogVisible = false },
            onStart = { count -> onAutoWrite(count); autoWriteDialogVisible = false },
        )
    }
}

@Composable
private fun ActionHint(text: String) {
    Text(
        text = text,
        color = SecondaryLabel,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

@Composable
private fun ContinueWritingDialog(packet: ContextPacket, onDismiss: () -> Unit, onStart: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 续写") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("会把结果直接接在当前章节正文末尾，不会新建章节。以下本地上下文会随本章发送到你的模型。")
                ContextSummary(packet)
            }
        },
        confirmButton = { Button(onClick = onStart) { Text("开始续写") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun AutoWriteDialog(packet: ContextPacket, onDismiss: () -> Unit, onStart: (Int) -> Unit) {
    var count by remember { mutableStateOf("1") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量写作") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("将依次生成新章节；每章出现本地门禁警告会立即停止。")
                ContextSummary(packet)
                OutlinedTextField(value = count, onValueChange = { count = it.filter(Char::isDigit) }, label = { Text("章节数，1-5") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onStart((count.toIntOrNull() ?: 1).coerceIn(1, 5)) }) { Text("开始") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ChapterRail(chapters: List<Chapter>, selectedId: Long, onSelect: (Long) -> Unit, onAdd: () -> Unit, enabled: Boolean) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        items(chapters, key = { it.id }) { item ->
            if (item.id == selectedId) {
                Button(onClick = { onSelect(item.id) }) { Text("第${item.number}章") }
            } else {
                TextButton(onClick = { onSelect(item.id) }) { Text("第${item.number}章") }
            }
        }
        item {
            IconButton(onClick = onAdd, enabled = enabled) { Icon(Icons.Outlined.Add, "新建章节") }
        }
    }
}

@Composable
private fun ContextSummary(packet: ContextPacket) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE5F0ED)),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lightbulb, null, tint = Teal)
            Spacer(Modifier.width(8.dp))
            val names = packet.relevantItems.take(2).joinToString("、") { it.name }
            Text(
                "上下文：${packet.relevantItems.size} 条设定 · ${packet.relevantEdges.size} 条关系 · ${packet.relevantChapters.size} 段摘录" + if (names.isBlank()) "" else " · $names",
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OutlineTab(
    project: NovelProject,
    chapters: List<Chapter>,
    selectedChapter: Chapter?,
    anchors: List<StoryAnchor>,
    config: ModelConfig,
    activeTasks: Set<GenerationTask>,
    onSavePlan: (String, Int) -> Unit,
    onSaveBeatSheet: (String) -> Unit,
    onSaveStyleGuide: (String) -> Unit,
    onGeneratePlan: () -> Unit,
    onGenerateBeatSheet: () -> Unit,
    onExtractStyleGuide: () -> Unit,
    onCancel: (GenerationTask) -> Unit,
    onAddAnchor: (Int, Int, String, String, String, String, String) -> Unit,
    onReviseOutline: (Int, String) -> Unit,
    onResolveOutlineCascade: () -> Unit,
) {
    var anchorDialogVisible by rememberSaveable { mutableStateOf(false) }
    var reviseDialogVisible by rememberSaveable { mutableStateOf(false) }
    val isStyleGenerating = GenerationTask.STYLE_GUIDE in activeTasks
    val isPlanGenerating = GenerationTask.CHAPTER_PLAN in activeTasks
    val isBeatGenerating = GenerationTask.BEAT_SHEET in activeTasks
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(project.genre + " · " + project.title, color = Red, style = MaterialTheme.typography.labelMedium)
            Text("章节大纲", style = MaterialTheme.typography.headlineSmall)
            if (project.premise.isNotBlank()) Text(project.premise, color = Color.Gray)
            OutlinedButton(onClick = { reviseDialogVisible = true }, modifier = Modifier.padding(top = 8.dp)) { Text("改纲级联") }
            ActionHint("改纲级联会标记受影响的章节设定；本章计划与场景分镜只改大纲字段，不会修改正文。")
            if (project.outlineRevisionReport.isNotBlank()) {
                Text(project.outlineRevisionReport, color = Gold, style = MaterialTheme.typography.labelSmall)
                Button(onClick = onResolveOutlineCascade) { Text("确认已复核全部待审项") }
            }
        }
        item { StyleGuideEditor(project.styleGuide, config, isStyleGenerating, onSaveStyleGuide, onExtractStyleGuide, { onCancel(GenerationTask.STYLE_GUIDE) }) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("大纲锚点", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { anchorDialogVisible = true }) { Icon(Icons.Outlined.Add, "添加大纲锚点") }
            }
        }
        if (anchors.isEmpty()) {
            item { Text("用锚点规定一段章节的冲突、禁区和章末张力；续写时会自动带入模型。", color = Color.Gray, style = MaterialTheme.typography.bodySmall) }
        } else {
            items(anchors, key = { "anchor-${it.id}" }) { anchor ->
                AnchorCard(anchor, selectedChapter?.number)
            }
        }
        selectedChapter?.let { chapter ->
            item { ChapterPlanEditor(chapter, config, isPlanGenerating, isBeatGenerating, onSavePlan, onSaveBeatSheet, onGeneratePlan, onGenerateBeatSheet, { onCancel(GenerationTask.CHAPTER_PLAN) }, { onCancel(GenerationTask.BEAT_SHEET) }) }
        }
        items(chapters, key = { it.id }) { chapter ->
            Card {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(chapter.number.toString(), color = Red, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(chapter.title, style = MaterialTheme.typography.titleSmall)
                        Text(chapter.outline.ifBlank { chapter.content.take(70).ifBlank { "待写" } }, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        if (chapter.targetWordCount > 0) {
                            val current = chapter.content.count { !it.isWhitespace() }
                            Text("$current / ${chapter.targetWordCount} 字", color = Teal, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
    if (anchorDialogVisible) {
        AnchorDialog(
            onDismiss = { anchorDialogVisible = false },
            onSave = { start, end, title, conflict, allowed, forbidden, tension ->
                onAddAnchor(start, end, title, conflict, allowed, forbidden, tension)
                anchorDialogVisible = false
            },
        )
    }
    if (reviseDialogVisible) {
        OutlineRevisionDialog(
            initialChapter = selectedChapter?.number ?: 1,
            onDismiss = { reviseDialogVisible = false },
            onApply = { from, description -> onReviseOutline(from, description); reviseDialogVisible = false },
        )
    }
}

@Composable
private fun OutlineRevisionDialog(initialChapter: Int, onDismiss: () -> Unit, onApply: (Int, String) -> Unit) {
    var fromChapter by remember { mutableStateOf(initialChapter.toString()) }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("改纲级联") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("从指定章节起标记受影响锚点、资料卡和关系，继续写作前需逐项复核。")
            OutlinedTextField(value = fromChapter, onValueChange = { fromChapter = it.filter(Char::isDigit) }, label = { Text("起始章节") }, singleLine = true)
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("改纲说明") }, minLines = 2)
        } },
        confirmButton = { Button(onClick = { onApply(fromChapter.toIntOrNull() ?: initialChapter, description) }) { Text("生成影响报告") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun AnchorCard(anchor: StoryAnchor, currentChapter: Int?) {
    val isCurrent = currentChapter != null && currentChapter in anchor.startChapter..anchor.endChapter
    Card(colors = CardDefaults.cardColors(containerColor = if (isCurrent) Color(0xFFE5F0ED) else Color(0xFFFFFDF8))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("第${anchor.startChapter}-${anchor.endChapter}章 · ${anchor.title}", style = MaterialTheme.typography.titleSmall, color = if (isCurrent) Teal else Ink)
            Text(anchor.coreConflict, style = MaterialTheme.typography.bodySmall)
            if (anchor.cascadePending) Text("改纲待审", color = Gold, style = MaterialTheme.typography.labelSmall)
            if (anchor.allowedPlot.isNotBlank()) Text("推进：${anchor.allowedPlot}", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            if (anchor.forbiddenReveals.isNotBlank()) Text("禁区：${anchor.forbiddenReveals}", color = Red, style = MaterialTheme.typography.labelSmall)
            if (anchor.mandatoryTension.isNotBlank()) Text("张力：${anchor.mandatoryTension}", color = Gold, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun AnchorDialog(
    onDismiss: () -> Unit,
    onSave: (Int, Int, String, String, String, String, String) -> Unit,
) {
    var start by remember { mutableStateOf("1") }
    var end by remember { mutableStateOf("10") }
    var title by remember { mutableStateOf("") }
    var conflict by remember { mutableStateOf("") }
    var allowed by remember { mutableStateOf("") }
    var forbidden by remember { mutableStateOf("") }
    var tension by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加大纲锚点") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item { OutlinedTextField(value = start, onValueChange = { start = it.filter(Char::isDigit) }, label = { Text("起始章节") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(value = end, onValueChange = { end = it.filter(Char::isDigit) }, label = { Text("结束章节") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("锚点标题") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = conflict, onValueChange = { conflict = it }, label = { Text("核心冲突") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
                item { OutlinedTextField(value = allowed, onValueChange = { allowed = it }, label = { Text("允许推进") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = forbidden, onValueChange = { forbidden = it }, label = { Text("禁止揭露，使用顿号分隔") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = tension, onValueChange = { tension = it }, label = { Text("必须保留的张力") }, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = { Button(onClick = { onSave(start.toIntOrNull() ?: 1, end.toIntOrNull() ?: 1, title, conflict, allowed, forbidden, tension) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun StyleGuideEditor(
    initialGuide: String,
    config: ModelConfig,
    isGenerating: Boolean,
    onSave: (String) -> Unit,
    onExtract: () -> Unit,
    onCancel: () -> Unit,
) {
    var guide by remember(initialGuide) { mutableStateOf(initialGuide) }
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("项目文风档案", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = guide,
                onValueChange = { guide = it; onSave(it) },
                label = { Text("叙事视角、节奏、对话、禁用表达") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = if (isGenerating) onCancel else onExtract,
                enabled = isGenerating || (config.baseUrl.isNotBlank() && config.apiKey.isNotBlank() && config.model.isNotBlank()),
            ) {
                Icon(if (isGenerating) Icons.Outlined.Close else Icons.Outlined.Lightbulb, null)
                Spacer(Modifier.width(6.dp))
                Text(if (isGenerating) "取消生成" else "从当前样章提取文风")
            }
        }
    }
}

@Composable
private fun ChapterPlanEditor(
    chapter: Chapter,
    config: ModelConfig,
    isPlanGenerating: Boolean,
    isBeatGenerating: Boolean,
    onSave: (String, Int) -> Unit,
    onSaveBeatSheet: (String) -> Unit,
    onGeneratePlan: () -> Unit,
    onGenerateBeatSheet: () -> Unit,
    onCancelPlan: () -> Unit,
    onCancelBeat: () -> Unit,
) {
    var outline by remember(chapter.id, chapter.outline) { mutableStateOf(chapter.outline) }
    var beatSheet by remember(chapter.id, chapter.beatSheet) { mutableStateOf(chapter.beatSheet) }
    var target by remember(chapter.id, chapter.targetWordCount) { mutableStateOf(chapter.targetWordCount.takeIf { it > 0 }?.toString().orEmpty()) }
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("第${chapter.number}章计划", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = outline,
                onValueChange = { outline = it; onSave(it, target.toIntOrNull() ?: 0) },
                label = { Text("本章冲突、转折与结尾钩子") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = target,
                onValueChange = { target = it.filter(Char::isDigit); onSave(outline, target.toIntOrNull() ?: 0) },
                label = { Text("目标字数，可留空") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = if (isPlanGenerating) onCancelPlan else onGeneratePlan,
                enabled = isPlanGenerating || (config.baseUrl.isNotBlank() && config.apiKey.isNotBlank() && config.model.isNotBlank()),
            ) {
                Icon(if (isPlanGenerating) Icons.Outlined.Close else Icons.Outlined.Lightbulb, null)
                Spacer(Modifier.width(6.dp))
                Text(if (isPlanGenerating) "取消本章计划" else "AI 生成本章计划")
            }
            OutlinedTextField(
                value = beatSheet,
                onValueChange = { beatSheet = it; onSaveBeatSheet(it) },
                label = { Text("Beat Sheet：按顺序展开的场景分镜") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = if (isBeatGenerating) onCancelBeat else onGenerateBeatSheet,
                enabled = isBeatGenerating || (config.baseUrl.isNotBlank() && config.apiKey.isNotBlank() && config.model.isNotBlank()),
            ) {
                Icon(if (isBeatGenerating) Icons.Outlined.Close else Icons.Outlined.Lightbulb, null)
                Spacer(Modifier.width(6.dp))
                Text(if (isBeatGenerating) "取消分镜" else "AI 生成分镜")
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ResourcesTab(
    items: List<StoryItem>,
    edges: List<StoryEdge>,
    isExtracting: Boolean,
    onAdd: (String, String, String, String) -> Unit,
    onUpdate: (StoryItem, String, String, String, String) -> Unit,
    onAddEdge: (Long, Long, String, String, Int) -> Unit,
    onExtractMemory: () -> Unit,
    onCancelExtraction: () -> Unit,
) {
    var dialogVisible by rememberSaveable { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<StoryItem?>(null) }
    var edgeDialogVisible by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("人物与伏笔", style = MaterialTheme.typography.headlineSmall)
                    Text("所有资料都会写入本机 SQLite。", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    ActionHint("书本图标会从当前章节提取人物、伏笔和关系；人物图标用于新增关系；加号用于手动新增资料。")
                }
                Row {
                    IconButton(onClick = if (isExtracting) onCancelExtraction else onExtractMemory) { Icon(if (isExtracting) Icons.Outlined.Close else Icons.Outlined.AutoStories, if (isExtracting) "取消知识图谱提取" else "从当前章节提取记忆") }
                    IconButton(onClick = { edgeDialogVisible = true }, enabled = items.size >= 2) { Icon(Icons.Outlined.People, "添加关系") }
                    IconButton(onClick = { dialogVisible = true }) { Icon(Icons.Outlined.Add, "添加资料") }
                }
            }
        }
        if (items.isEmpty()) {
            item {
                Card {
                    Column(Modifier.padding(18.dp)) {
                        Icon(Icons.Outlined.Lightbulb, null, tint = Gold)
                        Spacer(Modifier.height(8.dp))
                        Text("还没有资料卡", style = MaterialTheme.typography.titleSmall)
                        Text("添加人物、地点、伏笔、时间线或禁区；续写时会按当前情节自动检索。", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(items, key = { it.id }) { item ->
                Card(onClick = { editItem = item }) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (item.kind == "人物") Icons.Outlined.People else Icons.Outlined.Lightbulb, null, tint = Teal)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(item.kind + " · " + item.name + " · " + item.status, style = MaterialTheme.typography.titleSmall)
                            if (item.cascadePending) Text("改纲待审", color = Gold, style = MaterialTheme.typography.labelSmall)
                            Text(item.detail, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        if (edges.isNotEmpty()) {
            item { Text("关系图谱", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            items(edges, key = { "edge-${it.id}" }) { edge ->
                val source = items.firstOrNull { it.id == edge.sourceItemId }?.name ?: "已移除资料"
                val target = items.firstOrNull { it.id == edge.targetItemId }?.name ?: "已移除资料"
                Card {
                    Column(Modifier.padding(14.dp)) {
                        Text("$source  - ${edge.relation} -  $target", style = MaterialTheme.typography.titleSmall)
                        if (edge.description.isNotBlank()) Text(edge.description, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        if (edge.cascadePending) Text("改纲待审", color = Gold, style = MaterialTheme.typography.labelSmall)
                        Text("自第${edge.sinceChapter}章起", color = Teal, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
    if (dialogVisible) {
        StoryItemDialog(onDismiss = { dialogVisible = false }, onSave = { kind, name, detail, status ->
            onAdd(kind, name, detail, status)
            dialogVisible = false
        })
    }
    editItem?.let { item ->
        StoryItemDialog(
            item = item,
            onDismiss = { editItem = null },
            onSave = { kind, name, detail, status ->
                onUpdate(item, kind, name, detail, status)
                editItem = null
            },
        )
    }
    if (edgeDialogVisible) {
        StoryEdgeDialog(
            items = items,
            onDismiss = { edgeDialogVisible = false },
            onSave = { source, target, relation, description, since ->
                onAddEdge(source, target, relation, description, since)
                edgeDialogVisible = false
            },
        )
    }
}

@Composable
private fun StoryEdgeDialog(
    items: List<StoryItem>,
    onDismiss: () -> Unit,
    onSave: (Long, Long, String, String, Int) -> Unit,
) {
    var sourceId by remember { mutableStateOf(items.first().id) }
    var targetId by remember { mutableStateOf(items.drop(1).first().id) }
    var relation by remember { mutableStateOf("同盟") }
    var description by remember { mutableStateOf("") }
    var sinceChapter by remember { mutableStateOf("1") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加图谱关系") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item { Text("来源", style = MaterialTheme.typography.labelMedium) }
                items(items, key = { "source-${it.id}" }) { item ->
                    if (sourceId == item.id) Button(onClick = { sourceId = item.id }) { Text(item.name) }
                    else TextButton(onClick = { sourceId = item.id }) { Text(item.name) }
                }
                item { Text("目标", style = MaterialTheme.typography.labelMedium) }
                items(items, key = { "target-${it.id}" }) { item ->
                    if (targetId == item.id) Button(onClick = { targetId = item.id }) { Text(item.name) }
                    else TextButton(onClick = { targetId = item.id }) { Text(item.name) }
                }
                item { OutlinedTextField(value = relation, onValueChange = { relation = it }, label = { Text("关系，例如同盟/敌对/师徒/持有") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("关系说明") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = sinceChapter, onValueChange = { sinceChapter = it.filter(Char::isDigit) }, label = { Text("自第几章起") }, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = { Button(onClick = { onSave(sourceId, targetId, relation, description, sinceChapter.toIntOrNull() ?: 1) }, enabled = sourceId != targetId && relation.isNotBlank()) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ReviewTab(
    chapter: Chapter?,
    issues: List<QualityIssue>,
    repairPlan: String?,
    config: ModelConfig,
    isGenerating: Boolean,
    onGenerateRepairPlan: () -> Unit,
    onMarkQualityRepaired: () -> Unit,
    onCancel: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("发布前检查", style = MaterialTheme.typography.headlineSmall)
            Text("全部是本地、可解释规则。提示不锁定创作，最终决定权始终在作者。", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            ActionHint("生成修复计划只给出修改建议，不会自动改写正文；确认已修复会清除本章的待修复状态。")
        }
        if (chapter?.qualityStatus == ChapterQualityStatus.NEEDS_REPAIR) {
            item {
                Button(onClick = onMarkQualityRepaired) { Text("确认已修复本章") }
            }
        }
        items(issues) { issue ->
            AuditRow(issue.severity == QualitySeverity.INFO, issue.title, issue.detail)
        }
        item {
            OutlinedButton(
                onClick = if (isGenerating) onCancel else onGenerateRepairPlan,
                enabled = isGenerating || (config.baseUrl.isNotBlank() && config.apiKey.isNotBlank() && config.model.isNotBlank()),
            ) {
                Icon(if (isGenerating) Icons.Outlined.Close else Icons.Outlined.Lightbulb, null)
                Spacer(Modifier.width(6.dp))
                Text(if (isGenerating) "取消生成" else "生成修复计划")
            }
        }
        repairPlan?.let { plan ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4DA))) {
                    Column(Modifier.padding(14.dp)) {
                        Text("最短修复计划", style = MaterialTheme.typography.titleSmall)
                        Text(plan, color = Ink, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE5F0ED))) {
                Text("可检查：篇幅、占位符、重复段落、保密设定与结尾收束。", modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AuditRow(pass: Boolean, title: String, detail: String) {
    Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (pass) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber, null, tint = if (pass) Green else Gold)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(detail, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ModelSettingsScreen(config: ModelConfig, onSave: (ModelConfig) -> Unit, onTest: (ModelConfig) -> Unit) {
    var baseUrl by remember(config) { mutableStateOf(config.baseUrl) }
    var apiKey by remember(config) { mutableStateOf(config.apiKey) }
    var model by remember(config) { mutableStateOf(config.model) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("模型连接", color = Red, style = MaterialTheme.typography.labelMedium)
            Text("只填连接信息", style = MaterialTheme.typography.headlineMedium)
            Text("这里没有提示词输入。API Key 通过 Android Keystore 加密保存，调用时由手机直连 Base URL。", color = SecondaryLabel, style = MaterialTheme.typography.bodySmall)
        }
        item {
            OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL（HTTPS）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("模型名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item {
            val current = ModelConfig(baseUrl = baseUrl, apiKey = apiKey, model = model)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSave(current) }) {
                    Icon(Icons.Outlined.Key, null)
                    Spacer(Modifier.width(6.dp))
                    Text("保存到本机")
                }
                OutlinedButton(onClick = { onTest(current) }) { Text("测试连接") }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4DA))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("内置网文工作流", style = MaterialTheme.typography.titleSmall)
                    Text("不需要填写提示词。续写、章节计划、场景分镜、文风提取、知识图谱和修复计划均使用内置模板，并自动带入当前章节、资料卡、大纲锚点与文风档案。", color = Ink, style = MaterialTheme.typography.bodySmall)
                    ActionHint("保存到本机：加密保存这三项连接信息；测试连接：只验证模型接口是否可用，不会生成任何小说内容。")
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE5F0ED))) {
                Column(Modifier.padding(14.dp)) {
                    Text("本地数据库", style = MaterialTheme.typography.titleSmall)
                    Text("novelcraft.db 保存作品、章节、资料与本地状态。离线时可以继续写作；联网后才调用大模型。", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CreateProjectDialog(onDismiss: () -> Unit, onCreate: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var premise by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建作品") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("书名") }, singleLine = true)
                OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("题材") }, singleLine = true)
                OutlinedTextField(value = premise, onValueChange = { premise = it }, label = { Text("一句话设定") }, minLines = 2)
            }
        },
        confirmButton = { Button(onClick = { onCreate(title, genre, premise) }) { Text("创建") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun StoryItemDialog(
    item: StoryItem? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
) {
    var kind by remember(item) { mutableStateOf(item?.kind ?: "人物") }
    var name by remember(item) { mutableStateOf(item?.name ?: "") }
    var detail by remember(item) { mutableStateOf(item?.detail ?: "") }
    var status by remember(item) { mutableStateOf(item?.status ?: StoryItemStatus.ACTIVE) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "添加资料" else "编辑资料") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = kind, onValueChange = { kind = it }, label = { Text("类型，例如人物/伏笔/时间线/禁区") })
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") })
                OutlinedTextField(value = detail, onValueChange = { detail = it }, label = { Text("状态或说明") }, minLines = 2)
                Text("资料状态", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(StoryItemStatus.ACTIVE, StoryItemStatus.RESOLVED, StoryItemStatus.SECRET).forEach { option ->
                        if (status == option) {
                            Button(onClick = { status = option }) { Text(option) }
                        } else {
                            TextButton(onClick = { status = option }) { Text(option) }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(kind, name, detail, status) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
