package com.smartlease.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiException(message: String, val code: Int) : Exception(message)

data class Session(val access: String, val refresh: String, val expiresAt: Long, val userId: String, val email: String)

/** Talks to the same Supabase backend as the website. */
object Api {
    private const val BASE = "https://c--6c21d434-c16c-436f-9ebc-db4ebbb739cb-prod.lovable.cloud"
    private const val KEY = "sb_publishable_GPl30JZBmMsF24lud-3jqQ_TydoJMIv"
    private const val SITE = "https://rent-reach-app.lovable.app/dashboard"

    private fun errorMessage(text: String, code: Int): String = try {
        val o = JSONObject(text)
        listOf("msg", "message", "error_description", "error").firstNotNullOfOrNull { k ->
            o.optString(k, "").takeIf { it.isNotBlank() }
        } ?: "Request failed ($code)"
    } catch (e: Exception) { "Request failed ($code)" }

    private fun request(method: String, path: String, body: String? = null, token: String? = null, prefer: String? = null): String {
        val c = URL(BASE + path).openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            c.connectTimeout = 15000
            c.readTimeout = 20000
            c.setRequestProperty("apikey", KEY)
            if (token != null) c.setRequestProperty("Authorization", "Bearer $token")
            if (prefer != null) c.setRequestProperty("Prefer", prefer)
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = c.responseCode
            val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw ApiException(errorMessage(text, code), code)
            return text
        } finally {
            c.disconnect()
        }
    }

    private fun parseSession(text: String): Session? {
        val o = JSONObject(text)
        if (!o.has("access_token")) return null
        val user = o.optJSONObject("user")
        return Session(
            o.getString("access_token"), o.getString("refresh_token"),
            System.currentTimeMillis() / 1000 + o.optLong("expires_in", 3600),
            user?.optString("id") ?: "", user?.optString("email") ?: ""
        )
    }

    private fun creds(email: String, password: String) =
        JSONObject().put("email", email).put("password", password).toString()

    fun signIn(email: String, password: String): Session =
        parseSession(request("POST", "/auth/v1/token?grant_type=password", creds(email, password)))
            ?: throw ApiException("Sign in failed.", 400)

    /** Returns null when the account needs email confirmation first. */
    fun signUp(email: String, password: String): Session? =
        parseSession(request("POST", "/auth/v1/signup?redirect_to=$SITE", creds(email, password)))

    fun refresh(s: Session): Session =
        parseSession(request("POST", "/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", s.refresh).toString()))
            ?: throw ApiException("Session expired.", 401)

    fun select(table: String, order: String, token: String): List<JSONObject> {
        val arr = JSONArray(request("GET", "/rest/v1/$table?select=*&order=$order.desc", token = token))
        return List(arr.length()) { arr.getJSONObject(it) }
    }

    fun insert(table: String, row: JSONObject, token: String) {
        request("POST", "/rest/v1/$table", row.toString(), token, prefer = "return=minimal")
    }
}

object SessionStore {
    private fun prefs(c: Context) = c.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun load(c: Context): Session? {
        val p = prefs(c)
        val access = p.getString("access", null) ?: return null
        return Session(access, p.getString("refresh", "") ?: "", p.getLong("exp", 0L), p.getString("uid", "") ?: "", p.getString("email", "") ?: "")
    }

    fun save(c: Context, s: Session?) {
        val e = prefs(c).edit()
        e.clear()
        if (s != null) {
            e.putString("access", s.access).putString("refresh", s.refresh).putLong("exp", s.expiresAt)
                .putString("uid", s.userId).putString("email", s.email)
        }
        e.apply()
    }
}
