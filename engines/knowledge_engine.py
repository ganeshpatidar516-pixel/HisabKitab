import requests


def fetch_wikipedia(query):
    try:
        url = f"https://en.wikipedia.org/api/rest_v1/page/summary/{query}"
        response = requests.get(url)

        if response.status_code == 200:
            data = response.json()
            return data.get("extract")

    except Exception:
        pass

    return None


def handle_knowledge(intent, entities):

    # Basic business knowledge
    knowledge_base = {
        "gst": "GST भारत में वस्तुओं और सेवाओं पर लगाया जाने वाला टैक्स है।",
        "invoice": "इनवॉइस बिक्री का आधिकारिक बिल होता है जिसमें उत्पाद, कीमत और टैक्स की जानकारी होती है।",
        "profit": "प्रॉफिट वह राशि है जो कुल आय से सभी खर्च निकालने के बाद बचती है।",
        "marketing": "मार्केटिंग का मतलब अपने उत्पाद या सेवा को ग्राहकों तक पहुँचाने और बिक्री बढ़ाने की प्रक्रिया है।"
    }

    question = entities.get("question", "").lower()

    # 1️⃣ Local knowledge check
    for key in knowledge_base:
        if key in question:
            return {
                "source": "local",
                "answer": knowledge_base[key]
            }

    # 2️⃣ Wikipedia knowledge
    wiki_answer = fetch_wikipedia(question)
    if wiki_answer:
        return {
            "source": "wikipedia",
            "answer": wiki_answer
        }

    # 3️⃣ Fallback explanation (always give some knowledge)
    return {
        "source": "general",
        "answer": f"{question} व्यापार या ज्ञान से जुड़ा विषय हो सकता है। इसके बारे में सामान्य रूप से कहा जा सकता है कि यह व्यापार, वित्त, या बाजार से संबंधित जानकारी का हिस्सा है और इसे समझना व्यापार निर्णयों के लिए महत्वपूर्ण होता है।"
    }
