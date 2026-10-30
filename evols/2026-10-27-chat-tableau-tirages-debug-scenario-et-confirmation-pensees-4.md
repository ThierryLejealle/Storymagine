# 2026-10-27 - Chat : tableau des tirages [PICK] (🎲), debug du scénario envoyé (🐞), confirmation si message de pensées seules

## 1. Évolution demandée

- Afficher, via une icône de l'IHM, un tableau des paragraphes tirés par `[PICK]` / `[PICK-UNIQUE]`,
  dans l'ordre, sans autre explication (valable pour les parties démarrées après cette évolution).
- Bouton de debug donnant le texte de scénario réellement envoyé au LLM (prémisse + acte courant, sans
  historique).
- Avertissement (confirmation) quand le message du joueur n'est constitué que de pensées `(...)`.

## 2. Ce qui a été touché

- Domaine `pick` : `PickDraw` (type, `PickTag`, paragraphe), `PickDraws` (liste ordonnée),
  `PickResolution` (textes + tirages). `PickResolver.resolve` renvoie la résolution ; `resolveAll`
  inchangé en comportement.
- Infra : `PickDrawsFile` (format `@@ PICK[-UNIQUE] tag` + paragraphe), `run/current/resolved/picks.txt`
  écrit avant les `scenario*.txt`, `RunLayout.picksFile`, `RunArchiver` (picks.txt admis dans les zips :
  sauvegardes et archives le conservent).
- Port : `ChatStoragePort.pickDraws`. Service : `ChatService.pickDraws`, `ChatService.scenarioPrompt`
  (`ScenarioPromptView`).
- Web : `GET /picks`, `GET /scenario-prompt`, boutons 🎲 et 🐞 (pop-up d'analyse réutilisée, tableau
  Tag / Type / Tirage), `confirm()` avant envoi (et avant `DO`) si le message n'est que des `(...)`
  (miroir JavaScript de `PlayerThoughts.strip`).
- Tests : `PickResolverTest` (ordre et type des tirages), `PickDrawsFileTest`, `ChatFileStoragePicksTest`
  (enregistrement, reset, sauvegarde, run ancien sans tirage).
- Docs : `guide-redaction-chat.md` (§2.6, arborescence du run, §7.2), `specs/chat.md`.
- Aucun prompt modifié.

## 3. Résultat

`mvn test` à la racine : tous les tests passent. Les boutons 🎲, 🐞 et la confirmation ne sont pas
testés automatiquement (JavaScript) : à essayer dans le navigateur. Un run démarré avant cette évolution
affiche « Aucun tirage enregistré ».
