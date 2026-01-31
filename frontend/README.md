

# 📱 Secure AI-Based Voice Communication — Android Frontend

## 📌 Overview

This Android application is the **frontend client** for the project:

> **Secure AI-Based Voice Communication with Threat Detection and Priority Classification**

The app demonstrates **secure voice and text messaging** between two users using a **WhatsApp-like UI**, where:

* Voice messages are recorded on-device
* Audio is securely transmitted to a backend server
* Encryption and processing happen **only on the backend**
* No plaintext audio is stored on the device or server

This frontend is designed for **academic demonstration**, not production deployment.

---

## 🎯 Features Implemented (Frontend)

### ✅ Phase 1 — User Simulation

* Simulated login as predefined users (User A, User B, etc.)
* Simple user selection screen for demo purposes

### ✅ Phase 2 — Chat Interface

* WhatsApp-style chat UI
* Separate sender and receiver message alignment
* Text message sending and viewing

### ✅ Phase 3 — Secure Voice Recording

* Runtime microphone permission handling
* Audio recording using `AudioRecord`
* WAV file generation on device
* Safe handling of mic availability and lifecycle

### ✅ Phase 4 — Secure Voice Upload

* Recorded voice is sent to backend via multipart request
* Backend performs:

  * Encryption (AES key derived via X25519)
  * Secure storage
* Frontend never encrypts or stores sensitive keys

---

## 🏗️ Architecture (Frontend Perspective)

```
Android App
│
├── User Selection Screen
│
├── Chat List
│
├── Chat Activity
│   ├── Text Message → Backend
│   ├── Voice Record → Backend (/send-voice)
│
└── Backend Server
    ├── Encryption
    ├── Storage
    ├── Enhancement (future)
```

---

## 🛠️ Tech Stack

| Component       | Technology                |
| --------------- | ------------------------- |
| Language        | Kotlin                    |
| UI              | XML layouts               |
| Audio Recording | AudioRecord               |
| Networking      | HttpURLConnection         |
| Concurrency     | Kotlin Coroutines         |
| Target Platform | Android Emulator / Device |

---

## 📂 Project Structure (Frontend)

```
SecureVoiceChat/
│
├── app/
│   ├── src/main/java/com/example/securevoice/
│   │   ├── MainActivity.kt
│   │   ├── UserSelectActivity.kt
│   │   ├── ChatListActivity.kt
│   │   └── ChatActivity.kt
│   │
│   ├── src/main/res/layout/
│   │   ├── activity_chat.xml
│   │   ├── activity_chat_list.xml
│   │   └── activity_user_select.xml
│
├── build.gradle
├── settings.gradle
└── README.md
```

---

## ⚙️ Setup Instructions (Client / Evaluator)

### 🔹 Prerequisites

* **Android Studio** (Giraffe / Hedgehog or newer)
* **Android SDK 24+**
* Android Emulator (recommended)
  OR
* Physical Android device (USB debugging enabled)

---

### 🔹 Step 1 — Open Project

1. Open Android Studio
2. Select **Open**
3. Choose the `SecureVoiceChat` folder
4. Wait for Gradle sync to complete

---

### 🔹 Step 2 — Backend Requirement (IMPORTANT)

This frontend **requires the backend server to be running**.

Backend must be running at:

```
http://10.0.2.2:8000
```

For emulator:

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

> ⚠️ `10.0.2.2` is mandatory for Android Emulator
> Do NOT use `localhost` or `127.0.0.1`

---

### 🔹 Step 3 — Run the App

1. Start Android Emulator
2. Click **Run ▶**
3. Select emulator
4. App launches

---

## 🎤 Voice Recording Permissions

On first voice recording:

* Android will ask for **Microphone permission**
* Tap **Allow**

If permission is denied:

* Voice recording will not start
* App will not crash (safe handling implemented)

---

## 🧪 Demo Flow (For Evaluation)

### 🔹 Text Message Demo

1. Select **User A**
2. Choose **User B**
3. Send a text message
4. Switch to **User B**
5. Message appears in chat

---

### 🔹 Voice Message Demo

1. Open chat
2. Tap 🎤 to start recording
3. Speak
4. Tap 🎤 again to stop
5. Voice is uploaded securely to backend
6. Backend encrypts and stores the message

> 🎯 This demonstrates **secure voice transmission**

---

## 🔐 Security Notes (Important)

* No encryption keys are stored on the device
* Audio is never stored in plaintext on backend
* Encryption and decryption happen **only on server**
* Frontend acts as a **secure client**, not a cryptographic authority

---

## 🚧 Limitations (Academic Scope)

* User authentication is simulated
* No real-time calling (message-based only)
* Voice playback & enhancement UI planned for next phase
* Models are backend-only (no on-device ML)

---

## 🔮 Future Enhancements

* Enhanced voice playback
* Threat / urgency visualization
* On-device inference (optional)
* Real user authentication
* Push notifications

---

## 👩‍💻 Developed For

Academic demonstration of:

* Secure communication
* Cryptographic integration
* Android + backend coordination
* AI-assisted voice systems

---

## 📞 Support / Notes

If backend is not running or unreachable:

* Voice and text sending will fail gracefully
* Ensure backend server is active before demo

---

