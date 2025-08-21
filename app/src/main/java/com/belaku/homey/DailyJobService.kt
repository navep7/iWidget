package com.belaku.homey

import android.app.job.JobParameters
import android.app.job.JobService
import android.icu.util.Calendar
import android.util.Log
import com.belaku.homey.MainActivity.Companion.getNews


class DailyJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        // Your code to run once a day
        var cDate = Calendar.getInstance().get(Calendar.DATE)
        Log.d("DailyJob - $cDate : ${Calendar.getInstance().get(Calendar.HOUR_OF_DAY)}:${Calendar.getInstance().get(Calendar.MINUTE)}", "Daily job executed!")
        getNews( cDate - 1)
        jobFinished(params, false) // Indicate job is finished
        return true // Return true if work is being done on a separate thread
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return true // Return true to reschedule if conditions are met
    }
}