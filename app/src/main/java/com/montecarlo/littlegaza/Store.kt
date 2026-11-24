package com.montecarlo.littlegaza

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A child on this phone. Only a nickname and an avatar — no real personal data. */
data class Profile(
    val id: String,
    val name: String,
    val avatar: String,
    val placementDone: Boolean,
    val placed: Map<String, Int>,   // subjectId -> 1 (start level 1), 2 (start level 2), 3 (all unlocked)
    val stars: Map<String, Int>     // lessonId -> best stars (1..3)
)

/** Everything is stored locally on the phone. Nothing ever leaves the device. */
class Store(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("little_gaza", Context.MODE_PRIVATE)

    fun profiles(): List<Profile> {
        val arr = try {
            JSONArray(prefs.getString("profiles", "[]"))
        } catch (e: Exception) {
            JSONArray()
        }
        val out = mutableListOf<Profile>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(
                Profile(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    avatar = o.optString("avatar", "🐱"),
                    placementDone = o.optBoolean("placementDone", false),
                    placed = intMap(o.optJSONObject("placed")),
                    stars = intMap(o.optJSONObject("stars"))
                )
            )
        }
        return out
    }

    private fun intMap(o: JSONObject?): Map<String, Int> {
        if (o == null) return emptyMap()
        val m = mutableMapOf<String, Int>()
        val keys = o.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            m[k] = o.optInt(k, 0)
        }
        return m
    }

    private fun save(list: List<Profile>) {
        val arr = JSONArray()
        for (p in list) {
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("avatar", p.avatar)
            o.put("placementDone", p.placementDone)
            o.put("placed", JSONObject(p.placed as Map<*, *>))
            o.put("stars", JSONObject(p.stars as Map<*, *>))
            arr.put(o)
        }
        prefs.edit().putString("profiles", arr.toString()).apply()
    }

    fun upsert(p: Profile) {
        val list = profiles().toMutableList()
        val i = list.indexOfFirst { it.id == p.id }
        if (i >= 0) list[i] = p else list.add(p)
        save(list)
    }

    fun delete(id: String) = save(profiles().filter { it.id != id })

    var pin: String?
        get() = prefs.getString("pin", null)
        set(v) {
            prefs.edit().apply { if (v == null) remove("pin") else putString("pin", v) }.apply()
        }

    var easternDigits: Boolean
        get() = prefs.getBoolean("eastern", true)
        set(v) {
            prefs.edit().putBoolean("eastern", v).apply()
        }
}
