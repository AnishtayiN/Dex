package com.dexboost.viewmodel

import android.app.Activity
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import com.dexboost.shizuku.ShizukuManager
import com.dexboost.shizuku.ShizukuState
import kotlinx.coroutines.flow.StateFlow

class ShizukuViewModel(
    private val manager: ShizukuManager
) : ViewModel() {

    val state: StateFlow<ShizukuState> = manager.state

    init {
        refresh()
    }

    /** ShizukuManager.refresh() is synchronous and never throws. */
    fun refresh() {
        manager.refresh()
    }

    /** ShizukuManager.requestPermission() is synchronous and never throws. */
    fun requestPermission(activity: Activity) {
        manager.requestPermission(activity)
    }

    /**
     * Whether the official Shizuku app package is installed and visible.
     * The manager never produces NotInstalled itself (it only observes
     * the binder), so screens combine this with [state]: not present
     * means NotInstalled, present but binder dead means NotRunning.
     */
    fun isShizukuAppPresent(packageManager: PackageManager): Boolean {
        return manager.isShizukuAppPresent(packageManager)
    }
}
