package br.dia23.nodo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import br.dia23.nodo.R

/**
 * Montserrat, empacotada em res/font (licença SIL OFL, ver licenses/Montserrat-OFL.txt).
 * Cada arquivo é um peso; o Compose escolhe o arquivo certo pelo fontWeight do texto.
 * Pesos sem arquivo (ex.: Light) usam o mais próximo disponível.
 */
val Montserrat = FontFamily(
    Font(R.font.montserrat_regular, FontWeight.Normal),
    Font(R.font.montserrat_medium, FontWeight.Medium),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
    Font(R.font.montserrat_bold, FontWeight.Bold),
)

// Estilos padrão do Material 3 (tamanhos, pesos, espaçamentos); só trocamos a família da fonte.
private val defaults = Typography()

val Typography = Typography(
    displayLarge = defaults.displayLarge.copy(fontFamily = Montserrat),
    displayMedium = defaults.displayMedium.copy(fontFamily = Montserrat),
    displaySmall = defaults.displaySmall.copy(fontFamily = Montserrat),
    headlineLarge = defaults.headlineLarge.copy(fontFamily = Montserrat),
    headlineMedium = defaults.headlineMedium.copy(fontFamily = Montserrat),
    headlineSmall = defaults.headlineSmall.copy(fontFamily = Montserrat),
    titleLarge = defaults.titleLarge.copy(fontFamily = Montserrat),
    titleMedium = defaults.titleMedium.copy(fontFamily = Montserrat),
    titleSmall = defaults.titleSmall.copy(fontFamily = Montserrat),
    bodyLarge = defaults.bodyLarge.copy(fontFamily = Montserrat),
    bodyMedium = defaults.bodyMedium.copy(fontFamily = Montserrat),
    bodySmall = defaults.bodySmall.copy(fontFamily = Montserrat),
    labelLarge = defaults.labelLarge.copy(fontFamily = Montserrat),
    labelMedium = defaults.labelMedium.copy(fontFamily = Montserrat),
    labelSmall = defaults.labelSmall.copy(fontFamily = Montserrat),
)
