package fr.agroscan.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.text.Normalizer;

@Service
public class ScanAnalysisPresentationService {

    private static final List<String> HEALTHY_ADVICE = List.of(
            "La feuille ne presente pas de signe de maladie detecte par le modele.",
            "Continuez a surveiller regulierement la plante, surtout apres pluie, arrosage abondant ou forte chaleur.",
            "Maintenez une bonne aeration du feuillage et evitez de mouiller les feuilles lors de l'arrosage."
    );

    private static final List<String> DEFAULT_DISEASE_ADVICE = List.of(
            "Isolez si possible la plante ou les feuilles atteintes afin de limiter la propagation.",
            "Retirez les parties fortement touchees avec un outil propre, puis evacuez-les hors du compost.",
            "Surveillez l'evolution pendant les prochains jours et demandez conseil a un professionnel si les symptomes progressent."
    );

    private static final Map<String, String> PLANT_LABELS = Map.ofEntries(
            Map.entry("Apple", "Pommier"),
            Map.entry("Blueberry", "Myrtillier"),
            Map.entry("Cherry", "Cerisier"),
            Map.entry("Corn", "Mais"),
            Map.entry("Grape", "Vigne"),
            Map.entry("Orange", "Oranger"),
            Map.entry("Peach", "Pecher"),
            Map.entry("Pepper", "Poivron"),
            Map.entry("Potato", "Pomme de terre"),
            Map.entry("Raspberry", "Framboisier"),
            Map.entry("Soybean", "Soja"),
            Map.entry("Squash", "Courge"),
            Map.entry("Strawberry", "Fraisier"),
            Map.entry("Tomato", "Tomate")
    );

