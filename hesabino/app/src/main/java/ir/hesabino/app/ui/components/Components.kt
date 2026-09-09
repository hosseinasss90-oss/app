package ir.hesabino.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.hesabino.app.domain.model.DisplayCurrency
import ir.hesabino.app.domain.model.TxType
import ir.hesabino.app.ui.theme.Expense
import ir.hesabino.app.ui.theme.Income
import ir.hesabino.app.ui.theme.Transfer
import ir.hesabino.app.util.MoneyFormatter

@Composable
fun MoneyText(
    rials: Long,
    currency: DisplayCurrency,
    persianDigits: Boolean,
    privacy: Boolean,
    signed: Boolean = false,
    type: TxType? = null,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge,
) {
    val color = when (type) {
        TxType.EXPENSE -> Expense
        TxType.INCOME -> Income
        TxType.TRANSFER -> Transfer
        null -> MaterialTheme.colorScheme.onSurface
    }
    val text = if (privacy) MoneyFormatter.privacyMask()
    else MoneyFormatter.format(rials, currency, persianDigits, withUnit = true, signed = signed)
    Text(text = text, style = style, color = color, fontWeight = FontWeight.Bold, maxLines = 1)
}

@Composable
fun TxRow(
    title: String,
    subtitle: String,
    rials: Long,
    type: TxType,
    currency: DisplayCurrency,
    persianDigits: Boolean,
    privacy: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    when (type) {
                        TxType.EXPENSE -> Expense.copy(alpha = 0.12f)
                        TxType.INCOME -> Income.copy(alpha = 0.12f)
                        TxType.TRANSFER -> Transfer.copy(alpha = 0.12f)
                    },
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when (type) {
                    TxType.EXPENSE -> "−"
                    TxType.INCOME -> "+"
                    TxType.TRANSFER -> "⇄"
                },
                color = when (type) {
                    TxType.EXPENSE -> Expense
                    TxType.INCOME -> Income
                    TxType.TRANSFER -> Transfer
                },
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        MoneyText(
            rials = if (type == TxType.EXPENSE) -rials else rials,
            currency = currency,
            persianDigits = persianDigits,
            privacy = privacy,
            signed = true,
            type = type,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
fun DonutChart(
    slices: List<Pair<Color, Float>>,
    centerLabel: String,
    modifier: Modifier = Modifier.size(120.dp),
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(120.dp)) {
            val stroke = 18.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            var start = -90f
            val total = slices.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(0.001f)
            slices.forEach { (color, value) ->
                val sweep = (value / total) * 360f
                drawArc(
                    color = color,
                    startAngle = start,
                    sweepAngle = sweep - 2f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                start += sweep
            }
        }
        Text(centerLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HesabinoCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
    ) { Box(Modifier.padding(16.dp)) { content() } }
}

val ChartPalette = listOf(
    Color(0xFF0F6E56),
    Color(0xFF2F6F8F),
    Color(0xFFC47B12),
    Color(0xFF6B5B95),
    Color(0xFFC44536),
    Color(0xFF5B6E5B),
)
