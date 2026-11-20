package com.example.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.alarms.AlarmsScreen
import com.example.alarms.AlarmsViewModel
import com.example.analytics.InsightsScreen
import com.example.analytics.InsightsViewModel
import com.example.app.AppContainer
import com.example.core.model.StudyMode
import com.example.dashboard.DashboardScreen
import com.example.dashboard.DashboardViewModel
import com.example.essentials.EssentialsViewModel
import com.example.essentials.StudentEssentialsScreen
import com.example.exams.ActiveExamScreen
import com.example.exams.ExamResultDetailScreen
import com.example.exams.MockExamViewModel
import com.example.exams.MockExamsScreen
import com.example.exports.ExportCenterScreen
import com.example.exports.ExportViewModel
import com.example.focus.FocusModeScreen
import com.example.focus.FocusViewModel
import com.example.imports.ImportCenterScreen
import com.example.imports.ImportViewModel
import com.example.notebook.NotebookScreen
import com.example.notebook.NotebookViewModel
import com.example.notifications.NotificationHelper
import com.example.planner.DailyPlannerScreen
import com.example.planner.PlannerViewModel
import com.example.questions.QuestionEditorScreen
import com.example.questions.QuestionStudioScreen
import com.example.questions.QuestionStudioViewModel
import com.example.settings.SettingsScreen
import com.example.settings.SettingsViewModel
import com.example.study.StudyLibraryScreen
import com.example.study.StudySessionScreen
import com.example.study.StudyViewModel
import com.example.tools.ScientificToolsScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EueeAppShell(
    container: AppContainer,
    initialShortcutRoute: String? = null
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val dashboardVm = remember { DashboardViewModel(container.repository, container.preferencesRepository) }
    val studyVm = remember { StudyViewModel(container.repository, container.preferencesRepository) }
    val notebookVm = remember { NotebookViewModel(container.repository) }
    val examVm = remember { MockExamViewModel(container.repository, container.preferencesRepository) }
    val plannerVm = remember { PlannerViewModel(container.appContext, container.repository, container.preferencesRepository) }
    val insightsVm = remember { InsightsViewModel(container.repository) }
    val studioVm = remember { QuestionStudioViewModel(container.repository) }
    val importVm = remember { ImportViewModel(container.appContext, container.repository) }
    val exportVm = remember { ExportViewModel(container.appContext, container.repository) }
    val alarmsVm = remember { AlarmsViewModel(container.appContext, container.repository) }
    val focusVm = remember { FocusViewModel(container.appContext, container.repository, container.preferencesRepository) }
    val settingsVm = remember { SettingsViewModel(container.appContext, container.repository, container.preferencesRepository) }
    val essentialsVm = remember { EssentialsViewModel(container.repository, container.preferencesRepository) }

    // Request POST_NOTIFICATIONS politely once if Android 13+
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.hasNotificationPermission(context)) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Handle App Shortcut or Notification intent routing
    LaunchedEffect(initialShortcutRoute) {
        when (initialShortcutRoute) {
            "study_smart" -> {
                studyVm.startSession(StudyMode.SMART_STUDY)
                navController.navigate(Routes.STUDY_SESSION)
            }
            "focus" -> navController.navigate(Routes.FOCUS_MODE)
            "exams" -> navController.navigate(PrimarySection.MOCK_EXAMS.route)
            "planner" -> navController.navigate(PrimarySection.DAILY_PLANNER.route)
            "alarms" -> navController.navigate(Routes.ALARMS)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: PrimarySection.DASHBOARD.route

    val isFullScreenRoute = currentRoute == Routes.STUDY_SESSION ||
        currentRoute == Routes.ACTIVE_EXAM ||
        currentRoute.startsWith("question_editor") ||
        currentRoute.startsWith("exam_result")

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(PrimarySection.DASHBOARD.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val bottomNavSections = remember {
        listOf(
            PrimarySection.DASHBOARD,
            PrimarySection.STUDY_LIBRARY,
            PrimarySection.MOCK_EXAMS,
            PrimarySection.NOTEBOOK,
            PrimarySection.DAILY_PLANNER
        )
    }

    val currentSectionTitle = remember(currentRoute) {
        when {
            currentRoute == Routes.FOCUS_MODE -> "Focus & Do Not Disturb"
            currentRoute == Routes.ALARMS -> "Study & Exam Alarms"
            else -> PrimarySection.entries.firstOrNull { it.route == currentRoute }?.title ?: "EUEE Mastery"
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 840.dp

        if (isFullScreenRoute) {
            EueeNavHost(
                navController = navController,
                container = container,
                dashboardVm = dashboardVm,
                studyVm = studyVm,
                notebookVm = notebookVm,
                examVm = examVm,
                plannerVm = plannerVm,
                insightsVm = insightsVm,
                studioVm = studioVm,
                importVm = importVm,
                exportVm = exportVm,
                alarmsVm = alarmsVm,
                focusVm = focusVm,
                settingsVm = settingsVm,
                essentialsVm = essentialsVm,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isWideScreen) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(modifier = Modifier.width(260.dp)) {
                        DrawerMenuContent(
                            currentRoute = currentRoute,
                            onSelectRoute = { route -> navigateTopLevel(route) },
                            onOpenFocus = { navController.navigate(Routes.FOCUS_MODE) },
                            onOpenAlarms = { navController.navigate(Routes.ALARMS) }
                        )
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(currentSectionTitle, fontWeight = FontWeight.Bold) },
                            actions = {
                                IconButton(onClick = { navController.navigate(Routes.FOCUS_MODE) }) {
                                    Icon(Icons.Default.SelfImprovement, contentDescription = "Focus Mode")
                                }
                                IconButton(onClick = { navController.navigate(Routes.ALARMS) }) {
                                    Icon(Icons.Default.Alarm, contentDescription = "Alarms")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    EueeNavHost(
                        navController = navController,
                        container = container,
                        dashboardVm = dashboardVm,
                        studyVm = studyVm,
                        notebookVm = notebookVm,
                        examVm = examVm,
                        plannerVm = plannerVm,
                        insightsVm = insightsVm,
                        studioVm = studioVm,
                        importVm = importVm,
                        exportVm = exportVm,
                        alarmsVm = alarmsVm,
                        focusVm = focusVm,
                        settingsVm = settingsVm,
                        essentialsVm = essentialsVm,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(modifier = Modifier.width(288.dp)) {
                        DrawerMenuContent(
                            currentRoute = currentRoute,
                            onSelectRoute = { route ->
                                scope.launch { drawerState.close() }
                                navigateTopLevel(route)
                            },
                            onOpenFocus = {
                                scope.launch { drawerState.close() }
                                navController.navigate(Routes.FOCUS_MODE)
                            },
                            onOpenAlarms = {
                                scope.launch { drawerState.close() }
                                navController.navigate(Routes.ALARMS)
                            }
                        )
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(currentSectionTitle, fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("open_drawer_btn")
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open navigation menu")
                                }
                            },
                            actions = {
                                IconButton(onClick = { navController.navigate(Routes.FOCUS_MODE) }) {
                                    Icon(Icons.Default.SelfImprovement, contentDescription = "Focus Mode")
                                }
                                IconButton(onClick = { navController.navigate(Routes.ALARMS) }) {
                                    Icon(Icons.Default.Alarm, contentDescription = "Alarms")
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            bottomNavSections.forEach { section ->
                                NavigationBarItem(
                                    selected = currentRoute == section.route,
                                    onClick = { navigateTopLevel(section.route) },
                                    icon = { Icon(section.icon, contentDescription = section.title) },
                                    label = { Text(section.shortTitle, maxLines = 1) },
                                    modifier = Modifier.testTag("bottom_nav_${section.route}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    EueeNavHost(
                        navController = navController,
                        container = container,
                        dashboardVm = dashboardVm,
                        studyVm = studyVm,
                        notebookVm = notebookVm,
                        examVm = examVm,
                        plannerVm = plannerVm,
                        insightsVm = insightsVm,
                        studioVm = studioVm,
                        importVm = importVm,
                        exportVm = exportVm,
                        alarmsVm = alarmsVm,
                        focusVm = focusVm,
                        settingsVm = settingsVm,
                        essentialsVm = essentialsVm,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerMenuContent(
    currentRoute: String,
    onSelectRoute: (String) -> Unit,
    onOpenFocus: () -> Unit,
    onOpenAlarms: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp, horizontal = 12.dp)
    ) {
        Text(
            text = "EUEE Mastery",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        Text(
            text = "Offline Ethiopian Entrance Suite",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(14.dp))

        PrimarySection.entries.forEach { section ->
            NavigationDrawerItem(
                label = { Text(section.title) },
                selected = currentRoute == section.route,
                onClick = { onSelectRoute(section.route) },
                icon = { Icon(section.icon, contentDescription = null) },
                modifier = Modifier.testTag("drawer_item_${section.route}")
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
        Text(
            "OS Productivity Tools",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        NavigationDrawerItem(
            label = { Text("Focus & DND Timer") },
            selected = currentRoute == Routes.FOCUS_MODE,
            onClick = onOpenFocus,
            icon = { Icon(Icons.Default.SelfImprovement, contentDescription = null) },
            modifier = Modifier.testTag("drawer_item_focus")
        )
        NavigationDrawerItem(
            label = { Text("Alarms & Reminders") },
            selected = currentRoute == Routes.ALARMS,
            onClick = onOpenAlarms,
            icon = { Icon(Icons.Default.Alarm, contentDescription = null) },
            modifier = Modifier.testTag("drawer_item_alarms")
        )
    }
}

@Composable
private fun EueeNavHost(
    navController: androidx.navigation.NavHostController,
    container: AppContainer,
    dashboardVm: DashboardViewModel,
    studyVm: StudyViewModel,
    notebookVm: NotebookViewModel,
    examVm: MockExamViewModel,
    plannerVm: PlannerViewModel,
    insightsVm: InsightsViewModel,
    studioVm: QuestionStudioViewModel,
    importVm: ImportViewModel,
    exportVm: ExportViewModel,
    alarmsVm: AlarmsViewModel,
    focusVm: FocusViewModel,
    settingsVm: SettingsViewModel,
    essentialsVm: EssentialsViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = PrimarySection.DASHBOARD.route,
        modifier = modifier
    ) {
        composable(PrimarySection.DASHBOARD.route) {
            DashboardScreen(
                viewModel = dashboardVm,
                onStartStudyMode = { mode ->
                    studyVm.startSession(mode)
                    navController.navigate(Routes.STUDY_SESSION)
                },
                onResumeSession = { sessionId ->
                    studyVm.resumeSession(sessionId)
                    navController.navigate(Routes.STUDY_SESSION)
                },
                onResumeExam = {
                    examVm.resumeSavedExam { navController.navigate(Routes.ACTIVE_EXAM) }
                },
                onOpenLibrary = { navController.navigate(PrimarySection.STUDY_LIBRARY.route) },
                onOpenExams = { navController.navigate(PrimarySection.MOCK_EXAMS.route) },
                onOpenFocus = { navController.navigate(Routes.FOCUS_MODE) },
                onOpenAlarms = { navController.navigate(Routes.ALARMS) },
                onOpenPlanner = { navController.navigate(PrimarySection.DAILY_PLANNER.route) },
                onOpenTools = { navController.navigate(PrimarySection.SCIENTIFIC_TOOLS.route) },
                onOpenEssentials = { navController.navigate(PrimarySection.STUDENT_ESSENTIALS.route) }
            )
        }
        composable(PrimarySection.STUDY_LIBRARY.route) {
            StudyLibraryScreen(
                viewModel = studyVm,
                onLaunchStudyMode = { mode, duration ->
                    studyVm.startSession(mode, timedDurationMinutes = duration)
                    navController.navigate(Routes.STUDY_SESSION)
                },
                onOpenQuestionEditor = { qId -> navController.navigate(Routes.questionEditor(qId)) }
            )
        }
        composable(Routes.STUDY_SESSION) {
            StudySessionScreen(
                viewModel = studyVm,
                speechHelper = container.speechHelper,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(PrimarySection.NOTEBOOK.route) {
            NotebookScreen(viewModel = notebookVm)
        }
        composable(PrimarySection.MOCK_EXAMS.route) {
            MockExamsScreen(
                viewModel = examVm,
                onStartExam = { navController.navigate(Routes.ACTIVE_EXAM) },
                onResumeExam = { navController.navigate(Routes.ACTIVE_EXAM) },
                onOpenResult = { rId -> navController.navigate(Routes.examResult(rId)) }
            )
        }
        composable(Routes.ACTIVE_EXAM) {
            ActiveExamScreen(
                viewModel = examVm,
                onExitToLobby = { navController.popBackStack() },
                onExamCompleted = { resultId ->
                    navController.navigate(Routes.examResult(resultId)) {
                        popUpTo(PrimarySection.MOCK_EXAMS.route)
                    }
                }
            )
        }
        composable(
            route = Routes.EXAM_RESULT,
            arguments = listOf(navArgument("resultId") { type = NavType.LongType })
        ) { backStack ->
            val rId = backStack.arguments?.getLong("resultId") ?: 0L
            ExamResultDetailScreen(
                resultId = rId,
                viewModel = examVm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(PrimarySection.DAILY_PLANNER.route) {
            DailyPlannerScreen(
                viewModel = plannerVm,
                onOpenAlarms = { navController.navigate(Routes.ALARMS) }
            )
        }
        composable(PrimarySection.INSIGHTS.route) {
            InsightsScreen(viewModel = insightsVm)
        }
        composable(PrimarySection.STUDENT_ESSENTIALS.route) {
            StudentEssentialsScreen(
                viewModel = essentialsVm,
                onStartWeakTopicsDrill = {
                    studyVm.startSession(StudyMode.WEAK_TOPICS)
                    navController.navigate(Routes.STUDY_SESSION)
                }
            )
        }
        composable(PrimarySection.SCIENTIFIC_TOOLS.route) {
            ScientificToolsScreen()
        }
        composable(PrimarySection.QUESTION_STUDIO.route) {
            QuestionStudioScreen(
                viewModel = studioVm,
                onCreateQuestion = { navController.navigate(Routes.questionEditor(-1L)) },
                onEditQuestion = { qId -> navController.navigate(Routes.questionEditor(qId)) }
            )
        }
        composable(
            route = Routes.QUESTION_EDITOR,
            arguments = listOf(navArgument("questionId") { type = NavType.LongType })
        ) { backStack ->
            val qId = backStack.arguments?.getLong("questionId") ?: -1L
            QuestionEditorScreen(
                questionId = qId,
                viewModel = studioVm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(PrimarySection.IMPORT_CENTER.route) {
            ImportCenterScreen(viewModel = importVm)
        }
        composable(PrimarySection.EXPORT_CENTER.route) {
            ExportCenterScreen(viewModel = exportVm)
        }
        composable(PrimarySection.SETTINGS.route) {
            SettingsScreen(
                viewModel = settingsVm,
                onOpenExports = { navController.navigate(PrimarySection.EXPORT_CENTER.route) }
            )
        }
        composable(Routes.FOCUS_MODE) {
            FocusModeScreen(viewModel = focusVm)
        }
        composable(Routes.ALARMS) {
            AlarmsScreen(viewModel = alarmsVm)
        }
    }
}
