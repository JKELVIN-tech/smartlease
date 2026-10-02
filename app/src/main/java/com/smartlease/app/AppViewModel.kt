package com.smartlease.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AppViewModel(app: Application) : AndroidViewModel(app) {
    var session by mutableStateOf(SessionStore.load(app)); private set
    var ledger by mutableStateOf(Ledger()); private set
    var loading by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null)

    init { if (session != null) refresh() }

    private fun updateSession(s: Session?) {
        session = s
        SessionStore.save(getApplication(), s)
    }

    private fun renew(s: Session): Session = try {
        Api.refresh(s).also { updateSession(it) }
    } catch (e: ApiException) {
        updateSession(null); ledger = Ledger()
        throw ApiException("Session expired. Please sign in again.", 401)
    }

    private suspend fun <T> withAuth(block: (String) -> T): T = withContext(Dispatchers.IO) {
        var s = session ?: throw ApiException("Please sign in.", 401)
        if (System.currentTimeMillis() / 1000 > s.expiresAt - 60) s = renew(s)
        try {
            block(s.access)
        } catch (e: ApiException) {
            if (e.code != 401) throw e
            s = renew(s)
            block(s.access)
        }
    }

    private fun order(table: String) = if (table == "transactions") "occurred_on" else "created_at"

    private fun report(e: Exception) {
        message = if (e is ApiException) e.message else "Network error. Check your connection."
    }

    private fun Ledger.with(table: String, rows: List<JSONObject>) = when (table) {
        "properties" -> copy(properties = rows.map { it.toProperty() })
        "units" -> copy(units = rows.map { it.toUnit() })
        "tenants" -> copy(tenants = rows.map { it.toTenant() })
        "transactions" -> copy(txns = rows.map { it.toTxn() })
        else -> copy(messages = rows.map { it.toMsg() })
    }

    fun refresh() {
        viewModelScope.launch {
            loading = true
            try {
                ledger = withAuth { t ->
                    listOf("properties", "units", "tenants", "transactions", "messages")
                        .fold(Ledger()) { acc, table -> acc.with(table, Api.select(table, order(table), t)) }
                }
            } catch (e: Exception) { report(e) } finally { loading = false }
        }
    }

    fun authenticate(email: String, password: String, signUp: Boolean) {
        viewModelScope.launch {
            busy = true
            try {
                val s = withContext(Dispatchers.IO) { if (signUp) Api.signUp(email, password) else Api.signIn(email, password) }
                if (s == null) message = "Check your email to confirm your account." else { updateSession(s); refresh() }
            } catch (e: Exception) { report(e) } finally { busy = false }
        }
    }

    fun signOut() { updateSession(null); ledger = Ledger() }

    /** Inserts a row, reloads that table, then calls [onDone]. [ok] is shown as a toast when not null. */
    fun add(table: String, row: JSONObject, ok: String?, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            busy = true
            try {
                val rows = withAuth { t -> Api.insert(table, row, t); Api.select(table, order(table), t) }
                ledger = ledger.with(table, rows)
                if (ok != null) message = ok
                onDone()
            } catch (e: Exception) { report(e) } finally { busy = false }
        }
    }
}