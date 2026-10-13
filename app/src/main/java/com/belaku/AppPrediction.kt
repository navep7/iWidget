package com.belaku

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build


data class AppPrediction(val packageName: String, val launchCount: Int)

fun getPredictedAppsForCurrentTimeWindow(context: Context): ArrayList<AppPrediction> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        ?: return arrayListOf()

    val launcherPackages = getLauncherPackages(context)


    val currentCalendar = Calendar.getInstance()
    val currentMinutesOfDay = (currentCalendar.get(Calendar.HOUR_OF_DAY) * 60) + currentCalendar.get(Calendar.MINUTE)

    // Boundaries of our target 15-minute window (with an extra 15min smoothing padding)
    val windowStartMinutes = currentMinutesOfDay - 15
    val windowEndMinutes = currentMinutesOfDay + 15

    // Track launch frequencies strictly for this target window across history
    val appLaunchCounts = mutableMapOf<String, Int>()

    // Look back over the past 7 days
    val endTime = System.currentTimeMillis()
    val startTime = endTime - (7 * 24 * 60 * 60 * 1000L)

    val events = usageStatsManager.queryEvents(startTime, endTime)
    val event = UsageEvents.Event()

    while (events.hasNextEvent()) {
        events.getNextEvent(event)

        // Filter specifically for explicit user launches (Activity Resumed)
        if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
            val eventCalendar = Calendar.getInstance().apply { timeInMillis = event.timeStamp }
            val eventMinutesOfDay = (eventCalendar.get(Calendar.HOUR_OF_DAY) * 60) + eventCalendar.get(Calendar.MINUTE)

            // Handle standard windows as well as wraps over midnight boundaries
            val matchesWindow = if (windowStartMinutes >= 0 && windowEndMinutes <= 1440) {
                eventMinutesOfDay in windowStartMinutes..windowEndMinutes
            } else {
                // Midnight boundary wrap logic
                val normalizedStart = (windowStartMinutes + 1440) % 1440
                val normalizedEnd = (windowEndMinutes + 1440) % 1440
                eventMinutesOfDay >= normalizedStart || eventMinutesOfDay <= normalizedEnd
            }

            if (matchesWindow) {
                val pkg = event.packageName
                // Filter out system launchers or your own package if required
                if (pkg != null && pkg != context.packageName && !launcherPackages.contains(pkg) && isAppLaunchable(context, pkg) && isAppInstalled(context, pkg)) {
                    appLaunchCounts[pkg] = (appLaunchCounts[pkg] ?: 0) + 1
                }
            }
        }
    }


    // Sort descending by weight frequency and limit to top 3 results
    val results = appLaunchCounts.map { AppPrediction(it.key, it.value) }
        .sortedByDescending { it.launchCount }
        .take(3)

    return ArrayList(results)
}

private fun isAppLaunchable(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getLaunchIntentForPackage(packageName) != null
    } catch (_: Exception) {
        false
    }
}

private fun isAppInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}

fun getLauncherPackages(context: Context): Set<String> {
    val pm = context.packageManager
    val launcherPackages = mutableSetOf<String>()

    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
    }

    // Query both legacy and modern Android SDK configurations
    val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }

    for (resolveInfo in resolveInfos) {
        resolveInfo.activityInfo?.packageName?.let {
            launcherPackages.add(it)
        }
    }

    return launcherPackages
}
