package com.smartlease.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFmt = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH).withZone(ZoneId.systemDefault())
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH).withZone(ZoneId.systemDefault())

@Composable
fun InboxScreen(vm: AppViewModel, openThread: (String) -> Unit) {
    val d = vm.ledger
    val rows = d.tenants.map { t -> t to d.messages.firstOrNull { it.tenantId == t.id } }
        .sortedByDescending { it.second?.createdAt ?: Instant.EPOCH }
    PageHeader("Messages", "Tenant inbox")
    Column(Modifier.padding(horizontal = 20.dp)) {
        if (rows.isEmpty()) Empty("Add tenants to start messaging.")
        else Column(Modifier.fillMaxWidth().ledgerCard()) {
            rows.forEachIndexed { i, (t, last) ->
                if (i > 0) HorizontalDivider(color = Col.border)
                Row(Modifier.fillMaxWidth().clickable { openThread(t.id) }.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Initial(t.fullName)
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Text(t.fullName, Modifier.weight(1f), color = Col.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (last != null) Text(dayFmt.format(last.createdAt), color = Col.mutedFg, fontFamily = Mono, fontSize = 10.sp)
                        }
                        Text(
                            last?.body ?: "Unit ${d.units.firstOrNull { it.id == t.unitId }?.label ?: "—"} · No messages yet",
                            color = Col.mutedFg, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThreadScreen(vm: AppViewModel, tenantId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val d = vm.ledger
    val tenant = d.tenants.firstOrNull { it.id == tenantId }
    val thread = d.messages.filter { it.tenantId == tenantId }.reversed()
    val s = tenant?.let { tenantStatus(it, d.units, d.txns) }
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(thread.size) { if (thread.isNotEmpty()) listState.animateScrollToItem(thread.lastIndex) }

    val first = tenant?.fullName?.split(" ")?.firstOrNull() ?: ""
    val quick = if (s != null && s.balance > 0) "Hi $first, a friendly reminder that ${kes(s.balance)} rent is outstanding for this month."
    else "Hi $first, thank you for your payment this month."

    Column(modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().background(Col.background).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Col.ink, modifier = Modifier.clip(CircleShape).clickable(onClick = onBack).padding(4.dp))
            Column(Modifier.weight(1f)) {
                Text(tenant?.fullName ?: "Tenant", color = Col.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    (s?.unit?.let { "Unit ${it.label}" } ?: "No unit") + (tenant?.phone?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                    color = Col.mutedFg, fontFamily = Mono, fontSize = 11.sp
                )
            }
            if (s != null) StatusChip(s.status)
        }
        HorizontalDivider(color = Col.border)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (thread.isEmpty()) Text("No messages yet. Say hello.", Modifier.fillMaxWidth().padding(top = 40.dp), color = Col.mutedFg, fontSize = 12.sp, textAlign = TextAlign.Center)
            else LazyColumn(state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(thread, key = { it.id }) { m ->
                    val out = m.direction == "out"
                    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = if (out) 4.dp else 16.dp, bottomStart = if (out) 16.dp else 4.dp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (out) Arrangement.End else Arrangement.Start) {
                        Column(
                            Modifier.widthIn(max = 290.dp).clip(shape)
                                .background(if (out) Col.ink else Col.card)
                                .then(if (out) Modifier else Modifier.border(1.dp, Col.border, shape))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(m.body, color = if (out) Col.card else Col.ink, fontSize = 14.sp)
                            Text(timeFmt.format(m.createdAt), Modifier.align(Alignment.End).padding(top = 4.dp), color = (if (out) Col.card else Col.mutedFg).copy(alpha = 0.6f), fontFamily = Mono, fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Col.border)
        Column(Modifier.fillMaxWidth().background(Col.background).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                quick, color = Col.mutedFg, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clip(CircleShape).border(1.dp, Col.border, CircleShape).clickable { text = quick }.padding(horizontal = 12.dp, vertical = 5.dp)
            )
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.weight(1f).height(44.dp).background(Col.card, CircleShape).border(1.dp, Col.border, CircleShape).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (text.isEmpty()) Text("Write a message…", color = Col.mutedFg.copy(alpha = 0.7f), fontSize = 14.sp)
                    BasicTextField(text, { text = it }, Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(color = Col.ink, fontSize = 14.sp), cursorBrush = SolidColor(Col.primary))
                }
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Col.primary.copy(alpha = if (vm.busy) 0.6f else 1f)).clickable(enabled = !vm.busy && text.isNotBlank()) {
                        vm.add("messages", JSONObject().put("tenant_id", tenantId).put("direction", "out").put("body", text.trim()), null) { text = "" }
                    },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Col.card, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}
