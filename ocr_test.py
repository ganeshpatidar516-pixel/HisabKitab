import pytesseract
from PIL import Image

# image path
image_path = "test.jpg"

# open image
img = Image.open(image_path)

# OCR
text = pytesseract.image_to_string(img)

print("Detected Text:")
print(text)
