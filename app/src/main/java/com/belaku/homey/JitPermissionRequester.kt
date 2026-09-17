package com.belaku.homey

import android.app.AlertDialog
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor

/**
 * Requests runtime permissions *on demand*, i.e. at the moment the user taps the
 * feature that needs them, rather than dumping every permission dialog on the
 * user during first launch (which caused permission fatigue / high drop-off).
 *
 * Usage:
 *   private val permissionRequester by lazy { JitPermissionRequester(this) }
 *   ...
 *   permissionRequester.ensure(FeaturePermission.CONTACTS) { getFavoriteContacts(applicationContext) }
 *
 * Must be constructed while the Activity is still CREATED (e.g. as a field or in
 * onCreate) because it registers an ActivityResultLauncher.
 */
class JitPermissionRequester(private val activity: AppCompatActivity) {

    private var pending: FeaturePermission? = null
    private var onGranted: (() -> Unit)? = null
    private var onDenied: (() -> Unit)? = null

    private val launcher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val feature = pending
            val granted = result.values.isNotEmpty() && result.values.all { it }

            if (feature != null) {
                sharedPreferencesEditor.putBoolean(feature.prefKey, granted).apply()
                // Remember we already showed the system dialog once, so we can
                // route the user to Settings instead of silently doing nothing.
                sharedPreferencesEditor.putBoolean(feature.askedKey, true).apply()
            }

            if (granted) onGranted?.invoke() else onDenied?.invoke()

            pending = null
            onGranted = null
            onDenied = null
        }

    fun isGranted(feature: FeaturePermission): Boolean =
        feature.permissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PERMISSION_GRANTED
        }

    /**
     * Runs [onGranted] straight away if the feature's permissions are already held.
     * Otherwise shows a short, feature-specific rationale and then the system dialog.
     *
     * @param onDenied invoked when the user refuses (lets the caller degrade gracefully).
     */
    fun ensure(
        feature: FeaturePermission,
        onDenied: (() -> Unit)? = null,
        onGranted: () -> Unit
    ) {
        // Nothing to ask for on this API level (e.g. POST_NOTIFICATIONS pre-33).
        if (feature.permissions.isEmpty() || isGranted(feature)) {
            sharedPreferencesEditor.putBoolean(feature.prefKey, true).apply()
            onGranted()
            return
        }

        // A request is already in flight – ignore duplicate taps.
        if (pending != null) return

        val permanentlyDenied = sharedPreferences.getBoolean(feature.askedKey, false) &&
                feature.permissions.none { activity.shouldShowRequestPermissionRationale(it) }

        if (permanentlyDenied) {
            showSettingsDialog(feature, onDenied)
            return
        }

        this.pending = feature
        this.onGranted = onGranted
        this.onDenied = onDenied

        AlertDialog.Builder(activity)
            .setTitle(feature.rationaleTitle)
            .setMessage(feature.rationale)
            .setPositiveButton("Continue") { d, _ ->
                d.dismiss()
                launcher.launch(feature.permissions)
            }
            .setNegativeButton("Not now") { d, _ ->
                d.dismiss()
                this.pending = null
                this.onGranted = null
                this.onDenied = null
                onDenied?.invoke()
            }
            .show()
    }

    private fun showSettingsDialog(feature: FeaturePermission, onDenied: (() -> Unit)?) {
        AlertDialog.Builder(activity)
            .setTitle(feature.rationaleTitle)
            .setMessage("${feature.rationale}\n\nYou previously denied this. Enable it in Settings > Permissions to use the feature.")
            .setPositiveButton("Open settings") { d, _ ->
                d.dismiss()
                activity.startActivity(
                    android.content.Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.fromParts("package", activity.packageName, null)
                    )
                )
            }
            .setNegativeButton("Not now") { d, _ ->
                d.dismiss()
                onDenied?.invoke()
            }
            .show()
    }
}
