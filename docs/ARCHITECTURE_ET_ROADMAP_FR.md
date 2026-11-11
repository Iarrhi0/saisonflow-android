# Architecture et feuille de route SaisonFlow

## Résultat de la comparaison produit

| Application de référence | Fonctionnalité publiquement annoncée | Adaptation proposée (code original) |
|---|---|---|
| Kibo | Lecteur personnalisé, téléchargements intégrés, catalogue/anime | épisodes + saisons + bibliothèque |
| LOOKR – AI Video Search & Download | recherche de vidéos et exploration du catalogue | moteur de recherche + navigation (v0.4) |
| 1DM | lots, transferts multi-parties, reprise, gestion des files | moteur HTTP Range (v0.2) et téléchargement par lots (v0.1) |
| SnapTube | coller un lien, formats/qualités, visionnage hors ligne | saisie URL + sélection variantes + gestion fichiers |

Références publiques consultées en novembre 2026 :
- https://www.kiboanime.app/
- https://play.google.com/store/apps/details?id=io.kibo.clarity
- https://play.google.com/store/apps/details?id=com.wecloud.lookr
- https://play.google.com/store/apps/details?id=idm.internet.download.manager
- https://www.snaptube.io/

Ne pas copier l'interface, les logos, les marques, les fichiers APK ou le code de ces applications.

## v0.1 – livré dans ce ZIP (prototype)

- Écran et catalogue JSON de démonstration (sans URL de vidéo).
- Sélection épisodes cochés / saison entière.
- Gestion des sources directes 360p / 480p / Auto.
- Android DownloadManager, téléchargements groupés, historique SQLite illimité avec pagination.
- Partage externe générique via Android (pas d'API d'intégration officielle avec 1DM/SnapTube).
- GitHub Actions : tests Java et essai de compilation APK debug.

## v0.2 – Moteur HTTP « accéléré »

- OkHttp ou HttpURLConnection, stockage temporaire atomique.
- Pré-validation avec HEAD ou GET limité ; vérifier Content-Length, Accept-Ranges, ETag et Last-Modified.
- HTTP Range : fractions parallèles **seulement** lorsque le serveur prend en charge `206 Partial Content` ; si `200 OK`, rétrograder en téléchargement simple.
- Réglage de la concurrence **par fichier** : ex. 1, 2, 4, 8 connexions ; réglage des transferts simultanés 1–8 ; sauvegarde d'état durable via Room.
- Vitesse et ETA, gestion du stockage faible, reprise avec vérification ETag, retry avec backoff exponentiel.
- Pauses/reprises manuelles, annulation atomique, priorité, limite éventuelle de vitesse configurable (sans quota sur le nombre de tâches).
- Éviter de multiplier inutilement les connexions ou de dépasser les limites de serveurs.

## v0.3 – Flux et lecteur vidéo

- Télécharger les sources HLS `.m3u8` et DASH `.mpd` autorisées sans contournement DRM avec **AndroidX Media3 DownloadService/DownloadManager**.
- Choisir précisément les pistes correspondant à 360p et 480p dans un manifeste master si présentes ; choix des pistes de sous-titres VTT / SRT si autorisés et disponibles.
- Lecteur Media3/ExoPlayer avec vitesse, audio, sous-titres, reprise de la lecture et dossiers par série/saison.
- Boutons « Télécharger cet épisode », « Télécharger la saison », « Télécharger la sélection » depuis la fiche de lecture.

Documentation officielle Android :
- https://developer.android.com/media/media3/exoplayer/downloading-media
- https://developer.android.com/reference/android/app/DownloadManager
- https://developer.android.com/develop/background-work/services/fgs/timeout

## v0.4 – Catalogue & expérience

- Recherche multi-critères dans le **catalogue autorisé**, listes et favoris.
- Import JSON et API de contenu sous licence ; synchronisation optionnelle, mode hors ligne.
- Notifications nouvelles sorties, partage et Cast si légalement autorisé.
- FR / MG / EN, gestion consommation de données, estimation de taille avant transfert.
- Gestion des mises à jour et releases GitHub, tests d'interface sur Android 10–16.

## v1.0 – Production

- Nom/logo propres, règles de protection des données, politique de confidentialité, licences de catalogues et contenus, stabilité, tests sur appareils réels.
- Signature de sortie Android, CI compilations test + release, gestion du stockage de l'utilisateur.

## Exigences techniques et sécurité

- Aucun quota applicatif arbitraire sur les téléchargements cumulés. Limites effectives : stockage utilisateur, bande passante, restrictions Android, quotas des serveurs et droits de téléchargement.
- Au démarrage d'un lot, ne pas créer une `Thread` par épisode ; mettre les demandes en file, contrôler les connexions actives.
- Les flux protégés et contenus sans droit de téléchargement ne sont pas pris en charge ; ne pas contourner DRM, paiements ou accès.
- Les services de premier plan `dataSync` ont des restrictions de durée sur Android 15+ : préférer API officielles de transfert utilisateur ou un service de téléchargement adapté, avec notification et gestion des reprises.
- YouTube : ouvrir par l'application officielle; pas d'extraction non autorisée de médias.
- Pas de serveur distant imposé : catalogues autorisés fournis par l'utilisateur, téléchargement sur l'appareil.

## Critères d'acceptation avant annonce « APK prête »

1. GitHub Actions `Android CI` termine `success` et publie l'artifact `SaisonFlow-debug-apk`.
2. Tests manuels sur Android 10, 13, 15 : import catalogue, qualité, lot, fichier unique, notifications, absence de permission stockage excessive.
3. Le système n'étiquette jamais un fichier 720p comme 480p simplement parce que l'utilisateur a choisi 480p.
4. Épisode sans URL compatible : erreur explicite sans faux téléchargement.
5. Le téléchargement n'est pas déclaré terminé avant vérification système.
6. Sauvegarde/reprise après perte réseau et redémarrage à vérifier.
