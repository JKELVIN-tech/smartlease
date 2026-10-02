package com.smartlease.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.time.LocalDate

internal fun JSONObject.putOrNull(k: String, v: String) = put(k, if (v.isBlank()) JSONObject.NULL else v.trim())
internal fun parseDate(s: String) = runCatching { LocalDate.parse(s.trim()) }.getOrNull()

// ---------------- Properties ----------------

@Composable
fun PropertiesScreen(vm: AppViewModel) {
    val d = vm.ledger
    var showProperty by remember { mutableStateOf(false) }
    var unitFor by remember { mutableStateOf<String?>(null) }
    var open by rememberSaveable { mutableStateOf<String?>(null) }

    PageHeader("Properties", "${d.properties.size} buildings · ${d.units.size} units") { AddButton("Add property") { showProperty = true } }
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (d.properties.isEmpty()) Empty("Tap + to record your first property.")
        d.properties.forEach { p ->
            val pu = d.units.filter { it.propertyId == p.id }
            val isOpen = open == p.id
            Column(Modifier.fillMaxWidth().ledgerCard()) {
                Row(
                    Modifier.fillMaxWidth().clickable { open = if (isOpen) null else p.id }.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(40.dp).background(Col.accent, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text(p.name.take(1), color = Col.primary, fontFamily = Display, fontWeight = FontWeight.SemiBold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(p.name, color = Col.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${p.type ?: ""} · ${p.address?.ifBlank { null } ?: "No address"}", color = Col.mutedFg, fontFamily = Mono, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("${pu.size} units", color = Col.mutedFg, fontFamily = Mono, fontSize = 11.sp)
                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = Col.mutedFg, modifier = Modifier.size(18.dp).rotate(if (isOpen) 180f else 0f))
                }
                if (isOpen) {
                    Column(Modifier.fillMaxWidth().background(Col.background.copy(alpha = 0.5f))) {
                        pu.forEach { u ->
                            HorizontalDivider(color = Col.border)
                            val t = d.tenants.firstOrNull { it.unitId == u.id }
                            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Unit ${u.label}", color = Col.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${u.bedrooms} BR · ${t?.fullName ?: "Vacant"}", color = Col.mutedFg, fontFamily = Mono, fontSize = 11.sp)
                                }
                                Text(kes(u.rent), color = Col.ink, fontFamily = Mono, fontSize = 12.sp)
                            }
                        }
                        HorizontalDivider(color = Col.border)
                        Text("+ Add unit", Modifier.fillMaxWidth().clickable { unitFor = p.id }.padding(vertical = 10.dp), color = Col.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
    if (showProperty) PropertySheet(vm) { showProperty = false }
    unitFor?.let { pid -> UnitSheet(vm, pid) { unitFor = null; open = pid } }
}

@Composable
private fun PropertySheet(vm: AppViewModel, close: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Apartment") }
    Sheet("New property", close) {
        Field("Name") { Input(name, { name = it }, "Muthaiga Court") }
        Field("Address") { Input(address, { address = it }, "Kiambu Rd, Nairobi") }
        Field("Type") { Select(listOf("Apartment", "House", "Commercial", "Hostel").map { it to it }, type) { type = it } }
        SubmitButton("Save property", vm.busy) {
            if (name.isBlank()) vm.message = "Enter a property name."
            else vm.add("properties", JSONObject().put("name", name.trim()).put("address", address.trim()).put("property_type", type), "Property added", close)
        }
    }
}

@Composable
private fun UnitSheet(vm: AppViewModel, propertyId: String, close: () -> Unit) {
    var label by remember { mutableStateOf("") }
    var beds by remember { mutableStateOf("1") }
    var rent by remember { mutableStateOf("") }
    Sheet("New unit", close) {
        Field("Unit label") { Input(label, { label = it }, "A3") }
        Field("Bedrooms") { Input(beds, { beds = it }, keyboard = KeyboardType.Number) }
        Field("Monthly rent (KES)") { Input(rent, { rent = it }, "25000", KeyboardType.Number) }
        SubmitButton("Save unit", vm.busy) {
            if (label.isBlank() || rent.isBlank()) vm.message = "Enter a unit label and the monthly rent."
            else vm.add(
                "units",
                JSONObject().put("property_id", propertyId).put("label", label.trim())
                    .put("bedrooms", beds.toIntOrNull() ?: 1).put("rent", rent.toDoubleOrNull() ?: 0.0),
                "Unit added", close
            )
        }
    }
}

// ---------------- Tenants ----------------

@Composable
fun TenantsScreen(vm: AppViewModel, openThread: (String) -> Unit) {
    val d = vm.ledger
    val ctx = LocalContext.current
    var show by remember { mutableStateOf(false) }

    PageHeader("Tenants", "${d.tenants.size} on record") { AddButton("Add tenant") { show = true } }
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (d.tenants.isEmpty()) Empty("Tap + to add a tenant.")
        d.tenants.forEach { t ->
            val s = tenantStatus(t, d.units, d.txns)
            val prop = d.properties.firstOrNull { it.id == s.unit?.propertyId }
            Column(Modifier.fillMaxWidth().ledgerCard().padding(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Initial(t.fullName, toneFor(s.status))
                    Column(Modifier.weight(1f)) {
                        Text(t.fullName, color = Col.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (s.unit != null) "Unit ${s.unit.label} · ${prop?.name ?: ""}" else "No unit assigned", color = Col.mutedFg, fontFamily = Mono, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    StatusChip(s.status)
                }
                Row(Modifier.fillMaxWidth().padding(start = 52.dp, top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Paid ${kes(s.paid)} of ${kes(s.rent)}", color = Col.mutedFg, fontFamily = Mono, fontSize = 11.sp)
                        if (!t.leaseEnd.isNullOrBlank()) Text("Lease ends ${t.leaseEnd}", color = Col.mutedFg, fontFamily = Mono, fontSize = 11.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!t.phone.isNullOrBlank()) Box(
                            Modifier.size(32.dp).clip(CircleShape).border(1.dp, Col.border, CircleShape)
                                .clickable { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${t.phone}"))) },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Outlined.Phone, "Call", tint = Col.ink, modifier = Modifier.size(15.dp)) }
                        Box(Modifier.size(32.dp).clip(CircleShape).background(Col.ink).clickable { openThread(t.id) }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Chat, "Message", tint = Col.card, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
    if (show) TenantSheet(vm) { show = false }
}

@Composable
private fun TenantSheet(vm: AppViewModel, close: () -> Unit) {
    val d = vm.ledger
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var idNo by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    val free = d.units.filter { u -> d.tenants.none { it.unitId == u.id } }
    val options = listOf("" to "— Not assigned —") + free.map { u ->
        u.id to "${d.properties.firstOrNull { it.id == u.propertyId }?.name ?: ""} · ${u.label}"
    }
    Sheet("New tenant", close) {
        Field("Full name") { Input(name, { name = it }, "Amina Wanjiru") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { Field("Phone") { Input(phone, { phone = it }, "0712 345 678", KeyboardType.Phone) } }
            Box(Modifier.weight(1f)) { Field("ID number") { Input(idNo, { idNo = it }) } }
        }
        Field("Email") { Input(email, { email = it }, keyboard = KeyboardType.Email) }
        Field("Unit") { Select(options, unit) { unit = it } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { Field("Lease start") { Input(start, { start = it }, "YYYY-MM-DD") } }
            Box(Modifier.weight(1f)) { Field("Lease end") { Input(end, { end = it }, "YYYY-MM-DD") } }
        }
        SubmitButton("Save tenant", vm.busy) {
            when {
                name.isBlank() -> vm.message = "Enter the tenant's full name."
                start.isNotBlank() && parseDate(start) == null || end.isNotBlank() && parseDate(end) == null ->
                    vm.message = "Dates must look like 2026-01-31."
                else -> vm.add(
                    "tenants",
                    JSONObject().put("full_name", name.trim()).putOrNull("phone", phone).putOrNull("email", email)
                        .putOrNull("id_number", idNo).putOrNull("unit_id", unit)
                        .putOrNull("lease_start", start).putOrNull("lease_end", end),
                    "Tenant added", close
                )
            }
        }
    }
}