    private static final Map<String, DiseasePresentation> DISEASE_PRESENTATIONS = Map.ofEntries(
            disease("Apple__Apple_scab", "Tavelure du pommier",
                    "Ramassez les feuilles tombees et retirez les parties atteintes pour reduire les spores.",
                    "Taillez legerement pour ameliorer l'aeration de l'arbre.",
                    "En prevention, appliquez un traitement adapte au pommier au debut des periodes humides."),
            disease("Apple___Black_rot", "Pourriture noire du pommier",
                    "Supprimez les fruits momifies, feuilles et rameaux atteints.",
                    "Evitez les blessures sur les fruits et gardez une bonne hygiene autour de l'arbre.",
                    "Surveillez les nouvelles taches et intervenez rapidement sur les zones contaminees."),
            disease("Apple___Cedar_apple_rust", "Rouille grillagee du pommier",
                    "Retirez les feuilles fortement atteintes lorsque c'est possible.",
                    "Eloignez les genievriers proches si la maladie revient chaque saison.",
                    "Favorisez une bonne circulation de l'air et utilisez un traitement preventif adapte en periode a risque."),
            healthy("Apple___healthy", "Pommier sain"),

            healthy("Blueberry___healthy", "Myrtillier sain"),

            disease("Cherry___Powdery_mildew", "Oidium du cerisier",
                    "Retirez les jeunes pousses tres atteintes pour limiter la propagation.",
                    "Evitez les exces d'azote qui favorisent les pousses sensibles.",
                    "Ameliorez l'aeration et appliquez un traitement anti-oidium adapte si necessaire."),
            healthy("Cherry___healthy", "Cerisier sain"),

            disease("Corn___Cercospora_leaf_spot Gray_leaf_spot", "Cercosporiose du mais",
                    "Retirez ou enfouissez les residus de culture apres recolte.",
                    "Pratiquez une rotation des cultures pour reduire la pression de la maladie.",
                    "Surveillez les parcelles humides et privilegiez des varietes tolerantes si le probleme revient."),
            disease("Corn___Common_rust_", "Rouille commune du mais",
                    "Surveillez l'evolution des pustules, surtout par temps doux et humide.",
                    "Evitez les semis trop denses pour limiter l'humidite dans le feuillage.",
                    "En cas de forte attaque, demandez conseil pour un traitement fongicide adapte."),
            disease("Corn___Northern_Leaf_Blight", "Helminthosporiose du nord du mais",
                    "Retirez les residus infectes et evitez de resemer du mais au meme endroit trop vite.",
                    "Favorisez une bonne rotation et une parcelle bien aeree.",
                    "Choisissez des varietes resistantes lorsque la maladie est frequente localement."),
            healthy("Corn___healthy", "Mais sain"),

            disease("Grape___Black_rot", "Black-rot de la vigne",
                    "Retirez les grappes dessechees et les feuilles atteintes.",
                    "Taillez pour aerer la vigne et limiter l'humidite autour des grappes.",
                    "Mettez en place une protection preventive avant les periodes chaudes et humides."),
            disease("Grape___Esca_(Black_Measles)", "Esca de la vigne",
                    "Marquez les ceps atteints et taillez-les separement avec des outils desinfectes.",
                    "Evitez les grosses plaies de taille et protegez les coupes importantes.",
                    "Si un cep decline fortement, envisagez un recepage ou un remplacement."),
            disease("Grape___Leaf_blight_(Isariopsis_Leaf_Spot)", "Brulure des feuilles de la vigne",
                    "Retirez les feuilles tres atteintes et limitez l'humidite persistante.",
                    "Ameliorez l'aeration par une taille ou un palissage adapte.",
                    "Surveillez les nouvelles taches et appliquez une protection fongicide si la pression augmente."),
            healthy("Grape___healthy", "Vigne saine"),

            disease("Orange___Haunglongbing_(Citrus_greening)", "Huanglongbing des agrumes",
                    "Isolez l'arbre suspect et evitez de deplacer du materiel vegetal.",
                    "Surveillez la presence de psylles, insectes vecteurs de la maladie.",
                    "Contactez rapidement un professionnel ou un service phytosanitaire local, car cette maladie est grave."),

            disease("Peach___Bacterial_spot", "Tache bacterienne du pecher",
                    "Retirez les feuilles et fruits tres atteints lorsqu'ils sont accessibles.",
                    "Evitez l'arrosage sur le feuillage et favorisez une bonne aeration.",
                    "Utilisez des varietes tolerantes et un programme preventif adapte si la maladie revient."),
            healthy("Peach___healthy", "Pecher sain"),

            disease("Pepper,_bell___Bacterial_spot", "Tache bacterienne du poivron",
                    "Retirez les feuilles atteintes et evitez de manipuler les plants lorsqu'ils sont mouilles.",
                    "Arrosez au pied pour limiter les projections d'eau sur le feuillage.",
                    "Nettoyez les outils et evitez de replanter des solanacees au meme endroit trop rapidement."),
            healthy("Pepper,_bell___healthy", "Poivron sain"),

            disease("Potato___Early_blight", "Alternariose de la pomme de terre",
                    "Retirez les feuilles les plus atteintes et evitez de mouiller le feuillage.",
                    "Buttez correctement les plants et maintenez une fertilisation equilibree.",
                    "Pratiquez une rotation longue et utilisez un traitement preventif si les conditions sont favorables."),
            disease("Potato___Late_blight", "Mildiou de la pomme de terre",
                    "Retirez rapidement les parties atteintes et evitez de laisser des tubercules contamines au sol.",
                    "Limitez l'humidite du feuillage et espacez suffisamment les plants.",
                    "Intervenez rapidement avec une protection anti-mildiou adaptee, surtout par temps humide."),
            healthy("Potato___healthy", "Pomme de terre saine"),

            healthy("Raspberry___healthy", "Framboisier sain"),
            healthy("Soybean___healthy", "Soja sain"),

            disease("Squash___Powdery_mildew", "Oidium de la courge",
                    "Retirez les feuilles les plus couvertes de blanc pour reduire l'inoculum.",
                    "Espacez les plants et arrosez au pied plutot que sur les feuilles.",
                    "Appliquez un traitement anti-oidium adapte si l'attaque progresse."),
            healthy("Squash___healthy", "Courge saine"),

            disease("Strawberry___Leaf_scorch", "Brulure des feuilles du fraisier",
                    "Retirez les vieilles feuilles atteintes apres recolte.",
                    "Evitez les arrosages par aspersion et gardez une bonne circulation d'air.",
                    "Renouvelez les plants trop affaiblis et evitez les parcelles trop humides."),
            healthy("Strawberry___healthy", "Fraisier sain"),

            disease("Tomato___Bacterial_spot", "Tache bacterienne de la tomate",
                    "Retirez les feuilles atteintes et evitez de toucher les plants mouilles.",
                    "Arrosez au pied et espacez les plants pour accelerer le sechage.",
                    "Desinfectez les outils et evitez de replanter tomate, poivron ou pomme de terre au meme endroit."),
            disease("Tomato___Early_blight", "Alternariose de la tomate",
                    "Supprimez les feuilles basses atteintes et paillez le sol pour limiter les projections.",
                    "Arrosez au pied et evitez les stress hydriques.",
                    "Pratiquez la rotation des cultures et appliquez une protection fongicide si necessaire."),
            disease("Tomato___Late_blight", "Mildiou de la tomate",
                    "Retirez rapidement les feuilles ou plants fortement atteints.",
                    "Gardez le feuillage aussi sec que possible et evitez les arrosages le soir.",
                    "Protegez les plants avant ou pendant les periodes humides prolongees."),
            disease("Tomato___Leaf_Mold", "Cladosporiose de la tomate",
                    "Aerez la culture et reduisez l'humidite sous serre ou abri.",
                    "Retirez les feuilles touchees sans secouer les plants.",
                    "Evitez les arrosages sur le feuillage et choisissez des varietes resistantes si possible."),
            disease("Tomato___Septoria_leaf_spot", "Septoriose de la tomate",
                    "Retirez les feuilles basses tachees et evacuez les debris vegetaux.",
                    "Paillez le sol pour limiter les projections d'eau contaminee.",
                    "Respectez une rotation et espacez les plants pour ameliorer l'aeration."),
            disease("Tomato___Spider_mites Two-spotted_spider_mite", "Acariens tetranyques de la tomate",
                    "Douchez legerement le dessous des feuilles si la culture le permet et augmentez l'humidite ambiante.",
                    "Retirez les feuilles tres infestees et surveillez les toiles fines.",
                    "Introduisez ou favorisez des auxiliaires, ou utilisez un acaricide adapte en dernier recours."),
            disease("Tomato___Target_Spot", "Tache cible de la tomate",
                    "Retirez les feuilles atteintes et limitez l'humidite sur le feuillage.",
                    "Espacez les plants et evitez les cultures trop denses.",
                    "Appliquez une protection fongicide adaptee si les taches progressent rapidement."),
            disease("Tomato___Tomato_Yellow_Leaf_Curl_Virus", "Virus de l'enroulement jaune de la tomate",
                    "Retirez les plants fortement atteints pour limiter les reservoirs de virus.",
                    "Luttez contre les aleurodes, principaux vecteurs de la maladie.",
                    "Utilisez des plants sains et des varietes resistantes lorsque c'est possible."),
            disease("Tomato___Tomato_mosaic_virus", "Virus de la mosaique de la tomate",
                    "Retirez les plants suspects et evitez de manipuler les plants sains juste apres.",
                    "Desinfectez les mains, tuteurs et outils entre les plants.",
                    "N'utilisez que des semences ou plants certifies sains."),
            healthy("Tomato___healthy", "Tomate saine")
    );

