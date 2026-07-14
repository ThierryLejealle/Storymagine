# 2026-07-14 18h57 - Chat : styles d'affichage alternatifs (Immersif / Script) + retrait du style Lecture

## 1. Évolution demandée
L'utilisateur trouvait le texte du chat peu lisible dans le style "bulles" existant (bulles
colorées façon SMS, peu adaptées à de longues répliques narratives). Demande : proposer une
alternative pensée pour la lecture, en consultant Fable pour un avis de design, puis ajouter un
bouton pour choisir le style.

Une première proposition de Fable ("Mode Lecture" : flux manuscrit serif, colonne étroite 66ch)
a été implémentée, testée en aperçu navigateur, puis jugée trop étroite par l'utilisateur sur
grand écran. Deux styles supplémentaires très différents ont alors été demandés et implémentés :
"Immersif" (large, aéré, sans-serif, labels de locuteur) et "Script" (transcript pleine largeur
avec gouttière de locuteur, ensuite rétréci à 70rem car jugé trop large en plein écran).

Au tour suivant, l'utilisateur a indiqué ne pas aimer le style "Lecture" : il a été retiré
entièrement (CSS, option du menu, JS).

## 2. Ce qui a été touché
- `chat/src/main/resources/web/chat.html` :
  - Remplacement du bouton unique de bascule de style par un `<select id="styleModeSelect">`
    dans le header, avec options Bulles / Immersif / Script (Lecture ajouté puis retiré).
  - CSS scopé par classe sur `<body>` : `body.style-immersif` (large, sans-serif, labels
    `::before` "Toi"/"Personnage"), `body.style-script` (transcript, gouttière `::before`
    "TOI"/"PERSONNAGE", largeur finale plafonnée à 70rem).
  - JS : `applyStyleMode(mode)` toggle les classes sur `<body>`, persiste le choix dans
    `localStorage` (`storymagine-style-mode`), restauré au chargement.
- Aucun changement côté Java/serveur : uniquement HTML/CSS/JS statique.

## 3. Résultat
Aperçu vérifié dans le navigateur (fichier de test avec fausses répliques longues, généré dans
le scratchpad, jamais commité) pour les 3 itérations successives. Le menu déroulant permet de
basculer entre Bulles / Immersif / Script, choix mémorisé par session navigateur. Le style
Lecture a été proposé, testé, puis explicitement rejeté par l'utilisateur et retiré du code —
aucune trace résiduelle (CSS, option, référence JS) dans le fichier final.
