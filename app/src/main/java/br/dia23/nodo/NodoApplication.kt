package br.dia23.nodo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * A classe Application é criada uma vez, antes de qualquer tela.
 * @HiltAndroidApp faz o Hilt gerar o "contêiner" de dependências do app inteiro.
 * Precisa estar registrada no AndroidManifest (android:name=".NodoApplication").
 */
@HiltAndroidApp
class NodoApplication : Application()
