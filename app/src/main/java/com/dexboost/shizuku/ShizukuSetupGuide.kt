package com.dexboost.shizuku

/**
 * Plain-text source content for the Shizuku setup guide.
 *
 * These strings are deliberately not Android string resources: the UI
 * layer owns resources (including localisation) and maps these English
 * source texts to resources. Nothing here is a fake or shortcut step:
 * every step reflects how Shizuku actually has to be started and
 * approved on a non-rooted device, where Shizuku provides ADB/shell
 * level access, not root.
 */
object ShizukuSetupGuide {

    /** One titled step of a guide. [title] is a short heading, [body] the detail. */
    data class SetupStep(val title: String, val body: String)

    /**
     * The real setup sequence, in order.
     */
    fun steps(): List<SetupStep> = listOf(
        SetupStep(
            title = "Install the Shizuku app",
            body = "Install the official Shizuku app (package moe.shizuku.privileged.api). " +
                "DexBoost cannot work without it: DexBoost itself does not provide " +
                "privileged access, it only talks to the Shizuku server that this app starts."
        ),
        SetupStep(
            title = "Enable Developer options and a debugging connection",
            body = "On Android 11 and later, turn on Developer options, then turn on " +
                "Wireless debugging. On older versions, or as an alternative, connect " +
                "the phone to a computer and authorise it for ADB over USB. " +
                "Shizuku needs one of these debugging connections to start its server; " +
                "without it, Shizuku cannot run."
        ),
        SetupStep(
            title = "Start Shizuku and pair it",
            body = "Open the Shizuku app and tap Start. If you are using wireless " +
                "debugging, use the pairing flow Shizuku shows: open the wireless " +
                "debugging pairing screen, enter the pairing code Shizuku displays, " +
                "and confirm when the notification asks. Wait until Shizuku reports " +
                "that it is running before returning to DexBoost."
        ),
        SetupStep(
            title = "Grant DexBoost permission when prompted",
            body = "Back in DexBoost, request Shizuku permission and tap Allow in the " +
                "Shizuku dialog. Until you allow it, DexBoost can detect that Shizuku " +
                "is running but cannot run any privileged command, and it will " +
                "report permission denied rather than running anything."
        ),
        SetupStep(
            title = "After a reboot, start Shizuku again",
            body = "The Shizuku server does not survive a reboot. After restarting the " +
                "phone, the wireless-debugging activation (and, if needed, the pairing " +
                "step) must be repeated and Shizuku started again before DexBoost can " +
                "run privileged commands. This is expected behaviour, not a fault in " +
                "DexBoost."
        )
    )

    /**
     * Troubleshooting entries for the failures users actually hit.
     * Each entry names the symptom, what it means, and what to do.
     */
    fun troubleshooting(): List<SetupStep> = listOf(
        SetupStep(
            title = "Shizuku is not running",
            body = "DexBoost cannot reach the Shizuku server. Open the Shizuku app " +
                "and check whether it reports running. If not, repeat the setup: " +
                "make sure wireless debugging (Android 11+) or ADB from a computer " +
                "is enabled, then tap Start in Shizuku and complete pairing. " +
                "If wireless debugging was turned off, for example after a reboot, " +
                "turn it on again first."
        ),
        SetupStep(
            title = "Permission denied",
            body = "Shizuku is running, but DexBoost has not been allowed to use it. " +
                "Request permission again from DexBoost and tap Allow in the Shizuku " +
                "dialog. If you previously tapped Deny, open the Shizuku app, find " +
                "DexBoost in its authorised apps list, and allow it there; a denied " +
                "permission is not retried automatically."
        ),
        SetupStep(
            title = "Binder died (connection lost while using DexBoost)",
            body = "The connection to the Shizuku server was lost, for example because " +
                "Shizuku was stopped or the phone's debugging connection dropped. " +
                "No command is retried automatically after this: open the Shizuku app, " +
                "confirm it is running again, allow DexBoost if asked, and then retry " +
                "the action in DexBoost. If it keeps happening, restart Shizuku from " +
                "its Start screen."
        ),
        SetupStep(
            title = "Shizuku app not found",
            body = "DexBoost could not find the Shizuku app on this phone. Check that " +
                "the installed app is the official Shizuku app with package " +
                "moe.shizuku.privileged.api; a similarly named app is not the same " +
                "thing and will not work. If it is not installed, install it and " +
                "then follow the setup steps. If it is installed but still not " +
                "found, reinstall it from its official source and try again."
        )
    )
}
