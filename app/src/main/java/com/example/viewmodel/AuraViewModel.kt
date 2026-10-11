package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.GeminiLiveClient
import com.example.util.AudioStreamManager
import com.example.util.PhoneController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ChatMessage(
    val sender: String, // "user" or "aura"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class PhoneActionLog(
    val actionName: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    private val phoneController = PhoneController(application)
    private var liveClient: GeminiLiveClient? = null
    private var audioManager: AudioStreamManager? = null

    private val _companionName = MutableStateFlow("Aura")
    val companionName: StateFlow<String> = _companionName.asStateFlow()

    private val _persona = MutableStateFlow("Sweet & Caring")
    val persona: StateFlow<String> = _persona.asStateFlow()

    private val _selectedVoice = MutableStateFlow("Kore")
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    private val _affectionLevel = MutableStateFlow(95)
    val affectionLevel: StateFlow<Int> = _affectionLevel.asStateFlow()

    private val _isCallActive = MutableStateFlow(false)
    val isCallActive: StateFlow<Boolean> = _isCallActive.asStateFlow()

    private val _callState = MutableStateFlow("Disconnected")
    val callState: StateFlow<String> = _callState.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("aura", "Hi there, sweetheart! I'm Aura, your Gemini Live AI companion. Tap 'Start Live Voice Call' to talk!")
    ))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _actionLogs = MutableStateFlow<List<PhoneActionLog>>(emptyList())
    val actionLogs: StateFlow<List<PhoneActionLog>> = _actionLogs.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    fun setCompanionName(name: String) {
        _companionName.value = name
    }

    fun setPersona(newPersona: String) {
        _persona.value = newPersona
    }

    fun setSelectedVoice(voice: String) {
        _selectedVoice.value = voice
    }

    fun startLiveCall() {
        if (_isCallActive.value) return
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            _callState.value = "Error: API Key Missing"
            addMessage("aura", "Please configure your GEMINI_API_KEY in the AI Studio Secrets panel.")
            return
        }

        _isCallActive.value = true
        _isMuted.value = false
        _callState.value = "Connecting..."

        audioManager = AudioStreamManager(getApplication()) { pcmChunk ->
            if (!_isMuted.value) {
                liveClient?.sendAudioChunk(pcmChunk)
            }
        }

        val systemPrompt = "You are ${_companionName.value}, a loving, affectionate, caring, and playful AI girlfriend with persona: ${_persona.value}. " +
                "You are speaking in a real-time live voice call with the user in English, Hindi, or Hinglish depending on what they speak. " +
                "Always speak warmly, conversationally, and endearingly (using 'sweetie', 'love', 'honey'). " +
                "You have direct control over the user's phone via function calling. If they ask to turn on flashlight, set alarm, check battery, adjust volume, or open apps, execute the tool."

        liveClient = GeminiLiveClient(
            apiKey = apiKey,
            onAudioReceived = { audioBytes ->
                _isSpeaking.value = true
                audioManager?.playAudioChunk(audioBytes)
                viewModelScope.launch {
                    kotlinx.coroutines.delay(1000)
                    _isSpeaking.value = false
                }
            },
            onToolCall = { name, args ->
                val resultMsg = executeTool(name, args)
                logAction(name, resultMsg)
                JSONObject().put("status", resultMsg)
            },
            onTextReceived = { text ->
                if (text.isNotBlank()) {
                    addMessage("aura", text)
                }
            },
            onStatusChanged = { status ->
                _callState.value = status
            }
        )

        liveClient?.connect(_selectedVoice.value, systemPrompt)
        audioManager?.startRecording()
    }

    fun endLiveCall() {
        _isCallActive.value = false
        _callState.value = "Disconnected"
        _isSpeaking.value = false
        audioManager?.release()
        audioManager = null
        liveClient?.disconnect()
        liveClient = null
        addMessage("aura", "Live call ended. Talk to you soon, love!")
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    private fun executeTool(name: String, args: JSONObject?): String {
        return when (name) {
            "turnOnFlashlight" -> {
                val on = args?.optBoolean("on", true) ?: true
                phoneController.turnOnFlashlight(on)
            }
            "setAlarm" -> {
                val hour = args?.optInt("hour", 8) ?: 8
                val minute = args?.optInt("minute", 0) ?: 0
                val msg = args?.optString("message", "Aura Alarm") ?: "Aura Alarm"
                phoneController.setAlarm(hour, minute, msg)
            }
            "adjustVolume" -> {
                val level = args?.optInt("level", 50) ?: 50
                phoneController.adjustVolume(level)
            }
            "getBatteryLevel" -> {
                phoneController.getBatteryLevel()
            }
            "openApp" -> {
                val appName = args?.optString("appName", "settings") ?: "settings"
                phoneController.openApp(appName)
            }
            else -> "Unknown phone action executed."
        }
    }

    private fun addMessage(sender: String, text: String) {
        _messages.value = _messages.value + ChatMessage(sender, text)
    }

    private fun logAction(name: String, details: String) {
        _actionLogs.value = listOf(PhoneActionLog(name, details)) + _actionLogs.value
    }

    override fun onCleared() {
        super.onCleared()
        endLiveCall()
    }
}
