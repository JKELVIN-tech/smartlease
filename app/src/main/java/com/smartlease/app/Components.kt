package com.smartlease.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Display = FontFamily.Serif
val Mono = FontFamily.Monospace

fun Modifier.ledgerCard(): Modifier {
    val s = RoundedCornerShape(16.dp)
    return this.clip(s).background(Col.card).border(1.dp, Col.border, s)
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) =
    Text(text.uppercase(), modifier, color = Col.mutedFg, fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.8.sp)

@Composable
fun StatusChip(status: PayStatus) {
    val (fg, bg) = when (status) {
        PayStatus.PAID -> Col.paid to Col.paidSoft
        PayStatus.PARTIAL, PayStatus.DUE -> Col.warn to Col.warnSoft
        PayStatus.OVERDUE -> Col.primary to Col.accent
        PayStatus.VACANT -> Col.mutedFg to Col.secondary
    }
    Text(
        status.name, color = fg, fontFamily = Mono, fontSize = 10.sp,
        modifier = Modifier.background(bg, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

enum class Tone { ACCENT, WARN, PAID }
fun toneFor(s: PayStatus) = when (s) { PayStatus.PAID -> Tone.PAID; PayStatus.OVERDUE -> Tone.ACCENT; else -> Tone.WARN }

@Composable
fun Initial(name: String, tone: Tone = Tone.ACCENT) {
    val (fg, bg) = when (tone) {
        Tone.ACCENT -> Col.primary to Col.accent
        Tone.WARN -> Col.warn to Col.warnSoft
        Tone.PAID -> Col.paid to Col.paidSoft
    }
    Box(Modifier.size(40.dp).background(bg, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase().ifEmpty { "?" }, color = fg, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun PageHeader(title: String, sub: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            if (sub != null) Eyebrow(sub)
            Text(title, Modifier.padding(top = 4.dp), color = Col.ink, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 26.sp)
        }
        action?.invoke()
    }
}

@Composable
fun SectionTitle(title: String, meta: String? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Col.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (meta != null) Text(meta, color = Col.mutedFg, fontFamily = Mono, fontSize = 11.sp)
    }
}

@Composable
fun Empty(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().ledgerCard().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, color = Col.mutedFg, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (action != null) Text(action, Modifier.clickable(onClick = onAction).padding(top = 2.dp), color = Col.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AddButton(label: String, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(Col.ink).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Add, label, tint = Col.card, modifier = Modifier.size(18.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Col.card,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Col.ink, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                Icon(Icons.Filled.Close, "Close", tint = Col.mutedFg, modifier = Modifier.clip(CircleShape).clickable(onClick = onClose))
            }
            content()
        }
    }
}

@Composable
fun Field(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Eyebrow(label, Modifier.padding(bottom = 4.dp))
        content()
    }
}

@Composable
fun Input(
    value: String, onChange: (String) -> Unit, placeholder: String = "",
    keyboard: KeyboardType = KeyboardType.Text, password: Boolean = false
) {
    Box(
        Modifier.fillMaxWidth().height(44.dp)
            .background(Col.background, RoundedCornerShape(8.dp))
            .border(1.dp, Col.border, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) Text(placeholder, color = Col.mutedFg.copy(alpha = 0.7f), fontSize = 14.sp, maxLines = 1)
        BasicTextField(
            value, onChange, Modifier.fillMaxWidth(), singleLine = true,
            textStyle = TextStyle(color = Col.ink, fontSize = 14.sp),
            cursorBrush = SolidColor(Col.primary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None
        )
    }
}

@Composable
fun Select(options: List<Pair<String, String>>, value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box {
        Row(
            Modifier.fillMaxWidth().height(44.dp).clip(shape).background(Col.background)
                .border(1.dp, Col.border, shape).clickable { open = true }.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(options.firstOrNull { it.first == value }?.second ?: "", Modifier.weight(1f), color = Col.ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = Col.mutedFg)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(Col.card)) {
            options.forEach { (v, label) ->
                DropdownMenuItem(text = { Text(label, fontSize = 14.sp, color = Col.ink) }, onClick = { onChange(v); open = false })
            }
        }
    }
}

@Composable
fun SubmitButton(text: String, busy: Boolean, busyText: String = "Saving…", onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp).clip(RoundedCornerShape(12.dp))
            .background(Col.primary.copy(alpha = if (busy) 0.6f else 1f))
            .clickable(enabled = !busy, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(if (busy) busyText else text, color = Col.card, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
