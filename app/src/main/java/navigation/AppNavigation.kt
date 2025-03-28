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



// Define all the screens in our app
sealed class Screen(val route: String) {
    // Authentication screens
    object Login : Screen("login")
    object SignUp : Screen("signup")

    object Home : Screen("home")
    object NewEntry : Screen("new_entry")
    object EntryDetail : Screen("entry_detail/{entryId}") {
        fun createRoute(entryId: String) = "entry_detail/$entryId"
    }
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Home.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Define the navigation graph here

        composable(Screen.Login.route) {
            // LoginScreen will be created later
            // LoginScreen(navController)
        }

        composable(Screen.SignUp.route) {
            // SignUpScreen will be created later
            // SignUpScreen(navController)
        }
        // Main app routes - add these inside the NavHost lambda
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(Screen.NewEntry.route) {
            val viewModel = remember { JournalEntryViewModel() }
            JournalEntryScreen(navController = navController, viewModel = viewModel)
        }

// For EntryDetail, we need to pass a parameter
        composable(
            route = Screen.EntryDetail.route,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
            // EntryDetailScreen will be created later
            // EntryDetailScreen(navController, entryId)
        }

        composable(Screen.Settings.route) {
            // SettingsScreen will be created later
            // SettingsScreen(navController)
        }
    }
}