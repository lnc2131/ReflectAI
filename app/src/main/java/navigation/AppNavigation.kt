package navigation
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.compose.ui.Modifier
import screens.HomeScreen
import androidx.compose.runtime.remember
import screens.JournalEntryViewModel
import screens.JournalEntryScreen
import screens.LoginScreen
import screens.TestAPIScreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter



// Define all the screens in our app
sealed class Screen(val route: String) {
    // Authentication screens
    object Login : Screen("login")
    object SignUp : Screen("signup")

    object Home : Screen("home")
    object NewEntry : Screen("new_entry")
    
    // Entry with date parameter for creating entries on specific dates
    object NewEntryWithDate : Screen("new_entry/{date}") {
        fun createRoute(date: String) = "new_entry/$date"
    }
    
    // View/edit entry for a specific date
    object EntryForDate : Screen("entry_for_date/{date}") {
        fun createRoute(date: String) = "entry_for_date/$date"
    }
    
    // Detail view for a specific entry by ID
    object EntryDetail : Screen("entry_detail/{entryId}") {
        fun createRoute(entryId: String) = "entry_detail/$entryId"
    }
    
    object Settings : Screen("settings")
    object TestAPI : Screen("test_api")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Login.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Define the navigation graph here

        composable(Screen.Login.route) {
            LoginScreen(navController)
        }

        composable(Screen.SignUp.route) {
            LoginScreen(navController, isSignUp = true)
        }
        // Main app routes - add these inside the NavHost lambda
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(Screen.NewEntry.route) {
            // Today's date as default
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val viewModel = remember { JournalEntryViewModel(initialDate = today) }
            JournalEntryScreen(
                viewModel = viewModel,
                onBackClick = { navController.navigateUp() },
                initialDate = today,
                navController = navController
            )
        }
        
        // New entry with specific date
        composable(
            route = Screen.NewEntryWithDate.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val dateString = backStackEntry.arguments?.getString("date") ?: ""
            val viewModel = remember { JournalEntryViewModel(initialDate = dateString) }
            JournalEntryScreen(
                viewModel = viewModel,
                onBackClick = { navController.navigateUp() },
                initialDate = dateString,
                navController = navController
            )
        }
        
        // View/edit entry for a specific date
        composable(
            route = Screen.EntryForDate.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val dateString = backStackEntry.arguments?.getString("date") ?: ""
            println("EntryForDate route: dateString=$dateString")
            val viewModel = remember { JournalEntryViewModel(initialDate = dateString) }
            JournalEntryScreen(
                viewModel = viewModel,
                onBackClick = { navController.navigateUp() },
                initialDate = dateString,
                isExistingEntry = true,
                navController = navController
            )
        }

        // For EntryDetail, we need to pass a parameter
        composable(
            route = Screen.EntryDetail.route,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
            val viewModel = remember { JournalEntryViewModel(entryId = entryId) }
            JournalEntryScreen(
                viewModel = viewModel,
                onBackClick = { navController.navigateUp() },
                entryId = entryId,
                isExistingEntry = true,
                navController = navController
            )
        }

        composable(Screen.Settings.route) {
            // SettingsScreen will be created later
            // SettingsScreen(navController)
        }

        composable(Screen.TestAPI.route) {
            TestAPIScreen(onBackClick = { navController.navigateUp() })
        }
    }
}