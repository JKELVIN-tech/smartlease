package com.smartlease.app

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.Locale

data class Property(val id: String, val name: String, val address: String?, val type: String?)
data class RentUnit(val id: String, val propertyId: String, val label: String, val bedrooms: Int, val rent: Double)
data class Tenant(
    val id: String, val unitId: String?, val fullName: String, val phone: String?,
    val email: String?, val leaseStart: String?, val leaseEnd: String?
)
data class Txn(
    val id: String, val tenantId: String?, val unitId: String?, val kind: String, val category: String,
    val amount: Double, val method: String?, val note: String?, val occurredOn: LocalDate
)
data class Msg(val id: String, val tenantId: String, val body: String, val direction: String, val createdAt: Instant)

data class Ledger(
    val properties: List<Property> = emptyList(),
    val units: List<RentUnit> = emptyList(),
    val tenants: List<Tenant> = emptyList(),
    val txns: List<Txn> = emptyList(),
    val messages: List<Msg> = emptyList()
)

private fun JSONObject.str(k: String): String? = if (isNull(k)) null else getString(k)

fun JSONObject.toProperty() = Property(getString("id"), getString("name"), str("address"), str("property_type"))
fun JSONObject.toUnit() = RentUnit(getString("id"), getString("property_id"), getString("label"), optInt("bedrooms", 1), getDouble("rent"))
fun JSONObject.toTenant() = Tenant(
    getString("id"), str("unit_id"), getString("full_name"), str("phone"), str("email"),
    str("lease_start"), str("lease_end")
)
fun JSONObject.toTxn() = Txn(
    getString("id"), str("tenant_id"), str("unit_id"), getString("kind"), getString("category"),
    getDouble("amount"), str("method"), str("note"), LocalDate.parse(getString("occurred_on"))
)
fun JSONObject.toMsg() = Msg(
    getString("id"), getString("tenant_id"), getString("body"), getString("direction"),
    OffsetDateTime.parse(getString("created_at")).toInstant()
)

enum class PayStatus { PAID, PARTIAL, DUE, OVERDUE, VACANT }
data class TStatus(val unit: RentUnit?, val rent: Double, val paid: Double, val balance: Double, val status: PayStatus)

fun monthKey(d: LocalDate): String = YearMonth.from(d).toString()
fun kes(n: Double): String = "KES " + String.format(Locale.US, "%,d", Math.round(n))

/** Same rules as the website: overdue once past the 5th with nothing paid. */
fun tenantStatus(t: Tenant, units: List<RentUnit>, txns: List<Txn>, today: LocalDate = LocalDate.now()): TStatus {
    val unit = units.firstOrNull { it.id == t.unitId }
    val rent = unit?.rent ?: 0.0
    val mk = monthKey(today)
    val paid = txns.filter { it.tenantId == t.id && it.kind == "income" && monthKey(it.occurredOn) == mk }.sumOf { it.amount }
    val status = when {
        unit == null -> PayStatus.VACANT
        rent > 0 && paid >= rent -> PayStatus.PAID
        paid > 0 -> PayStatus.PARTIAL
        today.dayOfMonth > 5 -> PayStatus.OVERDUE
        else -> PayStatus.DUE
    }
    return TStatus(unit, rent, paid, maxOf(rent - paid, 0.0), status)
}
