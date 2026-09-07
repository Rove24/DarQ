package com.kieronquinn.app.darq.providers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.DeadObjectException
import android.os.IBinder
import com.kieronquinn.app.darq.BuildConfig
import com.kieronquinn.app.darq.IDarqService
import com.kieronquinn.app.darq.components.settings.DarqSharedPreferences
import com.kieronquinn.app.darq.model.shizuku.ShizukuConstants
import com.kieronquinn.app.darq.service.impl.DarqService
import com.kieronquinn.app.darq.service.root.DarqRootService
import com.kieronquinn.app.darq.utils.extensions.isShizukuInstalled
import com.kieronquinn.app.darq.utils.extensions.suspendCoroutineWithTimeout
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class DarqServiceConnectionProvider(private val context: Context, private val settings: DarqSharedPreferences) {

    companion object {
        private const val FIRST_ATTEMPT_TIMEOUT = 7000L
        private const val RETRY_ATTEMPT_TIMEOUT = 8000L
    }

    private val rootServiceIntent = Intent(context, DarqRootService::class.java)
    private var rootService: IDarqService? = null
    private val serviceLock = Mutex()

    private val darqProcessArgs = UserServiceArgs(ComponentName(context, DarqService::class.java)).apply {
        processNameSuffix(ShizukuConstants.SERVICE_NAME)
        debuggable(BuildConfig.DEBUG)
        version(BuildConfig.VERSION_CODE)
    }

    sealed class ServiceResult {
        data class Success(val service: IDarqService, val serviceType: ServiceType): ServiceResult()
        data class Failed(val reason: ServiceFailureReason): ServiceResult()
    }

    enum class ServiceFailureReason {
        TIMEOUT, SHIZUKU_PERMISSION_REQUIRED, SHIZUKU_NOT_STARTED, SHIZUKU_NOT_INSTALLED
    }

    enum class ServiceType {
        SHIZUKU, ROOT, UNKNOWN
    }

    private var serviceType: ServiceType = ServiceType.UNKNOWN

    fun isServiceConnected(): Boolean {
        val service = rootService ?: return false
        return try {
            service.ping()
            true
        } catch (e: Exception) {
            rootService = null
            false
        }
    }

    suspend fun getService(): ServiceResult {
        return serviceLock.withLock {
            withContext(Dispatchers.Main) {
                // 1. Fast path: reuse already connected, alive service
                rootService?.let { service ->
                    try {
                        service.ping()
                        return@withContext ServiceResult.Success(service, serviceType)
                    } catch (e: Exception) {
                        rootService = null
                    }
                }

                // 2. First attempt
                val firstResult = getServiceLocked(FIRST_ATTEMPT_TIMEOUT)
                if (firstResult is ServiceResult.Success) {
                    return@withContext firstResult
                }

                // If failed due to permissions or installation, do not retry
                if (firstResult is ServiceResult.Failed &&
                    firstResult.reason != ServiceFailureReason.TIMEOUT) {
                    return@withContext firstResult
                }

                // 3. If timed out, cleanup and retry once with delay
                cleanupService()
                kotlinx.coroutines.delay(300)

                val retryResult = getServiceLocked(RETRY_ATTEMPT_TIMEOUT)
                retryResult ?: ServiceResult.Failed(ServiceFailureReason.TIMEOUT)
            }
        }
    }

    private fun cleanupService() {
        rootService = null
        try {
            if (Shell.rootAccess()) {
                RootService.stop(rootServiceIntent)
            }
        } catch (e: Exception) {}
    }

    private fun Continuation<ServiceResult>.safeResume(result: ServiceResult) {
        if (context[Job]?.isActive != false) {
            try {
                resume(result)
            } catch (e: Exception) {}
        }
    }

    private suspend fun getServiceLocked(timeout: Long): ServiceResult? = suspendCoroutineWithTimeout(timeout) { continuation ->
        rootService?.let { service ->
            try {
                service.ping()
                continuation.safeResume(ServiceResult.Success(service, serviceType))
                return@suspendCoroutineWithTimeout
            } catch (e: Exception) {
                rootService = null
            }
        }
        val serviceConnection = object: ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                try {
                    val rootService = IDarqService.Stub.asInterface(service)
                    rootService.setupService()
                    this@DarqServiceConnectionProvider.rootService = rootService
                    continuation.safeResume(ServiceResult.Success(rootService, serviceType))
                } catch (e: Exception) {
                    continuation.safeResume(ServiceResult.Failed(ServiceFailureReason.TIMEOUT))
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                rootService = null
            }
        }
        if (Shell.rootAccess()) {
            serviceType = ServiceType.ROOT
            // Stop prior instance, then pause slightly before binding to avoid race
            RootService.stop(rootServiceIntent)
            GlobalScope.launch(Dispatchers.IO) {
                kotlinx.coroutines.delay(150)
                withContext(Dispatchers.Main) {
                    RootService.bind(rootServiceIntent, serviceConnection)
                }
            }
        } else if(context.isShizukuInstalled()) {
            serviceType = ServiceType.SHIZUKU
            GlobalScope.launch {
                runCatching {
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                        Shizuku.bindUserService(darqProcessArgs, serviceConnection)
                    } else {
                        continuation.safeResume(ServiceResult.Failed(ServiceFailureReason.SHIZUKU_PERMISSION_REQUIRED))
                    }
                }.onFailure { e ->
                    continuation.safeResume(ServiceResult.Failed(ServiceFailureReason.SHIZUKU_NOT_STARTED))
                }
            }
        } else {
            serviceType = ServiceType.UNKNOWN
            continuation.safeResume(ServiceResult.Failed(ServiceFailureReason.SHIZUKU_NOT_INSTALLED))
        }
    }

    private fun IDarqService.setupService(){
        GlobalScope.launch {
            withContext(Dispatchers.IO){
                onBind()
                setupSettings(settings.toIPCSetting())
                val enabledApps = settings.enabledApps
                enabledApps.forEachIndexed { index, app ->
                    setupWhitelist(index == 0, index == enabledApps.size - 1, app)
                }
            }
        }
    }

}