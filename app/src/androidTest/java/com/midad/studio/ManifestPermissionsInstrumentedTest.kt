package com.midad.studio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The product promise is exactly one Android permission: INTERNET (AI model download).
 * Any future SDK or dependency that quietly adds a permission fails this test.
 */
@RunWith(AndroidJUnit4::class)
class ManifestPermissionsInstrumentedTest {

    @Test
    fun onlyInternetPermissionIsRequested() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions?.toSet().orEmpty()
        // AndroidX registers its receivers with an app-private signature permission, which the
        // system merges into the manifest at build time. It is not a user-visible permission:
        // ignore the app's own namespace and assert that nothing else was added.
        val userVisible = requested.filterNot { it.startsWith(context.packageName + ".") }
        assertEquals(setOf(Manifest.permission.INTERNET), userVisible.toSet())
    }
}
