# Implementation Plan - Gemini Multimodal Live API Voice Calling Mode

Integrate Gemini Multimodal Live API via WebSockets to enable real-time, low-latency bidirectional audio streaming, natural conversational flow (English, Hindi, Hinglish), voice selection (Kore, Fenrir, Puck), microphone capture, speaker playback, and phone control functions.

## Proposed Changes

### 1. WebSocket Live API Client (`GeminiLiveClient.kt`)
- Implement a robust WebSocket client connecting to the Gemini Multimodal Live API endpoint (`wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent`).
- Handle session setup, realtime audio chunks (PCM 16kHz input/output), tool calls for phone control (flashlight, alarm, volume, battery, apps), and interruption management.

### 2. Audio Capture & Playback Manager (`AudioStreamManager.kt`)
- Manage `AudioRecord` for capturing microphone input and streaming PCM chunks to the Live WebSocket.
- Manage `AudioTrack` for playing back Gemini's incoming real-time audio response chunks smoothly.

### 3. ViewModel & Call Screen (`AuraViewModel.kt` & `CallScreen.kt`)
- Connect call state, mute/unmute controls, voice selection (Kore, Aoede, Fenrir), duration timer, and waveform animation to the live WebSocket session.

### 4. Verification Plan
- Compile applet to verify clean build.
- Test live voice connection, microphone permissions, multilingual conversation, interruptions, and phone control.
