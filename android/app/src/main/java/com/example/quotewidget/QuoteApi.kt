package com.example.quotewidget

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object QuoteApi {
    private const val BASE = "http://43.139.93.239:8000"

    data class Quote(val id: Int, val text: String, val created_at: String)

    fun fetchRandomText(): String? {
        return try {
            val url = URL("$BASE/quotes/raw")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "QuoteWidget-Android")
            if (conn.responseCode != 200) return null
            val text = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).readText().trim()
            conn.disconnect()
            if (text.isEmpty() || text == "null") null else text
        } catch (_: Exception) { null }
    }

    fun fetchAll(): List<Quote> {
        return try {
            val url = URL("$BASE/quotes?limit=200")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "QuoteWidget-Android")
            val json = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).readText()
            conn.disconnect()
            val root = JSONObject(json)
            val arr = root.getJSONArray("quotes")
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Quote(o.getInt("id"), o.getString("text"), o.getString("created_at"))
            }
        } catch (_: Exception) { emptyList() }
    }

    fun add(text: String): Boolean {
        return try {
            val url = URL("$BASE/quotes")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.doOutput = true
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            val body = JSONObject().put("text", text).toString()
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body) }
            conn.responseCode == 200
        } catch (_: Exception) { false }
    }

    fun delete(id: Int): Boolean {
        return try {
            val url = URL("$BASE/quotes/$id")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.requestMethod = "DELETE"
            conn.responseCode == 200
        } catch (_: Exception) { false }
    }

    fun update(id: Int, text: String): Boolean {
        return try {
            val url = URL("$BASE/quotes/$id")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.doOutput = true
            conn.requestMethod = "PUT"
            conn.setRequestProperty("Content-Type", "application/json")
            val body = JSONObject().put("text", text).toString()
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body) }
            conn.responseCode == 200
        } catch (_: Exception) { false }
    }

    // ====== Schedule APIs ======

    data class Schedule(val id: Int, val title: String, val date: String, val time: String, val done: Int)

    fun fetchSchedules(): List<Schedule> {
        return try {
            val url = URL("$BASE/schedules")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "QuoteWidget-Android")
            val json = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).readText()
            conn.disconnect()
            val root = JSONObject(json)
            val arr = root.getJSONArray("schedules")
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Schedule(o.getInt("id"), o.getString("title"),
                    o.optString("date", ""), o.optString("time", ""),
                    o.getInt("done"))
            }
        } catch (_: Exception) { emptyList() }
    }

    fun addSchedule(title: String, date: String, time: String): Boolean {
        return try {
            val url = URL("$BASE/schedules")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.doOutput = true
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            val body = JSONObject().apply {
                put("title", title)
                put("date", date)
                put("time", time)
            }.toString()
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body) }
            conn.responseCode == 200
        } catch (_: Exception) { false }
    }

    fun toggleSchedule(id: Int, done: Int): Boolean {
        return try {
            val url = URL("$BASE/schedules/$id")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.doOutput = true
            conn.requestMethod = "PUT"
            conn.setRequestProperty("Content-Type", "application/json")
            val body = JSONObject().put("done", done).toString()
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body) }
            conn.responseCode == 200
        } catch (_: Exception) { false }
    }

    fun deleteSchedule(id: Int): Boolean {
        return try {
            val url = URL("$BASE/schedules/$id")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.requestMethod = "DELETE"
            conn.responseCode == 200
        } catch (_: Exception) { false }
    }
}
