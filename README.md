# AgroScan

AgroScan est une application web de gestion et d'analyse d'images de feuilles. Un utilisateur peut importer une image, conserver son historique de scans, puis lancer une analyse automatique pour identifier la plante et detecter une eventuelle maladie.

## Fonctionnalites

- Authentification par JWT avec jetons de rafraichissement.
- Gestion du profil utilisateur.
- Administration des utilisateurs.
- Import d'images JPEG, PNG ou WebP jusqu'a 5 Mo.
- Stockage des scans et miniatures en base PostgreSQL.
- Recherche et pagination de l'historique des scans.
- Analyse d'un scan via un service Python PyTorch.
- Mise en cache du resultat d'analyse en base pour eviter de rappeler le modele inutilement.

## Architecture

Le projet est compose de trois services applicatifs :

- `frontend/` : application Angular 20.
- `backend/` : API Java 25 avec Spring Boot 4, Spring Security, JPA et Flyway.
- `model-service/` : microservice Flask/PyTorch charge d'executer les modeles `.pth`.

Le navigateur communique uniquement avec le backend Spring. Le backend appelle ensuite le service modele via HTTP lorsque l'utilisateur lance une analyse.

```text
Angular -> Spring Boot API -> Flask/PyTorch model service
                    |
                    v
                PostgreSQL
```

## Prerequis

- Java 25
- Maven ou le wrapper Maven fourni dans `backend/`
- Node.js et npm
- Python 3.12 ou compatible
- Docker Desktop pour PostgreSQL et les tests Testcontainers
- Les modeles PyTorch disponibles localement, hors du depot Git

## Demarrage local

Avant de lancer les services, copiez le fichier d'exemple puis remplissez les valeurs locales :

```powershell
Copy-Item .env.example .env
```

Le fichier `.env` contient les mots de passe, le secret JWT, le compte admin local et le chemin des modeles. Il est ignore par Git et ne doit jamais etre publie.

### 1. Base de donnees

Depuis la racine du projet :

```powershell
docker compose up -d
```

PostgreSQL ecoute sur `localhost:5432` avec les identifiants definis dans `.env`.

### 2. Service d'analyse

Depuis la racine du projet :

```powershell
cd model-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
$env:MODEL_DIR="C:\chemin\vers\vos\modeles"
python .\wsgi.py
```

Le service ecoute sur `http://localhost:5001`. Il doit rester ouvert dans son terminal pendant que l'application tourne.

### 3. Backend

Dans un second terminal :

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Le backend ecoute sur `http://localhost:8080`. Au lancement local, Spring importe automatiquement le fichier `.env` place a la racine du projet.

### 4. Frontend

Dans un troisieme terminal :

```powershell
cd frontend
npm install
npm start
```

Le frontend ecoute sur `http://localhost:4200`. Le proxy Angular transfere les appels `/api` vers le backend.

## Authentification

Au premier demarrage, le backend cree le compte administrateur defini par `ADMIN_EMAIL` et `ADMIN_PASSWORD`. Ces variables sont obligatoires afin d'eviter qu'un compte par defaut soit publie ou deploye par erreur.

## Analyse des scans

L'analyse se lance depuis l'interface sur un scan importe. Le frontend appelle :

```http
POST /api/scans/{id}/analysis
```

Le backend recupere l'image stockee en base, l'envoie au service Flask, sauvegarde le resultat dans la table `scans`, puis renvoie le diagnostic au frontend. Si un resultat existe deja pour ce scan, le backend le renvoie directement sans rappeler le modele.

Le resultat contient notamment :

- la plante identifiee ;
- la maladie ou l'etat sain ;
- un indicateur `healthy` ;
- un score de confiance ;
- la reponse JSON brute du service modele.

## Configuration

Les principales variables d'environnement sont :

| Variable | Defaut | Description |
| --- | --- | --- |
| `DB_HOST` | `localhost` | Hote PostgreSQL |
| `DB_PORT` | `5432` | Port PostgreSQL |
| `DB_NAME` | `agroscan` | Nom de la base |
| `DB_USER` | aucun | Utilisateur PostgreSQL |
| `DB_PASSWORD` | aucun | Mot de passe PostgreSQL |
| `JWT_SECRET` | aucun | Secret JWT, 32 caracteres minimum |
| `ALLOWED_ORIGINS` | `http://localhost:4200` | Origines autorisees pour les WebSockets, separees par des virgules |
| `MANAGEMENT_ENDPOINTS` | `health` | Endpoints Actuator rendus publics par l'application |
| `ADMIN_EMAIL` | aucun | E-mail admin cree au demarrage |
| `ADMIN_PASSWORD` | aucun | Mot de passe admin cree au demarrage |
| `MODEL_API_URL` | `http://localhost:5001` | URL du service d'analyse |
| `MODEL_DIR` | `./models` | Dossier local des fichiers `.pth` pour le service modele |

## Verifications

Backend :

```powershell
cd backend
.\mvnw.cmd compile
.\mvnw.cmd test
```

Les tests backend utilisent Testcontainers et necessitent Docker Desktop.

Frontend :

```powershell
cd frontend
npm run build
```

Service modele :

```powershell
cd model-service
.\.venv\Scripts\Activate.ps1
python -c "import app; print(app.device)"
```

## Production

En production, le service modele doit etre deploye comme un service interne separe. Le frontend continue d'appeler uniquement le backend Spring, et le backend est configure avec `MODEL_API_URL` pour joindre le service d'analyse.

Les secrets (`JWT_SECRET`, identifiants admin, mot de passe PostgreSQL) doivent etre fournis par l'environnement de deploiement. Ne publiez jamais de fichier `.env`, de dump SQL, de modele `.pth` ou de secret dans le depot.
