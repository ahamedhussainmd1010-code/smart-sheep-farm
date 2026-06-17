package com.example.myapplication2sheepfarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("AlarmReceiver", "Received intent: ${intent.action}")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule alarm on boot completed
            scheduleAlarm(context)
        } else {
            // Check compliance alerts and post notifications
            checkAndPostNotifications(context)
        }
    }

    private fun checkAndPostNotifications(context: Context) {
        val dbHelper = DatabaseHelper(context)
        val prefs = context.getSharedPreferences("farm_prefs", Context.MODE_PRIVATE)

        // Read simulated date, default to "2026-06-16" for consistency with the application
        val simulatedDateStr = prefs.getString("simulated_date", "2026-06-16") ?: "2026-06-16"
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayDate = try {
            sdf.parse(simulatedDateStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }

        val calToday = Calendar.getInstance().apply { time = todayDate }
        val currentYear = calToday.get(Calendar.YEAR)

        val animalsList = dbHelper.getAllAnimals()
        val vRecords = dbHelper.getAllVaccinationRecords()
        val dRecords = dbHelper.getAllDewormingRecords()
        val feedInventory = dbHelper.getAllFeedInventory()

        val annualSchedules = listOf(
            VaccinationSchedule("02-05", "FMD Vaccination", "Foot and Mouth Disease", true),
            VaccinationSchedule("03-05", "Sheep & Goat Pox Vaccination", "Capripoxvirus (Pox)", false),
            VaccinationSchedule("04-05", "HS Vaccination", "Hemorrhagic Septicemia", true),
            VaccinationSchedule("05-05", "ET + TT Vaccination", "Enterotoxemia + Tetanus Toxoid", false),
            VaccinationSchedule("07-05", "Bluetongue Vaccination", "Bluetongue Virus", true),
            VaccinationSchedule("08-05", "FMD Vaccination", "Foot and Mouth Disease Boost", false),
            VaccinationSchedule("09-05", "PPR Vaccination", "Peste des Petits Ruminants", true),
            VaccinationSchedule("10-05", "HS Vaccination", "Hemorrhagic Septicemia", false),
            VaccinationSchedule("11-05", "ET + TT Vaccination", "Enterotoxemia + Tetanus Toxoid Boost", true)
        )

        val alertList = mutableListOf<FarmAlert>()

        annualSchedules.forEach { schedule ->
            val targetDateStr = "$currentYear-${schedule.dateMonthDay}"
            val targetDate = try { sdf.parse(targetDateStr) } catch (e: Exception) { null } ?: return@forEach

            val diffMs = targetDate.time - todayDate.time
            val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()

            if (diffDays == 7) {
                alertList.add(
                    FarmAlert(
                        id = "rem_7_${schedule.dateMonthDay}",
                        title = "Vaccination Reminder (In 7 Days)",
                        message = "${schedule.vaccineName} for ${schedule.targetDisease} is scheduled on $targetDateStr.",
                        type = AlertType.INFO,
                        dateStr = targetDateStr
                    )
                )
            } else if (diffDays == 1) {
                alertList.add(
                    FarmAlert(
                        id = "rem_1_${schedule.dateMonthDay}",
                        title = "Vaccination Alert (Tomorrow)",
                        message = "${schedule.vaccineName} is due tomorrow ($targetDateStr). Prepare livestock records.",
                        type = AlertType.WARNING,
                        dateStr = targetDateStr
                    )
                )
            } else if (diffDays == 0) {
                alertList.add(
                    FarmAlert(
                        id = "rem_0_${schedule.dateMonthDay}",
                        title = "Vaccination Day!",
                        message = "${schedule.vaccineName} is due TODAY ($targetDateStr). Remember to log details after administration.",
                        type = AlertType.CRITICAL,
                        dateStr = targetDateStr
                    )
                )
            }

            if (diffDays < 0 && animalsList.isNotEmpty()) {
                val dateStart = Calendar.getInstance().apply {
                    time = targetDate
                    add(Calendar.DAY_OF_YEAR, -5)
                }.time
                val dateEnd = Calendar.getInstance().apply {
                    time = targetDate
                    add(Calendar.DAY_OF_YEAR, 10)
                }.time

                val vaccinatedCount = animalsList.count { animal ->
                    vRecords.any { rec ->
                        rec.animalId == animal.id &&
                                rec.vaccineName.contains(schedule.vaccineName.split(" ")[0], ignoreCase = true) &&
                                isDateWithinRange(rec.dateAdministered, dateStart, dateEnd)
                    }
                }

                if (vaccinatedCount < animalsList.size) {
                    val missedCount = animalsList.size - vaccinatedCount
                    alertList.add(
                        FarmAlert(
                            id = "miss_${schedule.dateMonthDay}",
                            title = "Missed Vaccination",
                            message = "${schedule.vaccineName} scheduled on $targetDateStr has pending records ($vaccinatedCount/${animalsList.size} administered). $missedCount animals missed.",
                            type = AlertType.CRITICAL,
                            dateStr = targetDateStr
                        )
                    )
                }
            }

            if (schedule.dewormingRequired && diffDays <= 0 && animalsList.isNotEmpty()) {
                val dateDewormStart = targetDate
                val dateDewormEnd = Calendar.getInstance().apply {
                    time = targetDate
                    add(Calendar.DAY_OF_YEAR, 14)
                }.time

                val dewormedCount = animalsList.count { animal ->
                    dRecords.any { rec ->
                        rec.animalId == animal.id &&
                                isDateWithinRange(rec.dateAdministered, dateDewormStart, dateDewormEnd)
                    }
                }

                if (dewormedCount < animalsList.size) {
                    val pendingDeworm = animalsList.size - dewormedCount
                    alertList.add(
                        FarmAlert(
                            id = "deworm_${schedule.dateMonthDay}",
                            title = "Pending Deworming Alert",
                            message = "Deworming activity scheduled for ${schedule.vaccineName} ($targetDateStr) is pending for $pendingDeworm animals.",
                            type = AlertType.WARNING,
                            dateStr = targetDateStr
                        )
                    )
                }
            }
        }

        feedInventory.forEach { feed ->
            if (feed.quantityInStock <= feed.lowStockThreshold) {
                alertList.add(
                    FarmAlert(
                        id = "feed_low_${feed.id}",
                        title = "Low Feed Stock Alert",
                        message = "${feed.feedName} has only ${feed.quantityInStock}${feed.unit} left.",
                        type = AlertType.WARNING,
                        dateStr = simulatedDateStr
                    )
                )
            }
        }

        val notifiedIds = (prefs.getStringSet("notified_alerts", emptySet()) ?: emptySet()).toMutableSet()
        var updated = false

        alertList.forEach { alert ->
            if (!notifiedIds.contains(alert.id)) {
                postSystemNotification(context, alert.id, alert.title, alert.message)
                notifiedIds.add(alert.id)
                updated = true
            }
        }

        if (updated) {
            prefs.edit().putStringSet("notified_alerts", notifiedIds).apply()
        }
    }

    private fun isDateWithinRange(dateToCheckStr: String, start: Date, end: Date): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(dateToCheckStr)
            date != null && !date.before(start) && !date.after(end)
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        fun postSystemNotification(context: Context, id: String, title: String, message: String) {
            if (Build.VERSION.SDK_INT >= 33) {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }

            try {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    id.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val builder = NotificationCompat.Builder(context, "farm_alerts_channel_v3")
                    .setSmallIcon(context.resources.getIdentifier("ic_launcher_foreground", "drawable", context.packageName).let { if (it != 0) it else android.R.drawable.ic_dialog_info })
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSound(soundUri)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.notify(id.hashCode(), builder.build())
                Log.i("AlarmReceiver", "Successfully posted system notification for alert ID: $id")
            } catch (e: SecurityException) {
                Log.e("AlarmReceiver", "SecurityException posting notification: ${e.message}")
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error posting notification: ${e.message}")
            }
        }

        fun scheduleAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + 5000
            val interval = AlarmManager.INTERVAL_HOUR

            try {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    interval,
                    pendingIntent
                )
                Log.i("AlarmReceiver", "Background alert checks alarm scheduled successfully.")
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error scheduling alarm: ${e.message}")
            }
        }
    }
}
