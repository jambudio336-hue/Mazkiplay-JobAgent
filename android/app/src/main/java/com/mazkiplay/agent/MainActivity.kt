package com.mazkiplay.agent

import android.content.Context
import android.os.Bundle
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
    private val prefs by lazy { getSharedPreferences("agent", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        chat = findViewById(R.id.chat)
        input = findViewById(R.id.input)
        send = findViewById(R.id.send)
        status = findViewById(R.id.status)

        findViewById<Button>(R.id.clear).setOnClickListener {
            while (history.length() > 0) history.remove(0)
            chat.text = "Conversation cleared."
        }
        findViewById<Button>(R.id.settings).setOnClickListener { showSettings() }
        send.setOnClickListener { sendMessage() }
        input.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
        }
    }

    private fun sendMessage() {
        val text = input.text.toString().trim()
        if (text.isEmpty() || !send.isEnabled) return
        input.setText("")
        append("You", text)
        history.put(JSONObject().put("role", "user").put("content", text))
        setBusy(true)
        lifecycleScope.launch {
            val answer = withContext(Dispatchers.IO) { callAgent() }
            append("Mazkiplay Agent", answer)
            history.put(JSONObject().put("role", "assistant").put("content", answer))
            setBusy(false)
        }
    }

    private fun callAgent(): String {
        return try {
            val body = JSONObject().put("messages", history).toString()
                .toRequestBody("application/json".toMediaType())
            val base = prefs.getString("api_url", BuildConfig.API_BASE_URL)?.trimEnd('/') ?: BuildConfig.API_BASE_URL
            val req = Request.Builder().url("$base/api/chat").post(body).build()
            client.newCall(req).execute().use { r ->
                val raw = r.body?.string().orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrNull()
                if (!r.isSuccessful) return "Gateway error: \${json?.optString("error", "HTTP \${r.code}") ?: "HTTP \${r.code}"}"
                json?.optJSONObject("message")?.optString("content", "No response.") ?: "No response."
            }
        } catch (e: Exception) {
            "Connection error: \${e.message ?: "unknown error"}"
        }
    }

    private fun append(who: String, text: String) {
        if (chat.text.isNotEmpty()) chat.append("\n\n")
        chat.append("$who\n$text")
    }

    private fun setBusy(busy: Boolean) {
        send.isEnabled = !busy
        status.text = if (busy) "● Thinking…" else "● Ready"
    }

    private fun showSettings() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 8, 32, 0)
        }
        val url = EditText(this).apply {
            hint = "https://your-agent-api.example"
            setText(prefs.getString("api_url", BuildConfig.API_BASE_URL))
            singleLine = true
        }
        box.addView(TextView(this).apply { text = "Backend API URL"; setPadding(0, 0, 0, 8) })
        box.addView(url)
        AlertDialog.Builder(this)
            .setTitle("Agent Settings")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                prefs.edit().putString("api_url", url.text.toString().trim().trimEnd('/')).apply()
                status.text = "● Endpoint saved"
            }.show()
    }
}