package com.montecarlo.littlegaza

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Green = Color(0xFF2E7D5B)
val Sand = Color(0xFFFFF8EC)
val Orange = Color(0xFFE8A33D)
val Soft = Color(0xFFE3F1E8)
val Ink = Color(0xFF2B2B2B)
val Line = Color(0xFFE2D6C2)
val WrongBg = Color(0xFFF3DCDC)

@Composable
fun LgTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Green,
            onPrimary = Color.White,
            secondary = Orange,
            background = Sand,
            surface = Color.White,
            onSurface = Ink,
            onBackground = Ink
        ),
        content = content
    )
}

/** Converts 0-9 to Arabic-Indic digits (٠-٩) when the setting is on. */
fun digits(s: String, eastern: Boolean): String {
    if (!eastern) return s
    val sb = StringBuilder(s.length)
    for (c in s) sb.append(if (c in '0'..'9') '٠' + (c - '0') else c)
    return sb.toString()
}

fun starsText(n: Int): String {
    val k = n.coerceIn(0, 3)
    return "⭐".repeat(k) + "☆".repeat(3 - k)
}

@Composable
fun Gap(h: Int) {
    Spacer(Modifier.height(h.dp))
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, onSpeak: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack) { Text("→ رجوع", fontSize = 18.sp, color = Green) }
        }
        Text(
            title,
            Modifier.weight(1f).padding(horizontal = 6.dp),
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = Green,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (onSpeak != null) {
            TextButton(onClick = onSpeak) { Text("🔊", fontSize = 26.sp) }
        }
    }
}

@Composable
fun BigButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Green,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 62.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
    ) {
        Text(text, fontSize = 21.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun Tile(
    emoji: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    bg: Color = Color.White,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, Line, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 40.sp)
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Ink)
        if (subtitle.isNotEmpty()) {
            Text(subtitle, fontSize = 15.sp, color = Color(0xFF6B6B6B), textAlign = TextAlign.Center)
        }
    }
}

/** Lays items out two per row. */
@Composable
fun <T> Grid2(items: List<T>, cell: @Composable RowScope.(T) -> Unit) {
    items.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { cell(it) }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Gap(12)
    }
}
