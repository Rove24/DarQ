package com.kieronquinn.app.darq.service.autodark

import android.app.*
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kieronquinn.app.darq.R
import com.kieronquinn.app.darq.components.settings.DarqSharedPreferences
import com.kieronquinn.app.darq.model.location.LatLng
import com.kieronquinn.app.darq.providers.DarqServiceConnectionProvider
import com.kieronquinn.app.darq.ui.activities.DarqActivity
import com.kieronquinn.app.darq.utils.TimeZoneUtils
import com.kieronquinn.app.darq.work.DarqSunriseSunsetWork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.shredzone.commons.suncalc.SunTimes
import java.util.*
import java.util.concurrent.TimeUnit

class DarqAutoDarkForegroundService: LifecycleService() {

    companion object {
        private const val NOTIFICATION_CHANNEL_AUTO_DARK = "channel_auto_dark"
        private const val NOTIFICATION_ID_AUTO_DARK = 1003
        const val KEY_ENABLE_DARK = "enable_dark"
        const val KEY_JUST_RESCHEDULE = "just_reschedule"
    }

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val launchIntent by lazy {
        Intent(this, DarqActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }
    }

    private val workManager by lazy {
        WorkManager.getInstance(this)
    }

    private val serviceProvider by inject<DarqServiceConnectionProvider>()
    private val settings by inject<DarqSharedPreferences>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_AUTO_DARK,
                getString(R.string.notification_channel_boot_auto_dark_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                this.description = getString(R.string.notification_channel_boot_auto_dark_description)
            })

        val justReschedule = intent?.getBooleanExtra(KEY_JUST_RESCHEDULE, false) ?: false
        val notificationContent = if(justReschedule){
            R.string.auto_dark_foreground_notification_content_changes
        }else{
            R.string.auto_dark_foreground_notification_content
        }

        val notification: Notification =
            Notification.Builder(this, NOTIFICATION_CHANNEL_AUTO_DARK)
                .setContentTitle(getText(R.string.auto_dark_foreground_notification_title))
                .setContentText(getText(notificationContent))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(launchIntent)
                .setTicker(getText(notificationContent))
                .build()

        startForeground(NOTIFICATION_ID_AUTO_DARK, notification)

        lifecycleScope.launchWhenCreated {
            if(!justReschedule) {
                val enableDark = intent?.getBooleanExtra(KEY_ENABLE_DARK, false) ?: false
                setDarkModeEnabled(enableDark)
            } else if(settings.autoDarkTheme) {
                val sunTimes = getNextTriggerTimes()
                val shouldBeDark = checkIfShouldBeDark(sunTimes)
                setDarkModeEnabled(shouldBeDark)
            }
            val nextTriggerTimes = getNextTriggerTimes()
            cancelAndScheduleWork(nextTriggerTimes)
            stopForeground(true)
            stopSelf()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun setDarkModeEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        val service = serviceProvider.getService()
        if(service is DarqServiceConnectionProvider.ServiceResult.Success){
            service.service.setNightMode(enabled)
        }else{
            showFailedNotification()
        }
    }

    private fun checkIfShouldBeDark(sunTimes: SunTimes?): Boolean {
        return if (settings.autoDarkScheduleType == DarqSharedPreferences.SCHEDULE_TYPE_CUSTOM) {
            val cal = Calendar.getInstance()
            val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val startMinutes = settings.autoDarkCustomStart
            val endMinutes = settings.autoDarkCustomEnd
            if (startMinutes < endMinutes) {
                nowMinutes in startMinutes until endMinutes
            } else if (startMinutes > endMinutes) {
                nowMinutes >= startMinutes || nowMinutes < endMinutes
            } else {
                false
            }
        } else {
            if (sunTimes == null) return false
            val now = System.currentTimeMillis()
            val sunrise = sunTimes.rise?.time ?: return false
            val sunset = sunTimes.set?.time ?: return false
            if (sunrise < sunset) {
                now < sunrise || now >= sunset
            } else {
                now >= sunset && now < sunrise
            }
        }
    }

    private suspend fun getNextTriggerTimes(): SunTimes? = withContext(Dispatchers.IO){
        val latLng = getTimezoneLocation() ?: return@withContext null
        SunTimes.compute().on(Calendar.getInstance()).at(latLng.latitude, latLng.longitude).execute()
    }

    /**
     *  Gets the user's country location using their timezone. This is nowhere near accurate for
     *  location use, but is good *enough* for sunrise / sunset
     */
    private fun getTimezoneLocation(): LatLng? {
        return TimeZoneUtils.getLatLngForTimezone(this, TimeZone.getDefault())
    }

    /**
     *  Schedules the sunrise/sunset or custom time work
     */
    private fun cancelAndScheduleWork(sunTimes: SunTimes?){
        workManager.cancelAllWorkByTag(DarqSunriseSunsetWork.TAG_SUNSET)
        workManager.cancelAllWorkByTag(DarqSunriseSunsetWork.TAG_SUNRISE)
        if(!settings.autoDarkTheme) return

        val now = System.currentTimeMillis()
        if (settings.autoDarkScheduleType == DarqSharedPreferences.SCHEDULE_TYPE_CUSTOM) {
            val startMinutes = settings.autoDarkCustomStart
            val endMinutes = settings.autoDarkCustomEnd

            val startCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, startMinutes / 60)
                set(Calendar.MINUTE, startMinutes % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (startCal.timeInMillis <= now) {
                startCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            val delayToStart = startCal.timeInMillis - now

            val endCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, endMinutes / 60)
                set(Calendar.MINUTE, endMinutes % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (endCal.timeInMillis <= now) {
                endCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            val delayToEnd = endCal.timeInMillis - now

            val sunsetWork = OneTimeWorkRequestBuilder<DarqSunriseSunsetWork>()
                .setInitialDelay(delayToStart, TimeUnit.MILLISECONDS)
                .addTag(DarqSunriseSunsetWork.TAG_SUNSET).build()
            workManager.enqueue(sunsetWork)

            val sunriseWork = OneTimeWorkRequestBuilder<DarqSunriseSunsetWork>()
                .setInitialDelay(delayToEnd, TimeUnit.MILLISECONDS)
                .addTag(DarqSunriseSunsetWork.TAG_SUNRISE).build()
            workManager.enqueue(sunriseWork)
        } else if (sunTimes != null) {
            val latLng = getTimezoneLocation()
            var sunriseTime = sunTimes.rise?.time
            var sunsetTime = sunTimes.set?.time
            if (sunriseTime != null && sunriseTime <= now && latLng != null) {
                val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                val tomorrowTimes = SunTimes.compute().on(tomorrow).at(latLng.latitude, latLng.longitude).execute()
                sunriseTime = tomorrowTimes.rise?.time
            }
            if (sunsetTime != null && sunsetTime <= now && latLng != null) {
                val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                val tomorrowTimes = SunTimes.compute().on(tomorrow).at(latLng.latitude, latLng.longitude).execute()
                sunsetTime = tomorrowTimes.set?.time
            }
            if (sunriseTime != null && sunriseTime > now) {
                val sunriseDelay = sunriseTime - now
                val sunriseWork = OneTimeWorkRequestBuilder<DarqSunriseSunsetWork>()
                    .setInitialDelay(sunriseDelay, TimeUnit.MILLISECONDS)
                    .addTag(DarqSunriseSunsetWork.TAG_SUNRISE).build()
                workManager.enqueue(sunriseWork)
            }
            if (sunsetTime != null && sunsetTime > now) {
                val sunsetDelay = sunsetTime - now
                val sunsetWork = OneTimeWorkRequestBuilder<DarqSunriseSunsetWork>()
                    .setInitialDelay(sunsetDelay, TimeUnit.MILLISECONDS)
                    .addTag(DarqSunriseSunsetWork.TAG_SUNSET).build()
                workManager.enqueue(sunsetWork)
            }
        }
    }

    private fun showFailedNotification() {
        val notification: Notification =
            Notification.Builder(this, NOTIFICATION_CHANNEL_AUTO_DARK)
                .setContentTitle(getText(R.string.auto_dark_foreground_notification_failed_title))
                .setContentText(getText(R.string.auto_dark_foreground_notification_failed_content))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(launchIntent)
                .setAutoCancel(true)
                .setStyle(Notification.BigTextStyle().bigText(getString(R.string.auto_dark_foreground_notification_failed_content)))
                .setTicker(getText(R.string.auto_dark_foreground_notification_failed_content))
                .build()
        notificationManager.notify(NOTIFICATION_ID_AUTO_DARK, notification)
    }

}