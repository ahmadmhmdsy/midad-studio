package com.midad.studio.navigation

import android.net.Uri

object Routes {
    const val HOME = "home"
    const val PHOTO = "studio/photo"
    const val VIDEO = "studio/video"
    const val DESIGN = "studio/design"
    const val AI = "studio/ai"
    const val PROJECTS = "projects"
    const val SETTINGS = "settings"
    const val LICENSES = "licenses"

    /** Query argument that turns a studio destination into "open this saved project". */
    const val ARG_PROJECT_ID = "projectId"

    /** Optional tab for the library: 0 = projects, 1 = exports. */
    const val ARG_PROJECTS_TAB = "tab"

    /**
     * Studio destinations accept an optional project id. Navigating to the bare [StudioKind.route]
     * still matches the same destination with a null argument.
     */
    fun projectPattern(kind: StudioKind): String =
        kind.route + "?" + ARG_PROJECT_ID + "={" + ARG_PROJECT_ID + "}"

    fun projectDestination(kind: StudioKind, projectId: String): String =
        kind.route + "?" + ARG_PROJECT_ID + "=" + Uri.encode(projectId)

    fun projectsPattern(): String = PROJECTS + "?" + ARG_PROJECTS_TAB + "={" + ARG_PROJECTS_TAB + "}"

    fun projectsDestination(tab: Int): String = PROJECTS + "?" + ARG_PROJECTS_TAB + "=" + tab.coerceIn(0, 1)
}

enum class StudioKind(val route: String) {
    PHOTO(Routes.PHOTO),
    VIDEO(Routes.VIDEO),
    DESIGN(Routes.DESIGN),
    AI(Routes.AI);

    companion object {
        fun fromRoute(route: String?): StudioKind? = entries.firstOrNull { it.route == route }
    }
}
