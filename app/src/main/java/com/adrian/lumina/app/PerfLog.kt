package com.adrian.lumina.app

import android.os.Process
import android.os.SystemClock
import android.util.Log

/** Hitos de arranque medidos desde el inicio del proceso (`adb logcat -s LuminaPerf`). Se registra solo la primera vez. */
object PerfLog {
    private val seen = HashSet<String>()

    @Synchronized
    fun mark(name: String) {
        if (!seen.add(name)) return
        Log.i("LuminaPerf", "$name: ${SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()} ms desde el inicio del proceso")
    }
}
