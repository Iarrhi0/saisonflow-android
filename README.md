# SaisonFlow – Android (prototype v0.1)

**Projet original et indépendant** de lecture et téléchargement de vidéos depuis des sources accessibles avec autorisation. Nom de travail modifiable avant diffusion.

## Ce qui fonctionne dans le code v0.1

- Écran Catalogue : séries, saisons, épisodes depuis un fichier JSON importé.
- Sélection d'un épisode, des épisodes cochés ou d'une saison entière.
- Préférences **360p / 480p / Auto**, sélectionnant les **URL réelles fournies** dans le catalogue ; aucune conversion fictive.
- Envoi d'un nombre non plafonné de demandes au **DownloadManager** d'Android (plusieurs téléchargements en file / potentiellement simultanés selon Android).
- Liens HTTPS directs vers fichiers `.mp4`, `.webm`, `.mkv`, `.mov`, `.m4v`, `.3gp`.
- Historique persistant en SQLite, chargé par pages de 40 *sans supprimer les anciennes entrées*.
- Consultation de l'état et de l'avancement, annulation/suppression, lecture par lecteur Android externe après téléchargement.
- Option réseau non facturé (généralement Wi-Fi), notifications via DownloadManager.
- Partage d'un lien à **une application tierce compatible** via le menu Partager, qui peut éventuellement proposer 1DM : **pas une intégration SDK officielle**.
- Réception d'un lien via le menu Partager d'Android.
- Code source MIT, tests Java sans SDK Android, actions GitHub pour tests et construction expérimentale d'APK debug.

## Ce qui n'est PAS encore intégré

