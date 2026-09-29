package com.mazkiplay.agent

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private val history = JSONArray()
    private lateinit var chat: TextView
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        chat = findViewById(R.id.chat); input = findViewById(R.id.input)
        findViewById<Button>(R.id.send).setOnClickListener { send() }
    }

    private fun send() {
        val text = input.text.toString().trim(); if (text.isEmpty()) return
        input.setText(""); chat.append("\n\nYou: $text")
        history.put(JSONObject().put("role", "user").put("content", text))
        lifecycleScope.launch {
            val answer = withContext(Dispatchers.IO) { callAgent() }
            chat.append("\n\nAgent: $answer")
            history.put(JSONObject().put("role", "assistant").put("content", answer))
        }
    }

    private fun callAgent(): String {
        return try {
            val body = JSONObject().put("messages", history).toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url("${BuildConfig.API_BASE_URL}/api/chat").post(body).build()
            client.newCall(req).execute().use { r ->
                val raw = r.body?.string().orEmpty()
                if (!r.isSuccessful) return "Gateway error: ${JSONObject(raw).optString("error", "HTTP ${r.code}")}"
                JSONObject(raw).optJSONObject("message")?.optString("content", "No response") ?: "No response"
            }
        } catch (e: Exception) { "Connection error: ${e.message}" }
    }
}