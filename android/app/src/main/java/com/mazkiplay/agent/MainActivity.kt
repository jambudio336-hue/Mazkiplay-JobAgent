package com.mazkiplay.agent

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
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
    private lateinit var send: Button
    private lateinit var status: TextView
    private lateinit var modelLabel: TextView
    private val prefs by lazy { getSharedPreferences("agent", Context.MODE_PRIVATE) }
    private val openRouterUrl = "https://openrouter.ai/api/v1/chat/completions"
    private val defaultModel = "openai/gpt-5.6"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        chat = findViewById(R.id.chat)
        input = findViewById(R.id.input)
        send = findViewById(R.id.send)
        status = findViewById(R.id.status)
        modelLabel = findViewById(R.id.modelLabel)

        restoreHistory()
        updateModelLabel()

        findViewById<Button>(R.id.clear).setOnClickListener { clearConversation() }
        findViewById<Button>(R.id.settings).setOnClickListener { showSettings() }
        findViewById<Button>(R.id.quickPlan).setOnClickListener { usePrompt("Buat rencana langkah demi langkah untuk tujuan berikut: ") }
        findViewById<Button>(R.id.quickJob).setOnClickListener { usePrompt("Bantu saya membuat CV dan strategi melamar pekerjaan untuk posisi berikut: ") }
        findViewById<Button>(R.id.quickIdeas).setOnClickListener { usePrompt("Berikan 10 ide produk atau bisnis yang realistis untuk masalah berikut, lalu jelaskan cara validasinya: ") }
        findViewById<Button>(R.id.quickTranslate).setOnClickListener { usePrompt("Terjemahkan teks berikut secara natural ke bahasa Inggris dan pertahankan maksudnya: ") }
        send.setOnClickListener { sendMessage() }
        input.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
        }
    }

    private fun usePrompt(prefix: String) {
        input.setText(prefix)
        input.setSelection(input.text.length)
        input.requestFocus()
    }

    private fun sendMessage() {
        val text = input.text.toString().trim()
        if (text.isEmpty() || !send.isEnabled) return
        val key = prefs.getString("openrouter_key", "").orEmpty()
        if (key.isBlank()) {
            showSettings()
            status.text = "● Add OpenRouter API key first"
            return
        }
        input.setText("")
        append("You", text)
        history.put(JSONObject().put("role", "user").put("content", text))
        saveHistory()
        setBusy(true)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { callOpenRouter(key) }
            append("Mazkiplay Agent", result.first)
            history.put(JSONObject().put("role", "assistant").put("content", result.first))
            saveHistory()
            status.text = if (result.second != null) "● " + result.second else "● Ready"
            setBusy(false)
        }
    }

    private fun callOpenRouter(key: String): Pair<String, String?> {
        return try {
            val model = prefs.getString("model", defaultModel).orEmpty().ifBlank { defaultModel }
            val maxTokens = prefs.getString("max_tokens", "2048")?.toIntOrNull()?.coerceIn(256, 8192) ?: 2048
            val system = prefs.getString("system_prompt",
                "You are Mazkiplay Agent, a capable personal AI agent. Be concise but useful. Separate plans from completed actions. Never claim an external action happened unless a connected tool actually executed it."
            ).orEmpty()

            val messages = JSONArray().put(JSONObject().put("role", "system").put("content", system))
            val start = maxOf(0, history.length() - 40)
            for (i in start until history.length()) messages.put(history.getJSONObject(i))

            val body = JSONObject()
                .put("model", model)
                .put("messages", messages)
                .put("temperature", 0.2)
                .put("max_tokens", maxTokens)
                .toString()
                .toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url(openRouterUrl)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://mazkiplay.ai")
                .addHeader("X-Title", "Mazkiplay Agent")
                .post(body)
                .build()

            client.newCall(req).execute().use { r ->
                val raw = r.body?.string().orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrNull()
                if (!r.isSuccessful) {
                    return ("OpenRouter error: " + (json?.optJSONObject("error")?.optString("message", "HTTP " + r.code) ?: "HTTP " + r.code)) to null
                }
                val message = json?.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                val content = message?.optString("content", "").orEmpty()
                if (content.isBlank()) return "No response returned by the model." to null
                val usage = json?.optJSONObject("usage")
                val usageText = usage?.let {
                    "Tokens: " + it.optInt("prompt_tokens", 0) + " in / " + it.optInt("completion_tokens", 0) + " out"
                }
                content to usageText
            }
        } catch (e: Exception) {
            "Connection error: " + (e.message ?: "unknown error") to null
        }
    }

    private fun append(who: String, text: String) {
        if (chat.text.isNotEmpty()) chat.append("\n\n")
        chat.append("$who\n$text")
        findViewById<ScrollView>(R.id.scroll).post {
            findViewById<ScrollView>(R.id.scroll).fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun setBusy(busy: Boolean) {
        send.isEnabled = !busy
        status.text = if (busy) "● Thinking…" else "● Ready"
    }

    private fun clearConversation() {
        while (history.length() > 0) history.remove(0)
        prefs.edit().remove("history").apply()
        chat.text = "Conversation cleared."
        status.text = "● Ready"
    }

    private fun saveHistory() {
        prefs.edit().putString("history", history.toString()).apply()
    }

    private fun restoreHistory() {
        val saved = prefs.getString("history", "").orEmpty()
        if (saved.isBlank()) return
        runCatching {
            val arr = JSONArray(saved)
            for (i in 0 until arr.length()) history.put(arr.getJSONObject(i))
            chat.text = ""
            for (i in 0 until history.length()) {
                val m = history.getJSONObject(i)
                append(if (m.optString("role") == "user") "You" else "Mazkiplay Agent", m.optString("content"))
            }
        }
    }

    private fun updateModelLabel() {
        val model = prefs.getString("model", defaultModel).orEmpty().ifBlank { defaultModel }
        modelLabel.text = "OpenRouter • " + model + " • local key"
    }

    private fun showSettings() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 8, 32, 0)
        }
        val key = EditText(this).apply {
            hint = "sk-or-v1-…"
            setText(prefs.getString("openrouter_key", ""))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSingleLine = true
        }
        val model = EditText(this).apply {
            hint = defaultModel
            setText(prefs.getString("model", defaultModel))
            isSingleLine = true
        }
        val tokens = EditText(this).apply {
            hint = "2048"
            setText(prefs.getString("max_tokens", "2048"))
            inputType = InputType.TYPE_CLASS_NUMBER
            isSingleLine = true
        }
        val prompt = EditText(this).apply {
            hint = "System prompt"
            setText(prefs.getString("system_prompt", "You are Mazkiplay Agent, a capable personal AI agent. Be concise but useful."))
            minLines = 3
            maxLines = 6
        }
        box.addView(TextView(this).apply { text = "OpenRouter API key (stored only on this device)" })
        box.addView(key)
        box.addView(TextView(this).apply { text = "Model ID"; setPadding(0, 16, 0, 0) })
        box.addView(model)
        box.addView(TextView(this).apply { text = "Max output tokens"; setPadding(0, 16, 0, 0) })
        box.addView(tokens)
        box.addView(TextView(this).apply { text = "System prompt"; setPadding(0, 16, 0, 0) })
        box.addView(prompt)

        AlertDialog.Builder(this)
            .setTitle("Agent Settings")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                prefs.edit()
                    .putString("openrouter_key", key.text.toString().trim())
                    .putString("model", model.text.toString().trim().ifBlank { defaultModel })
                    .putString("max_tokens", tokens.text.toString().trim().ifBlank { "2048" })
                    .putString("system_prompt", prompt.text.toString().trim())
                    .apply()
                updateModelLabel()
                status.text = "● Settings saved"
            }.show()
    }
}