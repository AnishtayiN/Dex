package com.dexboost.shizuku

import android.app.Activity

/**
 * Small wrapper around [ShizukuManager] for the one permission question
 * the UI needs to ask: "may DexBoost run privileged commands right now?"
 *
 * Honest by design: Shizuku permission is granted by the user through
 * a system dialog that this code cannot answer on their behalf, so
 * there is no blocking "wait for the dialog" behaviour here and no
 * granted result is ever fabricated.
 */
class ShizukuPermissionController(private val manager: ShizukuManager) {

    /**
     * Returns `true` only if Shizuku is already [ShizukuState.Ready].
     *
     * Otherwise it triggers [ShizukuManager.requestPermission] (a
     * no-op when the binder is not alive) and returns `false`
     * immediately. The caller must observe [ShizukuManager.state] for
     * the result: when the user answers the dialog, the manager
     * refreshes its state to [ShizukuState.Ready] or
     * [ShizukuState.PermissionDenied], and the caller can then retry
     * or show the setup guide.
     *
     * This function is `suspend` to match the call-site contract of
     * the permission flow, even though it does not itself suspend:
     * waiting here for a user-driven dialog would block the caller
     * for an unbounded time and would risk being misread as a
     * synchronous grant check. It never returns `true` for a
     * permission that has not actually been granted.
     */
    suspend fun ensurePermission(activity: Activity): Boolean {
        if (manager.state.value == ShizukuState.Ready) return true
        manager.requestPermission(activity)
        return false
    }
}
