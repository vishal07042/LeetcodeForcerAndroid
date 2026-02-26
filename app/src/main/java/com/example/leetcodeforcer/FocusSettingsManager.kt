package com.example.leetcodeforcer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class FocusSession(
    val id: Long,
    val days: Set<Int>,
    val startMinute: Int,
    val endMinute: Int
)

object FocusSettingsManager {
    private const val PREFS_NAME = "focus_settings_prefs"
    private const val KEY_WHITELIST = "user_whitelist"
    private const val KEY_BLACKLIST = "user_blacklist"
    private const val KEY_SESSIONS = "focus_sessions"

    private val ALWAYS_ALLOWED_PACKAGES = setOf(
        "com.google.android.permissioncontroller",
        "com.google.android.gms",
        "com.android.vending",
        "com.whatsapp",
        "com.miui.securityadd",
        "com.google.android.keep",
        "com.focus.mobile.focus",
        "cc.forestapp",
        "droom.sleepIfUCan",
        "org.brilliant.android",
        "app.getatoms.android",
        "com.anthropic.claude",
        "com.phonepe.app",
        "com.miui.securitycenter",
        "com.miui.powerkeeper",
        "com.miui.cleanmaster",
        "miui.systemui.plugin",
        "com.google.android.packageinstaller",
        "com.miui.home",
        "com.android.settings",
        "com.android.systemui",
        "com.android.settingsaccessibility",
        "com.mi.globalminusscreen",
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher3",
        "com.example.leetcodeforcer",
        "com.google.android.apps.docs.editors.docs",
        "com.google.android.apps.docs.editors.sheets",
        "com.google.android.apps.docs.editors.slides",
        "com.google.android.gm",
        "notion.id",
        "com.example.peedo",
        "ai.x.grok",
        "com.wlxd.pomochallenge",
        "com.google.android.apps.messaging",
        "com.google.android.apps.bard"
    )

    fun getWhitelist(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_WHITELIST, emptySet()) ?: emptySet()
    }

    fun getBlacklist(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_BLACKLIST, emptySet()) ?: emptySet()
    }

    fun addToWhitelist(context: Context, pkg: String) {
        val normalized = normalizePackage(pkg) ?: return
        if (ALWAYS_ALLOWED_PACKAGES.contains(normalized)) return
        val whitelist = getWhitelist(context).toMutableSet()
        val blacklist = getBlacklist(context).toMutableSet()
        blacklist.remove(normalized)
        whitelist.add(normalized)
        persistLists(context, whitelist, blacklist)
    }

    fun addToBlacklist(context: Context, pkg: String) {
        val normalized = normalizePackage(pkg) ?: return
        if (ALWAYS_ALLOWED_PACKAGES.contains(normalized)) return
        val whitelist = getWhitelist(context).toMutableSet()
        val blacklist = getBlacklist(context).toMutableSet()
        whitelist.remove(normalized)
        blacklist.add(normalized)
        persistLists(context, whitelist, blacklist)
    }

    fun removeFromWhitelist(context: Context, pkg: String) {
        val whitelist = getWhitelist(context).toMutableSet()
        if (whitelist.remove(pkg)) {
            persistLists(context, whitelist, getBlacklist(context))
        }
    }

    fun removeFromBlacklist(context: Context, pkg: String) {
        val blacklist = getBlacklist(context).toMutableSet()
        if (blacklist.remove(pkg)) {
            persistLists(context, getWhitelist(context), blacklist)
        }
    }

    fun getSessions(context: Context): List<FocusSession> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SESSIONS, "[]") ?: "[]"
        val json = JSONArray(raw)
        val list = mutableListOf<FocusSession>()
        for (i in 0 until json.length()) {
            val item = json.optJSONObject(i) ?: continue
            val id = item.optLong("id", System.currentTimeMillis())
            val startMinute = item.optInt("startMinute", 0).coerceIn(0, 1439)
            val endMinute = item.optInt("endMinute", 0).coerceIn(0, 1439)
            val dayArray = item.optJSONArray("days") ?: JSONArray()
            val days = mutableSetOf<Int>()
            for (j in 0 until dayArray.length()) {
                val day = dayArray.optInt(j, -1)
                if (day in Calendar.SUNDAY..Calendar.SATURDAY) {
                    days.add(day)
                }
            }
            if (days.isNotEmpty()) {
                list.add(FocusSession(id = id, days = days, startMinute = startMinute, endMinute = endMinute))
            }
        }
        return list.sortedBy { it.id }
    }

    fun addSession(context: Context, session: FocusSession) {
        val sessions = getSessions(context).toMutableList()
        sessions.add(session)
        saveSessions(context, sessions)
    }

    fun removeSession(context: Context, id: Long) {
        val sessions = getSessions(context).filterNot { it.id == id }
        saveSessions(context, sessions)
    }

    fun isFocusSessionActiveNow(context: Context): Boolean {
        val sessions = getSessions(context)
        if (sessions.isEmpty()) {
            return true
        }
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_WEEK)
        val currentMinute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        return sessions.any { session ->
            if (!session.days.contains(currentDay)) {
                false
            } else if (session.startMinute == session.endMinute) {
                true
            } else if (session.startMinute < session.endMinute) {
                currentMinute in session.startMinute until session.endMinute
            } else {
                currentMinute >= session.startMinute || currentMinute < session.endMinute
            }
        }
    }

    fun isAlwaysAllowedPackage(pkg: String): Boolean {
        return ALWAYS_ALLOWED_PACKAGES.contains(pkg) ||
            pkg.contains("launcher") ||
            pkg.contains("systemui") ||
            pkg.contains("input") ||
            pkg.contains("dialer") ||
            pkg.contains("contacts") ||
            pkg.contains("home") ||
            pkg.contains("settings")
    }

    private fun normalizePackage(pkg: String?): String? {
        val value = pkg?.trim()?.lowercase() ?: return null
        if (value.isBlank() || !value.contains('.')) return null
        return value
    }

    private fun persistLists(context: Context, whitelist: Set<String>, blacklist: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet(KEY_WHITELIST, whitelist)
            .putStringSet(KEY_BLACKLIST, blacklist)
            .apply()
    }

    private fun saveSessions(context: Context, sessions: List<FocusSession>) {
        val array = JSONArray()
        sessions.forEach { session ->
            val obj = JSONObject()
            val days = JSONArray()
            session.days.sorted().forEach { day -> days.put(day) }
            obj.put("id", session.id)
            obj.put("days", days)
            obj.put("startMinute", session.startMinute.coerceIn(0, 1439))
            obj.put("endMinute", session.endMinute.coerceIn(0, 1439))
            array.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SESSIONS, array.toString()).apply()
    }
}
