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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val client=OkHttpClient()
    private val history=JSONArray()
    private lateinit var chat:TextView
    private lateinit var input:EditText
    private lateinit var send:Button
    private lateinit var status:TextView
    private lateinit var modelLabel:TextView
    private lateinit var clock:TextView
    private lateinit var pageTitle:TextView
    private lateinit var dashboardPage:LinearLayout
    private lateinit var jobsPage:LinearLayout
    private lateinit var applyPage:LinearLayout
    private lateinit var aiPage:LinearLayout
    private lateinit var jobResults:TextView
    private val prefs by lazy{getSharedPreferences("agent",Context.MODE_PRIVATE)}
    private val openRouterUrl="https://openrouter.ai/api/v1/chat/completions"
    private val defaultModel="openai/gpt-5.6"

    override fun onCreate(b:Bundle?){
        super.onCreate(b);setContentView(R.layout.activity_main)
        chat=findViewById(R.id.chat);input=findViewById(R.id.input);send=findViewById(R.id.send)
        status=findViewById(R.id.status);modelLabel=findViewById(R.id.modelLabel);clock=findViewById(R.id.clock)
        pageTitle=findViewById(R.id.pageTitle);dashboardPage=findViewById(R.id.dashboardPage)
        jobsPage=findViewById(R.id.jobsPage);applyPage=findViewById(R.id.applyPage);aiPage=findViewById(R.id.aiPage)
        jobResults=findViewById(R.id.jobResults);restoreHistory();updateModelLabel();updateClock();setupActions();showPage("dashboard")
    }

    private fun setupActions(){
        findViewById<Button>(R.id.navHome).setOnClickListener{showPage("dashboard")}
        findViewById<Button>(R.id.navJobs).setOnClickListener{showPage("jobs")}
        findViewById<Button>(R.id.navApply).setOnClickListener{showPage("applications")}
        findViewById<Button>(R.id.navAi).setOnClickListener{showPage("ai")}
        findViewById<Button>(R.id.settings).setOnClickListener{showSettings()}
        findViewById<Button>(R.id.startHunt).setOnClickListener{showPage("jobs")}
        findViewById<Button>(R.id.buildCv).setOnClickListener{usePrompt("Buat CV ATS profesional berdasarkan profil saya. Minta data yang masih kurang lalu susun CV satu halaman: ")}
        findViewById<Button>(R.id.writeLetter).setOnClickListener{usePrompt("Buat surat lamaran kerja profesional dan personal untuk lowongan berikut. Sertakan subjek email dan versi singkat: ")}
        findViewById<Button>(R.id.searchJobs).setOnClickListener{searchJobs()}
        findViewById<Button>(R.id.clear).setOnClickListener{clearConversation()}
        findViewById<Button>(R.id.quickPlan).setOnClickListener{usePrompt("Buat rencana langkah demi langkah untuk tujuan berikut: ")}
        findViewById<Button>(R.id.quickJob).setOnClickListener{usePrompt("Bantu saya membuat CV dan strategi melamar pekerjaan untuk posisi berikut: ")}
        findViewById<Button>(R.id.quickIdeas).setOnClickListener{usePrompt("Berikan 10 ide produk atau bisnis realistis untuk masalah berikut: ")}
        findViewById<Button>(R.id.quickTranslate).setOnClickListener{usePrompt("Terjemahkan teks berikut secara natural ke bahasa Inggris: ")}
        send.setOnClickListener{sendMessage()}
        input.setOnEditorActionListener{_,a,_->if(a==EditorInfo.IME_ACTION_SEND){sendMessage();true}else false}
    }

    private fun showPage(p:String){
        dashboardPage.visibility=if(p=="dashboard")LinearLayout.VISIBLE else LinearLayout.GONE
        jobsPage.visibility=if(p=="jobs")LinearLayout.VISIBLE else LinearLayout.GONE
        applyPage.visibility=if(p=="applications")LinearLayout.VISIBLE else LinearLayout.GONE
        aiPage.visibility=if(p=="ai")LinearLayout.VISIBLE else LinearLayout.GONE
        pageTitle.text=when(p){"jobs"->"Job Hunting";"applications"->"Application Center";"ai"->"AI Agent";else->"Dashboard"}
    }

    private fun updateClock(){
        clock.text=SimpleDateFormat("EEEE, dd MMMM yyyy • HH:mm:ss",Locale.getDefault()).format(Date())
        clock.postDelayed({updateClock()},1000)
    }

    private fun usePrompt(s:String){showPage("ai");input.setText(s);input.setSelection(input.text.length);input.requestFocus()}

    private fun searchJobs(){
        val q=findViewById<EditText>(R.id.jobQuery).text.toString().trim()
        val loc=findViewById<EditText>(R.id.jobLocation).text.toString().trim()
        val progress=findViewById<ProgressBar>(R.id.jobProgress)
        progress.visibility=ProgressBar.VISIBLE;jobResults.text="Searching live public job feeds…"
        lifecycleScope.launch{
            val result=withContext(Dispatchers.IO){fetchJobs(q,loc)}
            progress.visibility=ProgressBar.GONE;jobResults.text=result
        }
    }

    private fun fetchJobs(q:String,loc:String):String{
        val raw=try{
            client.newCall(Request.Builder().url("https://www.arbeitnow.com/api/job-board-api").get().build()).execute().use{it.body?.string().orEmpty()}
        }catch(e:Exception){return "Live search unavailable: "+(e.message?:"connection error")}
        val out=StringBuilder("LIVE JOB FEED • "+SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(Date())+"\n\n")
        var n=0
        runCatching{
            val data=JSONObject(raw).optJSONArray("data")?:JSONArray()
            for(i in 0 until data.length()){
                val j=data.getJSONObject(i);val title=j.optString("title");val company=j.optString("company_name")
                val location=j.optString("location");val hay=(title+" "+company+" "+location+" "+j.optString("description")).lowercase()
                if(q.isNotBlank()&&!hay.contains(q.lowercase()))continue
                if(loc.isNotBlank()&&!hay.contains(loc.lowercase()))continue
                n++
                if(n<=20){
                    out.append(n).append(". ").append(title).append("\n   ").append(company).append(" • ").append(location).append("\n")
                    if(j.optBoolean("remote"))out.append("   REMOTE • ")
                    out.append(j.optString("job_types")).append("\n   APPLY: ").append(j.optString("url")).append("\n\n")
                }
            }
        }.onFailure{return "Could not parse live job feed: "+(it.message?:"invalid response")}
        return if(n==0)"No matching listings found. Try a broader keyword/location." else out.toString()
    }

    private fun sendMessage(){
        val text=input.text.toString().trim();if(text.isEmpty()||!send.isEnabled)return
        val key=prefs.getString("openrouter_key","").orEmpty()
        if(key.isBlank()){showSettings();status.text="● Add OpenRouter API key first";return}
        input.setText("");append("You",text);history.put(JSONObject().put("role","user").put("content",text));saveHistory();setBusy(true)
        lifecycleScope.launch{
            val result=withContext(Dispatchers.IO){callOpenRouter(key)}
            append("Mazkiplay Agent",result.first);history.put(JSONObject().put("role","assistant").put("content",result.first));saveHistory()
            status.text=if(result.second!=null)"● "+result.second else "● ONLINE";setBusy(false)
        }
    }

    private fun callOpenRouter(key:String):Pair<String,String?>=try{
        val model=prefs.getString("model",defaultModel).orEmpty().ifBlank{defaultModel}
        val maxTokens=prefs.getString("max_tokens","2048")?.toIntOrNull()?.coerceIn(256,8192)?:2048
        val system=prefs.getString("system_prompt","You are Mazkiplay Agent, an AI job-hunting and productivity agent. Help users discover jobs, tailor CVs, write cover letters and prepare applications. Never claim an application was sent unless the device actually completed an external action.").orEmpty()
        val messages=JSONArray().put(JSONObject().put("role","system").put("content",system))
        for(i in maxOf(0,history.length()-40) until history.length())messages.put(history.getJSONObject(i))
        val body=JSONObject().put("model",model).put("messages",messages).put("temperature",0.2).put("max_tokens",maxTokens).toString().toRequestBody("application/json".toMediaType())
        val req=Request.Builder().url(openRouterUrl).addHeader("Authorization","Bearer "+key).addHeader("Content-Type","application/json").addHeader("HTTP-Referer","https://mazkiplay.ai").addHeader("X-Title","Mazkiplay Agent").post(body).build()
        client.newCall(req).execute().use{r->
            val raw=r.body?.string().orEmpty();val json=runCatching{JSONObject(raw)}.getOrNull()
            if(!r.isSuccessful)return ("OpenRouter error: "+(json?.optJSONObject("error")?.optString("message","HTTP "+r.code)?:"HTTP "+r.code)) to null
            val content=json?.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content","").orEmpty()
            if(content.isBlank())return "No response returned by the model." to null
            val u=json?.optJSONObject("usage")
            content to u?.let{"Tokens: "+it.optInt("prompt_tokens")+" in / "+it.optInt("completion_tokens")+" out"}
        }
    }catch(e:Exception){"Connection error: "+(e.message?:"unknown error") to null}

    private fun append(who:String,text:String){
        if(chat.text.isNotEmpty())chat.append("\n\n");chat.append(who+"\n"+text)
        findViewById<ScrollView>(R.id.scroll).post{findViewById<ScrollView>(R.id.scroll).fullScroll(ScrollView.FOCUS_DOWN)}
    }
    private fun setBusy(b:Boolean){send.isEnabled=!b;status.text=if(b)"● AI THINKING…" else "● ONLINE"}
    private fun clearConversation(){while(history.length()>0)history.remove(0);prefs.edit().remove("history").apply();chat.text="Conversation cleared.";status.text="● ONLINE"}
    private fun saveHistory(){prefs.edit().putString("history",history.toString()).apply()}
    private fun restoreHistory(){
        val s=prefs.getString("history","").orEmpty();if(s.isBlank())return
        runCatching{val a=JSONArray(s);for(i in 0 until a.length())history.put(a.getJSONObject(i));chat.text="";for(i in 0 until history.length()){val m=history.getJSONObject(i);append(if(m.optString("role")=="user")"You" else "Mazkiplay Agent",m.optString("content"))}}
    }
    private fun updateModelLabel(){modelLabel.text="OpenRouter • "+prefs.getString("model",defaultModel).orEmpty().ifBlank{defaultModel}+" • local key"}

    private fun showSettings(){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(32,8,32,0)}
        val key=EditText(this).apply{hint="sk-or-v1-…";setText(prefs.getString("openrouter_key",""));inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD;isSingleLine=true}
        val model=EditText(this).apply{hint=defaultModel;setText(prefs.getString("model",defaultModel));isSingleLine=true}
        val tokens=EditText(this).apply{hint="2048";setText(prefs.getString("max_tokens","2048"));inputType=InputType.TYPE_CLASS_NUMBER;isSingleLine=true}
        val prompt=EditText(this).apply{hint="System prompt";setText(prefs.getString("system_prompt","You are Mazkiplay Agent, an AI job-hunting and productivity agent."));minLines=3;maxLines=6}
        box.addView(TextView(this).apply{text="OpenRouter API key (stored on this device)"});box.addView(key)
        box.addView(TextView(this).apply{text="Model ID";setPadding(0,16,0,0)});box.addView(model)
        box.addView(TextView(this).apply{text="Max output tokens";setPadding(0,16,0,0)});box.addView(tokens)
        box.addView(TextView(this).apply{text="System prompt";setPadding(0,16,0,0)});box.addView(prompt)
        AlertDialog.Builder(this).setTitle("Agent Settings").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->prefs.edit().putString("openrouter_key",key.text.toString().trim()).putString("model",model.text.toString().trim().ifBlank{defaultModel}).putString("max_tokens",tokens.text.toString().trim().ifBlank{"2048"}).putString("system_prompt",prompt.text.toString().trim()).apply();updateModelLabel();status.text="● Settings saved"}.show()
    }
}