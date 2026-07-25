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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private val Ink = Color(0xFF20211E)
private val Paper = Color(0xFFF5F2EA)
private val Red = Color(0xFFB84536)
private val Teal = Color(0xFF28756B)
private val Green = Color(0xFF3D7A4B)
private val Gold = Color(0xFFB58322)

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
    val contextPacket by viewModel.contextPacket.collectAsStateWithLifecycle()
    val qualityIssues by viewModel.qualityIssues.collectAsStateWithLifecycle()
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
            surface = Color(0xFFFFFDF8),
            onBackground = Ink,
            onSurface = Ink,
        ),
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
                        onClick = { if (project != null) destination = MainDestination.WORKSPACE },
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
                                contextPacket = contextPacket,
                                qualityIssues = qualityIssues,
                                config = modelConfig,
                                isGenerating = viewModel.isGenerating.collectAsStateWithLifecycle().value,
                                onSelectChapter = viewModel::selectChapter,
                                onSaveChapter = viewModel::saveChapter,
                                onRenameChapter = viewModel::renameChapter,
                                onAddChapter = viewModel::addChapter,
                                onSaveChapterPlan = viewModel::saveChapterPlan,
                                onAddStoryItem = viewModel::addStoryItem,
                                onUpdateStoryItem = viewModel::updateStoryItem,
                                onGenerate = viewModel::generateContinuation,
                                onGeneratePlan = viewModel::generateChapterPlan,
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
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Paper),
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
    )
}

@Composable
private fun StatusMessage(text: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE5F0ED)),
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
            Text("LOCAL-FIRST NOVEL WORKSPACE", color = Red, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text("今天，写哪一章？", style = MaterialTheme.typography.headlineMedium)
            Text("作品存在本机 SQLite，模型密钥只保存在 Android Keystore。", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
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
    OutlinedButton(onClick = onClick, modifier = modifier.height(90.dp)) {
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, null, tint = Red)
            Spacer(Modifier.height(6.dp))
            Text(title)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
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
                Text(project.genre, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                if (project.premise.isNotBlank()) {
                    Text(project.premise, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
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
    contextPacket: ContextPacket,
    qualityIssues: List<QualityIssue>,
    config: ModelConfig,
    isGenerating: Boolean,
    onSelectChapter: (Long) -> Unit,
    onSaveChapter: (String) -> Unit,
    onRenameChapter: (String) -> Unit,
    onAddChapter: () -> Unit,
    onSaveChapterPlan: (String, Int) -> Unit,
    onAddStoryItem: (String, String, String, String) -> Unit,
    onUpdateStoryItem: (StoryItem, String, String, String, String) -> Unit,
    onGenerate: () -> Unit,
    onGeneratePlan: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            WorkspaceTab.entries.forEachIndexed { index, tab ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(tab.label) })
            }
        }
        when (WorkspaceTab.entries[selectedTab]) {
            WorkspaceTab.WRITE -> WriteTab(
                chapters, selectedChapter, contextPacket, config, isGenerating,
                onSelectChapter, onSaveChapter, onRenameChapter, onAddChapter, onGenerate,
            )
            WorkspaceTab.OUTLINE -> OutlineTab(project, chapters, selectedChapter, config, isGenerating, onSaveChapterPlan, onGeneratePlan)
            WorkspaceTab.RESOURCES -> ResourcesTab(storyItems, onAddStoryItem, onUpdateStoryItem)
            WorkspaceTab.REVIEW -> ReviewTab(qualityIssues)
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
    isGenerating: Boolean,
    onSelectChapter: (Long) -> Unit,
    onSave: (String) -> Unit,
    onRename: (String) -> Unit,
    onAddChapter: () -> Unit,
    onGenerate: () -> Unit,
) {
    if (chapter == null) {
        EmptyWorkspace()
        return
    }
    var draft by remember(chapter.id, chapter.content) { mutableStateOf(chapter.content) }
    var title by remember(chapter.id, chapter.title) { mutableStateOf(chapter.title) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ChapterRail(chapters, chapter.id, onSelectChapter, onAddChapter)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it; onRename(it) },
            label = { Text("第${chapter.number}章标题") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Text(draft.count { !it.isWhitespace() }.toString() + " 字 · 自动保存到 SQLite", color = Color.Gray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 8.dp))
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it; onSave(it) },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            label = { Text("小说正文") },
            minLines = 12,
        )
        ContextSummary(contextPacket)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onGenerate,
                enabled = !isGenerating && config.baseUrl.isNotBlank() && config.apiKey.isNotBlank() && config.model.isNotBlank(),
            ) {
                Icon(Icons.Outlined.Lightbulb, null)
                Spacer(Modifier.width(6.dp))
                Text(if (isGenerating) "续写中" else "AI 续写")
            }
            OutlinedButton(onClick = { onSave(draft) }) {
                Icon(Icons.Outlined.Save, null)
                Spacer(Modifier.width(6.dp))
                Text("保存")
            }
        }
        if (config.baseUrl.isBlank() || config.apiKey.isBlank() || config.model.isBlank()) {
            Text("请先在“我的”填写 Base URL、API Key 与模型名称。", color = Gold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun ChapterRail(chapters: List<Chapter>, selectedId: Long, onSelect: (Long) -> Unit, onAdd: () -> Unit) {
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
            IconButton(onClick = onAdd) { Icon(Icons.Outlined.Add, "新建章节") }
        }
    }
}

