package com.midad.studio

import android.app.Application
import android.content.ComponentCallbacks2
import android.util.Log
import com.midad.studio.core.export.ExportManager
import com.midad.studio.studio.ai.AiBootstrap

class MidadApp : Application() {

    /**
     * A cut-out holds a native interpreter, a full-size mask and several full-size ARGB
     * bitmaps at once. On a phone with little free memory that is enough to be killed, so
     * the interpreter is released as soon as the app leaves the foreground or the system
     * reports pressure; it reloads on the next cut-out.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            AiBootstrap.releaseInterpreter()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        AiBootstrap.releaseInterpreter()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Wire the on-device AI implementations once, before any studio can look them up.
        // A failure here must never prevent the app from starting: the AI Lab degrades to
        // "no models" and every other studio keeps working.
        runCatching { AiBootstrap.install(this) }
            .onFailure { Log.w("MidadApp", "AI bootstrap failed", it) }
        // A process killed between MediaStore insert and publish leaves a pending row.
        // Sweep only rows owned by this package, off the main thread.
        Thread {
            runCatching { ExportManager.cleanupPendingExports(this) }
                .onFailure { Log.w("MidadApp", "Pending export cleanup failed", it) }
        }.start()
    }

    companion object {
        lateinit var instance: MidadApp
            private set
    }
}
