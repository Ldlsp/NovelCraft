package com.mozhou.novelcraft

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Image as CoverImage
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Divider
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer

// ── iOS-style design tokens ────────────────────────────────────
// Background layers  (iOS systemGrouped hierarchy)
private val IosBackground    = Color(0xFFF2F2F7)   // systemGroupedBackground
private val IosSurface       = Color(0xFFFFFFFF)   // systemBackground (cards)
private val IosGroupedFill   = Color(0xFFEFEFF4)   // secondaryGroupedBackground
private val IosSeparator     = Color(0xFFE5E5EA)   // separator
// Text hierarchy  (iOS label scale)
private val IosLabel         = Color(0xFF1C1C1E)   // label
private val IosSecondLabel   = Color(0xFF8E8E93)   // secondaryLabel
// Brand palette  (朱砂 identity stays)
private val Brand            = Color(0xFFC8401A)   // Cinnabar brand accent
private val BrandTint        = Color(0xFFFAEAE3)   // brand mist
private val BrandTeal        = Color(0xFF3B7A72)   // Verdigris teal
private val BrandGold        = Color(0xFF9B6B1A)   // Studio gold
private val BrandGreen       = Color(0xFF2F6B4A)   // Green ink
// Semantic card surfaces  (lighter, iOS-toned)
private val CardBlue         = Color(0xFFEBF3FF)   // info
private val CardGreen        = Color(0xFFE8F5EE)   // success
private val CardMint         = Color(0xFFF0F9F4)   // calm
private val CardAmber        = Color(0xFFFFF8E6)   // warning
private val CardCream        = Color(0xFFFFFDF8)   // neutral
// ── Backward-compat aliases ───────────────────────────────────
private val Scroll           = IosBackground
private val PaperLight       = IosGroupedFill
private val PaperMid         = IosSeparator
private val SurfaceWhite     = IosSurface
private val InkDeep          = IosLabel
private val InkFaint         = IosSecondLabel
private val Cinnabar         = Brand
private val Verdigris        = BrandTeal
private val StudioGold       = BrandGold
private val GreenInk         = BrandGreen
private val MistWarm         = Color(0xFFFAEAE3)
private val MistGreenTint    = CardGreen
private val MistRed          = BrandTint
private val MistCool         = CardBlue
private val Ink              = IosLabel
private val Paper            = IosBackground
private val Teal             = BrandTeal
private val Red              = Brand
private val Gold             = BrandGold
private val Green            = BrandGreen
private val SecondaryLabel   = IosSecondLabel
private val MistBlue         = CardBlue
private val MistTeal         = CardGreen
private val MistCoral        = BrandTint
private const val SURPER_AI_URL = "https://surperai.top/"
// ── iOS-style shape system (Apple 采用 13.5dp 应用图标，卡片约 16dp) ───
private val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),    // chips
    small      = RoundedCornerShape(12.dp),   // small cards
    medium     = RoundedCornerShape(16.dp),   // list cards (iOS standard)
    large      = RoundedCornerShape(20.dp),   // dialogs, sheets
)
// ── iOS-scale typography (SF Pro 映射到 Sans Serif，标题放大) ───────
private val StudioTypography = Typography(
    displayLarge   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,    fontSize = 34.sp, lineHeight = 41.sp),  // iOS Large Title
    displayMedium  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,    fontSize = 28.sp, lineHeight = 34.sp),  // iOS Title 1
    displaySmall   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,fontSize = 22.sp, lineHeight = 28.sp),  // iOS Title 2
    headlineLarge  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,fontSize = 20.sp, lineHeight = 25.sp),  // iOS Title 3
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,fontSize = 17.sp, lineHeight = 22.sp),  // iOS Headline
    headlineSmall  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,fontSize = 15.sp, lineHeight = 20.sp),
    titleLarge     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,fontSize = 17.sp, lineHeight = 22.sp),  // iOS Body (emphasized)
    titleMedium    = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,fontSize = 15.sp, lineHeight = 20.sp),
    titleSmall     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,  fontSize = 13.sp, lineHeight = 18.sp),
    bodyLarge      = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,  fontSize = 17.sp, lineHeight = 22.sp),  // iOS Body
    bodyMedium     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,  fontSize = 15.sp, lineHeight = 20.sp),
    bodySmall      = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,  fontSize = 13.sp, lineHeight = 18.sp),  // iOS Callout
    labelLarge     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,  fontSize = 13.sp),                       // iOS Footnote
    labelMedium    = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,  fontSize = 12.sp),                       // iOS Caption 1
    labelSmall     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,  fontSize = 11.sp, letterSpacing = 0.3.sp), // iOS Caption 2
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        window.statusBarColor = AndroidColor.rgb(242, 242, 247)       // IosBackground
        window.navigationBarColor = AndroidColor.rgb(242, 242, 247)   // IosBackground
        setContent { NovelCraftApp() }
    }
}

private enum class MainDestination { SHELF, WORKSPACE, SETTINGS }
private enum class WorkspaceTab(val label: String) { WRITE("写作"), PROJECT("作品"), OUTLINE("大纲"), RESOURCES("资料"), REVIEW("审核") }

@Composable
private fun NovelCraftApp(viewModel: NovelViewModel = viewModel()) {
    val context = LocalContext.current
    val uiPreferences = remember { context.getSharedPreferences("novelcraft_ui", Context.MODE_PRIVATE) }
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val project by viewModel.selectedProject.collectAsStateWithLifecycle()
    val importAnalysis by viewModel.importAnalysis.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val selectedChapter by viewModel.selectedChapter.collectAsStateWithLifecycle()
    val latestRevision by viewModel.latestRevision.collectAsStateWithLifecycle()
    val latestEditorialReview by viewModel.latestEditorialReview.collectAsStateWithLifecycle()
    val resumableAutoWriteRun by viewModel.resumableAutoWriteRun.collectAsStateWithLifecycle()
    val researchNotes by viewModel.researchNotes.collectAsStateWithLifecycle()
    val onlineResearchResults by viewModel.onlineResearchResults.collectAsStateWithLifecycle()
    val isOnlineResearching by viewModel.isOnlineResearching.collectAsStateWithLifecycle()
    val storyItems by viewModel.storyItems.collectAsStateWithLifecycle()
    val anchors by viewModel.anchors.collectAsStateWithLifecycle()
    val edges by viewModel.edges.collectAsStateWithLifecycle()
    val contextPacket by viewModel.contextPacket.collectAsStateWithLifecycle()
    val streamedContinuation by viewModel.streamedContinuation.collectAsStateWithLifecycle()
    val qualityIssues by viewModel.qualityIssues.collectAsStateWithLifecycle()
    val repairPlan by viewModel.repairPlan.collectAsStateWithLifecycle()
    val projectProfileSuggestion by viewModel.projectProfileSuggestion.collectAsStateWithLifecycle()
    val ideationDraft by viewModel.ideationDraft.collectAsStateWithLifecycle()
    val styleProfiles by viewModel.styleProfiles.collectAsStateWithLifecycle()
    val pacingEvents by viewModel.pacingEvents.collectAsStateWithLifecycle()
    val batchReviewRuns by viewModel.batchReviewRuns.collectAsStateWithLifecycle()
    val reviewIssues by viewModel.reviewIssues.collectAsStateWithLifecycle()
    val modelConfig by viewModel.modelConfig.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val generationTasks by viewModel.generationTasks.collectAsStateWithLifecycle()
    LaunchedEffect(message) {
        message?.let { text ->
            Toast.makeText(context.applicationContext, text, Toast.LENGTH_LONG).show()
            viewModel.clearMessage(text)
        }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(importAnalysis?.status, generationTasks.isNotEmpty()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            (importAnalysis?.status in setOf(ImportAnalysisStatus.QUEUED, ImportAnalysisStatus.RUNNING) || generationTasks.isNotEmpty()) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    var destination by rememberSaveable { mutableStateOf(MainDestination.SHELF) }
    var workspaceTab by rememberSaveable { mutableIntStateOf(0) }
    var createProjectVisible by rememberSaveable { mutableStateOf(false) }
    var ideationVisible by rememberSaveable { mutableStateOf(false) }
    var onboardingStep by rememberSaveable { mutableIntStateOf(if (uiPreferences.getBoolean("onboarding_seen", false)) -1 else 0) }
    var exportDialogVisible by rememberSaveable { mutableStateOf(false) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importDocument(uri)
            destination = MainDestination.WORKSPACE
        }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) viewModel.exportDocument(uri)
    }
    val docxExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) { uri -> uri?.let(viewModel::exportDocx) }
    val epubExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/epub+zip")) { uri -> uri?.let(viewModel::exportEpub) }
    val pdfExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri -> uri?.let(viewModel::exportPdf) }
    val backupExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(viewModel::exportProjectBackup) }
    val backupImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::restoreProjectBackup) }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary          = Brand,
            onPrimary        = IosSurface,
            secondary        = BrandTeal,
            onSecondary      = IosSurface,
            tertiary         = BrandGold,
            background       = IosBackground,
            surface          = IosSurface,
            onBackground     = IosLabel,
            onSurface        = IosLabel,
            surfaceVariant   = IosGroupedFill,
            outline          = IosSeparator,
            outlineVariant   = Color(0xFFD1D1D6),
        ),
        shapes = StudioShapes,
        typography = StudioTypography,
    ) {
        Scaffold(
            containerColor = IosBackground,
            topBar = {
                Column {
                    AppTopBar(
                        destination = destination,
                        projectTitle = project?.title,
                        onBack = { destination = MainDestination.SHELF },
                        onExport = { if (project != null) exportDialogVisible = true },
                    )
                    if (destination == MainDestination.WORKSPACE && project != null) {
                        WorkspaceTabStrip(
                            selectedTab = workspaceTab,
                            onSelect = { workspaceTab = it },
                        )
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = IosSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(width = 0.5.dp, color = IosSeparator, shape = RoundedCornerShape(0.dp)),
                ) {
                    listOf(
                        Triple(MainDestination.SHELF, Icons.Outlined.AutoStories, "书架"),
                        Triple(MainDestination.WORKSPACE, Icons.Outlined.MenuBook, "写作"),
                        Triple(MainDestination.SETTINGS, Icons.Outlined.Settings, "我的"),
                    ).forEach { (dest, icon, label) ->
                        val selected = destination == dest
                        val scale by animateFloatAsState(
                            targetValue = if (selected) 1.08f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            label = "navScale",
                        )
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (dest == MainDestination.WORKSPACE && project == null) ideationVisible = true
                                else destination = dest
                            },
                            icon = {
                                Icon(
                                    icon,
                                    label,
                                    modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale),
                                )
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            alwaysShowLabel = true,
                            colors = studioNavigationColors(),
                        )
                    }
                }
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                when (destination) {
                    MainDestination.SHELF -> ShelfScreen(
                        projects = projects,
                        onCreate = { ideationVisible = true },
                        onImport = { importer.launch(arrayOf("text/plain", "text/markdown", "text/*", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/epub+zip", "application/pdf")) },
                        onIdeate = { ideationVisible = true },
                        onRestore = { backupImporter.launch(arrayOf("application/json", "text/json")) },
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
                                latestRevision = latestRevision,
                                latestEditorialReview = latestEditorialReview,
                                resumableAutoWriteRun = resumableAutoWriteRun,
                                researchNotes = researchNotes,
                                storyItems = storyItems,
                                anchors = anchors,
                                edges = edges,
                                contextPacket = contextPacket,
                                streamedContinuation = streamedContinuation,
                                qualityIssues = qualityIssues,
                                repairPlan = repairPlan,
                                config = modelConfig,
                                importAnalysis = importAnalysis,
                                activeTasks = generationTasks,
                                selectedTab = workspaceTab,
                                onSelectChapter = viewModel::selectChapter,
                                onSaveChapter = viewModel::saveChapter,
                                onRenameChapter = viewModel::renameChapter,
                                onAddChapter = viewModel::addChapter,
                                onDeleteChapter = viewModel::deleteCurrentChapter,
                                onDeleteProject = viewModel::deleteCurrentProject,
                                onSaveProjectProfile = viewModel::saveProjectProfile,
                                onSaveWritingPolicy = viewModel::saveWritingPolicy,
                                onGenerateCover = viewModel::generateCover,
                                onUploadCover = viewModel::importCover,
                                onExport = { exportDialogVisible = true },
                                onBackup = { project?.let { backupExporter.launch("${it.title}-完整备份.json") } },
                                styleProfiles = styleProfiles,
                                pacingEvents = pacingEvents,
                                eventMatrixRules = viewModel.eventMatrixRules.collectAsStateWithLifecycle().value,
                                batchReviewRuns = batchReviewRuns,
                                reviewIssues = reviewIssues,
                                gateReports = viewModel.gateReports.collectAsStateWithLifecycle().value,
                                aiTraceReport = viewModel.aiTraceReport(),
                                researchPlan = viewModel.researchPlan(),
                                onlineResearchResults = onlineResearchResults,
                                isOnlineResearching = isOnlineResearching,
                                profileSuggestion = projectProfileSuggestion,
                                onGenerateProjectProfile = viewModel::generateProjectProfile,
                                onStartImportAnalysis = viewModel::startImportAnalysis,
                                onCancelImportAnalysis = viewModel::cancelImportAnalysis,
                                onSaveLongFormBlueprint = viewModel::saveLongFormBlueprint,
                                onGenerateLongFormBlueprint = viewModel::generateLongFormBlueprint,
                                onSavePacing = viewModel::savePacing,
                                onSaveChapterPlan = viewModel::saveChapterPlan,
                                onSaveBeatSheet = viewModel::saveBeatSheet,
                                onSaveStyleGuide = viewModel::saveStyleGuide,
                                onAddStoryItem = viewModel::addStoryItem,
                                onUpdateStoryItem = viewModel::updateStoryItem,
                                onAddAnchor = viewModel::addAnchor,
                                onAddEdge = viewModel::addEdge,
                                onExtractMemory = viewModel::extractMemoryFromCurrentChapter,
                                onAddResearchNote = viewModel::addResearchNote,
                                onUpdateResearchNote = viewModel::updateResearchNote,
                                onDeleteResearchNote = viewModel::deleteResearchNote,
                                onAnalyzeReference = viewModel::analyzeReferenceStructure,
                                onSearchOnlineResearch = viewModel::searchOnlineResearch,
                                onCollectOnlineResearch = viewModel::collectOnlineResearch,
                                onClearOnlineResearch = viewModel::clearOnlineResearchResults,
                                onGenerate = viewModel::generateContinuation,
                                onAutoWrite = viewModel::autoWriteChapters,
                                onResumeAutoWrite = viewModel::resumeAutoWrite,
                                onGeneratePlan = viewModel::generateChapterPlan,
                                onGenerateBeatSheet = viewModel::generateBeatSheet,
                                onExtractStyleGuide = viewModel::extractStyleGuideFromCurrentChapter,
                                onCancelGeneration = viewModel::cancelGeneration,
                                onGenerateRepairPlan = viewModel::generateRepairPlan,
                                onMarkQualityRepaired = viewModel::markCurrentChapterQualityRepaired,
                                onRetryLifecycle = viewModel::retryCurrentChapterLifecycle,
                                onRewriteChapter = viewModel::rewriteCurrentChapterForGate,
                                onHumanizeChapter = viewModel::humanizeCurrentChapter,
                                onRestoreRevision = viewModel::restoreLatestRevision,
                                onGenerateEditorialReview = viewModel::generateEditorialReview,
                                onRunEditorialTeam = viewModel::runEditorialTeamReview,
                                onReviseOutline = viewModel::reviseOutline,
                                onResolveOutlineCascade = viewModel::resolveOutlineCascade,
                                onSavePacingEvent = viewModel::savePacingEvent,
                                onSaveEventMatrixRule = viewModel::saveEventMatrixRule,
                                onAddEventMatrixRule = viewModel::addEventMatrixRule,
                                onDeleteEventMatrixRule = viewModel::deleteEventMatrixRule,
                                pacingRecommendation = viewModel.pacingRecommendation(),
                                onSaveStyleProfile = viewModel::saveCurrentStyleProfile,
                                onApplyStyleProfile = viewModel::applyStyleProfile,
                                onDeleteStyleProfile = viewModel::deleteStyleProfile,
                                onBatchReview = viewModel::generateBatchEditorialReview,
                                onSetReviewIssueResolved = viewModel::setReviewIssueResolved,
                            )
                        }
                    }
                    MainDestination.SETTINGS -> ModelSettingsScreen(
                        config = modelConfig,
                        onSave = viewModel::saveModelConfig,
                        onTest = viewModel::testModelConfig,
                        onTestImage = viewModel::testImageModelConfig,
                        onShowGuide = { onboardingStep = 0 },
                        onOpenModelGateway = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SURPER_AI_URL))) }
                                .onFailure { viewModel.clearMessage() }
                        },
                    )
                }
            }
        }
        if (exportDialogVisible && project != null) {
            ExportFormatDialog(
                onDismiss = { exportDialogVisible = false },
                onMarkdown = { exporter.launch("${project!!.title}.md"); exportDialogVisible = false },
                onDocx = { docxExporter.launch("${project!!.title}.docx"); exportDialogVisible = false },
                onEpub = { epubExporter.launch("${project!!.title}.epub"); exportDialogVisible = false },
                onPdf = { pdfExporter.launch("${project!!.title}.pdf"); exportDialogVisible = false },
            )
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
        if (ideationVisible) {
            IdeationWizard(
                draft = ideationDraft,
                canUseAi = modelConfig.baseUrl.isNotBlank() && modelConfig.apiKey.isNotBlank() && modelConfig.model.isNotBlank(),
                isGenerating = GenerationTask.PROJECT_PROFILE in generationTasks,
                onDismiss = { ideationVisible = false },
                onSave = viewModel::saveIdeationDraft,
                onGenerate = viewModel::generateGuidedIdeation,
                onCancelGeneration = { viewModel.cancelGeneration(GenerationTask.PROJECT_PROFILE) },
                onFinish = { draft ->
                    if (viewModel.finishIdeationDraft(draft)) {
                        ideationVisible = false
                        destination = MainDestination.WORKSPACE
                    }
                },
            )
        }
        if (onboardingStep >= 0) {
            OnboardingDialog(
                step = onboardingStep,
                onDismiss = {
                    uiPreferences.edit().putBoolean("onboarding_seen", true).apply()
                    onboardingStep = -1
                },
                onNext = { onboardingStep += 1 },
                onOpenModelGateway = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SURPER_AI_URL))) },
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
        MainDestination.SHELF     -> "墨舟"
        MainDestination.WORKSPACE -> projectTitle ?: "创作空间"
        MainDestination.SETTINGS  -> "我的"
    }
    Column {
        TopAppBar(
            title = {
                Text(
                    title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            navigationIcon = {
                if (destination == MainDestination.WORKSPACE) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.KeyboardArrowLeft, "返回书架", modifier = Modifier.size(28.dp))
                    }
                }
            },
            actions = {
                if (destination == MainDestination.WORKSPACE) {
                    IconButton(onClick = onExport) {
                        Icon(Icons.Outlined.FileDownload, "导出作品")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor             = IosSurface,
                titleContentColor          = IosLabel,
                navigationIconContentColor = Brand,
                actionIconContentColor     = Brand,
            ),
        )
        Divider(color = IosSeparator, thickness = 0.5.dp)
    }
}

