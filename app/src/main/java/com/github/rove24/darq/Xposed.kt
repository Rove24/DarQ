package com.github.rove24.darq

import android.annotation.SuppressLint
import android.app.Activity
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import android.view.View
import com.kieronquinn.app.darq.BuildConfig
import com.kieronquinn.app.darq.components.settings.XposedSharedPreferences
import com.kieronquinn.app.darq.model.xposed.XposedSettings
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class Xposed : XposedModule() {

    companion object {
        private const val TAG = "DarQXposed"
        private const val SHARED_PREFS_FILENAME = "${BuildConfig.APPLICATION_ID}_prefs"
    }

    private var xposedSettings: XposedSettings? = null

    private val isDarkMode: Boolean
        get() = try {
            (Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        } catch (t: Throwable) {
            false
        }

    override fun onPackageReady(param: PackageReadyParam) {
        val packageName = param.packageName
        val classLoader = param.classLoader

        if (packageName == BuildConfig.APPLICATION_ID) {
            setupSelfHooks(classLoader)
            return
        }

        setupAppHooks(packageName, classLoader)
    }

    private fun setupSelfHooks(classLoader: ClassLoader) {
        try {
            val selfHooksClass = classLoader.loadClass("com.kieronquinn.app.darq.model.xposed.XposedSelfHooks")
            val isEnabledMethod = selfHooksClass.getDeclaredMethod("isXposedModuleEnabled")
            hook(isEnabledMethod).intercept {
                true
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to hook XposedSelfHooks", t)
        }
    }

    @SuppressLint("BlockedPrivateApi", "DiscouragedPrivateApi")
    private fun setupAppHooks(packageName: String, classLoader: ClassLoader) {
        // Hook View.setForceDarkAllowed(boolean)
        try {
            val setForceDarkAllowedMethod = View::class.java.getDeclaredMethod("setForceDarkAllowed", Boolean::class.javaPrimitiveType)
            hook(setForceDarkAllowedMethod).intercept { chain ->
                if (xposedSettings == null) {
                    xposedSettings = getXposedSettings(packageName)
                }
                if (xposedSettings?.enabled == true) {
                    chain.proceed(arrayOf(true))
                } else {
                    chain.proceed()
                }
            }
        } catch (t: Throwable) {
            // View may not have method or already hooked
        }

        // Hook Activity.onResume() for status bar color fix
        try {
            val onResumeMethod = Activity::class.java.getDeclaredMethod("onResume")
            hook(onResumeMethod).intercept { chain ->
                val result = chain.proceed()
                if (xposedSettings == null) {
                    xposedSettings = getXposedSettings(packageName)
                }
                if (xposedSettings?.enabled == true && xposedSettings?.invertStatus == true) {
                    val activity = chain.thisObject as? Activity
                    if (activity != null && isDarkMode) {
                        activity.window?.decorView?.post {
                            activity.window?.decorView?.run {
                                systemUiVisibility = systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                            }
                        }
                    }
                }
                result
            }
        } catch (t: Throwable) {
            // Activity onResume hook failed
        }

        // Hook HardwareRenderer.setForceDark(boolean)
        try {
            val hardwareRendererClass = classLoader.loadClass("android.graphics.HardwareRenderer")
            val setForceDarkMethod = hardwareRendererClass.getDeclaredMethod("setForceDark", Boolean::class.javaPrimitiveType)
            hook(setForceDarkMethod).intercept { chain ->
                if (xposedSettings == null) {
                    xposedSettings = getXposedSettings(packageName)
                }
                if (xposedSettings?.enabled == true && xposedSettings?.aggressiveDark == true) {
                    chain.proceed(arrayOf(isDarkMode))
                } else {
                    chain.proceed()
                }
            }
        } catch (t: Throwable) {
            // HardwareRenderer not found
        }
    }

    private fun getXposedSettings(targetPackageName: String): XposedSettings? {
        return try {
            val remotePrefs: SharedPreferences = getRemotePreferences(SHARED_PREFS_FILENAME)
            val xposedPreferences = XposedSharedPreferences(remotePrefs)
            val darqEnabled = xposedPreferences.enabled
            val appSelected = xposedPreferences.enabledApps.contains(targetPackageName)
            val alwaysUseForceDark = xposedPreferences.alwaysForceDark
            if (!darqEnabled || (!appSelected && !alwaysUseForceDark)) {
                XposedSettings(enabled = false)
            } else {
                XposedSettings(
                    enabled = true,
                    aggressiveDark = xposedPreferences.xposedAggressiveDark,
                    invertStatus = xposedPreferences.xposedInvertStatus
                )
            }
        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to get XposedSettings for $targetPackageName", e)
            }
            null
        }
    }
}
