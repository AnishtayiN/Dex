package com.dexboost.shizuku

import android.app.Activity
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

/**
 * The state of the Shizuku connection, as far as DexBoost can observe it.
 *
 * The Shizuku API has no direct "is the Shizuku app installed" query, so
 * [ShizukuState.NotInstalled] is intentionally not produced by [refresh]:
 * [refresh] only observes the binder. The UI combines the two signals:
 * if [isShizukuAppPresent] returns `false`, show NotInstalled; if the
 * app is present but the binder is dead, [ShizukuState.NotRunning] is
 * the correct state. Keeping those two checks separate is honest about
 * what each one actually proves.
 */
sealed class ShizukuState {
    /** The Shizuku app package was not found by [ShizukuManager.isShizukuAppPresent]. */
    object NotInstalled : ShizukuState()

    /** The Shizuku server binder is not reachable (Shizuku not started). */
    object NotRunning : ShizukuState()

    /** The binder was alive and then died; a [refresh] is already triggered. */
    object BinderDead : ShizukuState()

    /** The binder is alive, but DexBoost has not been granted permission. */
    object PermissionDenied : ShizukuState()

    /** The binder is alive and DexBoost has permission: commands may run. */
    object Ready : ShizukuState()

    /** A Shizuku call failed in a way that fits no other state. */
    object Unknown : ShizukuState()
}

/**
 * Observes the Shizuku binder and permission state using the real
 * Shizuku 13.1.5 API ([rikka.shizuku.Shizuku]).
 *
 * No-arg constructor on purpose, so the state logic stays testable
 * without an Android [android.content.Context]: nothing here needs a
 * Context except [isShizukuAppPresent], which takes the
 * [PackageManager] it needs as a parameter instead.
 *
 * Every Shizuku call is guarded so a failure never throws to the UI:
 * binder problems surface as [ShizukuState.NotRunning] (or
 * [ShizukuState.BinderDead] from the dead-listener), permission-check
 * problems as [ShizukuState.Unknown]. In environments without the
 * Shizuku classes at all, listener registration and [refresh] degrade
 * to [ShizukuState.Unknown] instead of crashing.
 *
 * Shizuku here means ADB/shell-level privilege from the Shizuku server
 * process. It is not root, and this class never claims otherwise.
 */
class ShizukuManager {

    private val _state = MutableStateFlow<ShizukuState>(ShizukuState.Unknown)

    /** Current Shizuku state; updated by [refresh] and by binder listeners. */
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    /**
     * Optional callback invoked with the grant result whenever a
     * permission request completes, before the state is refreshed.
     * Set with [setPermissionResultCallback]. The Activity-forwarded
     * path ([onRequestPermissionResult]) invokes it too, so callers
     * only need one of the two delivery paths, not both.
     */
    private var pendingPermissionCallback: ((Int) -> Unit)? = null

    private val binderReceivedListener = object : Shizuku.OnBinderReceivedListener {
        override fun onBinderReceived() {
            refresh()
        }
    }

    private val binderDeadListener = object : Shizuku.OnBinderDeadListener {
        override fun onBinderDead() {
            _state.value = ShizukuState.BinderDead
            refresh()
        }
    }

