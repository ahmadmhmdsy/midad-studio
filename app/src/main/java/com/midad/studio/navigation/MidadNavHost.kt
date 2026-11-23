package com.midad.studio.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.midad.studio.home.HomeScreen
import com.midad.studio.projects.ProjectsScreen
import com.midad.studio.settings.LicensesScreen
import com.midad.studio.settings.SettingsScreen
import com.midad.studio.studio.ai.AiStudioScreen
import com.midad.studio.studio.design.DesignStudioScreen
import com.midad.studio.studio.photo.PhotoStudioScreen
import com.midad.studio.studio.video.VideoStudioScreen

@Composable
fun MidadNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenStudio = { kind -> navController.navigate(kind.route) },
                onOpenProjects = { navController.navigate(Routes.projectsDestination(0)) },
                onOpenExports = { navController.navigate(Routes.projectsDestination(1)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = Routes.projectPattern(StudioKind.PHOTO),
            arguments = projectArguments(),
        ) { entry ->
            PhotoStudioScreen(
                projectId = entry.arguments?.getString(Routes.ARG_PROJECT_ID),
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.projectPattern(StudioKind.VIDEO),
            arguments = projectArguments(),
        ) { entry ->
            VideoStudioScreen(
                projectId = entry.arguments?.getString(Routes.ARG_PROJECT_ID),
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.projectPattern(StudioKind.DESIGN),
            arguments = projectArguments(),
        ) { entry ->
            DesignStudioScreen(
                projectId = entry.arguments?.getString(Routes.ARG_PROJECT_ID),
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.projectPattern(StudioKind.AI),
            arguments = projectArguments(),
        ) { entry ->
            AiStudioScreen(
                projectId = entry.arguments?.getString(Routes.ARG_PROJECT_ID),
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.projectsPattern(),
            arguments = listOf(
                navArgument(Routes.ARG_PROJECTS_TAB) {
                    type = NavType.IntType
                    defaultValue = 0
                },
            ),
        ) { entry ->
            ProjectsScreen(
                onBack = { navController.popBackStack() },
                onNewProject = { kind -> navController.navigate(kind.route) },
                onOpenProject = { kind, projectId ->
                    navController.navigate(Routes.projectDestination(kind, projectId))
                },
                initialTab = entry.arguments?.getInt(Routes.ARG_PROJECTS_TAB) ?: 0,
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenModels = { navController.navigate(Routes.AI) },
                onOpenLicenses = { navController.navigate(Routes.LICENSES) },
            )
        }
        composable(Routes.LICENSES) {
            LicensesScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun projectArguments() = listOf(
    navArgument(Routes.ARG_PROJECT_ID) {
        type = NavType.StringType
        nullable = true
        defaultValue = null
    },
)
