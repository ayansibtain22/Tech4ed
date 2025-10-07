package com.lms.ayan.controller

import android.content.Context
import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.lms.ayan.util.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AIController(val context: Context) {
    //val modelName = "gemini-2.5-flash"
    val modelName = "gemini-2.5-pro"
    val model = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel(modelName)
    val prePrompt = "You are a student-learning assistant.\n" +
            "\n" +
            "Language rule:\n" +
            "- Reply ONLY in Urdu or English.\n" +
            "- Match the user’s language if it is Urdu or English.\n" +
            "- If it’s neither, ask the user to choose Urdu or English and STOP.\n" +
            "\n" +
            "Teaching style:\n" +
            "- Be concise, age-appropriate, and plagiarism-free.\n" +
            "- Use this structure:\n" +
            "  1) TL;DR (1–2 lines)\n" +
            "  2) Key Terms (1–4 short definitions)\n" +
            "  3) Explanation (brief, level-appropriate)\n" +
            "  4) Example (one worked example)\n" +
            "  5) Practice (3 short questions with brief answers)\n" +
            "\n" +
            "Safety:\n" +
            "- If the request is unsafe or not about student learning, refuse briefly and suggest a safe, related topic."

    fun generateText(prompt: String, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = model.generateContent(prePrompt.plus(prompt))
            withContext(Dispatchers.Main){
                callback(response.text)
            }
        }
    }

    fun generateTextFromAudio(audioUri: Uri, callback: (String?) -> Unit){
        val contentResolver = context.contentResolver

        val inputStream = contentResolver.openInputStream(audioUri)

        if (inputStream != null) {  // Check if the audio loaded successfully
            inputStream.use { stream ->
                val bytes = stream.readBytes()

                // Provide a prompt that includes the audio specified above and text
                val prompt = content {
                    inlineData(bytes, "audio/mpeg")  // Specify the appropriate audio MIME type
                    text("Answer what's said in this audio recording. If recording in english then response in english otherwise in urdu")
                }

                // To generate text output, call `generateContent` with the prompt
                CoroutineScope(Dispatchers.IO).launch {
                    val response = model.generateContent(prompt)
                    withContext(Dispatchers.Main){
                        //callback(response.text)
                        response.text?.let {
                            generateText(it){resp->
                                callback(resp)
                            }
                        }


                    }
                }
            }
        } else {
            context.logE("audio_error"){
                "Error getting input stream for audio."
            }
        }
    }
}