**Compilation debug vérifiée dans GitHub Actions le 11 novembre 2026**, avec 19 assertions Java et Android Lint réussis : [exécution de validation](https://github.com/Iarrhi0/saisonflow-android/actions/runs/43012589788). Pas de test sur émulateur/téléphone ; une compilation réussie ne valide pas encore le fonctionnement sur appareil.

- Pas de téléchargement segmenté/multithread au sein de l'app comme 1DM (la concurrence du DownloadManager est déterminée par Android).
- Pas de boutons pause/reprise manuels (Android peut suspendre/reprendre selon réseau, mais l'API DownloadManager ne fournit pas de pause manuelle).
- Pas de téléchargement HLS `.m3u8`, MPEG-DASH `.mpd`, sous-titres ou sélection de pistes dans les flux.
- Pas d'extraction d'URL de lecteur de site, de contournement DRM, de déchiffrement ni d'intégration du catalogue AniWave/Kibo/LOOKR.
- Pas de recherche mondiale ni de player intégré dans ce prototype.
- Pas de conversion de vidéo 1080p en 480p/360p. Cette fonctionnalité nécessiterait un transcodeur et les droits sur le fichier.

## Compilation et intégration continue

Le projet Android est extrait à la racine de `Iarrhi0/saisonflow-android`.
L'archive d'origine est conservée pour référence ; les sources extraites sont désormais la version à modifier.

Le workflow `.github/workflows/android-ci.yml` s'exécute pour chaque push sur `main`, chaque Pull Request vers `main` et manuellement dans Actions.
Il utilise JDK 17, Gradle 8.9, Android Gradle Plugin 8.7.3 et Android SDK 35.
Il exécute les tests Java de régression, Android Lint, les éventuels tests unitaires Gradle, puis `assembleDebug`.
L'artifact `SaisonFlow-debug-apk` n'est publié qu'après réussite de ces étapes ; il est conservé 14 jours.
Les rapports de vérification sont publiés séparément, y compris en cas d'échec.
La tâche Gradle `testDebugUnitTest` ne contient pas encore de tests Android : les tests existants sont dans `tests/CoreTest.java`.

Pour une compilation locale avec JDK 17, Gradle 8.9 et SDK 35 installés :

```bash
bash tests/run-core-tests.sh
gradle --no-daemon :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
```

Configurer `ANDROID_HOME` ou `sdk.dir` dans `local.properties` (non versionné).
L'APK debug se trouve dans `app/build/outputs/apk/debug/app-debug.apk`.
Il s'agit d'une version de test signée avec une clé debug, pas d'une version de production.

## Anciennes instructions de création du dépôt (archive initiale)

1. Ouvrir **https://github.com/new** et créer un dépôt vide `saisonflow-android` (public ou privé au choix).
2. **Ne pas** cocher « Add a README file », car le projet en contient déjà un.
3. Sur votre PC, décompresser l'archive `SaisonFlow_GitHub.zip` ; déposer le contenu du dossier `SaisonFlow_GitHub` à la racine du dépôt (pas le dossier parent) via « uploading an existing file » dans GitHub. La méthode git est préférable si votre navigateur refuse les dossiers imbriqués.
4. Valider (Commit changes). Les actions GitHub « Core unit tests » et « Android debug APK » sont définies dans `.github/workflows/`.
5. Dans **Actions**, vérifier la construction. Si elle réussit, ouvrir le run `Android debug APK`, puis télécharger l'artifact `SaisonFlow-debug-apk`.

Alternative en ligne de commande pour un dépôt déjà créé :

```bash
cd SaisonFlow_GitHub
git init
git add .
git commit -m "Initial SaisonFlow Android prototype"
git branch -M main
git remote add origin https://github.com/VOTRE_COMPTE/saisonflow-android.git
git push -u origin main
```

### Android Studio (Windows 10/11)

Installer Android Studio et Android SDK 35, ouvrir le dossier `SaisonFlow_GitHub`, attendre la synchronisation Gradle. La version Android Gradle Plugin est `8.7.3` et le JDK requis est **17**. Si aucun Gradle wrapper local n'est généré, l'Action GitHub utilise Gradle 8.9 directement. Installer un appareil Android ou émulateur puis utiliser **Run** pour tester. Tester sur vidéos de démonstration que vous êtes autorisé à télécharger.

## Catalogue JSON : séries, saisons et qualités réelles

Voir `app/src/main/assets/catalogue-exemple.json` : l'exemple est **volontairement vide de vidéos**. Insérer dans chaque champ une URL HTTPS directe **pour la bonne résolution**. Les URL doivent provenir de médias dont vous disposez des droits d'utilisation/téléchargement :

```json
{
  "version": 1,
  "series": [
    {
      "title": "Ma série",
      "seasons": [
        {
          "number": 1,
          "episodes": [
            {
              "number": 1,
              "title": "Épisode pilote",
              "qualities": {
                "360p": "https://votre-serveur.example/videos/pilote-360.mp4",
                "480p": "https://votre-serveur.example/videos/pilote-480.mp4"
              }
            }
          ]
        }
      ]
    }
  ]
}
```

Les URL en `.example` sont **fictives et ne marcheront pas**. L'application n'invente pas une qualité absente ; `Auto` privilégie 480p, puis 360p, puis une source `original` lorsqu'elle est fournie. Une URL vers une page YouTube ne remplace pas l'URL directe d'un fichier vidéo.

## Architecture

```text
SaisonFlow_GitHub/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/catalogue-exemple.json
│       └── java/com/saisonflow/app/
│           ├── MainActivity.java       # UI mobile + navigation
│           ├── Catalogue.java          # parseur JSON des séries/saisons
│           ├── Episode.java            # modèle de données
│           ├── QualitySelector.java    # résolution URL 360p/480p
│           ├── DownloadEngine.java     # DownloadManager Android
│           └── DownloadStore.java      # historique SQLite sans quota
├── docs/ARCHITECTURE_ET_ROADMAP_FR.md
├── .github/workflows/
│   ├── core-tests.yml
│   └── android-debug.yml
└── tests/
    ├── CoreTest.java
    └── run-core-tests.sh
```

## Tests exécutés pendant la préparation

- `bash tests/run-core-tests.sh` : **19 assertions réussies** (qualité réelle, Auto, repli après URL invalide, schémas de liens, validation fichiers directs).
- Vérification de syntaxe XML pour AndroidManifest/styles et du JSON catalogue : **réussie**.
- Android Lint et `assembleDebug` : **réussis sur GitHub Actions**. La tâche `testDebugUnitTest` indique `NO-SOURCE` ; les 19 assertions sont exécutées séparément par le script Java.
- Lancement sur un téléphone ou émulateur : **non effectué**.

## Limites et droits

SaisonFlow n'inclut aucun fichier, logo, clé de signature, source ou catalogue provenant d'AniWave, Kibo, LOOKR, 1DM ou Snaptube. C'est un produit indépendant. Il est destiné aux vidéos personnelles, libres ou dont l'utilisation hors ligne est autorisée. La prise en charge des médias soumis à DRM, restriction contractuelle ou absence de permission n'est pas prévue.

Sans limite **applicative** sur le nombre total d'entrées, pas sans limites physiques : la mémoire, l'espace libre, la bande passante, le serveur et les restrictions Android s'appliquent toujours.
