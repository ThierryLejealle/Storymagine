# 2026-10-28 - Chat : fichier optionnel style.txt (conseil de style commun aux personnages)

## 1. Évolution demandée

Un fichier optionnel `style.txt`, lu s'il existe et fourni à l'agent qui joue les personnages, sous
l'intitulé « conseil stylistique ». Factorise le style quand plusieurs personnages le partagent et le
sépare du contenu des fiches. Non vide seulement : sinon aucun impact.

## 2. Ce qui a été touché

- Fichier à la racine du scénario (pas de répertoire : un seul fichier, partagé). `LegacyLayout` l'exclut
  de la détection des anciennes fiches (un `style.txt` commençant par `# Titre` aurait été refusé).
- Domaine : `ScenarioStyle` (`NONE` si absent ou vide), 8e composant de `ChatScenario` (constructeurs plus
  courts conservés). Lecture à chaque chargement par `ChatFileStorageAdapter.readStyle`, sans copie dans
  `resolved/` : ⟳ relit le fichier.
- **Prompt (delta validé)** : `ChatPromptBuilder` ajoute `STYLE ADVICE:` + le texte entre `STORY SO FAR` et
  les règles de format, seulement si le style n'est pas vide. Ancien : rien. Raison : le style était
  répété dans chaque fiche. Objectif : une consigne commune et séparée du contenu narratif.
  Résumeur et analyses 🔍 / 🧠 ne le reçoivent pas.
- Validation : `ScenarioValidator.checkStyleText` (erreur si ancienne syntaxe ou commande entre accolades),
  avertissement au-delà de 1 200 caractères.
- Bouton 🐞 : section « Conseil stylistique ».
- Tests : `ScenarioStyleLoadingTest`, `ChatPromptBuilderTest` (position du bloc, absence totale sans style),
  `ScenarioValidatorTest`.
- Docs : guide (§1, §2.7, §2.10, §7.4), `specs/chat.md`, `RoleplayNarrator.md`, raccourci du skill.

## 3. Résultat

`mvn clean test` : tous les tests passent. Non essayé sur un vrai modèle : à vérifier que « advice » est
suivi assez fidèlement par un petit modèle (sinon passer à une formulation plus directive, nouveau delta).
