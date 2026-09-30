package br.dia23.nodo

import android.app.Application
import br.dia23.nodo.feature.pomodoro.PomodoroController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * A classe Application é criada uma vez, antes de qualquer tela.
 * @HiltAndroidApp faz o Hilt gerar o "contêiner" de dependências do app inteiro.
 * Precisa estar registrada no AndroidManifest (android:name=".NodoApplication").
 */
@HiltAndroidApp
class NodoApplication : Application() {

    // Injeção por campo: o Hilt preenche isto dentro do super.onCreate().
    @Inject lateinit var pomodoroController: PomodoroController

    override fun onCreate() {
        super.onCreate()
        // Se um pomodoro terminou com o app fechado (ou o celular reiniciou e perdeu o alarme),
        // salva a sessão e refaz alarme/notificação.
        pomodoroController.restore()
    }
}