// Workspace tab strip: icon + label + Brand underline indicator
@Composable
private fun WorkspaceTabStrip(selectedTab: Int, onSelect: (Int) -> Unit) {
    // One icon per tab, in WorkspaceTab.entries order
    val tabIcons = listOf(
        Icons.Outlined.Edit,         // 写作
        Icons.Outlined.Book,         // 作品
        Icons.Outlined.Description,  // 大纲
        Icons.Outlined.Lightbulb,    // 资料
        Icons.Outlined.CheckCircle,  // 审核
    )
    Column(Modifier.fillMaxWidth().background(IosSurface)) {
        Row(Modifier.fillMaxWidth()) {
            WorkspaceTab.entries.forEachIndexed { i, tab ->
                val selected = selectedTab == i
                val contentColor by animateColorAsState(
                    targetValue = if (selected) Brand else IosSecondLabel,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "tabColor",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(i) }
                        .padding(top = 10.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        tabIcons[i],
                        contentDescription = tab.label,
                        modifier = Modifier.size(20.dp),
                        tint = contentColor,
                    )
                    Text(
                        tab.label,
                        color = contentColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
        // Per-tab underline indicator animated in/out
        Row(Modifier.fillMaxWidth().height(2.dp)) {
            WorkspaceTab.entries.forEachIndexed { i, _ ->
                val indicatorColor by animateColorAsState(
                    targetValue = if (selectedTab == i) Brand else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "indicator",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 14.dp)
                        .background(indicatorColor, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                )
            }
        }
        Divider(color = IosSeparator, thickness = 0.5.dp)
    }
}

@Composable
private fun studioNavigationColors() = NavigationBarItemDefaults.colors(
    selectedIconColor   = Brand,
    selectedTextColor   = Brand,
    indicatorColor      = BrandTint,
    unselectedIconColor = IosSecondLabel,
    unselectedTextColor = IosSecondLabel,
)

@Composable
private fun ShelfScreen(
    projects: List<NovelProject>,
    onCreate: () -> Unit,
    onImport: () -> Unit,
    onIdeate: () -> Unit,
    onRestore: () -> Unit,
    onOpen: (NovelProject) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(IosBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
    ) {
        // Hero action area
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IosSurface)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Primary action
                Button(
                    onClick = onCreate,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Brand),
                ) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("新建作品", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                // Secondary quick-action icons (iOS home-screen style)
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) {
                    listOf(
                        Triple(Icons.Outlined.FileOpen, "导入文件", onImport),
                        Triple(Icons.Outlined.Lightbulb, "AI 灵感", onIdeate),
                        Triple(Icons.Outlined.SettingsBackupRestore, "恢复备份", onRestore),
                    ).forEach { (icon, label, action) ->
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { action() }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(IosGroupedFill, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(icon, null, modifier = Modifier.size(22.dp), tint = Brand)
                            }
                            Text(label, style = MaterialTheme.typography.labelSmall, color = IosSecondLabel)
                        }
                    }
                }
            }
            Divider(color = IosSeparator, thickness = 0.5.dp)
        }

        // Section header
        item {
            Text(
                if (projects.isEmpty()) "开始写作" else "最近作品",
                style = MaterialTheme.typography.labelMedium,
                color = IosSecondLabel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }

        if (projects.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .background(IosSurface, RoundedCornerShape(16.dp))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Outlined.AutoStories, null, tint = Brand, modifier = Modifier.size(40.dp))
                    Text("还没有作品", style = MaterialTheme.typography.titleMedium, color = IosLabel)
                    Text(
                        "从一个灵感开始，或导入已有文件继续写。",
                        color = IosSecondLabel,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            // White card grouped list
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .background(IosSurface, RoundedCornerShape(16.dp))
                        .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
                ) {
                    projects.forEachIndexed { index, project ->
                        ProjectShelfItem(project, Modifier.fillMaxWidth(), onOpen)
                        if (index < projects.lastIndex) {
                            Divider(
                                color = IosSeparator,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 96.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Returns a human-readable relative time string (Chinese) for a timestamp. */
private fun relativeTime(updatedAt: Long): String {
    val delta = System.currentTimeMillis() - updatedAt
    return when {
        delta < 60_000L          -> "刚刚"
        delta < 3_600_000L       -> "${delta / 60_000}分钟前"
        delta < 86_400_000L      -> "${delta / 3_600_000}小时前"
        delta < 7 * 86_400_000L  -> "${delta / 86_400_000}天前"
        else -> {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = updatedAt
            "${cal.get(java.util.Calendar.MONTH) + 1}月${cal.get(java.util.Calendar.DAY_OF_MONTH)}日"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectShelfItem(project: NovelProject, modifier: Modifier, onOpen: (NovelProject) -> Unit) {
    Row(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onOpen(project) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProjectCover(project, Modifier.width(56.dp).aspectRatio(0.68f))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (project.genre.isNotBlank()) {
                Text(
                    project.genre,
                    color = Brand,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.5.sp,
                )
            }
            Text(
                project.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = IosLabel,
            )
            if (project.summary.isNotBlank()) {
                Text(
                    project.summary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = IosSecondLabel,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        // Time + chevron column on the right
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                relativeTime(project.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = IosSecondLabel,
            )
            Icon(
                Icons.Outlined.KeyboardArrowRight,
                null,
                tint = Color(0xFFC7C7CC),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ProjectCover(project: NovelProject, modifier: Modifier = Modifier) {
    val bitmap = remember(project.coverPath) {
        project.coverPath.takeIf { it.isNotBlank() }?.let { path -> BitmapFactory.decodeFile(path)?.asImageBitmap() }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = "${project.title}封面", contentScale = ContentScale.Crop, modifier = modifier.clip(RoundedCornerShape(10.dp)))
    } else {
        val (bgColor, fgColor) = when ((project.id % 5).toInt()) {
            0    -> Color(0xFFE8DFD0) to Color(0xFF6B5030)
            1    -> Color(0xFFD8E4DE) to Color(0xFF2E5C4A)
            2    -> Color(0xFFE8D8D0) to Color(0xFF6B3A2A)
            3    -> Color(0xFFD8DCE8) to Color(0xFF3A4870)
            else -> Color(0xFFEADFF0) to Color(0xFF5A3A70)
        }
        Box(
            modifier = modifier.clip(RoundedCornerShape(10.dp)).background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            ) {
                Text(
                    project.title.firstOrNull()?.toString() ?: "书",
                    color = fgColor,
                    style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp),
                )
                if (project.genre.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(project.genre.take(4), color = fgColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun EmptyWorkspace() {
    Column(
        modifier = Modifier.fillMaxSize().background(IosBackground).padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(BrandTint, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.MenuBook, null, tint = Brand, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("打开一部作品", style = MaterialTheme.typography.headlineMedium, color = IosLabel, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("从书架选择作品，或新建一个故事。", style = MaterialTheme.typography.bodyMedium, color = IosSecondLabel, textAlign = TextAlign.Center)
    }
}

@Composable
private fun WorkspaceScreen(
    project: NovelProject,
    chapters: List<Chapter>,
    selectedChapter: Chapter?,
    latestRevision: ChapterRevision?,
    latestEditorialReview: EditorialReview?,
    resumableAutoWriteRun: AutoWriteRun?,
    researchNotes: List<ResearchNote>,
    storyItems: List<StoryItem>,
    anchors: List<StoryAnchor>,
    edges: List<StoryEdge>,
    contextPacket: ContextPacket,
    streamedContinuation: String,
    qualityIssues: List<QualityIssue>,
    repairPlan: String?,
    config: ModelConfig,
    importAnalysis: ImportAnalysisRun?,
    activeTasks: Set<GenerationTask>,
    selectedTab: Int,
    onSelectChapter: (Long) -> Unit,
    onSaveChapter: (String) -> Unit,
    onRenameChapter: (String) -> Unit,
    onAddChapter: (NextChapterAction) -> Unit,
    onDeleteChapter: () -> Unit,
    onDeleteProject: () -> Unit,
    onSaveProjectProfile: (String, String, String, String, String, String, String) -> Unit,
    onSaveWritingPolicy: (String, String, Int, Int) -> Unit,
    onGenerateCover: () -> Unit,
    onUploadCover: (android.net.Uri) -> Unit,
    onExport: () -> Unit,
    onBackup: () -> Unit,
    styleProfiles: List<StyleProfile>,
    pacingEvents: List<ChapterPacingEvent>,
    eventMatrixRules: List<EventMatrixRule>,
    batchReviewRuns: List<BatchReviewRun>,
    reviewIssues: List<ReviewIssue>,
    gateReports: List<ChapterGateReport>,
    aiTraceReport: AiTraceReport,
    researchPlan: ResearchPlan?,
    onlineResearchResults: List<OnlineResearchResult>,
    isOnlineResearching: Boolean,
    profileSuggestion: ProjectProfileSuggestion?,
    onGenerateProjectProfile: () -> Unit,
    onStartImportAnalysis: () -> Unit,
    onCancelImportAnalysis: () -> Unit,
    onSaveLongFormBlueprint: (String) -> Unit,
    onGenerateLongFormBlueprint: () -> Unit,
    onSavePacing: (Int, Int, String) -> Unit,
    onSaveChapterPlan: (String, Int) -> Unit,
    onSaveBeatSheet: (String) -> Unit,
    onSaveStyleGuide: (String) -> Unit,
    onAddStoryItem: (String, String, String, String) -> Unit,
    onUpdateStoryItem: (StoryItem, String, String, String, String) -> Unit,
    onAddAnchor: (Int, Int, String, String, String, String, String) -> Unit,
    onAddEdge: (Long, Long, String, String, Int) -> Unit,
    onExtractMemory: () -> Unit,
    onAddResearchNote: (String, String, String, String, Boolean) -> Unit,
    onUpdateResearchNote: (ResearchNote, String, String, String, String, Boolean) -> Unit,
    onDeleteResearchNote: (ResearchNote) -> Unit,
    onAnalyzeReference: (ResearchNote) -> Unit,
    onSearchOnlineResearch: (String) -> Unit,
    onCollectOnlineResearch: (OnlineResearchResult) -> Unit,
    onClearOnlineResearch: () -> Unit,
    onGenerate: (String) -> Unit,
    onAutoWrite: (Int) -> Unit,
    onResumeAutoWrite: () -> Unit,
    onGeneratePlan: () -> Unit,
    onGenerateBeatSheet: () -> Unit,
    onExtractStyleGuide: () -> Unit,
    onCancelGeneration: (GenerationTask) -> Unit,
    onGenerateRepairPlan: () -> Unit,
    onMarkQualityRepaired: () -> Unit,
    onRetryLifecycle: () -> Unit,
    onRewriteChapter: () -> Unit,
    onHumanizeChapter: () -> Unit,
    onRestoreRevision: () -> Unit,
    onGenerateEditorialReview: () -> Unit,
    onRunEditorialTeam: () -> Unit,
    onReviseOutline: (Int, String) -> Unit,
    onResolveOutlineCascade: () -> Unit,
    onSavePacingEvent: (String, String, String) -> Unit,
    onSaveEventMatrixRule: (EventMatrixRule) -> Unit,
    onAddEventMatrixRule: (String, Int, String) -> Unit,
    onDeleteEventMatrixRule: (EventMatrixRule) -> Unit,
    pacingRecommendation: PacingRecommendation?,
    onSaveStyleProfile: (String) -> Unit,
    onApplyStyleProfile: (StyleProfile) -> Unit,
    onDeleteStyleProfile: (StyleProfile) -> Unit,
    onBatchReview: (Int, Int) -> Unit,
    onSetReviewIssueResolved: (ReviewIssue, Boolean) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        ImportAnalysisStatusStrip(
            run = importAnalysis,
            isLegacyPendingImport = project.genre == "待 AI 分析",
            onStart = onStartImportAnalysis,
            onCancel = onCancelImportAnalysis,
        )
        when (WorkspaceTab.entries[selectedTab]) {
            WorkspaceTab.WRITE -> WriteTab(
                chapters, selectedChapter, contextPacket, streamedContinuation, config, activeTasks, resumableAutoWriteRun, project.automationLevel, pacingRecommendation,
                onSelectChapter, onSaveChapter, onRenameChapter, onAddChapter, onDeleteChapter, onGenerate, onAutoWrite, onResumeAutoWrite, onCancelGeneration,
            )
            WorkspaceTab.PROJECT -> ProjectTab(
                project = project,
                isCoverGenerating = GenerationTask.COVER in activeTasks,
                isProfileGenerating = GenerationTask.PROJECT_PROFILE in activeTasks,
                isBlueprintGenerating = GenerationTask.LONG_FORM_BLUEPRINT in activeTasks,
                hasImageConfig = config.imageBaseUrl.isNotBlank() && config.imageApiKey.isNotBlank() && config.imageModel.isNotBlank(),
                hasTextConfig = config.baseUrl.isNotBlank() && config.apiKey.isNotBlank() && config.model.isNotBlank(),
                profileSuggestion = profileSuggestion,
                onSaveProfile = onSaveProjectProfile,
                onSaveWritingPolicy = onSaveWritingPolicy,
                onGenerateCover = onGenerateCover,
                onUploadCover = onUploadCover,
                onExport = onExport,
                onBackup = onBackup,
                onCancelCover = { onCancelGeneration(GenerationTask.COVER) },
                onGenerateProfile = onGenerateProjectProfile,
                onCancelProfile = { onCancelGeneration(GenerationTask.PROJECT_PROFILE) },
                onSaveBlueprint = onSaveLongFormBlueprint,
                onGenerateBlueprint = onGenerateLongFormBlueprint,
                onCancelBlueprint = { onCancelGeneration(GenerationTask.LONG_FORM_BLUEPRINT) },
                onSavePacing = onSavePacing,
                onDeleteProject = onDeleteProject,
            )
            WorkspaceTab.OUTLINE -> OutlineTab(project, chapters, selectedChapter, anchors, config, activeTasks, onSaveChapterPlan, onSaveBeatSheet, onSaveStyleGuide, onGeneratePlan, onGenerateBeatSheet, onExtractStyleGuide, onCancelGeneration, onAddAnchor, onReviseOutline, onResolveOutlineCascade, pacingEvents, eventMatrixRules, pacingRecommendation, onSavePacingEvent, onSaveEventMatrixRule, onAddEventMatrixRule, onDeleteEventMatrixRule, styleProfiles, onSaveStyleProfile, onApplyStyleProfile, onDeleteStyleProfile)
            WorkspaceTab.RESOURCES -> ResourcesTab(storyItems, edges, researchNotes, researchPlan, onlineResearchResults, isOnlineResearching, GenerationTask.MEMORY_EXTRACTION in activeTasks, GenerationTask.REFERENCE_ANALYSIS in activeTasks, onAddStoryItem, onUpdateStoryItem, onAddEdge, onExtractMemory, onAddResearchNote, onUpdateResearchNote, onDeleteResearchNote, onAnalyzeReference, onSearchOnlineResearch, onCollectOnlineResearch, onClearOnlineResearch, { onCancelGeneration(GenerationTask.MEMORY_EXTRACTION) }, { onCancelGeneration(GenerationTask.REFERENCE_ANALYSIS) })
            WorkspaceTab.REVIEW -> ReviewTab(
                selectedChapter,
                latestRevision,
                latestEditorialReview,
                qualityIssues,
                repairPlan,
                config,
                GenerationTask.REPAIR_PLAN in activeTasks,
                GenerationTask.CHAPTER_LIFECYCLE in activeTasks,
                onGenerateRepairPlan,
                onMarkQualityRepaired,
                onRetryLifecycle,
                onRewriteChapter,
                onHumanizeChapter,
                onRestoreRevision,
                GenerationTask.EDITORIAL_REVIEW in activeTasks,
                onGenerateEditorialReview,
                { onCancelGeneration(GenerationTask.EDITORIAL_REVIEW) },
                GenerationTask.EDITORIAL_TEAM in activeTasks,
                onRunEditorialTeam,
                { onCancelGeneration(GenerationTask.EDITORIAL_TEAM) },
                { onCancelGeneration(GenerationTask.REPAIR_PLAN) },
                { onCancelGeneration(GenerationTask.CHAPTER_LIFECYCLE) },
                chapters, batchReviewRuns, reviewIssues, gateReports, aiTraceReport, GenerationTask.BATCH_REVIEW in activeTasks, onBatchReview, onSetReviewIssueResolved, { onCancelGeneration(GenerationTask.BATCH_REVIEW) },
            )
        }
    }
}

@Composable
private fun WorkspaceContextStrip(project: NovelProject, chapter: Chapter?, tab: WorkspaceTab, chapterCount: Int) {
    Row(
        Modifier.fillMaxWidth().background(IosGroupedFill).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(tab.label, color = Brand, style = MaterialTheme.typography.labelLarge)
            Text(chapter?.let { "第 ${it.number} 章 · ${it.title}" } ?: "共 $chapterCount 章", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(if (project.automationLevel == "自动推进") "自动推进" else "作者掌控", color = IosSecondLabel, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StudioPageHeader(title: String, detail: String, eyebrow: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(eyebrow, color = BrandTeal, style = MaterialTheme.typography.labelLarge)
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(detail, color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ImportAnalysisStatusStrip(
    run: ImportAnalysisRun?,
    isLegacyPendingImport: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    if (run == null && !isLegacyPendingImport) return
    val status = run?.status
    val isRunning = status in setOf(ImportAnalysisStatus.QUEUED, ImportAnalysisStatus.RUNNING)
    val label = when (status) {
        ImportAnalysisStatus.QUEUED -> "导入分析排队中"
        ImportAnalysisStatus.RUNNING -> run.stage
        ImportAnalysisStatus.WAITING_FOR_CONFIG -> "需要模型配置"
        ImportAnalysisStatus.WAITING_FOR_NETWORK -> "等待网络恢复"
        ImportAnalysisStatus.COMPLETED -> "导入分析已完成"
        ImportAnalysisStatus.FAILED -> "导入分析失败"
        ImportAnalysisStatus.CANCELLED -> "导入分析已取消"
        else -> "导入正文待分析"
    }
    val detail = run?.detail?.ifBlank { "正在准备分析" } ?: "已导入正文，可提炼作品资料和文风"
    Row(
        modifier = Modifier.fillMaxWidth().background(if (status == ImportAnalysisStatus.FAILED) Color(0xFFFFF1EE) else IosGroupedFill).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(if (status == ImportAnalysisStatus.COMPLETED) Icons.Outlined.CloudDone else Icons.Outlined.Description, null, tint = if (status == ImportAnalysisStatus.FAILED) Brand else BrandTeal)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(if (run != null) "${run.progress}% · $detail" else detail, color = IosSecondLabel, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (isRunning) {
            IconButton(onClick = onCancel) { Icon(Icons.Outlined.Close, "取消后台导入分析") }
        } else {
            TextButton(onClick = onStart) { Text(if (status == ImportAnalysisStatus.COMPLETED) "重新分析" else "开始") }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ProjectTab(
    project: NovelProject,
    isCoverGenerating: Boolean,
    isProfileGenerating: Boolean,
    isBlueprintGenerating: Boolean,
    hasImageConfig: Boolean,
    hasTextConfig: Boolean,
    profileSuggestion: ProjectProfileSuggestion?,
    onSaveProfile: (String, String, String, String, String, String, String) -> Unit,
    onSaveWritingPolicy: (String, String, Int, Int) -> Unit,
    onGenerateCover: () -> Unit,
    onUploadCover: (android.net.Uri) -> Unit,
    onExport: () -> Unit,
    onBackup: () -> Unit,
    onCancelCover: () -> Unit,
    onGenerateProfile: () -> Unit,
    onCancelProfile: () -> Unit,
    onSaveBlueprint: (String) -> Unit,
    onGenerateBlueprint: () -> Unit,
    onCancelBlueprint: () -> Unit,
    onSavePacing: (Int, Int, String) -> Unit,
    onDeleteProject: () -> Unit,
) {
    var title by remember(project.id) { mutableStateOf(project.title) }
    var genre by remember(project.id) { mutableStateOf(project.genre) }
    var premise by remember(project.id) { mutableStateOf(project.premise) }
    var summary by remember(project.id) { mutableStateOf(project.summary) }
    var tags by remember(project.id) { mutableStateOf(project.tags) }
    var audience by remember(project.id) { mutableStateOf(project.targetAudience) }
    var protagonist by remember(project.id) { mutableStateOf(project.protagonistName) }
    var blueprint by remember(project.id) { mutableStateOf(project.longFormBlueprint) }
    var targetChapters by remember(project.id) { mutableStateOf(project.targetChapterCount.takeIf { it > 0 }?.toString().orEmpty()) }
    var targetWords by remember(project.id) { mutableStateOf(project.targetWordCount.takeIf { it > 0 }?.toString().orEmpty()) }
    var pacingProfile by remember(project.id) { mutableStateOf(project.pacingProfile) }
    var forbiddenContent by remember(project.id) { mutableStateOf(project.forbiddenContent) }
    var automationLevel by remember(project.id) { mutableStateOf(project.automationLevel) }
    var chapterWordCountMin by remember(project.id) { mutableStateOf(project.targetChapterWordCount.toString()) }
    var chapterWordCountMax by remember(project.id) { mutableStateOf(project.targetChapterWordCountMax.toString()) }
    var deleteVisible by rememberSaveable { mutableStateOf(false) }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(onUploadCover) }
    LaunchedEffect(profileSuggestion) {
        profileSuggestion?.let { suggestion ->
            if (suggestion.genre.isNotBlank()) genre = suggestion.genre
            if (suggestion.premise.isNotBlank()) premise = suggestion.premise
            if (suggestion.summary.isNotBlank()) summary = suggestion.summary
            if (suggestion.tags.isNotBlank()) tags = suggestion.tags
            if (suggestion.targetAudience.isNotBlank()) audience = suggestion.targetAudience
            if (suggestion.protagonistName.isNotBlank()) protagonist = suggestion.protagonistName
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(IosBackground).imePadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                ProjectCover(project, Modifier.width(132.dp).aspectRatio(0.68f))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                    Text("作品资料", color = Brand, style = MaterialTheme.typography.labelMedium)
                    Text("可用独立图像 AI 生成，也可从本地上传。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = if (isCoverGenerating) onCancelCover else onGenerateCover,
                    enabled = isCoverGenerating || hasImageConfig,
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Icon(if (isCoverGenerating) Icons.Outlined.Close else Icons.Outlined.CoverImage, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isCoverGenerating) "取消生成" else "AI 生成")
                }
                OutlinedButton(onClick = { coverPicker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Outlined.FileOpen, null)
                    Spacer(Modifier.width(6.dp))
                    Text("上传封面")
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Outlined.FileDownload, null)
                Spacer(Modifier.width(6.dp))
                Text("导出作品（Markdown）")
            }
            TextButton(onClick = onBackup, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Save, null)
                Spacer(Modifier.width(6.dp))
                Text("导出完整项目备份（可恢复）")
            }
            Spacer(Modifier.height(8.dp))
            if (!hasImageConfig) ActionHint("先在“我的”配置独立的封面 AI。封面生成不会使用文本模型，也不需要你填写绘图提示词。")
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = IosSurface), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("作品设定", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("作品名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("题材") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = premise, onValueChange = { premise = it }, label = { Text("一句话设定") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("小说简介") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("标签，用逗号分隔") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = audience, onValueChange = { audience = it }, label = { Text("目标读者") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = protagonist, onValueChange = { protagonist = it }, label = { Text("主角名") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = if (isProfileGenerating) onCancelProfile else onGenerateProfile, enabled = isProfileGenerating || hasTextConfig, modifier = Modifier.weight(1f)) {
                            Icon(if (isProfileGenerating) Icons.Outlined.Close else Icons.Outlined.Lightbulb, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (isProfileGenerating) "取消生成" else "AI 补全")
                        }
                        Button(onClick = { onSaveProfile(title, genre, premise, summary, tags, audience, protagonist) }, modifier = Modifier.weight(1f)) { Text("确认保存") }
                    }
                    ActionHint("AI 补全会先填入表单，确认保存后才会修改作品；简介、标签和主角名会自动用于封面与后续写作上下文。")
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("长篇路线图", style = MaterialTheme.typography.titleMedium)
                    Text("分卷目标、阶段冲突、升级节奏与伏笔约束会自动进入后续写作上下文。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = blueprint,
                        onValueChange = { blueprint = it; onSaveBlueprint(it) },
                        label = { Text("可编辑的长篇路线图") },
                        minLines = 7,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = if (isBlueprintGenerating) onCancelBlueprint else onGenerateBlueprint,
                        enabled = isBlueprintGenerating || hasTextConfig,
                    ) {
                        Icon(if (isBlueprintGenerating) Icons.Outlined.Close else Icons.Outlined.AutoStories, null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isBlueprintGenerating) "取消生成" else "AI 生成长篇路线图")
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardGreen), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("长篇节奏约束", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = targetChapters, onValueChange = { targetChapters = it.filter(Char::isDigit); onSavePacing(targetChapters.toIntOrNull() ?: 0, targetWords.toIntOrNull() ?: 0, pacingProfile) }, label = { Text("目标章节") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = targetWords, onValueChange = { targetWords = it.filter(Char::isDigit); onSavePacing(targetChapters.toIntOrNull() ?: 0, targetWords.toIntOrNull() ?: 0, pacingProfile) }, label = { Text("目标总字数") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("慢燃", "均衡", "快节奏").forEach { profile ->
                            if (pacingProfile == profile) Button(onClick = { pacingProfile = profile; onSavePacing(targetChapters.toIntOrNull() ?: 0, targetWords.toIntOrNull() ?: 0, profile) }) { Text(profile) }
                            else OutlinedButton(onClick = { pacingProfile = profile; onSavePacing(targetChapters.toIntOrNull() ?: 0, targetWords.toIntOrNull() ?: 0, profile) }) { Text(profile) }
                        }
                    }
                    Text("慢燃：更多铺垫和人物关系，冲突逐步升级。均衡：推进、冲突和铺垫保持平衡。快节奏：更快进入冲突与转折。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                    Text("完成度未到 70% 时，系统会拦截“最终胜利 / 一切结束”等可能过早收束的主线表达。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardAmber), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("创作策略", style = MaterialTheme.typography.titleMedium)
                    Text("禁区、自动化程度与单章长度会直接进入续写、批量写作和章节门禁。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(forbiddenContent, { forbiddenContent = it }, label = { Text("核心禁区（剧情、价值观或不能提前揭露的内容）") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    Text("自动化程度", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("保守", "半自动", "自动推进").forEach { level ->
                            if (automationLevel == level) Button(onClick = { automationLevel = level }) { Text(level) } else OutlinedButton(onClick = { automationLevel = level }) { Text(level) }
                        }
                    }
                    Text("保守：只按明确指令续写。半自动：AI 准备计划和分镜，你决定何时生成。自动推进：AI 可直接规划和续写，仍保留审核与回滚。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(chapterWordCountMin, { chapterWordCountMin = it.filter(Char::isDigit) }, label = { Text("单章最少字数") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(chapterWordCountMax, { chapterWordCountMax = it.filter(Char::isDigit) }, label = { Text("单章最多字数") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Button(onClick = { onSaveWritingPolicy(forbiddenContent, automationLevel, chapterWordCountMin.toIntOrNull() ?: 3_000, chapterWordCountMax.toIntOrNull() ?: 5_000) }, modifier = Modifier.fillMaxWidth()) { Text("保存创作策略") }
                }
            }
        }
        item {
            TextButton(onClick = { deleteVisible = true }) {
                Icon(Icons.Outlined.Delete, null, tint = Color(0xFFFF3B30))
                Spacer(Modifier.width(4.dp))
                Text("删除整部作品", color = Color(0xFFFF3B30))
            }
        }
    }
    if (deleteVisible) {
        AlertDialog(
            onDismissRequest = { deleteVisible = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("删除《${project.title}》？") },
            text = { Text("章节、资料、大纲与本地封面都会删除，无法恢复。") },
            confirmButton = { Button(onClick = { onDeleteProject(); deleteVisible = false }) { Text("删除作品") } },
            dismissButton = { TextButton(onClick = { deleteVisible = false }) { Text("取消") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WriteTab(
    chapters: List<Chapter>,
    chapter: Chapter?,
    contextPacket: ContextPacket,
    streamedContinuation: String,
    config: ModelConfig,
    activeTasks: Set<GenerationTask>,
    resumableAutoWriteRun: AutoWriteRun?,
    automationLevel: String,
    pacingRecommendation: PacingRecommendation?,
    onSelectChapter: (Long) -> Unit,
    onSave: (String) -> Unit,
    onRename: (String) -> Unit,
    onAddChapter: (NextChapterAction) -> Unit,
    onDeleteChapter: () -> Unit,
    onGenerate: (String) -> Unit,
    onAutoWrite: (Int) -> Unit,
    onResumeAutoWrite: () -> Unit,
    onCancel: (GenerationTask) -> Unit,
) {
    if (chapter == null) {
        EmptyWorkspace()
        return
    }
    var draft by remember(chapter.id, chapter.content) { mutableStateOf(chapter.content) }
    var continueDialogVisible by rememberSaveable { mutableStateOf(false) }
    var nextChapterDialogVisible by rememberSaveable { mutableStateOf(false) }
    var autoWriteDialogVisible by rememberSaveable { mutableStateOf(false) }
    var deleteChapterVisible by rememberSaveable { mutableStateOf(false) }
    var renameChapterVisible by rememberSaveable { mutableStateOf(false) }
    var moreMenuVisible by rememberSaveable { mutableStateOf(false) }
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    val streamingScroll = rememberScrollState()
    val activeWritingTask = when {
        GenerationTask.CONTINUATION in activeTasks -> GenerationTask.CONTINUATION
        GenerationTask.OPENING_CHAPTER in activeTasks -> GenerationTask.OPENING_CHAPTER
        else -> null
    }
    val isContinuing = activeWritingTask != null
    val isAutoWriting = GenerationTask.AUTO_WRITE in activeTasks
    val predecessor = chapters.filter { it.number < chapter.number && it.content.isNotBlank() }.maxByOrNull { it.number }
    val visibleCharacterCount = (if (isContinuing) streamedContinuation else draft).count { !it.isWhitespace() }
    LaunchedEffect(streamedContinuation) {
        if (isContinuing && streamedContinuation.isNotBlank()) {
            streamingScroll.animateScrollTo(streamingScroll.maxValue)
        }
    }
    Column(Modifier.fillMaxSize().background(IosBackground).imePadding()) {
        // Chapter header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(IosSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "第 ${chapter.number} 章",
                    color = Brand,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    chapter.title.ifBlank { "未命名章节" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = IosLabel,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${visibleCharacterCount} 字",
                    style = MaterialTheme.typography.labelMedium,
                    color = IosLabel,
                    fontWeight = FontWeight.Medium,
                )
                Text("已自动保存", color = IosSecondLabel, style = MaterialTheme.typography.labelSmall)
                predecessor?.takeIf { it.lifecycleStatus != ChapterLifecycleStatus.PASSED }?.let {
                    Text("前情待确认", color = BrandGold, style = MaterialTheme.typography.labelSmall)
                }
            }
            Box {
                IconButton(onClick = { moreMenuVisible = true }) {
                    Icon(Icons.Outlined.MoreVert, "更多写作操作", tint = IosSecondLabel)
                }
                DropdownMenu(
                    expanded = moreMenuVisible,
                    onDismissRequest = { moreMenuVisible = false },
                    modifier = Modifier.background(IosSurface),
                ) {
                    DropdownMenuItem(text = { Text("立即保存到本机") }, leadingIcon = { Icon(Icons.Outlined.Save, null) }, onClick = { onSave(draft); moreMenuVisible = false })
                    DropdownMenuItem(text = { Text("修改本章标题") }, leadingIcon = { Icon(Icons.Outlined.Description, null) }, onClick = { renameChapterVisible = true; moreMenuVisible = false })
                    DropdownMenuItem(text = { Text("全文搜索章节内容") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, onClick = { searchVisible = true; moreMenuVisible = false })
                    DropdownMenuItem(
                        text = { Text(if (isAutoWriting) "取消批量写作" else "批量生成后续章节") },
                        leadingIcon = { Icon(Icons.Outlined.AutoStories, null) },
                        onClick = { if (isAutoWriting) onCancel(GenerationTask.AUTO_WRITE) else autoWriteDialogVisible = true; moreMenuVisible = false },
                        enabled = isAutoWriting || (config.baseUrl.isNotBlank() && config.apiKey.isNotBlank() && config.model.isNotBlank()),
                    )
                    resumableAutoWriteRun?.let { run ->
                        DropdownMenuItem(
                            text = { Text("继续批量写作（${run.completedCount}/${run.requestedCount}）") },
                            leadingIcon = { Icon(Icons.Outlined.AutoStories, null) },
                            onClick = { onResumeAutoWrite(); moreMenuVisible = false },
                            enabled = !isAutoWriting && config.baseUrl.isNotBlank() && config.apiKey.isNotBlank() && config.model.isNotBlank(),
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("删除本章", color = Red) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = Red) },
                        onClick = { deleteChapterVisible = true; moreMenuVisible = false },
                        enabled = chapters.size > 1,
                    )
                }
            }
        }
        ChapterNavigator(chapters, chapter.id, onSelectChapter)
        Spacer(Modifier.height(4.dp))
        // Editor card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 8.dp)
                .background(IosSurface, RoundedCornerShape(16.dp)),
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("正文草稿", color = IosSecondLabel, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.weight(1f))
                    if (chapter.qualityStatus == ChapterQualityStatus.NEEDS_REPAIR) {
                        Text("待修复", color = Brand, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (isContinuing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(CardBlue, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                            .verticalScroll(streamingScroll),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).background(BrandTeal, RoundedCornerShape(3.dp)))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (activeWritingTask == GenerationTask.OPENING_CHAPTER) "AI 正在生成第一章" else "AI 正在续写本章",
                                color = BrandTeal,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            streamedContinuation.ifBlank { "正在连接模型..." },
                            color = IosLabel,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { value -> draft = value; onSave(value) },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        placeholder = { Text("从这里开始写...", color = IosSecondLabel) },
                        minLines = 1,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Brand,
                            unfocusedBorderColor = IosSeparator,
                            focusedContainerColor = IosSurface,
                            unfocusedContainerColor = IosSurface,
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        // Action bar — AI续写 is primary (wider, Brand-filled), 新建章节 is secondary
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            // Primary CTA: AI续写 / 取消生成
            Button(
                onClick = { if (activeWritingTask != null) onCancel(activeWritingTask) else if (automationLevel == "自动推进") onGenerate("") else continueDialogVisible = true },
                modifier = Modifier.weight(1.6f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = isContinuing || (config.baseUrl.isNotBlank() && config.apiKey.isNotBlank() && config.model.isNotBlank()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isContinuing) IosGroupedFill else Brand,
                    contentColor   = if (isContinuing) Brand else IosSurface,
                ),
            ) {
                Icon(if (isContinuing) Icons.Outlined.Close else Icons.Outlined.Lightbulb, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isContinuing) "取消生成" else "AI 续写", style = MaterialTheme.typography.labelLarge)
            }
            // Secondary: new chapter
            OutlinedButton(
                onClick = { nextChapterDialogVisible = true },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IosSeparator),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = IosLabel),
            ) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("新建章节", style = MaterialTheme.typography.labelLarge)
            }
        }
        if (config.baseUrl.isBlank() || config.apiKey.isBlank() || config.model.isBlank()) {
            Text(
                "AI 功能需要先在「我的」填写模型连接。",
                color = BrandGold,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
    }
    if (continueDialogVisible) {
        ContinueWritingDialog(
            packet = contextPacket,
            pacingRecommendation = pacingRecommendation,
            onDismiss = { continueDialogVisible = false },
            onStart = { direction -> onGenerate(direction); continueDialogVisible = false },
        )
    }
    if (nextChapterDialogVisible) {
        NextChapterDialog(
            onDismiss = { nextChapterDialogVisible = false },
            onChoose = { action -> onAddChapter(action); nextChapterDialogVisible = false },
        )
    }
    if (autoWriteDialogVisible) {
        AutoWriteDialog(
            packet = contextPacket,
            onDismiss = { autoWriteDialogVisible = false },
            onStart = { count -> onAutoWrite(count); autoWriteDialogVisible = false },
        )
    }
    if (deleteChapterVisible) {
        AlertDialog(
            onDismissRequest = { deleteChapterVisible = false },
            title = { Text("删除第${chapter.number}章？") },
            text = { Text("正文、计划和分镜会从本机删除，无法恢复。") },
            confirmButton = { Button(onClick = { onDeleteChapter(); deleteChapterVisible = false }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { deleteChapterVisible = false }) { Text("取消") } },
        )
    }
    if (renameChapterVisible) {
        ChapterTitleDialog(
            initialTitle = chapter.title,
            onDismiss = { renameChapterVisible = false },
            onSave = { onRename(it); renameChapterVisible = false },
        )
    }
    if (searchVisible) ChapterSearchDialog(chapters, onDismiss = { searchVisible = false }, onSelect = { onSelectChapter(it.id); searchVisible = false })
}

@Composable
private fun ChapterTitleDialog(initialTitle: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改章节标题") },
        text = { OutlinedTextField(title, { title = it }, label = { Text("章节标题") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Button(onClick = { onSave(title) }, enabled = title.isNotBlank()) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

// iOS-style section header for grouped content
@Composable
private fun IosGroupHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = IosSecondLabel,
        letterSpacing = 0.8.sp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp),
    )
}

// iOS-style grouped card section
@Composable
private fun IosSection(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .background(IosSurface, RoundedCornerShape(16.dp)),
    ) { content() }
}

@Composable
private fun NextChapterDialog(onDismiss: () -> Unit, onChoose: (NextChapterAction) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("开始下一章", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("当前章节会先完成记忆与一致性闭环，通过后再执行选择。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = { onChoose(NextChapterAction.GENERATE_WITH_AI) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Outlined.AutoStories, null)
                    Spacer(Modifier.width(8.dp))
                    Text("新建并由 AI 生成")
                }
                OutlinedButton(
                    onClick = { onChoose(NextChapterAction.CREATE_BLANK) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("只新建空白章")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ContinueWritingDialog(packet: ContextPacket, pacingRecommendation: PacingRecommendation?, onDismiss: () -> Unit, onStart: (String) -> Unit) {
    var direction by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("AI 续写") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("会先补齐本章计划与分镜，再把结果接在当前正文末尾；不会新建章节。以下本地上下文会随本章发送到你的模型。")
                pacingRecommendation?.let { Text("节奏预检：建议 ${it.pace} 档 · ${it.eventType}。${it.reason}", color = BrandTeal, style = MaterialTheme.typography.bodySmall) }
                Text("先选一个走向，或补充一句作者意图。留空时 AI 会按大纲和未解决锚点自然推进。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("推进当前冲突并留下反转", "强化人物关系与动机", "揭露一条线索但不揭底").forEach { option ->
                        if (direction == option) Button(onClick = { direction = option }) { Text(option) } else OutlinedButton(onClick = { direction = option }) { Text(option) }
                    }
                }
                OutlinedTextField(direction, { direction = it }, label = { Text("本次剧情方向（可选）") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                ContextSummary(packet)
            }
        },
        confirmButton = { Button(onClick = { onStart(direction) }) { Text("开始续写") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ChapterSearchDialog(chapters: List<Chapter>, onDismiss: () -> Unit, onSelect: (Chapter) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, chapters) { StorySearch.find(chapters, query) }
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(20.dp), title = { Text("全文检索") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query, { query = it }, label = { Text("输入至少两个字") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            results.take(6).forEach { result -> Card(onClick = { onSelect(result.chapter) }, colors = CardDefaults.cardColors(containerColor = IosSurface), shape = RoundedCornerShape(10.dp)) { Column(Modifier.padding(10.dp)) { Text("第${result.chapter.number}章 · ${result.chapter.title}（${result.count}处）", style = MaterialTheme.typography.titleSmall); Text(result.preview, color = IosSecondLabel, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall) } } }
            if (query.length >= 2 && results.isEmpty()) Text("未找到匹配内容", color = IosSecondLabel)
        }
    }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } })
}

@Composable
private fun AutoWriteDialog(packet: ContextPacket, onDismiss: () -> Unit, onStart: (Int) -> Unit) {
    var count by remember { mutableStateOf("1") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("批量写作") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("每章依次生成计划、分镜和正文，并在门禁警告时立即停止；已完成的计划、分镜和草稿均保存在本机。")
                ContextSummary(packet)
                OutlinedTextField(value = count, onValueChange = { count = it.filter(Char::isDigit) }, label = { Text("章节数，1-5") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onStart((count.toIntOrNull() ?: 1).coerceIn(1, 5)) }) { Text("开始") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ChapterNavigator(chapters: List<Chapter>, selectedId: Long, onSelect: (Long) -> Unit) {
    val currentIndex = chapters.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    var chapterMenuVisible by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IosSurface)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { if (currentIndex > 0) onSelect(chapters[currentIndex - 1].id) },
            enabled = currentIndex > 0,
        ) {
            Icon(Icons.Outlined.KeyboardArrowLeft, "上一章", tint = if (currentIndex > 0) Brand else IosSecondLabel)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            TextButton(
                onClick = { chapterMenuVisible = true },
                colors = ButtonDefaults.textButtonColors(contentColor = Brand),
            ) {
                Text(
                    "第 ${chapters.getOrNull(currentIndex)?.number ?: "-"} 章",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            DropdownMenu(
                expanded = chapterMenuVisible,
                onDismissRequest = { chapterMenuVisible = false },
                modifier = Modifier.background(IosSurface),
            ) {
                chapters.forEach { item ->
                    DropdownMenuItem(
                        text = { Text("第${item.number}章 · ${item.title}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = { onSelect(item.id); chapterMenuVisible = false },
                    )
                }
            }
        }
        IconButton(
            onClick = { if (currentIndex < chapters.lastIndex) onSelect(chapters[currentIndex + 1].id) },
            enabled = currentIndex < chapters.lastIndex,
        ) {
            Icon(Icons.Outlined.KeyboardArrowRight, "下一章", tint = if (currentIndex < chapters.lastIndex) Brand else IosSecondLabel)
        }
    }
    Divider(color = IosSeparator, thickness = 0.5.dp)
}

// iOS-style helper text (replaces ActionHint)
@Composable
private fun ActionHint(text: String) {
    Text(
        text = text,
        color = IosSecondLabel,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
    )
}

@Composable
private fun ContextSummary(packet: ContextPacket) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardGreen, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lightbulb, null, tint = BrandTeal, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            val names = packet.relevantItems.take(2).joinToString("、") { it.name }
            Text(
                "上下文：${packet.relevantItems.size} 设定 · ${packet.relevantEdges.size} 关系 · ${packet.relevantChapters.size} 摘录" + if (names.isBlank()) "" else " · $names",
                color = IosSecondLabel,
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
    pacingEvents: List<ChapterPacingEvent>,
    eventMatrixRules: List<EventMatrixRule>,
    pacingRecommendation: PacingRecommendation?,
    onSavePacingEvent: (String, String, String) -> Unit,
    onSaveEventMatrixRule: (EventMatrixRule) -> Unit,
    onAddEventMatrixRule: (String, Int, String) -> Unit,
    onDeleteEventMatrixRule: (EventMatrixRule) -> Unit,
    styleProfiles: List<StyleProfile>,
    onSaveStyleProfile: (String) -> Unit,
    onApplyStyleProfile: (StyleProfile) -> Unit,
    onDeleteStyleProfile: (StyleProfile) -> Unit,
) {
    var anchorDialogVisible by rememberSaveable { mutableStateOf(false) }
    var reviseDialogVisible by rememberSaveable { mutableStateOf(false) }
    var pacingDialogVisible by rememberSaveable { mutableStateOf(false) }
    var styleDialogVisible by rememberSaveable { mutableStateOf(false) }
    var styleSearch by rememberSaveable { mutableStateOf("") }
    val isStyleGenerating = GenerationTask.STYLE_GUIDE in activeTasks
    val isPlanGenerating = GenerationTask.CHAPTER_PLAN in activeTasks
    val isBeatGenerating = GenerationTask.BEAT_SHEET in activeTasks
    LazyColumn(Modifier.fillMaxSize().background(IosBackground).imePadding(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(project.genre + " · " + project.title, color = BrandTeal, style = MaterialTheme.typography.labelMedium)
            if (project.premise.isNotBlank()) Text(project.premise, color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { reviseDialogVisible = true }, modifier = Modifier.padding(top = 8.dp)) { Text("改纲级联") }
            ActionHint("改纲级联会标记受影响的章节设定；本章计划与场景分镜只改大纲字段，不会修改正文。")
            if (project.outlineRevisionReport.isNotBlank()) {
                Text(project.outlineRevisionReport, color = BrandGold, style = MaterialTheme.typography.labelSmall)
                Button(onClick = onResolveOutlineCascade) { Text("确认已复核全部待审项") }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StyleGuideEditor(project.styleGuide, config, isStyleGenerating, onSaveStyleGuide, onExtractStyleGuide, { onCancel(GenerationTask.STYLE_GUIDE) })
                GenreStylePresetCard(project.genre, onSaveStyleGuide)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardMint), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("节奏工作台", style = MaterialTheme.typography.titleMedium)
                    pacingRecommendation?.let { Text("建议：${it.pace}档 · ${it.eventType}。${it.reason}", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall) }
                    val current = pacingEvents.firstOrNull { it.chapterId == selectedChapter?.id }
                    Text(current?.let { "本章已登记：${it.pace}档 · ${it.eventType}" } ?: "本章尚未登记事件", color = BrandTeal, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { pacingDialogVisible = true }, enabled = selectedChapter != null) { Text("登记本章节奏") }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("跨作品文风库", style = MaterialTheme.typography.titleMedium)
                    Text("可把当前文风保存为档案，并在其他作品中复用。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { styleDialogVisible = true }, enabled = project.styleGuide.isNotBlank()) { Text("保存当前文风") }
                        styleProfiles.firstOrNull { it.genre == project.genre }?.let { profile -> TextButton(onClick = { onApplyStyleProfile(profile) }) { Text("应用「${profile.name}」") } }
                    }
                    OutlinedTextField(styleSearch, { styleSearch = it }, label = { Text("检索文风名称、题材或关键词") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    styleProfiles.filter { styleSearch.isBlank() || "${it.name} ${it.genre} ${it.keywords}".contains(styleSearch, ignoreCase = true) }.take(5).forEach { profile ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(profile.name + if (profile.genre.isBlank()) "" else " · ${profile.genre}", style = MaterialTheme.typography.bodySmall); if (profile.metrics.isNotBlank()) Text(profile.metrics, color = IosSecondLabel, style = MaterialTheme.typography.labelSmall); if (profile.keywords.isNotBlank()) Text(profile.keywords, color = BrandTeal, style = MaterialTheme.typography.labelSmall) }
                            TextButton(onClick = { onApplyStyleProfile(profile) }) { Text("使用") }
                            IconButton(onClick = { onDeleteStyleProfile(profile) }) { Icon(Icons.Outlined.Delete, "删除文风") }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IosGroupHeader("大纲锚点")
                IconButton(onClick = { anchorDialogVisible = true }) { Icon(Icons.Outlined.Add, "添加大纲锚点", tint = Brand) }
            }
        }
        if (anchors.isEmpty()) {
            item { Text("用锚点规定一段章节的冲突、禁区和章末张力；续写时会自动带入模型。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall) }
        } else {
            items(anchors, key = { "anchor-${it.id}" }) { anchor ->
                AnchorCard(anchor, selectedChapter?.number)
            }
        }
        selectedChapter?.let { chapter ->
            item { ChapterPlanEditor(chapter, config, isPlanGenerating, isBeatGenerating, onSavePlan, onSaveBeatSheet, onGeneratePlan, onGenerateBeatSheet, { onCancel(GenerationTask.CHAPTER_PLAN) }, { onCancel(GenerationTask.BEAT_SHEET) }) }
        }
        item { PacingMatrixCard(pacingEvents, eventMatrixRules, selectedChapter?.number, onSaveEventMatrixRule, onAddEventMatrixRule, onDeleteEventMatrixRule) }
        items(chapters, key = { it.id }) { chapter ->
            Card(colors = CardDefaults.cardColors(containerColor = IosSurface), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(chapter.number.toString(), color = Brand, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(chapter.title, style = MaterialTheme.typography.titleSmall)
                        Text(chapter.outline.ifBlank { chapter.content.take(70).ifBlank { "待写" } }, color = IosSecondLabel, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        if (chapter.targetWordCount > 0) {
                            val current = chapter.content.count { !it.isWhitespace() }
                            Text("$current / ${chapter.targetWordCount} 字", color = BrandTeal, style = MaterialTheme.typography.labelSmall)
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
    if (pacingDialogVisible) PacingEventDialog(eventMatrixRules, onDismiss = { pacingDialogVisible = false }, onSave = { type, pace, note -> onSavePacingEvent(type, pace, note); pacingDialogVisible = false })
    if (styleDialogVisible) StyleProfileDialog(onDismiss = { styleDialogVisible = false }, onSave = { onSaveStyleProfile(it); styleDialogVisible = false })
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun GenreStylePresetCard(genre: String, onApply: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("题材文风预设", style = MaterialTheme.typography.titleSmall)
            Text("一键写入项目文风档案，之后仍可按自己的笔触继续编辑。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GenreStylePresets.forGenre(genre).forEach { (name, guide) -> OutlinedButton(onClick = { onApply(guide) }) { Text(name) } }
            }
        }
    }
}

@Composable
private fun PacingMatrixCard(
    events: List<ChapterPacingEvent>,
    rules: List<EventMatrixRule>,
    chapterNumber: Int?,
    onSaveRule: (EventMatrixRule) -> Unit,
    onAddRule: (String, Int, String) -> Unit,
    onDeleteRule: (EventMatrixRule) -> Unit,
) {
    if (chapterNumber == null) return
    var addRuleVisible by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardMint),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("事件矩阵", style = MaterialTheme.typography.titleSmall)
            Text("每类事件独立冷却；冲突最多连续两章，五章内会提醒补关系或世界描写。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
            PacingPlanner.matrix(events, rules, chapterNumber).forEach { cell ->
                val last = cell.lastChapter?.let { "上次第 $it 章" } ?: "尚未触发"
                val availability = if (cell.cooldownRemaining == 0) "可安排" else "还需 ${cell.cooldownRemaining} 章"
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${cell.eventType} · $last · $availability", color = if (cell.cooldownRemaining == 0) Green else Gold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    rules.firstOrNull { it.ruleKey == cell.ruleKey }?.let { rule ->
                        TextButton(onClick = { onSaveRule(rule.copy(enabled = !rule.enabled)) }) { Text(if (rule.enabled) "停用" else "启用") }
                        IconButton(onClick = { onDeleteRule(rule) }) { Icon(Icons.Outlined.Delete, "删除事件规则") }
                    }
                }
            }
            TextButton(onClick = { addRuleVisible = true }) { Text("添加自定义事件") }
        }
    }
    if (addRuleVisible) EventMatrixRuleDialog(onDismiss = { addRuleVisible = false }, onSave = { label, cooldown, category -> onAddRule(label, cooldown, category); addRuleVisible = false })
}

@Composable
private fun OutlineRevisionDialog(initialChapter: Int, onDismiss: () -> Unit, onApply: (Int, String) -> Unit) {
    var fromChapter by remember { mutableStateOf(initialChapter.toString()) }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
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
    Card(colors = CardDefaults.cardColors(containerColor = if (isCurrent) CardGreen else IosSurface), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("第${anchor.startChapter}-${anchor.endChapter}章 · ${anchor.title}", style = MaterialTheme.typography.titleSmall, color = if (isCurrent) BrandTeal else IosLabel)
            Text(anchor.coreConflict, style = MaterialTheme.typography.bodySmall)
            if (anchor.cascadePending) Text("改纲待审", color = BrandGold, style = MaterialTheme.typography.labelSmall)
            if (anchor.allowedPlot.isNotBlank()) Text("推进：${anchor.allowedPlot}", color = IosSecondLabel, style = MaterialTheme.typography.labelSmall)
            if (anchor.forbiddenReveals.isNotBlank()) Text("禁区：${anchor.forbiddenReveals}", color = Brand, style = MaterialTheme.typography.labelSmall)
            if (anchor.mandatoryTension.isNotBlank()) Text("张力：${anchor.mandatoryTension}", color = BrandGold, style = MaterialTheme.typography.labelSmall)
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
        shape = RoundedCornerShape(20.dp),
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
    Card(colors = CardDefaults.cardColors(containerColor = CardCream), shape = RoundedCornerShape(12.dp)) {
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
    Card(colors = CardDefaults.cardColors(containerColor = CardCream), shape = RoundedCornerShape(12.dp)) {
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
    researchNotes: List<ResearchNote>,
    researchPlan: ResearchPlan?,
    onlineResearchResults: List<OnlineResearchResult>,
    isOnlineResearching: Boolean,
    isExtracting: Boolean,
    isAnalyzingReference: Boolean,
    onAdd: (String, String, String, String) -> Unit,
    onUpdate: (StoryItem, String, String, String, String) -> Unit,
    onAddEdge: (Long, Long, String, String, Int) -> Unit,
    onExtractMemory: () -> Unit,
    onAddResearchNote: (String, String, String, String, Boolean) -> Unit,
    onUpdateResearchNote: (ResearchNote, String, String, String, String, Boolean) -> Unit,
    onDeleteResearchNote: (ResearchNote) -> Unit,
    onAnalyzeReference: (ResearchNote) -> Unit,
    onSearchOnlineResearch: (String) -> Unit,
    onCollectOnlineResearch: (OnlineResearchResult) -> Unit,
    onClearOnlineResearch: () -> Unit,
    onCancelExtraction: () -> Unit,
    onCancelReferenceAnalysis: () -> Unit,
) {
    var dialogVisible by rememberSaveable { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<StoryItem?>(null) }
    var edgeDialogVisible by rememberSaveable { mutableStateOf(false) }
    var researchDialogVisible by rememberSaveable { mutableStateOf(false) }
    var onlineResearchVisible by rememberSaveable { mutableStateOf(false) }
    var editResearch by remember { mutableStateOf<ResearchNote?>(null) }
    LazyColumn(Modifier.fillMaxSize().background(IosBackground).imePadding(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    IosGroupHeader("资料库")
                    Text("所有资料仅保存在本机。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                    ActionHint("书本图标会从当前章节提取人物、伏笔和关系；人物图标用于新增关系；加号用于手动新增资料。")
                }
                Row {
                    IconButton(onClick = if (isExtracting) onCancelExtraction else onExtractMemory) { Icon(if (isExtracting) Icons.Outlined.Close else Icons.Outlined.AutoStories, if (isExtracting) "取消知识图谱提取" else "从当前章节提取记忆", tint = Brand) }
                    IconButton(onClick = { edgeDialogVisible = true }, enabled = items.size >= 2) { Icon(Icons.Outlined.People, "添加关系", tint = Brand) }
                    IconButton(onClick = { dialogVisible = true }) { Icon(Icons.Outlined.Add, "添加资料", tint = Brand) }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    IosGroupHeader("调研知识库")
                    Text("仅保存你的摘要、来源和创作可用事实；不导入或复现受版权保护的正文。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                }
                Row {
                    IconButton(onClick = { onlineResearchVisible = true }) { Icon(Icons.Outlined.Search, "联网调研与引用", tint = Brand) }
                    IconButton(onClick = { researchDialogVisible = true }) { Icon(Icons.Outlined.Add, "添加调研笔记", tint = Brand) }
                }
            }
        }
        researchPlan?.let { plan -> item {
            Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("调研计划助手", style = MaterialTheme.typography.titleSmall)
                    Text("关键词：${plan.keywords.joinToString("、")}", color = BrandTeal, style = MaterialTheme.typography.bodySmall)
                    if (plan.gaps.isNotEmpty()) Text("待补缺口：${plan.gaps.joinToString("、")}", color = BrandGold, style = MaterialTheme.typography.bodySmall)
                    Text(plan.quick, color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                    Text(plan.standard, color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                    Text(plan.deep, color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                }
            }
        } }
        item {
            val health = remember(items, edges) { StoryGraphHealth.inspect(items, edges, emptyList()) }
            Card(colors = CardDefaults.cardColors(containerColor = if (health.any { it.severity == "错误" }) CardAmber else CardGreen)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("图谱健康检查", style = MaterialTheme.typography.titleSmall)
                    health.take(3).forEach { issue -> Text("${issue.severity} · ${issue.title}：${issue.detail}", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        item {
            val characters = items.filter { it.kind.contains("角色") || it.kind.contains("人物") }
            val timeline = items.filter { it.kind.contains("时间") || it.kind.contains("事件") }
            val clues = items.filter { it.kind.contains("伏笔") || it.kind.contains("线索") }
            Card(colors = CardDefaults.cardColors(containerColor = CardAmber), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("专用追踪", style = MaterialTheme.typography.titleSmall)
                    Text("人物状态 ${characters.size} 项 · 时间线 ${timeline.size} 项 · 伏笔线索 ${clues.count { it.status != StoryItemStatus.RESOLVED }} 待回收 / ${clues.size} 总计", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                    characters.filter { it.status != StoryItemStatus.RESOLVED }.take(3).forEach { Text("人物：${it.name} · ${it.status}", color = BrandTeal, style = MaterialTheme.typography.labelSmall) }
                    timeline.take(2).forEach { Text("时间线：${it.name}", color = IosLabel, style = MaterialTheme.typography.labelSmall) }
                    clues.filter { it.status != StoryItemStatus.RESOLVED }.take(3).forEach { Text("待回收：${it.name}", color = BrandGold, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
        if (researchNotes.isEmpty()) {
            item { Text("尚无调研笔记", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall) }
        } else {
            items(researchNotes, key = { "research-${it.id}" }) { note ->
                Card(onClick = { editResearch = note }, colors = CardDefaults.cardColors(containerColor = IosSurface), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(note.title, style = MaterialTheme.typography.titleSmall)
                        if (note.tags.isNotBlank()) Text(note.tags, color = BrandTeal, style = MaterialTheme.typography.labelSmall)
                        Text(note.content, color = IosSecondLabel, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (items.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = IosSurface), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Icon(Icons.Outlined.Lightbulb, null, tint = BrandGold)
                        Spacer(Modifier.height(8.dp))
                        Text("还没有资料卡", style = MaterialTheme.typography.titleSmall)
                        Text("添加人物、地点、伏笔、时间线或禁区；续写时会按当前情节自动检索。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(items, key = { it.id }) { item ->
                Card(onClick = { editItem = item }, colors = CardDefaults.cardColors(containerColor = IosSurface), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (item.kind == "人物") Icons.Outlined.People else Icons.Outlined.Lightbulb, null, tint = BrandTeal)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(item.kind + " · " + item.name + " · " + item.status, style = MaterialTheme.typography.titleSmall)
                            if (item.cascadePending) Text("改纲待审", color = BrandGold, style = MaterialTheme.typography.labelSmall)
                            Text(item.detail, color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        if (edges.isNotEmpty()) {
            item { IosGroupHeader("关系图谱", modifier = Modifier.padding(top = 8.dp)) }
            items(edges, key = { "edge-${it.id}" }) { edge ->
                val source = items.firstOrNull { it.id == edge.sourceItemId }?.name ?: "已移除资料"
                val target = items.firstOrNull { it.id == edge.targetItemId }?.name ?: "已移除资料"
                Card(colors = CardDefaults.cardColors(containerColor = IosSurface), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("$source  - ${edge.relation} -  $target", style = MaterialTheme.typography.titleSmall)
                        if (edge.description.isNotBlank()) Text(edge.description, color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                        if (edge.cascadePending) Text("改纲待审", color = BrandGold, style = MaterialTheme.typography.labelSmall)
                        Text("自第${edge.sinceChapter}章起", color = BrandTeal, style = MaterialTheme.typography.labelSmall)
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
    if (researchDialogVisible) {
        ResearchNoteDialog(onDismiss = { researchDialogVisible = false }, onSave = { title, url, tags, content, rightsConfirmed ->
            onAddResearchNote(title, url, tags, content, rightsConfirmed)
            researchDialogVisible = false
        })
    }
    if (onlineResearchVisible) {
        OnlineResearchDialog(
            results = onlineResearchResults,
            isSearching = isOnlineResearching,
            onDismiss = { onlineResearchVisible = false; onClearOnlineResearch() },
            onSearch = onSearchOnlineResearch,
            onCollect = onCollectOnlineResearch,
        )
    }
    editResearch?.let { note ->
        ResearchNoteDialog(
            note = note,
            onDismiss = { editResearch = null },
            onSave = { title, url, tags, content, rightsConfirmed -> onUpdateResearchNote(note, title, url, tags, content, rightsConfirmed); editResearch = null },
            onDelete = { onDeleteResearchNote(note); editResearch = null },
            isAnalyzing = isAnalyzingReference,
            onAnalyze = { if (isAnalyzingReference) onCancelReferenceAnalysis() else onAnalyzeReference(note) },
        )
    }
}

@Composable
private fun ResearchNoteDialog(
    note: ResearchNote? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Boolean) -> Unit,
    onDelete: (() -> Unit)? = null,
    isAnalyzing: Boolean = false,
    onAnalyze: (() -> Unit)? = null,
) {
    var title by remember(note) { mutableStateOf(note?.title.orEmpty()) }
    var url by remember(note) { mutableStateOf(note?.sourceUrl.orEmpty()) }
    var tags by remember(note) { mutableStateOf(note?.tags.orEmpty()) }
    var content by remember(note) { mutableStateOf(note?.content.orEmpty()) }
    var rightsConfirmed by remember(note) { mutableStateOf(note?.rightsConfirmed ?: false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(if (note == null) "添加调研笔记" else "编辑调研笔记") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(url, { url = it }, label = { Text("来源链接，可留空") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(tags, { tags = it }, label = { Text("标签") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(content, { content = it }, label = { Text("可验证的事实、结构或灵感摘要") }, minLines = 5, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rightsConfirmed, onCheckedChange = { rightsConfirmed = it })
                    Text("我确认这是自写摘要或我拥有处理授权，不含受保护正文")
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(title, url, tags, content, rightsConfirmed) }, enabled = title.isNotBlank() && content.isNotBlank()) { Text("保存") } },
        dismissButton = { Row { onDelete?.let { TextButton(onClick = it) { Text("删除", color = Red) } }; onAnalyze?.let { TextButton(onClick = it) { Text(if (isAnalyzing) "取消提炼" else "AI 结构提炼") } }; TextButton(onClick = onDismiss) { Text("取消") } } },
    )
}

@Composable
private fun OnlineResearchDialog(
    results: List<OnlineResearchResult>,
    isSearching: Boolean,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
    onCollect: (OnlineResearchResult) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("联网调研") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("检索公开资料，收录时保留原始链接与检索时间；仅保存短摘要，不导入原文。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("调研关键词") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSearch(query) }, enabled = query.isNotBlank() && !isSearching) {
                        Text(if (isSearching) "检索中" else "检索")
                    }
                }
                if (results.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.height(330.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(results, key = { it.sourceUrl }) { result ->
                            Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(10.dp)) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(result.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("${result.sourceLabel} · ${result.retrievedAt}", style = MaterialTheme.typography.labelSmall, color = BrandTeal)
                                    Text(result.excerpt, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = {
                                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.sourceUrl))) }
                                        }) { Text("打开来源") }
                                        TextButton(onClick = { onCollect(result) }) { Text("收录引用") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ExportFormatDialog(
    onDismiss: () -> Unit,
    onMarkdown: () -> Unit,
    onDocx: () -> Unit,
    onEpub: () -> Unit,
    onPdf: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("导出作品", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("选择格式，正文与引用来源一并导出", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                listOf(
                    Triple(Icons.Outlined.Description, "Markdown", "编辑与版本备份") to onMarkdown,
                    Triple(Icons.Outlined.Description, "Word · DOCX", "适合在电脑上继续编辑") to onDocx,
                    Triple(Icons.Outlined.MenuBook, "EPUB", "电子书阅读器格式") to onEpub,
                    Triple(Icons.Outlined.FileDownload, "PDF", "排版定稿，不可编辑") to onPdf,
                ).forEach { (info, action) ->
                    val (icon, label, desc) = info
                    Card(
                        onClick = action,
                        colors = CardDefaults.cardColors(containerColor = IosSurface),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(icon, null, tint = Cinnabar, modifier = Modifier.size(20.dp))
                            Column {
                                Text(label, style = MaterialTheme.typography.titleSmall)
                                Text(desc, color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Outlined.KeyboardArrowRight, null, tint = InkFaint, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
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
        shape = RoundedCornerShape(20.dp),
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
    latestRevision: ChapterRevision?,
    latestEditorialReview: EditorialReview?,
    issues: List<QualityIssue>,
    repairPlan: String?,
    config: ModelConfig,
    isGenerating: Boolean,
    isLifecycleRunning: Boolean,
    onGenerateRepairPlan: () -> Unit,
    onMarkQualityRepaired: () -> Unit,
    onRetryLifecycle: () -> Unit,
    onRewriteChapter: () -> Unit,
    onHumanizeChapter: () -> Unit,
    onRestoreRevision: () -> Unit,
    isEditorialReviewing: Boolean,
    onGenerateEditorialReview: () -> Unit,
    onCancelEditorialReview: () -> Unit,
    isEditorialTeamRunning: Boolean,
    onRunEditorialTeam: () -> Unit,
    onCancelEditorialTeam: () -> Unit,
    onCancel: () -> Unit,
    onCancelLifecycle: () -> Unit,
    chapters: List<Chapter>,
    batchReviewRuns: List<BatchReviewRun>,
    reviewIssues: List<ReviewIssue>,
    gateReports: List<ChapterGateReport>,
    aiTraceReport: AiTraceReport,
    isBatchReviewing: Boolean,
    onBatchReview: (Int, Int) -> Unit,
    onSetReviewIssueResolved: (ReviewIssue, Boolean) -> Unit,
    onCancelBatchReview: () -> Unit,
) {
    var batchDialogVisible by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().background(IosBackground).imePadding(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            IosGroupHeader("本章审阅")
            Text("每个阶段都有可回看的记录，最终决定权始终在作者。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
            ActionHint("生成修复计划只给出修改建议，不会自动改写正文；确认已修复会清除本章的待修复状态。")
        }
        if (chapter?.qualityStatus == ChapterQualityStatus.NEEDS_REPAIR) {
            item {
                Button(onClick = onMarkQualityRepaired) { Text("修复后重新检查") }
            }
        }
        chapter?.let { current ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("写作闭环：${ChapterLifecycleStatus.label(current.lifecycleStatus)}", style = MaterialTheme.typography.titleSmall)
                        if (current.lifecycleDetail.isNotBlank()) Text(current.lifecycleDetail, color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                        if (current.lifecycleStatus == ChapterLifecycleStatus.MEMORY_FAILED || current.lifecycleStatus == ChapterLifecycleStatus.PROCESSING || isLifecycleRunning) {
                            OutlinedButton(
                                onClick = if (isLifecycleRunning) onCancelLifecycle else onRetryLifecycle,
                                enabled = isLifecycleRunning || (config.baseUrl.isNotBlank() && config.apiKey.isNotBlank() && config.model.isNotBlank()),
                            ) {
                                Icon(if (isLifecycleRunning) Icons.Outlined.Close else Icons.Outlined.AutoStories, null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (isLifecycleRunning) "取消章节闭环" else "重试章节闭环")
                            }
                        }
                    }
                }
            }
        }
        if (gateReports.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardMint), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("章节闭环审计", style = MaterialTheme.typography.titleSmall)
                        Text("每次闭环都会保存阶段结果；连续两次失败会要求作者人工确认。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                        gateReports.take(8).forEach { report ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                Icon(if (report.passed) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber, null, tint = if (report.passed) Green else Red)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(report.stage, style = MaterialTheme.typography.labelLarge)
                                    Text(report.content.take(240), color = IosSecondLabel, style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (chapter?.content?.isNotBlank() == true) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardAmber), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("AI 编辑", style = MaterialTheme.typography.titleSmall)
                        Text("每次改写前都会在本机保留一份可撤回的正文备份，改写后自动重新跑记忆和门禁。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = onRewriteChapter,
                                modifier = Modifier.weight(1f),
                                enabled = config.baseUrl.isNotBlank() && config.apiKey.isNotBlank() && config.model.isNotBlank(),
                            ) { Text("按门禁改写", maxLines = 1) }
                            OutlinedButton(
                                onClick = onHumanizeChapter,
                                modifier = Modifier.weight(1f),
                                enabled = config.baseUrl.isNotBlank() && config.apiKey.isNotBlank() && config.model.isNotBlank(),
                            ) { Text("去 AI 味", maxLines = 1) }
                        }
                        latestRevision?.let { revision ->
                            TextButton(onClick = onRestoreRevision) { Text("撤回上次 AI 改写（${revision.reason}）") }
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardAmber), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("AI 痕迹检测", style = MaterialTheme.typography.titleSmall)
                    Text("风险指数 ${aiTraceReport.score}/100", color = if (aiTraceReport.score >= 45) Red else Teal, style = MaterialTheme.typography.labelLarge)
                    aiTraceReport.findings.forEach { Text("· $it", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall) }
                    aiTraceReport.suggestions.forEach { Text(it, color = IosLabel, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("编辑审稿", style = MaterialTheme.typography.titleSmall)
                    Text("独立生成一份 P0/P1/P2 审稿记录，不会改动正文。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        onClick = if (isEditorialReviewing) onCancelEditorialReview else onGenerateEditorialReview,
                        enabled = isEditorialReviewing || (chapter?.content?.isNotBlank() == true && config.baseUrl.isNotBlank() && config.apiKey.isNotBlank() && config.model.isNotBlank()),
                    ) {
                        Icon(if (isEditorialReviewing) Icons.Outlined.Close else Icons.Outlined.Lightbulb, null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isEditorialReviewing) "取消审稿" else "生成编辑审稿")
                    }
                    latestEditorialReview?.let { review ->
                        Text(review.content, color = IosLabel, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardMint), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("编辑团队", style = MaterialTheme.typography.titleSmall)
                    Text("总策划检查章节目标与钩子；角色校对检查人物和时间线；文字编辑检查表达。不会自动改正文。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        onClick = if (isEditorialTeamRunning) onCancelEditorialTeam else onRunEditorialTeam,
                        enabled = isEditorialTeamRunning || (chapter?.content?.isNotBlank() == true && config.baseUrl.isNotBlank() && config.apiKey.isNotBlank() && config.model.isNotBlank()),
                    ) {
                        Icon(if (isEditorialTeamRunning) Icons.Outlined.Close else Icons.Outlined.People, null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isEditorialTeamRunning) "取消团队审稿" else "启动编辑团队")
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardMint), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("批量审稿台账", style = MaterialTheme.typography.titleSmall)
                    Text("跨章节检查人物、时间线、节奏与伏笔；P0/P1/P2 会保留到待办台账中。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = if (isBatchReviewing) onCancelBatchReview else { { batchDialogVisible = true } }, enabled = isBatchReviewing || chapters.any { it.content.isNotBlank() }) { Text(if (isBatchReviewing) "取消批量审稿" else "开始批量审稿") }
                    batchReviewRuns.firstOrNull()?.let { Text("最近：第${it.startChapter}-${it.endChapter}章 · 第${it.round}轮", color = BrandTeal, style = MaterialTheme.typography.labelSmall) }
                    reviewIssues.take(5).forEach { issue ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text("${issue.severity}${if (issue.chapterNumber > 0) " · 第${issue.chapterNumber}章" else " · 全局"}", color = if (issue.severity == "P0") Red else Gold, style = MaterialTheme.typography.labelSmall); Text(issue.summary, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                            TextButton(onClick = { onSetReviewIssueResolved(issue, issue.status != "resolved") }) { Text(if (issue.status == "resolved") "重开" else "解决") }
                        }
                    }
                }
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
                Card(colors = CardDefaults.cardColors(containerColor = CardAmber), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("最短修复计划", style = MaterialTheme.typography.titleSmall)
                        Text(plan, color = IosLabel, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardGreen), shape = RoundedCornerShape(12.dp)) {
                Text("可检查：篇幅、占位符、重复段落、保密设定与结尾收束。", modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (batchDialogVisible) BatchReviewDialog(chapters, onDismiss = { batchDialogVisible = false }, onStart = { from, to -> onBatchReview(from, to); batchDialogVisible = false })
}

@Composable
private fun AuditRow(pass: Boolean, title: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = IosSurface)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (pass) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber, null, tint = if (pass) BrandGreen else BrandGold)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(detail, color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ProtocolSelector(
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: "选择协议"
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text("协议 · $selectedLabel", modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.KeyboardArrowDown, "选择模型协议")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, label) ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(label)
                            Text(protocolDescription(key), color = IosSecondLabel, style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    onClick = { onSelect(key); expanded = false },
                )
            }
        }
    }
}

private fun protocolDescription(protocol: String) = when (protocol) {
    "anthropic" -> "Claude 原生 Messages API"
    "gemini" -> "Google Gemini API"
    "azure" -> "Azure OpenAI 部署端点"
    else -> "通用 Chat Completions，适用于多数服务商"
}

@Composable
private fun ModelSettingsScreen(
    config: ModelConfig,
    onSave: (ModelConfig) -> Unit,
    onTest: (ModelConfig) -> Unit,
    onTestImage: (ModelConfig) -> Unit,
    onShowGuide: () -> Unit,
    onOpenModelGateway: () -> Unit,
) {
    var protocol by remember(config) { mutableStateOf(config.protocol) }
    var baseUrl by remember(config) { mutableStateOf(config.baseUrl) }
    var apiKey by remember(config) { mutableStateOf(config.apiKey) }
    var model by remember(config) { mutableStateOf(config.model) }
    var imageBaseUrl by remember(config) { mutableStateOf(config.imageBaseUrl) }
    var imageApiKey by remember(config) { mutableStateOf(config.imageApiKey) }
    var imageModel by remember(config) { mutableStateOf(config.imageModel) }
    var imageProtocol by remember(config) { mutableStateOf(config.imageProtocol) }
    var reviewerBaseUrl by remember(config) { mutableStateOf(config.reviewerBaseUrl) }
    var reviewerApiKey by remember(config) { mutableStateOf(config.reviewerApiKey) }
    var reviewerModel by remember(config) { mutableStateOf(config.reviewerModel) }
    var reviewerProtocol by remember(config) { mutableStateOf(config.reviewerProtocol.ifBlank { config.protocol }) }
    var reviewerExpanded by rememberSaveable { mutableStateOf(config.reviewerBaseUrl.isNotBlank() || config.reviewerModel.isNotBlank()) }
    val current = ModelConfig(
        provider = protocol,
        protocol = protocol,
        baseUrl = baseUrl,
        apiKey = apiKey,
        model = model,
        imageBaseUrl = imageBaseUrl,
        imageApiKey = imageApiKey,
        imageModel = imageModel,
        imageProtocol = imageProtocol,
        reviewerBaseUrl = reviewerBaseUrl,
        reviewerApiKey = reviewerApiKey,
        reviewerModel = reviewerModel,
        reviewerProtocol = reviewerProtocol,
    )
    LazyColumn(Modifier.fillMaxSize().background(IosBackground).imePadding(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // App identity header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(BrandTint, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "墨",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                        ),
                        color = Brand,
                    )
                }
                Text("墨舟", style = MaterialTheme.typography.displaySmall, color = IosLabel)
                Text(
                    "AI 长篇创作工作台",
                    style = MaterialTheme.typography.bodySmall,
                    color = IosSecondLabel,
                )
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardGreen),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CloudDone, null, tint = BrandTeal, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Surper AI", style = MaterialTheme.typography.titleMedium, color = IosLabel)
                    }
                    Text("统一 API 网关，支持 OpenAI、Claude、Gemini 等多种模型协议", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenModelGateway, modifier = Modifier.height(36.dp)) {
                            Text("打开服务", style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(onClick = onShowGuide, modifier = Modifier.height(36.dp)) {
                            Text("新手引导", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        item {
            IosGroupHeader("模型连接")
            Text("密钥通过 Android Keystore 加密存储", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IosSurface),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AutoStories, null, tint = Cinnabar, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("文本创作", style = MaterialTheme.typography.titleMedium)
                    }
                    ProtocolSelector(
                        selected = protocol,
                        options = listOf("openai" to "OpenAI 兼容", "anthropic" to "Anthropic", "gemini" to "Gemini", "azure" to "Azure OpenAI"),
                        onSelect = { protocol = it },
                    )
                    OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("模型名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { onTest(current) }, modifier = Modifier.weight(1f).height(44.dp)) { Text("测试连接") }
                        Button(onClick = { onSave(current) }, modifier = Modifier.weight(1f).height(44.dp)) { Text("保存") }
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBlue),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = BrandTeal, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("审稿模型（可选）", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { reviewerExpanded = !reviewerExpanded }) { Text(if (reviewerExpanded) "收起" else "展开") }
                    }
                    if (reviewerExpanded) {
                        Text("留空则使用文本创作模型", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                        ProtocolSelector(
                            selected = reviewerProtocol,
                        options = listOf("openai" to "OpenAI 兼容", "anthropic" to "Claude", "gemini" to "Gemini", "azure" to "Azure OpenAI"),
                            onSelect = { reviewerProtocol = it },
                        )
                        OutlinedTextField(value = reviewerBaseUrl, onValueChange = { reviewerBaseUrl = it }, label = { Text("Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = reviewerApiKey, onValueChange = { reviewerApiKey = it }, label = { Text("API Key") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = reviewerModel, onValueChange = { reviewerModel = it }, label = { Text("模型名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { onSave(current) }, modifier = Modifier.fillMaxWidth().height(44.dp)) { Text("保存") }
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IosSurface),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CoverImage, null, tint = BrandGold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("封面生成", style = MaterialTheme.typography.titleMedium)
                    }
                    ProtocolSelector(
                        selected = imageProtocol,
                        options = listOf("openai" to "OpenAI 兼容", "gemini" to "Gemini"),
                        onSelect = { imageProtocol = it },
                    )
                    OutlinedTextField(value = imageBaseUrl, onValueChange = { imageBaseUrl = it }, label = { Text("Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = imageApiKey, onValueChange = { imageApiKey = it }, label = { Text("API Key") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = imageModel, onValueChange = { imageModel = it }, label = { Text("模型名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { onTestImage(current) }, modifier = Modifier.weight(1f).height(44.dp)) { Text("测试") }
                        Button(onClick = { onSave(current) }, modifier = Modifier.weight(1f).height(44.dp)) { Text("保存") }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingDialog(
    step: Int,
    onDismiss: () -> Unit,
    onNext: () -> Unit,
    onOpenModelGateway: () -> Unit,
) {
    val title = when (step) {
        0 -> "从一本书开始"
        1 -> "连接你的模型"
        2 -> "开始写作"
        else -> "模型服务资源"
    }
    val body = when (step) {
        0 -> "在书架新建作品，或导入 TXT、Markdown、DOCX、EPUB、PDF。所有作品、章节和设定默认保存在本机。"
        1 -> "在“我的”填写自己的 Base URL、API Key 与模型名称。密钥受 Android Keystore 加密保护；没有网络时仍可继续编辑本地作品。"
        2 -> "写作页可直接续写当前章节，也可一键新建下一章。续写时会带入设定、章节计划、图谱关系和相关历史片段。"
        else -> "Surper Ai 提供多协议模型 API 网关，可作为模型配置的一个服务来源。是否使用完全由你决定。"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${step + 1} / 4", color = BrandTeal, style = MaterialTheme.typography.labelMedium)
            Text(body, color = IosLabel, style = MaterialTheme.typography.bodyMedium)
        } },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (step == 3) TextButton(onClick = onOpenModelGateway) { Text("打开服务") }
                Button(onClick = if (step == 3) onDismiss else onNext) { Text(if (step == 3) "完成" else "下一步") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("稍后查看") } },
    )
}

@Composable
private fun CreateProjectDialog(onDismiss: () -> Unit, onCreate: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var premise by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("新建作品", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("书名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("题材") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = premise,
                    onValueChange = { premise = it },
                    label = { Text("一句话设定") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { Button(onClick = { onCreate(title, genre, premise) }, enabled = title.isNotBlank()) { Text("创建") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun IdeationWizard(
    draft: IdeationDraft?,
    canUseAi: Boolean,
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onSave: (IdeationDraft) -> Unit,
    onGenerate: (String, String) -> Unit,
    onCancelGeneration: () -> Unit,
    onFinish: (IdeationDraft) -> Unit,
) {
    var current by remember(draft?.id, draft?.updatedAt) { mutableStateOf(draft ?: IdeationDraft()) }
    val readyToStart = current.title.isNotBlank() && current.premise.isNotBlank() && current.conflict.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(if (readyToStart) "你的开书资料" else "想写一个怎样的故事？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (readyToStart) {
                    Text(current.title, color = BrandTeal, style = MaterialTheme.typography.titleMedium)
                    Text("${current.genre} · ${current.targetAudience}", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                    Text(current.premise, style = MaterialTheme.typography.bodyMedium)
                    Text("主角：${current.protagonist}\n冲突：${current.conflict}\n悬念：${current.promise}", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                } else {
                    OutlinedTextField(
                        value = current.premise,
                        onValueChange = { current = current.copy(premise = it) },
                        label = { Text("一个画面、人物、情绪或故事念头") },
                        minLines = 3,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("不限", "都市", "玄幻", "悬疑", "科幻", "言情").forEach { option ->
                            val selected = (option == "不限" && current.genre.isBlank()) || current.genre == option
                            if (selected) Button(onClick = { current = current.copy(genre = if (option == "不限") "" else option) }) { Text(option) }
                            else OutlinedButton(onClick = { current = current.copy(genre = option) }) { Text(option) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                readyToStart -> Button(onClick = { onFinish(current) }) { Text("开始写作") }
                isGenerating -> Button(onClick = onCancelGeneration) { Text("取消生成") }
                canUseAi -> Button(onClick = { onGenerate(current.premise, current.genre) }) { Text("AI 整理开书资料") }
                else -> Button(onClick = { onFinish(current) }) { Text("直接开始") }
            }
        },
        dismissButton = { TextButton(onClick = { onSave(current); onDismiss() }) { Text("稍后再说") } },
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun PacingEventDialog(rules: List<EventMatrixRule>, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    val types = listOf("情绪铺垫") + rules.filter { it.enabled }.map { it.label }
    var type by remember(rules) { mutableStateOf(types.first()) }; var pace by remember { mutableStateOf("中") }; var note by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(20.dp), title = { Text("登记节奏事件") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("事件类型", style = MaterialTheme.typography.labelMedium); FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { types.forEach { if (type == it) Button(onClick = { type = it }) { Text(it) } else TextButton(onClick = { type = it }) { Text(it) } } }
        Text("节奏档位", style = MaterialTheme.typography.labelMedium); Row { listOf("慢", "中", "快").forEach { if (pace == it) Button(onClick = { pace = it }) { Text("${it}档") } else TextButton(onClick = { pace = it }) { Text("${it}档") } } }
        OutlinedTextField(note, { note = it }, label = { Text("本章事件说明") }, minLines = 2)
    } }, confirmButton = { Button(onClick = { onSave(type, pace, note) }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun EventMatrixRuleDialog(onDismiss: () -> Unit, onSave: (String, Int, String) -> Unit) {
    var label by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("自定义") }
    var cooldown by remember { mutableStateOf("2") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("添加事件规则") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("规则会参与续写建议、章节质量门禁和冷却计算。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(label, { label = it }, label = { Text("事件名称") }, singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text("分类") }, singleLine = true)
                OutlinedTextField(cooldown, { cooldown = it.filter(Char::isDigit) }, label = { Text("冷却章节数") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onSave(label, cooldown.toIntOrNull() ?: 2, category) }, enabled = label.isNotBlank()) { Text("添加") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun StyleProfileDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(20.dp), title = { Text("保存文风档案") }, text = { OutlinedTextField(name, { name = it }, label = { Text("档案名称") }) }, confirmButton = { Button(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun BatchReviewDialog(chapters: List<Chapter>, onDismiss: () -> Unit, onStart: (Int, Int) -> Unit) {
    var from by remember { mutableStateOf(chapters.firstOrNull()?.number?.toString().orEmpty()) }; var to by remember { mutableStateOf(chapters.lastOrNull()?.number?.toString().orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(20.dp), title = { Text("批量审稿范围") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("审稿会建立新一轮 P0/P1/P2 台账，不会改动正文。", color = IosSecondLabel, style = MaterialTheme.typography.bodySmall); OutlinedTextField(from, { from = it.filter(Char::isDigit) }, label = { Text("起始章节") }, singleLine = true); OutlinedTextField(to, { to = it.filter(Char::isDigit) }, label = { Text("结束章节") }, singleLine = true) } }, confirmButton = { Button(onClick = { onStart(from.toIntOrNull() ?: 1, to.toIntOrNull() ?: Int.MAX_VALUE) }) { Text("开始审稿") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
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
        shape = RoundedCornerShape(20.dp),
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
