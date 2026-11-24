package com.montecarlo.littlegaza

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Exercise(
    val type: String,          // "choice", "count" or "listen"
    val prompt: String,
    val speak: String,
    val show: String,
    val count: Int,
    val countEmoji: String,
    val options: List<String>,
    val answer: Int
)

data class Lesson(
    val id: String,
    val subjectId: String,
    val level: Int,
    val title: String,
    val icon: String,
    val introBig: String,
    val introText: String,
    val introSpeak: String,
    val exercises: List<Exercise>
)

data class Level(val level: Int, val title: String, val lessons: List<Lesson>)

data class Subject(val id: String, val title: String, val speakTitle: String, val icon: String, val levels: List<Level>) {
    val allLessons: List<Lesson> = levels.flatMap { it.lessons }
}

data class PlacementQ(val level: Int, val exercise: Exercise)

data class Story(val title: String, val icon: String, val pages: List<String>)

class Content(
    val subjects: List<Subject>,
    val placement: Map<String, List<PlacementQ>>,
    val stories: List<Story>
) {
    fun subject(id: String): Subject = subjects.first { it.id == id }

    fun lesson(id: String): Lesson? {
        for (s in subjects) for (l in s.allLessons) if (l.id == id) return l
        return null
    }

    companion object {
        fun load(ctx: Context): Content {
            val text = ctx.assets.open("content.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            return parse(JSONObject(text))
        }

        private fun strings(a: JSONArray?): List<String> {
            if (a == null) return emptyList()
            return List(a.length()) { a.getString(it) }
        }

        private fun exercise(o: JSONObject) = Exercise(
            type = o.optString("type", "choice"),
            prompt = o.optString("prompt"),
            speak = o.optString("speak"),
            show = o.optString("show"),
            count = o.optInt("count", 0),
            countEmoji = o.optString("countEmoji"),
            options = strings(o.optJSONArray("options")),
            answer = o.optInt("answer", 0)
        )

        private fun parse(root: JSONObject): Content {
            val subjects = mutableListOf<Subject>()
            val sArr = root.getJSONArray("subjects")
            for (i in 0 until sArr.length()) {
                val so = sArr.getJSONObject(i)
                val sid = so.getString("id")
                val levels = mutableListOf<Level>()
                val lvArr = so.getJSONArray("levels")
                for (j in 0 until lvArr.length()) {
                    val lo = lvArr.getJSONObject(j)
                    val lvNum = lo.getInt("level")
                    val lessons = mutableListOf<Lesson>()
                    val lsArr = lo.getJSONArray("lessons")
                    for (k in 0 until lsArr.length()) {
                        val l = lsArr.getJSONObject(k)
                        val exArr = l.getJSONArray("exercises")
                        lessons.add(
                            Lesson(
                                id = l.getString("id"),
                                subjectId = sid,
                                level = lvNum,
                                title = l.optString("title"),
                                icon = l.optString("icon"),
                                introBig = l.optString("introBig"),
                                introText = l.optString("introText"),
                                introSpeak = l.optString("introSpeak"),
                                exercises = List(exArr.length()) { exercise(exArr.getJSONObject(it)) }
                            )
                        )
                    }
                    levels.add(Level(lvNum, lo.optString("title"), lessons))
                }
                subjects.add(Subject(sid, so.optString("title"), so.optString("speakTitle", so.optString("title")), so.optString("icon"), levels))
            }

            val placement = mutableMapOf<String, List<PlacementQ>>()
            val pObj = root.optJSONObject("placement")
            if (pObj != null) {
                val keys = pObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val arr = pObj.getJSONArray(key)
                    placement[key] = List(arr.length()) {
                        val q = arr.getJSONObject(it)
                        PlacementQ(q.getInt("level"), exercise(q.getJSONObject("exercise")))
                    }
                }
            }

            val stories = mutableListOf<Story>()
            val stArr = root.optJSONArray("stories")
            if (stArr != null) {
                for (i in 0 until stArr.length()) {
                    val so = stArr.getJSONObject(i)
                    stories.add(Story(so.optString("title"), so.optString("icon"), strings(so.optJSONArray("pages"))))
                }
            }
            return Content(subjects, placement, stories)
        }
    }
}
