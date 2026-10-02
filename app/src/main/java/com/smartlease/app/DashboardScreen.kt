package com.smartlease.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as DateStyle
import java.util.Locale

private val statusOrder = listOf(PayStatus.OVERDUE, PayStatus.PARTIAL, PayStatus.DUE, PayStatus.PAID, PayStatus.VACANT)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(vm: AppViewModel, openThread: (String) -> Unit, goTab: (Int) -> Unit) {
    val d = vm.ledger
    val today = LocalDate.now()
    val mk = monthKey(today)
    val prevMk = monthKey(today.minusMonths(1))
    fun income(k: String) = d.txns.filter { it.kind == "income" && monthKey(it.occurredOn) == k }.sumOf { it.amount }
    val collected = income(mk)
    val prev = income(prevMk)
    val change = if (prev > 0) (collected - prev) / prev * 100 else 0.0

    val statuses = d.tenants.map { it to tenantStatus(it, d.units, d.txns) }
    val occupied = d.tenants.mapNotNull { it.unitId }.toSet().size
    val occupancy = if (d.units.isNotEmpty()) Math.round(occupied.toDouble() / d.units.size * 100).toInt() else 0
    val overdue = statuses.count { it.second.status == PayStatus.OVERDUE }
    val outstanding = statuses.sumOf { it.second.balance }

    val months = (5 downTo 0).map { YearMonth.from(today).minusMonths(it.toLong()) }
    val labels = months.map { it.month.getDisplayName(DateStyle.SHORT, Locale.ENGLISH) }
    val inc = months.map { income(it.toString()) }
    val exp = months.map { m -> d.txns.filter { it.kind == "expense" && monthKey(it.occurredOn) == m.toString() }.sumOf { it.amount } }
    val feed = statuses.sortedBy { statusOrder.indexOf(it.second.status) }.take(4)
    val slices = listOf(
        PayStatus.PAID to Col.chart2, PayStatus.PARTIAL to Col.chart3,
        PayStatus.DUE to Col.chart4, PayStatus.OVERDUE to Col.chart1
    ).map { (s, c) -> statuses.count { it.second.status == s }.toFloat() to c }.filter { it.first > 0f }

    // header
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).background(Col.ink, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
            Text("S", color = Col.background, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text("SmartLease", color = Col.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("LEDGER", color = Col.mutedFg, fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.sp)
        }
        Box(
            Modifier.size(32.dp).clip(CircleShape).border(1.dp, Col.border, CircleShape).clickable { vm.signOut() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.AutoMirrored.Outlined.Logout, "Sign out", tint = Col.mutedFg, modifier = Modifier.size(16.dp)) }
    }

    // hero
    Column(Modifier.fillMaxWidth().background(Col.card)) {
        HorizontalDivider(color = Col.border)
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Eyebrow("Rent collected · ${today.month.getDisplayName(DateStyle.SHORT, Locale.ENGLISH)} ${today.year}")
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(kes(collected), color = Col.ink, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 32.sp)
                if (prev > 0) {
                    val up = change >= 0
                    Text(
                        (if (up) "+" else "") + String.format(Locale.US, "%.1f", change) + "%",
                        color = if (up) Col.paid else Col.primary, fontFamily = Mono, fontSize = 11.sp,
                        modifier = Modifier.background(if (up) Col.paidSoft else Col.accent, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("$occupancy%" to "occupancy", "$overdue" to "overdue", kes(outstanding) to "outstanding").forEach { (v, l) ->
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Col.ink)) { append(v) }
                        append(" $l")
                    }, color = Col.mutedFg, fontSize = 11.sp)
                }
            }
            AreaChart(inc, labels, Modifier.fillMaxWidth().padding(top = 12.dp).height(96.dp))
        }
        HorizontalDivider(color = Col.border)
    }

    // action feed
    Column(Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp)) {
        SectionTitle("Action feed", "This month")
        if (feed.isEmpty()) Empty("No tenants yet.", "Add a tenant") { goTab(2) }
        else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            feed.forEach { (t, s) ->
                Row(
                    Modifier.fillMaxWidth().ledgerCard().clickable { openThread(t.id) }.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Initial(t.fullName, toneFor(s.status))
                    Column(Modifier.weight(1f)) {
                        Text(t.fullName, color = Col.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            s.unit?.let { u -> "Unit ${u.label} · ${d.properties.firstOrNull { it.id == u.propertyId }?.name ?: ""}" } ?: "No unit",
                            color = Col.mutedFg, fontFamily = Mono, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    StatusChip(s.status)
                }
            }
        }
    }

    // analytics
    Column(Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp)) {
        SectionTitle("Analytics", "6 months")
        Column(Modifier.fillMaxWidth().ledgerCard().padding(12.dp)) {
            Eyebrow("Income vs expenses", Modifier.padding(bottom = 8.dp))
            BarChart(listOf(inc, exp), listOf(Col.chart2, Col.chart1), labels, Modifier.fillMaxWidth().height(144.dp))
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp).height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight().ledgerCard().padding(12.dp)) {
                Eyebrow("Payment status")
                if (slices.isEmpty()) Text("No data yet", Modifier.fillMaxWidth().padding(vertical = 32.dp), color = Col.mutedFg, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                else Donut(slices, Modifier.fillMaxWidth().height(96.dp).padding(top = 8.dp), stroke = 14.dp)
            }
            Column(Modifier.weight(1f).fillMaxHeight().ledgerCard().padding(12.dp)) {
                Eyebrow("Net this month")
                Spacer(Modifier.weight(1f))
                Text(kes(inc[5] - exp[5]), Modifier.padding(top = 24.dp), color = Col.ink, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                Text("${d.units.size} units · ${d.tenants.size} tenants", color = Col.mutedFg, fontFamily = Mono, fontSize = 11.sp)
            }
        }
    }

    // properties
    Column(Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp)) {
        SectionTitle("Properties", d.properties.size.toString())
        if (d.properties.isEmpty()) Empty("No properties yet.", "Add one") { goTab(1) }
        else Column(Modifier.fillMaxWidth().ledgerCard()) {
            d.properties.take(4).forEachIndexed { i, p ->
                if (i > 0) HorizontalDivider(color = Col.border)
                val pu = d.units.filter { it.propertyId == p.id }
                val occ = pu.count { u -> d.tenants.any { it.unitId == u.id } }
                val pct = if (pu.isNotEmpty()) occ.toFloat() / pu.size else 0f
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(p.name, color = Col.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("$occ/${pu.size} units", color = Col.mutedFg, fontFamily = Mono, fontSize = 11.sp)
                    }
                    Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(6.dp).clip(CircleShape).background(Col.secondary)) {
                        Box(Modifier.fillMaxWidth(pct).fillMaxHeight().background(if (pct >= 0.8f) Col.warn else Col.paid))
                    }
                }
            }
        }
    }
}
