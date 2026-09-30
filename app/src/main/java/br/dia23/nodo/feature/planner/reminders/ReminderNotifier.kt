package br.dia23.nodo.feature.planner.reminders

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.dia23.nodo.R
import br.dia23.nodo.feature.planner.data.EventType
import br.dia23.nodo.feature.planner.data.PlannerEventEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    init {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.notif_channel_reminders))
                .build(),
        )
    }

    fun showDailyReminder(cards: Int) {
        notify(
            ID_DAILY,
            builder()
                .setContentTitle(context.resources.getQuantityString(R.plurals.reminder_daily_title, cards, cards))
                .setContentText(context.getString(R.string.reminder_daily_text))
                .build(),
        )
    }

    fun showEventReminder(event: PlannerEventEntity, daysLeft: Int) {
        val whenText = when {
            daysLeft <= 0 -> context.getString(R.string.countdown_today)
            daysLeft == 1 -> context.getString(R.string.countdown_tomorrow)
            else -> context.resources.getQuantityString(R.plurals.countdown_in_days, daysLeft, daysLeft)
        }
        val typeText = context.getString(
            if (event.type == EventType.EXAM) R.string.event_type_exam else R.string.event_type_deadline,
        )
        notify(
            // Um id por evento: dois avisos no mesmo dia não se substituem.
            event.id.hashCode(),
            builder()
                .setContentTitle(event.title)
                .setContentText(context.getString(R.string.reminder_event_text, typeText, whenText))
                .build(),
        )
    }

    private fun builder() = NotificationCompat.Builder(context, CHANNEL)
        .setSmallIcon(R.drawable.ic_calendar_month)
        .setAutoCancel(true)
        .setContentIntent(openAppIntent())

    private fun openAppIntent(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun notify(id: Int, notification: Notification) {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) manager.notify(id, notification)
    }

    private companion object {
        const val CHANNEL = "study_reminders"
        const val ID_DAILY = 100
    }
}
