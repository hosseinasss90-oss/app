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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.hesabino.app.domain.model.DisplayCurrency
import ir.hesabino.app.domain.model.TxType
import ir.hesabino.app.ui.theme.Draft
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

/** سرتیتر بخش‌ها با یک توضیح کوتاه زیر عنوان (بهبود نام‌گذاری/توضیح هر بخش). */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

/** کارت محتوای استاندارد با گوشه‌های نرم‌تر و خط‌کشی ظریف. */
@Composable
fun HesabinoCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
    ) { Box(Modifier.padding(18.dp)) { content() } }
}

/** بدنهٔ رنگی ظریف برای دسته/نوع‌های مختلف. */
@Composable
fun TintedCircle(
    color: Color,
    content: String,
    modifier: Modifier = Modifier.size(44.dp),
) {
    Box(
        modifier = modifier.background(color.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(content, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}

/** نمایش یک مقدار آماری داخل یک «بنر» کوچک رنگی. */
@Composable
fun StatBanner(
    label: String,
    value: String,
    iconColor: Color,
    containerColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = iconColor)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        }
    }
}

/** ردیف تراکنش (عنوان، زیرعنوان، مبلغ رنگی). */
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
                .size(44.dp)
                .background(
                    when (type) {
                        TxType.EXPENSE -> Expense.copy(alpha = 0.14f)
                        TxType.INCOME -> Income.copy(alpha = 0.14f)
                        TxType.TRANSFER -> Transfer.copy(alpha = 0.14f)
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

/** نمودار دونات (نسبت‌ها). */
@Composable
fun DonutChart(
    slices: List<Pair<Color, Float>>,
    centerLabel: String,
    modifier: Modifier = Modifier.size(140.dp),
    strokeWidth: androidx.compose.ui.unit.Dp = 20.dp,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(140.dp)) {
            val stroke = strokeWidth.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            var start = -90f
            val total = slices.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(0.001f)
            slices.forEach { (color, value) ->
                val sweep = (value / total) * 360f
                drawArc(
                    color = color,
                    startAngle = start,
                    sweepAngle = sweep - 1.5f,
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

/** نمودار میله‌ای عمودی ساده (مثلاً هزینه/درآمد ماهانه). */
@Composable
fun BarChart(
    groups: List<ChartGroup>,
    barWidth: Float = 0.5f,
    modifier: Modifier = Modifier,
    gridColor: Color = Color(0x22000000),
) {
    Canvas(modifier.fillMaxWidth().height(160.dp)) {
        if (groups.isEmpty()) return@Canvas
        val maxVal = (groups.flatMap { it.values }.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val labelSpace = 0.05f
        val chartHeight = size.height
        val groupW = size.width / groups.size
        val n = groups[0].values.size.coerceAtLeast(1)
        // grid line
        drawLine(gridColor, Offset(0f, chartHeight), Offset(size.width, chartHeight), 1f)
        groups.forEachIndexed { gi, group ->
            val slot = groupW * (1f - labelSpace)
            val barW = (slot / n) * barWidth
            val spacing = slot / n
            group.values.forEachIndexed { vi, v ->
                val barH = (v / maxVal) * (chartHeight - 8f)
                val x = gi * groupW + labelSpace * groupW / 2 + vi * spacing + (spacing - barW) / 2
                val color = group.colors.getOrElse(vi % group.colors.size) { Primary }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, chartHeight - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barW / 2, barW / 2),
                )
            }
        }
    }
}

/** نمودار خطی برای نمایش روند. */
@Composable
fun LineChart(
    points: List<Float>,
    color: Color = Primary,
    labelsCount: Int = points.size,
    modifier: Modifier = Modifier,
    fillUnder: Boolean = true,
    gridColor: Color = Color(0x22000000),
) {
    Canvas(modifier.fillMaxWidth().height(160.dp)) {
        if (points.size < 2) return@Canvas
        val maxVal = (points.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val minVal = (points.minOrNull() ?: 0f).coerceAtMost(0f)
        val range = (maxVal - minVal).coerceAtLeast(1f)
        val stepX = if (points.size > 1) size.width / (points.size - 1) else size.width
        val padY = 12f
        val chartBottom = size.height - padY
        fun y(v: Float): Float = chartBottom - (v - minVal) / range * (size.height - padY * 2)

        // grid
        drawLine(gridColor, Offset(0f, chartBottom), Offset(size.width, chartBottom), 1f)

        val pts = points.mapIndexed { i, v -> Offset(i * stepX, y(v)) }
        // fill under
        if (fillUnder) {
            val path = androidx.compose.ui.graphics.Path()
            path.moveTo(pts.first().x, chartBottom)
            pts.forEach { path.lineTo(it.x, it.y) }
            path.lineTo(pts.last().x, chartBottom)
            path.close()
            drawPath(
                path,
                Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.02f)),
                    startY = pts.minOf { it.y },
                    endY = chartBottom,
                ),
            )
        }
        // line
        for (i in 0 until pts.size - 1) {
            drawLine(color, pts[i], pts[i + 1], strokeWidth = 8f, cap = StrokeCap.Round)
        }
        pts.forEach { p -> drawCircle(color.copy(alpha = 0.2f), radius = 9f, center = p) }
        pts.forEach { p -> drawCircle(color, radius = 4f, center = p) }
    }
}

/** نگاشت دادهٔ نمودار میله‌ای. */
data class ChartGroup(
    val label: String,
    val values: List<Float>,
    val colors: List<Color>,
)

/** نقطه‌های رنگی + برچسب برای راهنمای نمودار. */
@Composable
fun ChartLegend(entries: List<Pair<Color, String>>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        entries.forEach { (color, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(10.dp).background(color, CircleShape))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

val ChartPalette = listOf(
    Color(0xFF0F6E56),
    Color(0xFF2F6F8F),
    Color(0xFFC47B12),
    Color(0xFF6B5B95),
    Color(0xFFC44536),
    Color(0xFF5B6E5B),
)
val DonutPalette = listOf(
    Color(0xFF00856B),
    Color(0xFF2F6F8F),
    Color(0xFFE9A23B),
    Color(0xFF7C6BD6),
    Color(0xFFD65C4B),
    Color(0xFF5B8E5B),
    Color(0xFF9A5B8E),
)

internal val Primary = Color(0xFF00856B)

private val ExpenseFill = listOf(Expense, Income)