    private val permissionResultListener =
        object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (requestCode != REQUEST_CODE) return
                invokePendingCallback(grantResult)
                refresh()
            }
        }

    init {
        try {
            Shizuku.addBinderReceivedListener(binderReceivedListener)
        } catch (_: Throwable) {
            // No Shizuku runtime in this environment; refresh() below
            // will report Unknown instead of crashing.
        }
        try {
            Shizuku.addBinderDeadListener(binderDeadListener)
        } catch (_: Throwable) {
            // See above.
        }
        try {
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (_: Throwable) {
            // See above.
        }
        refresh()
    }

    /**
     * Recomputes [state] from the live binder:
     * - binder not reachable -> [ShizukuState.NotRunning]
     * - binder alive, permission not granted -> [ShizukuState.PermissionDenied]
     * - binder alive, permission granted -> [ShizukuState.Ready]
     *
     * Deliberately uses only [Shizuku.pingBinder] and
     * [Shizuku.checkSelfPermission], the two stable public queries for
     * this decision. No server-version call is needed to answer it, so
     * none is made and none can fail here.
     *
     * Never throws.
     */
    fun refresh() {
        val binderAlive: Boolean = try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            _state.value = ShizukuState.NotRunning
            return
        }
        if (!binderAlive) {
            _state.value = ShizukuState.NotRunning
            return
        }
        val granted: Boolean = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            _state.value = ShizukuState.Unknown
            return
        }
        _state.value = if (granted) ShizukuState.Ready else ShizukuState.PermissionDenied
    }

    /**
     * Checks whether the official Shizuku manager app is installed.
     *
     * The Shizuku app's package is `moe.shizuku.privileged.api`. This
     * only proves the package is installed and visible to DexBoost; it
     * says nothing about whether the Shizuku server is currently
     * running. Combine it with [state]: not present -> NotInstalled,
     * present but binder dead -> NotRunning.
     *
     * Note for the UI layer: on Android 11+ package visibility rules
     * apply, so a `false` here means "not detected by DexBoost", which
     * the UI reports as NotInstalled per the agreed mapping. It is
     * never used to claim the server is running.
     *
     * Never throws.
     */
    @Suppress("DEPRECATION")
    fun isShizukuAppPresent(packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(SHIZUKU_APP_PACKAGE, 0)
            true
        } catch (_: NameNotFoundException) {
            false
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Sets (or clears, with `null`) the callback invoked when a
     * permission request completes. See [pendingPermissionCallback].
     */
    fun setPermissionResultCallback(callback: ((Int) -> Unit)?) {
        pendingPermissionCallback = callback
    }

    /**
     * Asks Shizuku to show its permission dialog for DexBoost.
     *
     * Only acts when the binder is alive; otherwise it is a no-op,
     * because the dialog cannot be shown when Shizuku is not running.
     * The [activity] parameter is part of the call-site contract (the
     * Activity owns the request lifecycle and forwards its result via
     * [onRequestPermissionResult]); the Shizuku API itself takes only
     * the request code, so the Activity is not otherwise used here.
     *
     * This is asynchronous: the result arrives later through the
     * permission-result listener or through [onRequestPermissionResult].
     * Callers must observe [state] for the outcome; this function
     * returns nothing about the user's decision.
     *
     * Never throws.
     */
    fun requestPermission(activity: Activity) {
        try {
            if (!Shizuku.pingBinder()) return
        } catch (_: Throwable) {
            return
        }
        try {
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (_: Throwable) {
            // Binder died between the check and the request; the
            // dead-listener (or the next refresh) reports NotRunning.
            _state.value = ShizukuState.NotRunning
        }
    }

    /**
     * Forwards a permission result from the hosting Activity.
     *
     * Shizuku delivers its result through its own listener, but the
     * Activity is the component that owns the request lifecycle, so
     * it forwards here as well; both paths invoke the pending
     * callback (if any) and then refresh the state. Results for other
     * request codes are ignored.
     *
     * Never throws.
     */
    fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode != REQUEST_CODE) return
        try {
            invokePendingCallback(grantResult)
        } catch (_: Throwable) {
            // A failing callback must not break state reporting.
        }
        refresh()
    }

    private fun invokePendingCallback(grantResult: Int) {
        val callback = pendingPermissionCallback
        pendingPermissionCallback = null
        try {
            callback?.invoke(grantResult)
        } catch (_: Throwable) {
            // Caller callback failure must not break state reporting.
        }
    }

    companion object {
        /** Request code used for every Shizuku permission request. */
        const val REQUEST_CODE = 1001

        /** Package of the official Shizuku manager app. */
        const val SHIZUKU_APP_PACKAGE = "moe.shizuku.privileged.api"
    }
}