@Composable
private fun ContextSummary(packet: ContextPacket) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE5F0ED)),
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lightbulb, null, tint = Teal)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("本次续写上下文", style = MaterialTheme.typography.labelLarge)
                val names = packet.relevantItems.take(3).joinToString("、") { it.name }
                Text(
                    "${packet.relevantItems.size} 条设定 · ${packet.relevantChapters.size} 段章节摘录" + if (names.isBlank()) "" else "：$names",
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun OutlineTab(
    project: NovelProject,
    chapters: List<Chapter>,
    selectedChapter: Chapter?,
    config: ModelConfig,
    isGenerating: Boolean,
    onSavePlan: (String, Int) -> Unit,
    onGeneratePlan: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(project.genre + " · " + project.title, color = Red, style = MaterialTheme.typography.labelMedium)
            Text("章节大纲", style = MaterialTheme.typography.headlineSmall)
            if (project.premise.isNotBlank()) Text(project.premise, color = Color.Gray)
        }
        selectedChapter?.let { chapter ->
            item { ChapterPlanEditor(chapter, config, isGenerating, onSavePlan, onGeneratePlan) }
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
}

@Composable
private fun ChapterPlanEditor(
    chapter: Chapter,
    config: ModelConfig,
    isGenerating: Boolean,
    onSave: (String, Int) -> Unit,
    onGeneratePlan: () -> Unit,
) {
    var outline by remember(chapter.id, chapter.outline) { mutableStateOf(chapter.outline) }
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
                onClick = onGeneratePlan,
                enabled = !isGenerating && config.baseUrl.isNotBlank() && config.apiKey.isNotBlank() && config.model.isNotBlank(),
            ) {
                Icon(Icons.Outlined.Lightbulb, null)
                Spacer(Modifier.width(6.dp))
                Text(if (isGenerating) "生成中" else "AI 生成本章计划")
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ResourcesTab(
    items: List<StoryItem>,
    onAdd: (String, String, String, String) -> Unit,
    onUpdate: (StoryItem, String, String, String, String) -> Unit,
) {
    var dialogVisible by rememberSaveable { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<StoryItem?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("人物与伏笔", style = MaterialTheme.typography.headlineSmall)
                    Text("所有资料都会写入本机 SQLite。", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { dialogVisible = true }) { Icon(Icons.Outlined.Add, "添加资料") }
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
                            Text(item.detail, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
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
}

@Composable
private fun ReviewTab(issues: List<QualityIssue>) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("发布前检查", style = MaterialTheme.typography.headlineSmall)
            Text("全部是本地、可解释规则。提示不锁定创作，最终决定权始终在作者。", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        items(issues) { issue ->
            AuditRow(issue.severity == QualitySeverity.INFO, issue.title, issue.detail)
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
    var provider by remember(config) { mutableStateOf(config.provider) }
    var baseUrl by remember(config) { mutableStateOf(config.baseUrl) }
    var apiKey by remember(config) { mutableStateOf(config.apiKey) }
    var model by remember(config) { mutableStateOf(config.model) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("YOUR MODEL, YOUR KEY", color = Red, style = MaterialTheme.typography.labelMedium)
            Text("模型与本机存储", style = MaterialTheme.typography.headlineMedium)
            Text("API Key 通过 Android Keystore 加密保存，调用时由手机直连 Base URL，服务端不会保存密钥。", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        item {
            OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("服务商标识") }, modifier = Modifier.fillMaxWidth())
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
            val current = ModelConfig(provider, baseUrl, apiKey, model)
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
