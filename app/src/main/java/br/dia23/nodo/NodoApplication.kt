package br.dia23.nodo

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import br.dia23.nodo.feature.planner.reminders.ReminderScheduler
import br.dia23.nodo.feature.pomodoro.PomodoroController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * A classe Application é criada uma vez, antes de qualquer tela.
 * @HiltAndroidApp faz o Hilt gerar o "contêiner" de dependências do app inteiro.
 * Precisa estar registrada no AndroidManifest (android:name=".NodoApplication").
 *
 * Configuration.Provider: entrega ao WorkManager uma configuração com a fábrica do Hilt,
 * para os Workers receberem repositórios por injeção (a inicialização automática foi desligada no manifesto).
 */
@HiltAndroidApp
class NodoApplication : Application(), Configuration.Provider {

    // Injeção por campo: o Hilt preenche isto dentro do super.onCreate().
    @Inject lateinit var pomodoroController: PomodoroController
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Se um pomodoro terminou com o app fechado (ou o celular reiniciou e perdeu o alarme),
        // salva a sessão e refaz alarme/notificação.
        pomodoroController.restore()
        // Garante o lembrete diário agendado (se estiver ligado).
        reminderScheduler.restoreDaily()
    }
}
