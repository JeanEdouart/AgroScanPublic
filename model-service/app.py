import base64
import binascii
import io
import os
from pathlib import Path

from flask import Flask, jsonify, request
from PIL import Image, UnidentifiedImageError
import torch
import torch.nn as nn
from torchvision import models, transforms


CLASSES_ESPECES = [
    "Apple", "Blueberry", "Cherry", "Corn", "Grape",
    "Orange", "Peach", "Pepper", "Potato", "Raspberry",
    "Soybean", "Squash", "Strawberry", "Tomato",
]

CLASSES_MALADIES = {
    "Apple": ["Apple__Apple_scab", "Apple___Black_rot", "Apple___Cedar_apple_rust", "Apple___healthy"],
    "Blueberry": ["Blueberry___healthy"],
    "Cherry": ["Cherry___Powdery_mildew", "Cherry___healthy"],
    "Corn": ["Corn___Cercospora_leaf_spot Gray_leaf_spot", "Corn___Common_rust_", "Corn___Northern_Leaf_Blight", "Corn___healthy"],
    "Grape": ["Grape___Black_rot", "Grape___Esca_(Black_Measles)", "Grape___Leaf_blight_(Isariopsis_Leaf_Spot)", "Grape___healthy"],
    "Orange": ["Orange___Haunglongbing_(Citrus_greening)"],
    "Peach": ["Peach___Bacterial_spot", "Peach___healthy"],
    "Pepper": ["Pepper,_bell___Bacterial_spot", "Pepper,_bell___healthy"],
    "Potato": ["Potato___Early_blight", "Potato___Late_blight", "Potato___healthy"],
    "Raspberry": ["Raspberry___healthy"],
    "Soybean": ["Soybean___healthy"],
    "Squash": ["Squash___Powdery_mildew", "Squash___healthy"],
    "Strawberry": ["Strawberry___Leaf_scorch", "Strawberry___healthy"],
    "Tomato": [
        "Tomato___Bacterial_spot", "Tomato___Early_blight", "Tomato___Late_blight",
        "Tomato___Leaf_Mold", "Tomato___Septoria_leaf_spot",
        "Tomato___Spider_mites Two-spotted_spider_mite", "Tomato___Target_Spot",
        "Tomato___Tomato_Yellow_Leaf_Curl_Virus", "Tomato___Tomato_mosaic_virus",
        "Tomato___healthy",
    ],
}

MODEL_A_PATH = "modele_A_plantes_2.0.pth"
MODEL_B_PATHS = {
    "Apple": "modele_B_apple.pth",
    "Blueberry": "modele_B_blueberry.pth",
    "Cherry": "modele_B_cherry.pth",
    "Corn": "modele_B_corn.pth",
    "Grape": "modele_B_grape.pth",
    "Orange": "modele_B_orange.pth",
    "Peach": "modele_B_peach.pth",
    "Pepper": "modele_B_bellpepper.pth",
    "Potato": "modele_B_potato.pth",
    "Raspberry": "modele_B_raspberry.pth",
    "Soybean": "modele_B_soybean.pth",
    "Squash": "modele_B_squash.pth",
    "Strawberry": "modele_B_strawberry.pth",
    "Tomato": "modele_B_tomato.pth",
}

ALLOWED_MEDIA_TYPES = {"image/jpeg", "image/png", "image/webp"}
MAX_IMAGE_BYTES = 5 * 1024 * 1024

app = Flask(__name__)
model_dir = Path(os.environ.get("MODEL_DIR", "models"))
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
loaded_models = {}

image_transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize([0.485, 0.456, 0.406], [0.229, 0.224, 0.225]),
])


def load_model(model_path, num_classes):
    resolved_path = model_dir / model_path
    if not resolved_path.exists():
        raise FileNotFoundError(f"Model file not found: {resolved_path}")
    model = models.mobilenet_v2(weights=None)
    in_features = model.classifier[1].in_features
    model.classifier[1] = nn.Linear(in_features, num_classes)
    model.load_state_dict(torch.load(resolved_path, map_location=device, weights_only=True))
    model = model.to(device)
    model.eval()
    return model


def get_model(cache_key, model_path, num_classes):
    if cache_key not in loaded_models:
        loaded_models[cache_key] = load_model(model_path, num_classes)
    return loaded_models[cache_key]


def prepare_image(image_base64, image_media_type):
    if image_media_type not in ALLOWED_MEDIA_TYPES:
        raise ValueError("Unsupported image media type")
    try:
        image_bytes = base64.b64decode(image_base64, validate=True)
    except (binascii.Error, ValueError) as exception:
        raise ValueError("Invalid base64 image") from exception
    if len(image_bytes) > MAX_IMAGE_BYTES:
        raise ValueError("Image is too large")
    try:
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    except UnidentifiedImageError as exception:
        raise ValueError("Invalid image") from exception
    return image_transform(image).unsqueeze(0).to(device)


def predict_class(model, image_tensor, classes):
    with torch.no_grad():
        logits = model(image_tensor)
        probabilities = torch.softmax(logits, dim=1)
        confidence, index = torch.max(probabilities, dim=1)
    return classes[index.item()], confidence.item()


@app.get("/health")
def health():
    return jsonify({
        "status": "ok",
        "device": str(device),
    })


@app.post("/analyze")
def analyze():
    payload = request.get_json(silent=True) or {}
    image_base64 = payload.get("imageBase64")
    image_media_type = payload.get("imageMediaType")
    if not image_base64 or not image_media_type:
        return jsonify({"message": "imageBase64 and imageMediaType are required"}), 400

    try:
        image_tensor = prepare_image(image_base64, image_media_type)
        plant_model = get_model("plant", MODEL_A_PATH, len(CLASSES_ESPECES))
        plant, plant_confidence = predict_class(plant_model, image_tensor, CLASSES_ESPECES)

        disease_classes = CLASSES_MALADIES[plant]
        disease_model = get_model(f"disease:{plant}", MODEL_B_PATHS[plant], len(disease_classes))
        disease, disease_confidence = predict_class(disease_model, image_tensor, disease_classes)
    except ValueError as exception:
        return jsonify({"message": str(exception)}), 400
    except Exception as exception:
        app.logger.exception("Analysis failed")
        return jsonify({"message": "Analysis failed"}), 500

    confidence = min(plant_confidence, disease_confidence)
    return jsonify({
        "plant": plant,
        "disease": disease,
        "healthy": disease.endswith("___healthy"),
        "confidence": confidence,
        "plantConfidence": plant_confidence,
        "diseaseConfidence": disease_confidence,
    })


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", "5001")))
