# 2026-10-31 - Seuil d'alerte du style commun relevé à 1 500 caractères

## Évolution demandée
Passer de 1 200 à 1 500 caractères le seuil au-delà duquel un `style.txt` déclenche un avertissement.

## Ce qui a été touché
- `ScenarioValidator.STYLE_WARNING_LENGTH` : 1200 -> 1500 (le test utilise la constante).
- `specs/chat.md`, `chatscenarios/guide-redaction-chat.md` §2.10 et
  `.claude/skills/chat-scenario-writing/SKILL.md` : seuil mis à jour.
- Aucun prompt modifié.

## Résultat
Un style de 1 200 à 1 500 caractères ne déclenche plus d'avertissement.
