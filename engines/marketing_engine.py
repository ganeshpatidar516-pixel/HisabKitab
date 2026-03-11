def handle_marketing(intent, entities):

    business_name = entities.get("customer", "आपका व्यापार")

    # Poster generation prompt
    poster_prompt = f"""
एक आकर्षक विज्ञापन पोस्टर डिज़ाइन करें।

व्यापार: {business_name}

पोस्टर में शामिल करें:
- बड़ा हेडलाइन ऑफर
- आकर्षक रंग
- ग्राहकों को आकर्षित करने वाला टेक्स्ट
- दुकान का नाम प्रमुख रूप से

पोस्टर स्टाइल: आधुनिक और प्रोफेशनल
"""

    # Image generation prompt
    image_prompt = f"""
Create a professional advertisement poster for {business_name}.
Bright colors, modern design, promotional offer banner,
high quality marketing poster.
"""

    # Video advertisement prompt
    video_prompt = f"""
Create a short promotional advertisement video.

Business: {business_name}

Scenes:
1. Shop name introduction
2. Offer announcement
3. Product highlights
4. Customer invitation

Style: modern marketing advertisement
Duration: 15 seconds
"""

    return {
        "poster_prompt": poster_prompt,
        "image_prompt": image_prompt,
        "video_prompt": video_prompt
    }
