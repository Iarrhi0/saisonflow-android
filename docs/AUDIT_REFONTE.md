# Audit du 11 novembre 2026
Base examinée : 99736bf (PR 2). Main contient encore le ZIP initial. La CI de la branche compile avec Java 17, Gradle 8.9 et SDK 35, installe sur API 30 et capture l’accueil. La taille mesurée de cette ancienne APK est de 1 874 243 octets.

| Fonction | Constat initial |
|---|---|
| Recherche | Miruro et French Stream sont des index locaux. Internet Archive répond en ligne. Une correspondance de titre ne confirme ni une saison ni une vidéo. |
| Interface | Vues Android Java, sept illustrations, pages fournisseurs en WebView. Le visuel cible comprend aussi la fiche et la feuille de sélection. |
| Transfert | DownloadManager système et historique SQLite, contrôle de statut par identifiant. Pas de plan de saison idempotent et pas de reprise manuelle ou segmentation. |
| Lecture | VideoView. Aucun téléchargement Media3 de pistes HLS/DASH ou sous-titres. |
| Qualité | Une URL manuelle pouvait être étiquetée 480p sans analyse. |

Première étape : fiche native Kotlin/Compose et feuille Material 3. Les métadonnées seules conduisent à un état indisponible. Pour la collection de démonstration Blender, chaque fichier déclaré est sondé en HTTPS sur un préfixe MP4 avant sélection. Cette sonde vérifie un en-tête, pas l’intégrité du fichier complet, les pistes ou le DRM. La base SQLite et les identifiants de téléchargement existants sont conservés.

Étapes restantes : connecteurs configurables avec provenance des droits, rapprochement par identifiant, saisons réelles, politiques langue/qualité, migration Room avec tests, file persistante dédupliquée, Media3 et lecture hors ligne, essais de coupure et stockage, recette sur appareil physique.
