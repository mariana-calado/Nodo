package br.dia23.nodo.feature.pomodoro.notification

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
import br.dia23.nodo.feature.pomodoro.domain.PomodoroPhase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notificações do Pomodoro:
 * - uma "fixa" (canal silencioso) com a contagem regressiva enquanto a fase roda ou está pausada;
 * - um alerta (canal com som) quando a fase termina.
 */
@Singleton
class PomodoroNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    init {
        // Canais são obrigatórios no Android 8+; a versão "Compat" ignora isso nas versões antigas.
        // Criar de novo um canal existente não faz nada, então é seguro chamar sempre.
        manager.createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(CHANNEL_TIMER, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName(context.getString(R.string.notif_channel_timer))
                    .setShowBadge(false)
                    .build(),
                NotificationChannelCompat.Builder(CHANNEL_ALERTS, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName(context.getString(R.string.notif_channel_alerts))
                    .build(),
            ),
        )
    }

    fun showRunning(phase: PomodoroPhase, subjectName: String?, endsAt: Long) {
        notify(
            ID_TIMER,
            baseBuilder(CHANNEL_TIMER)
                .setContentTitle(context.getString(phase.titleRes()))
                .setContentText(subjectName ?: context.getString(R.string.pomodoro_no_subject))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                // O próprio sistema desenha a contagem regressiva até `endsAt`: o app não precisa atualizar nada.
                .setShowWhen(true)
                .setWhen(endsAt)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .build(),
        )
    }

    fun showPaused(phase: PomodoroPhase, subjectName: String?, remainingMs: Long) {
        val totalSeconds = (remainingMs + 999) / 1000
        val remaining = String.format(Locale.ROOT, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
        notify(
            ID_TIMER,
            baseBuilder(CHANNEL_TIMER)
                .setContentTitle(context.getString(phase.titleRes()))
                .setContentText(
                    listOfNotNull(context.getString(R.string.notif_paused, remaining), subjectName).joinToString(" · "),
                )
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build(),
        )
    }

    fun cancelTimer() = manager.cancel(ID_TIMER)

    fun showPhaseFinished(finished: PomodoroPhase, next: PomodoroPhase) {
        cancelTimer()
        val title = if (finished == PomodoroPhase.FOCUS) R.string.notif_focus_done else R.string.notif_break_done
        val text = when (next) {
            PomodoroPhase.FOCUS -> R.string.notif_next_focus
            PomodoroPhase.SHORT_BREAK -> R.string.notif_next_short_break
            PomodoroPhase.LONG_BREAK -> R.string.notif_next_long_break
        }
        notify(
            ID_ALERT,
            baseBuilder(CHANNEL_ALERTS)
                .setContentTitle(context.getString(title))
                .setContentText(context.getString(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Android 7 (antes dos canais) usa a prioridade
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun baseBuilder(channel: String) = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_timer)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setContentIntent(openAppIntent())

    /** Tocar na notificação traz o app de volta como estava (mesmo efeito de tocar no ícone). */
    private fun openAppIntent(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun notify(id: Int, notification: Notification) {
        // Android 13+: sem a permissão, o sistema descartaria a notificação. O timer continua funcionando no app.
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) manager.notify(id, notification)
    }

    private fun PomodoroPhase.titleRes(): Int = when (this) {
        PomodoroPhase.FOCUS -> R.string.pomodoro_phase_focus
        PomodoroPhase.SHORT_BREAK -> R.string.pomodoro_phase_short_break
        PomodoroPhase.LONG_BREAK -> R.string.pomodoro_phase_long_break
    }

    private companion object {
        const val CHANNEL_TIMER = "pomodoro_timer"
        const val CHANNEL_ALERTS = "pomodoro_alerts"
        const val ID_TIMER = 1
        const val ID_ALERT = 2
    }
}
