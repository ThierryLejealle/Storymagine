# 2026-07-16 13h30 - Nouveau scénario de démo : Le Donjon des Trois Couronnes

## 1. Demande

"Tu pourrais me générer une histoire plus RPG pour tester le mode à plusieurs [...] une histoire
médieval fantastic. Tu es Alex, voleur dans une équipe RPG avec une paladine, une mage et une
prêtresse [...] forêt, donjon, gobelins, orcs, trolls, énigmes [...] chacun des trois personnages,
tu leur trouves un nom, une histoire marrante et des détails perso qui les rendent vraiment unique
[...] Des secrets cachés ? Des choses embarrassantes ?" — scénario 100% original (noms et intrigue
inventés, aucun personnage ni contenu repris d'une œuvre existante), pensé pour stress-tester le
moteur multi-PNJ (interjection, continuité, plusieurs personnalités distinctes en scène).

Noms choisis par l'utilisateur après plusieurs propositions : Céleste (paladine), Nina (mage),
Florimel (prêtresse). Secrets affinés sur plusieurs itérations à la demande de l'utilisateur
("plus croustillant", puis "je préférerais presque les secrets d'avant, ne cherche pas à lier les
personnages", puis "n'hésite pas à mettre des secrets plus coquins sur tous les personnages") —
version finale : trois secrets indépendants (aucun lien romantique/narratif tissé entre les PNJ),
chacun avec sa propre saveur.

## 2. Ce qui a été touché

Nouveau répertoire `chatscenarios/donjon-trois-couronnes/` :
- `scenario.txt` — Fondval (village), Forêt de Loirsombre, Ruines de Karn-Ithal, la Couronne Noire
  (relique maudite qui réveille et renforce gobelins → orcs → troll). 10 chapitres, 16 actes
  feuilles (rencontre, préparatifs, départ, forêt avec embuscade + énigme du gardien de pierre,
  patrouille d'orcs, entrée du donjon avec porte à trois couronnes + hall effondré, profondeurs
  avec nid de gobelins + salle aux miroirs, troll, chambre du trône, retour). Convention "Joueur :
  Alex" + 3e personne partout (Prémisse et lignes `[...]`), conforme à `chatscenarios/CLAUDE.md`.
- `celeste.txt`, `nina.txt`, `florimel.txt` — fiches publiques + `# SECRET`.

## 3. Vérification

Chargé via le code (`ChatFileStorageAdapter.loadScenario` + `ChatSession.fresh`, script jetable
supprimé après usage, voir §5 de `rules.md`) : 3 PNJ, 16 actes numérotés correctement (1.1 à 10.1),
tours NARRATOR d'ouverture mentionnant bien les trois PNJ dès l'acte 1.1 (règle du §2.4 de
`rules.md` — éviter l'ambiguïté de présence qui avait fait boucler Sheera sur `quete-des-amazones`).
Grep : aucune fuite de 2e personne dans la Prémisse ni les lignes `[...]`.

Non testé en conditions réelles (nécessite Ollama + modèle chargé) — à faire par l'utilisateur.