    public AnalysisPresentation presentation(String plant, String disease, Boolean healthy) {
        DiseasePresentation diseasePresentation = DISEASE_PRESENTATIONS.get(disease);
        boolean healthyResult = Boolean.TRUE.equals(healthy);
        return new AnalysisPresentation(
                plantLabel(plant),
                diseasePresentation == null ? readableDisease(disease, healthyResult) : diseasePresentation.label(),
                diseasePresentation == null
                        ? healthyResult ? HEALTHY_ADVICE : DEFAULT_DISEASE_ADVICE
                        : diseasePresentation.advice()
        );
    }

    public List<String> matchingPlantKeys(String search) {
        String normalizedSearch = normalize(search);
        if (normalizedSearch.isBlank()) return List.of();
        return PLANT_LABELS.entrySet().stream()
                .filter(entry -> normalize(entry.getKey()).contains(normalizedSearch)
                        || normalize(entry.getValue()).contains(normalizedSearch))
                .map(Map.Entry::getKey)
                .toList();
    }

    public List<String> matchingDiseaseKeys(String search) {
        String normalizedSearch = normalize(search);
        if (normalizedSearch.isBlank()) return List.of();
        return DISEASE_PRESENTATIONS.entrySet().stream()
                .filter(entry -> normalize(entry.getKey()).contains(normalizedSearch)
                        || normalize(entry.getValue().label()).contains(normalizedSearch))
                .map(Map.Entry::getKey)
                .toList();
    }

    private static Map.Entry<String, DiseasePresentation> healthy(String key, String label) {
        return Map.entry(key, new DiseasePresentation(label, HEALTHY_ADVICE));
    }

    private static Map.Entry<String, DiseasePresentation> disease(String key, String label, String... advice) {
        return Map.entry(key, new DiseasePresentation(label, List.of(advice)));
    }

    private static String readableDisease(String disease, boolean healthy) {
        if (healthy) return "Aucune maladie detectee";
        if (disease == null || disease.isBlank()) return "Maladie non determinee";
        int separator = disease.indexOf("___");
        String value = separator >= 0 ? disease.substring(separator + 3) : disease;
        return value.replace("__", " ").replace('_', ' ').trim();
    }

    private static String plantLabel(String plant) {
        if (plant == null || plant.isBlank()) return "Plante non determinee";
        return PLANT_LABELS.getOrDefault(plant, plant);
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase()
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    public record AnalysisPresentation(String plantLabel, String diseaseLabel, List<String> advice) {
    }

    private record DiseasePresentation(String label, List<String> advice) {
    }
}
