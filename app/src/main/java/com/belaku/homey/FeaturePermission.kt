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

    /** "② Place Info" tile */
    PLACE_INFO(
        permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
        rationaleTitle = "Show place info?",
        rationale = "Device location lets the widget display \"② Place Info\" (city, weather) for where you are.",
        prefKey = "LP"
    ),

    /** "④ Favorite Contacts" tile */
    CONTACTS(
        permissions = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        ),
        rationaleTitle = "Show your favourite contacts?",
        rationale = "Contacts access lets the widget list your \"④ Favorite Contacts\" so you can dial them in one tap.",
        prefKey = "RCP"
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
    );

    /** Key used to remember that the system dialog was already shown once. */
    val askedKey: String get() = "asked_$prefKey"
}
