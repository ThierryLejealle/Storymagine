# 2026-11-04 - Popup de survol d'un personnage : statut et partie publique de la fiche

## Évolution demandée
Au survol d'une vignette de personnage, seule une infobulle native donnait le statut (niveau d'activité et origine).
Demande : y ajouter la partie publique de la fiche du personnage. Précisions de l'auteur : ignorer les lignes de
titre `#`, et ne montrer que la partie publique (jamais le `# SECRET`).

## Ce qui a été touché
- `NpcView` : nouveau champ `publicSheet`.
- `ChatWebServer.castView` : la fiche est celle de l'acte courant (`ChatScenario.sheetAt` : tags `## tag` activés et
  deltas publics appliqués), partie publique seulement ; `withoutHeadings` retire les lignes de titre `#`.
  Elle est recalculée à chaque rafraîchissement du panneau, donc elle suit les changements d'acte, les retours en
  arrière et les rechargements.
- `chat.html` : le `title` natif est remplacé par une popup maison (statut en italique, puis la fiche en texte brut),
  placée à gauche de la vignette si la place le permet, avec défilement au-delà de 60 % de la hauteur.
- Tests (`ChatWebServerEndpointsTest`) : la vue du casting porte la fiche publique sans titre et jamais le secret ;
  `withoutHeadings` retire les titres, y compris indentés.
- Spécification : `specs/chat.md`. Aucun prompt d'agent modifié.

## Résultat
Tests verts depuis la racine du projet. Rendu en texte brut (pas de mise en forme).
**Non vérifié à la main** : l'aspect et le placement de la popup dans le navigateur. La page est lue dans les
ressources compilées : il faut reconstruire et relancer le chat.
**À savoir** : `mvn -pl chat` seul utilise un ancien jar de `commun` s'il n'a pas été réinstallé
(`NoSuchMethodError` sur `GenerationOptions`) ; lancer les tests depuis la racine.
