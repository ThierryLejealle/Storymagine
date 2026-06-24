# 2026-06-23 20h30 — Déplacement de story/modele vers scenarios/modele

## Evolution demandée
Déplacer le répertoire `story/modele` dans le répertoire `scenarios/` pour regrouper
le modèle de scénario avec les scénarios existants.

## Ce qui a été touché
- `story/modele/` → déplacé en `scenarios/modele/` (13 fichiers)
- `story/` → supprimé (vide après déplacement)
- `init-story.bat` ligne 7 : `%~dp0story\modele` → `%~dp0scenarios\modele`
- `redacteur/infra/StoryTemplateFileAdapter.java` javadoc : `story/modele` → `scenarios/modele`

## Résultat
Le répertoire `scenarios/` contient désormais 4 entrées : `1998/`, `as-du-ciel/`,
`modele/`, `une-rencontre/`. Le répertoire `story/` n'existe plus.
Le bat et la javadoc sont cohérents avec le nouvel emplacement.
