package com.example.data

import android.util.Base64
import android.util.Log
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveClient(
    private val apiKey: String,
    private val onAudioReceived: (ByteArray) -> Unit,
    private val onToolCall: (String, JSONObject?) -> JSONObject?,
    private val onTextReceived: (String) -> Unit,
    private val onStatusChanged: (String) -> Unit
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    fun connect(voiceName: String = "Kore", systemInstruction: String = "You are Aura, a loving AI girlfriend.") {
        onStatusChanged("Connecting Live API...")
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                onStatusChanged("Connected")
                sendSetup(voiceName, systemInstruction)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    handleServerMessage(json)
                } catch (e: Exception) {
                    Log.e("GeminiLiveClient", "Message parse error", e)
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                onStatusChanged("Disconnected: $reason")
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("GeminiLiveClient", "WebSocket failure", t)
                onStatusChanged("Connection Error: ${t.localizedMessage}")
            }
        })
    }

    private fun sendSetup(voiceName: String, systemInstruction: String) {
        val setupJson = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", "models/gemini-2.5-flash-native-audio-preview-12-2025")
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().put("AUDIO"))
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", voiceName)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                })
            })
        }
        webSocket?.send(setupJson.toString())
    }

    fun sendAudioChunk(pcmBytes: ByteArray) {
        try {
            val base64Audio = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
            val msg = JSONObject().apply {
                put("realtimeInput", JSONObject().apply {
                    put("mediaChunks", JSONArray().put(JSONObject().apply {
                        put("mimeType", "audio/pcm")
                        put("data", base64Audio)
                    }))
                })
            }
            webSocket?.send(msg.toString())
        } catch (e: Exception) {
            Log.e("GeminiLiveClient", "Send audio error", e)
        }
    }

    private fun handleServerMessage(json: JSONObject) {
        if (json.has("serverContent")) {
            val serverContent = json.getJSONObject("serverContent")
            if (serverContent.has("modelTurn")) {
                val modelTurn = serverContent.getJSONObject("modelTurn")
                val parts = modelTurn.optJSONArray("parts")
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (part.has("inlineData")) {
                            val inlineData = part.getJSONObject("inlineData")
                            val base64Data = inlineData.optString("data")
                            if (base64Data.isNotEmpty()) {
                                val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                onAudioReceived(audioBytes)
                            }
                        }
                        if (part.has("text")) {
                            onTextReceived(part.getString("text"))
                        }
                    }
                }
            }
        }
        if (json.has("toolCall")) {
            val toolCall = json.getJSONObject("toolCall")
            val functionCalls = toolCall.optJSONArray("functionCalls")
            if (functionCalls != null) {
                for (i in 0 until functionCalls.length()) {
                    val fc = functionCalls.getJSONObject(i)
                    val callId = fc.optString("id")
                    val name = fc.optString("name")
                    val args = fc.optJSONObject("args")
                    val result = onToolCall(name, args)
                    sendToolResponse(callId, name, result)
                }
            }
        }
    }

    private fun sendToolResponse(callId: String, name: String, result: JSONObject?) {
        val respJson = JSONObject().apply {
            put("toolResponse", JSONObject().apply {
                put("functionResponses", JSONArray().put(JSONObject().apply {
                    put("response", JSONObject().put("result", result ?: JSONObject().put("status", "success")))
                    put("id", callId)
                    put("name", name)
                }))
            })
        }
        webSocket?.send(respJson.toString())
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "Call ended by user")
            webSocket = null
        } catch (e: Exception) {
            Log.e("GeminiLiveClient", "Disconnect error", e)
        }
    }
}
