package br.dia23.nodo.feature.pomodoro.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.dia23.nodo.core.di.ApplicationScope
import br.dia23.nodo.feature.pomodoro.PomodoroController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agenda um alarme do sistema para o fim da fase. Ele "acorda" o app mesmo que o processo
 * tenha sido encerrado, e o receiver abaixo finaliza a fase (salva a sessão e avisa).
 */
@Singleton
class PomodoroAlarmScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(atMillis: Long) {
        // Alarme "inexato": não exige a permissão especial de alarme exato (Android 12+).
        // Com o app aberto, quem termina a fase no segundo certo é o PomodoroController;
        // o alarme é a garantia para quando o app está fechado (pode atrasar um pouco em modo soneca).
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent())
    }

    fun cancel() = alarmManager.cancel(pendingIntent())

    // Mesmo Intent + mesmo requestCode = mesmo PendingIntent: por isso o cancel() encontra o alarme agendado.
    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, PomodoroAlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/**
 * O Hilt não cria BroadcastReceivers por construtor. Um @EntryPoint é a "porta" para pegar
 * dependências do grafo do Hilt a partir de classes que o Android instancia sozinho.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PomodoroReceiverEntryPoint {
    fun controller(): PomodoroController

    @ApplicationScope
    fun applicationScope(): CoroutineScope
}

class PomodoroAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val entryPoint = EntryPointAccessors.fromApplication(context, PomodoroReceiverEntryPoint::class.java)
        // goAsync(): avisa o sistema que o trabalho continua depois do onReceive (gravar no banco é assíncrono).
        val pendingResult = goAsync()
        entryPoint.applicationScope().launch {
            try {
                entryPoint.controller().finishIfDue()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
