package br.dia23.nodo.feature.stats.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

/** Um pedaço de uma barra (barras empilhadas têm vários, ex.: acertos + erros). */
data class BarSegment(val value: Float, val color: Color)

data class Bar(val label: String, val segments: List<BarSegment>) {
    val total: Float get() = segments.sumOf { it.value.toDouble() }.toFloat()
}

/**
 * Gráfico de barras desenhado à mão no Canvas (sem biblioteca de gráficos).
 *
 * O Canvas não "sabe" o que desenhou, então o leitor de tela não teria o que ler:
 * por isso o gráfico recebe um `contentDescription` que resume o conteúdo.
 */
@Composable
fun BarChart(
    bars: List<Bar>,
    contentDescription: String,
    maxValueLabel: (Float) -> String,
    modifier: Modifier = Modifier,
) {
    // TextMeasurer mede e prepara textos para desenhar no Canvas (o Canvas não tem um "Text").
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val maxValue = bars.maxOfOrNull { it.total }?.takeIf { it > 0f } ?: 1f
    // Com 30 barras os rótulos se sobreporiam: mostramos no máximo ~8, sempre incluindo o último (hoje).
    val labelEvery = ((bars.size + 7) / 8).coerceAtLeast(1)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(180.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        if (bars.isEmpty()) return@Canvas

        val topSpace = 20.dp.toPx() // espaço para o rótulo do valor máximo
        val bottomSpace = 20.dp.toPx() // espaço para os rótulos dos dias
        val chartHeight = size.height - topSpace - bottomSpace
        val baseline = topSpace + chartHeight
        val slotWidth = size.width / bars.size
        val barWidth = slotWidth * 0.6f
        val gridStroke = 1.dp.toPx()

        // Linha do valor máximo, com o número à esquerda, e a linha de base.
        drawLine(gridColor, Offset(0f, topSpace), Offset(size.width, topSpace), gridStroke)
        drawText(textMeasurer, maxValueLabel(maxValue), Offset(0f, 0f), labelStyle)
        drawLine(gridColor, Offset(0f, baseline), Offset(size.width, baseline), gridStroke)

        bars.forEachIndexed { index, bar ->
            val left = slotWidth * index + (slotWidth - barWidth) / 2

            // Empilha os segmentos de baixo para cima. Coordenada Y do Canvas cresce para BAIXO.
            var bottom = baseline
            bar.segments.forEach { segment ->
                if (segment.value <= 0f) return@forEach
                val height = chartHeight * (segment.value / maxValue)
                drawRoundRect(
                    color = segment.color,
                    topLeft = Offset(left, bottom - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
                bottom -= height
            }

            if ((bars.lastIndex - index) % labelEvery == 0) {
                val label = textMeasurer.measure(bar.label, labelStyle)
                drawText(
                    textLayoutResult = label,
                    topLeft = Offset(left + barWidth / 2 - label.size.width / 2f, baseline + 4.dp.toPx()),
                )
            }
        }
    }
}
