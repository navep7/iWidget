package com.belaku.homey

import android.Manifest
import android.os.Build

/**
 * Groups runtime permissions by the *widget feature* that actually needs them,
 * so permissions can be asked just-in-time instead of all at once on first launch.
 *
 * [prefKey] is kept identical to the legacy SharedPreferences flags used by
 * MainActivity / NewAppWidget so existing state keeps working.
 */
enum class FeaturePermission(
    val permissions: Array<String>,
    val rationaleTitle: String,
    val rationale: String,
    val prefKey: String
) {

    /** "① Steps Count" tile */
    STEPS(
        permissions = arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
        rationaleTitle = "Show your step count?",
        rationale = "Physical Activity access lets the widget recognise walking and display \"① Steps Count\".",
        prefKey = "ARP"
    ),

    CONTACTS(
        permissions = arrayOf(Manifest.permission.READ_CONTACTS),
        rationaleTitle = "to get Google account info",
        rationale = "to Display Google account initials.",
        prefKey = "RC"
    ),

    /** "② Place Info" tile */
    PLACE_INFO(
        permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
        rationaleTitle = "Show place info?",
        rationale = "Device location lets the widget display \"② Place Info\" (city, weather) for where you are.",
        prefKey = "LP"
    ),

    /** Dialling straight from the widget */
    CALL(
        permissions = arrayOf(Manifest.permission.CALL_PHONE),
        rationaleTitle = "Dial directly from the widget?",
        rationale = "Phone access lets the widget place the call immediately instead of only opening the dialler.",
        prefKey = "CPP"
    ),

    /** Bluetooth status indicator */
    BLUETOOTH(
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT) else emptyArray(),
        rationaleTitle = "Show Bluetooth status?",
        rationale = "Nearby Devices access lets the widget indicate whether Bluetooth is connected.",
        prefKey = "BP"
    ),

    /** Reminder notifications */
    REMINDERS(
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray(),
        rationaleTitle = "Notify you about reminders?",
        rationale = "Notification access is needed so the reminders you set can actually alert you.",
        prefKey = "PNP"
    ),

    /** Read all incoming notifications for voice narration */
    NOTIFICATIONS(
        permissions = emptyArray(),
        rationaleTitle = "Permission Request to Read all incoming notifications",
        rationale = "App needs permission to Read all incoming notifications to notify you with Voice..",
        prefKey = "NRO"
    ),

    /** App usage statistics for screen time and frequent apps */
    USAGE_STATS(
        permissions = emptyArray(), // Special permission, handled manually
        rationaleTitle = "Show app usage?",
        rationale = "App Usage access lets the widget display your screen time and suggest frequent apps.",
        prefKey = "AUS"
    );

    /** Key used to remember that the system dialog was already shown once. */
    val askedKey: String get() = "asked_$prefKey"
}
