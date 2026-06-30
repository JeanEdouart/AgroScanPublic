# AgroScan model service

Microservice HTTP Flask utilise par le backend Spring pour analyser les images de feuilles.

## Demarrage local

```powershell
cd model-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
$env:MODEL_DIR="C:\chemin\vers\vos\modeles"
python wsgi.py
```

Le service ecoute sur `http://localhost:5001`.

## Endpoints

- `GET /health` verifie que le service repond.
- `POST /analyze` attend `imageBase64` et `imageMediaType`, puis renvoie:

```json
{
  "plant": "Apple",
  "disease": "Apple___Black_rot",
  "healthy": false,
  "confidence": 0.94,
  "plantConfidence": 0.97,
  "diseaseConfidence": 0.94
}
```

## Configuration backend

Le backend Spring appelle `MODEL_API_URL`, avec `http://localhost:5001` par defaut.
