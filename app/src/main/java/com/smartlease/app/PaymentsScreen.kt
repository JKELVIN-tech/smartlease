package com.smartlease.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.time.LocalDate

@Composable
fun PaymentsScreen(vm: AppViewModel) {
    val d = vm.ledger
    var show by remember { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf("all") }
    val today = LocalDate.now()
    val mk = monthKey(today)
    val month = d.txns.filter { monthKey(it.occurredOn) == mk }
    val inc = month.filter { it.kind == "income" }.sumOf { it.amount }
    val exp = month.filter { it.kind == "expense" }.sumOf { it.amount }
    val days = today.lengthOfMonth()
    val daily = (1..days).map { day -> month.filter { it.kind == "income" && it.occurredOn.dayOfMonth == day }.sumOf { it.amount } }
    val dayLabels = (1..days).map { if ((it - 1) % 5 == 0) it.toString() else "" }
    val cats = d.txns.filter { it.kind == "expense" }.groupBy { it.category }.map { (k, v) -> k to v.sumOf { it.amount } }
    val palette = listOf(Col.chart1, Col.chart2, Col.chart3, Col.chart4, Col.chart5)
    val list = d.txns.filter { filter == "all" || it.kind == filter }

    PageHeader("Payments", "Ledger") { AddButton("Record transaction") { show = true } }

    Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f).ledgerCard().padding(12.dp)) {
            Eyebrow("In · this month")
            Text(kes(inc), Modifier.padding(top = 4.dp), color = Col.paid, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        }
        Column(Modifier.weight(1f).ledgerCard().padding(12.dp)) {
            Eyebrow("Out · this month")
            Text(kes(exp), Modifier.padding(top = 4.dp), color = Col.primary, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        }
    }

    Column(Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp)) {
        Column(Modifier.fillMaxWidth().ledgerCard().padding(12.dp)) {
            Eyebrow("Daily collections", Modifier.padding(bottom = 4.dp))
            BarChart(listOf(daily), listOf(Col.chart2), dayLabels, Modifier.fillMaxWidth().height(112.dp))
        }
        if (cats.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp).ledgerCard().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Donut(cats.mapIndexed { i, c -> c.second.toFloat() to palette[i % 5] }, Modifier.size(96.dp), stroke = 16.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Eyebrow("Expenses by category")
                    cats.take(4).forEachIndexed { i, (name, value) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(8.dp).background(palette[i % 5], CircleShape))
                                Text(name, color = Col.ink, fontSize = 11.sp)
                            }
                            Text(kes(value), color = Col.ink, fontFamily = Mono, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    Column(Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp)) {
        Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("all" to "All", "income" to "Income", "expense" to "Expenses").forEach { (k, l) ->
                val on = filter == k
                Text(
                    l, color = if (on) Col.background else Col.mutedFg, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(CircleShape).background(if (on) Col.ink else Color.Transparent)
                        .border(1.dp, if (on) Col.ink else Col.border, CircleShape)
                        .clickable { filter = k }.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
        SectionTitle("Transactions", list.size.toString())
        if (list.isEmpty()) Empty("No transactions yet.")
        else Column(Modifier.fillMaxWidth().ledgerCard()) {
            list.forEachIndexed { i, t ->
                if (i > 0) HorizontalDivider(color = Col.border)
                val tn = d.tenants.firstOrNull { it.id == t.tenantId }
                val u = d.units.firstOrNull { it.id == t.unitId }
                val isIn = t.kind == "income"
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).background(if (isIn) Col.paidSoft else Col.accent, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text(if (isIn) "+" else "−", color = if (isIn) Col.paid else Col.primary, fontFamily = Mono, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(t.category + (u?.let { " · ${it.label}" } ?: ""), color = Col.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${t.occurredOn} · ${tn?.fullName ?: t.note ?: t.method ?: ""}", color = Col.mutedFg, fontFamily = Mono, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text((if (isIn) "+" else "−") + kes(t.amount), color = if (isIn) Col.paid else Col.primary, fontFamily = Mono, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
    if (show) PaymentSheet(vm) { show = false }
}

@Composable
private fun PaymentSheet(vm: AppViewModel, close: () -> Unit) {
    val d = vm.ledger
    var kind by remember { mutableStateOf("income") }
    var amount by remember { mutableStateOf("") }
    var tenant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Rent") }
    var method by remember { mutableStateOf("M-Pesa") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var note by remember { mutableStateOf("") }
    val cats = if (kind == "income") listOf("Rent", "Deposit", "Water", "Service charge", "Other")
    else listOf("Repairs", "Utilities", "Security", "Cleaning", "Tax", "Other")

    Sheet("Record transaction", close) {
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp).background(Col.secondary, RoundedCornerShape(12.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("income" to "Payment in", "expense" to "Expense").forEach { (k, l) ->
                Box(
                    Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (kind == k) Col.card else Color.Transparent)
                        .clickable { kind = k; category = if (k == "income") "Rent" else "Repairs" },
                    contentAlignment = Alignment.Center
                ) { Text(l, color = if (kind == k) Col.ink else Col.mutedFg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
        Field("Amount (KES)") { Input(amount, { amount = it }, keyboard = KeyboardType.Decimal) }
        Field(if (kind == "income") "Tenant" else "Related tenant (optional)") {
            Select(listOf("" to "— Select —") + d.tenants.map { it.id to it.fullName }, tenant) { tenant = it }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { Field("Category") { Select(cats.map { it to it }, category) { category = it } } }
            Box(Modifier.weight(1f)) { Field("Method") { Select(listOf("M-Pesa", "Bank", "Cash", "Cheque").map { it to it }, method) { method = it } } }
        }
        Field("Date") { Input(date, { date = it }, "YYYY-MM-DD") }
        Field("Note") { Input(note, { note = it }, "Ref / description") }
        SubmitButton("Save", vm.busy) {
            val a = amount.toDoubleOrNull()
            val day = parseDate(date)
            when {
                a == null || a <= 0 -> vm.message = "Enter the amount."
                kind == "income" && tenant.isBlank() -> vm.message = "Select the tenant."
                day == null -> vm.message = "Date must look like 2026-01-31."
                else -> vm.add(
                    "transactions",
                    JSONObject().put("kind", kind).put("category", category).put("amount", a).put("method", method)
                        .put("occurred_on", day.toString())
                        .put("tenant_id", if (tenant.isBlank()) JSONObject.NULL else tenant)
                        .put("unit_id", d.tenants.firstOrNull { it.id == tenant }?.unitId ?: JSONObject.NULL)
                        .putOrNull("note", note),
                    if (kind == "income") "Payment recorded" else "Expense recorded", close
                )
            }
        }
    }
}
