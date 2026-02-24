from fastapi import APIRouter, UploadFile, File, Form
from pathlib import Path
import json
from fastapi.responses import FileResponse
import tempfile
import soundfile as sf
import os

from app.services.enhancement_service import enhance_audio

from app.crypto.key_exchange import derive_shared_secret, generate_keypair
from app.crypto.aes import derive_aes_key, encrypt_audio
from app.crypto.server_keys import load_or_create_server_key

from app.services.message_service import (
    save_message,
    list_messages,
    STORAGE_AUDIO,
    STORAGE_META,
    save_text_message
)

from app.utils.helpers import load_and_decrypt_audio
from app.services.stt_service import speech_to_text
from app.services.classify_service import classify_message

router = APIRouter(prefix="/api")

# 🔐 Persistent server key (CRITICAL)
SERVER_PRIVATE_KEY, SERVER_PUBLIC_KEY = load_or_create_server_key()


# -----------------------------
# Upload & Encrypt Voice
# -----------------------------
@router.post("/send-voice")
async def send_voice(
    sender: str = Form(...),
        receiver: str = Form(...),

    audio: UploadFile = File(...)
):
    # Read audio bytes
    audio_bytes = await audio.read()

    # 🔑 Simulated client keypair (demo-safe)
    client_private, client_public = generate_keypair()

    # 🔐 Shared secret
    shared_secret = derive_shared_secret(
        SERVER_PRIVATE_KEY, client_public
    )

    # 🔒 AES key
    aes_key = derive_aes_key(shared_secret)

    # 🔐 Encrypt audio
    nonce, ciphertext = encrypt_audio(aes_key, audio_bytes)

    # 📦 Store encrypted message


    metadata = save_message(
        sender=sender,
        receiver=receiver,
        nonce=nonce,
        ciphertext=ciphertext,
        aes_key=aes_key
    )


    return {
        "message": "Voice message stored securely",
        "metadata": metadata
    }



@router.post("/send-text")
async def send_text(
    sender: str = Form(...),
    receiver: str = Form(...),
    text: str = Form(...)
):
    metadata = save_text_message(
        sender=sender,
        receiver=receiver,
        text=text
    )

    return {
        "message": "Text message sent",
        "data": metadata
    }

# -----------------------------
# List Messages (Inbox)
# -----------------------------
@router.get("/messages")
def get_messages():
    return list_messages()

@router.post("/mark-read/{user}/{contact}")
def mark_read(user: str, contact: str):
    for meta_file in STORAGE_META.glob("*.json"):
        meta = json.loads(meta_file.read_text())

        if (
            meta.get("receiver") == user and
            meta.get("sender") == contact and
            not meta.get("is_read", False)
        ):
            meta["is_read"] = True
            meta_file.write_text(json.dumps(meta, indent=2))

    return {"status": "ok"}


# -----------------------------
# DEMO: Get Enhanced Audio
# -----------------------------
@router.get("/enhance/{message_id}")
def get_enhanced_audio(message_id: str):
    encrypted_path = STORAGE_AUDIO / f"{message_id}.bin"
    meta_path = STORAGE_META / f"{message_id}.json"

    if not encrypted_path.exists() or not meta_path.exists():
        return {"error": "Message not found"}

    # 🔓 Decrypt audio
    audio_bytes = load_and_decrypt_audio(
        encrypted_path,
        meta_path
    )

    # 🎧 Enhance audio (Objective 2)
    enhanced_audio = enhance_audio(audio_bytes)

    # Save enhanced audio to temp file
    enhanced_path = STORAGE_AUDIO / f"{message_id}_enhanced.wav"

    with open(enhanced_path, "wb") as f:
        f.write(enhanced_audio)

    return FileResponse(
        path=enhanced_path,
        media_type="audio/wav",
        filename=f"{message_id}_enhanced.wav"
    )


# -----------------------------
# Decrypt → Enhance → Transcribe → Classify (IDEMPOTENT)
# -----------------------------
@router.get("/transcribe/{message_id}")
def transcribe_message(message_id: str):
    encrypted_path = STORAGE_AUDIO / f"{message_id}.bin"
    meta_path = STORAGE_META / f"{message_id}.json"

    if not encrypted_path.exists() or not meta_path.exists():
        return {"error": "Message not found"}

    meta = json.loads(meta_path.read_text())

    # 🔒 If already processed → DO NOT recompute
    if meta.get("transcription") and meta.get("priority"):
        return {
            "message_id": message_id,
            "text": meta["transcription"],
            "priority": meta["priority"]
        }

    # 🔓 Decrypt
    audio_bytes = load_and_decrypt_audio(encrypted_path, meta_path)

    # 🎧 Enhance
    enhanced_audio = enhance_audio(audio_bytes)

    # 🧠 STT
    transcription = speech_to_text(enhanced_audio)

    # 🧠 Classify
    priority = classify_message(transcription)

    # 🔄 Persist
    meta["transcription"] = transcription
    meta["priority"] = priority

    meta_path.write_text(json.dumps(meta, indent=2))

    return {
        "message_id": message_id,
        "text": transcription,
        "priority": priority
    }

@router.get("/messages/{user}")
def get_messages_for_user(user: str):
    all_messages = list_messages()

    filtered = [
        m for m in all_messages
        if m.get("sender") == user or m.get("receiver") == user
    ]

    return filtered
