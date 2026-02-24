from transformers import pipeline

# Lightweight BERT-based urgency classifier
urgency_classifier = pipeline(
    "text-classification",
    model="distilbert-base-uncased-finetuned-sst-2-english"
)

URGENT_KEYWORDS = {
    "urgent", "immediately", "asap", "help", "emergency", "now"
}

def detect_urgency(text: str) -> bool:
    text_lower = text.lower()

    # Rule 1 — Strong keyword match (primary trigger)
    for word in URGENT_KEYWORDS:
        if word in text_lower:
            return True

    # Rule 2 — Sentiment only strengthens urgency, not create it
    result = urgency_classifier(text)[0]

    if (
        result["label"] == "NEGATIVE"
        and result["score"] > 0.9
        and any(w in text_lower for w in ["help", "emergency"])
    ):
        return True

    return False