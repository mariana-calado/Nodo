package br.dia23.nodo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.dia23.nodo.core.navigation.NodoApp
import br.dia23.nodo.ui.theme.NodoTheme
import dagger.hilt.android.AndroidEntryPoint

// @AndroidEntryPoint: permite que esta Activity (e os composables dentro dela) recebam dependências do Hilt,
// como o hiltViewModel() usado nas telas.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NodoTheme {
                NodoApp()
            }
        }
    }
}
