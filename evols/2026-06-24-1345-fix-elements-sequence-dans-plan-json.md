# 2026-06-24 13h45 - Fix : éléments de séquence séparés → intégrés dans le plan JSON

## Evolution demandée
Les checks par séquence (et plus généralement tous les éléments de séquence) étaient passés
aux critiques comme sections séparées ("Checks par sequence"), obligeant le LLM à
faire la correspondance mental entre la section et le JSON. Ils doivent être embarqués
directement dans chaque objet de séquence du JSON, à côté des beats/sensoriels/ton_et_rythme.
Exclusion : les personnages restent mutualisés globalement (décision explicite).

## Ce qui a été touché

### `ScenarioFormatters`
- Suppression de `planChecksPerSequence` (mort après fix)
- Ajout de `enrichPlanJson(String planJson, List<Sequence> sequences)` :
  injecte dans chaque objet JSON de séquence les champs `checks`, `contraintes`, `focus`, `lore`
  issus du `Sequence` correspondant (par numéro de séquence dans le JSON)
- Helpers privés : `buildSeqAnnotation`, `extractSeqNum`, `jsonEscape`

### `PlanCoherenceCriticInput`
- Suppression du champ `sequenceChecks` (maintenant dans le plan enrichi)

### `PlanCoherenceCritic`
- Suppression de la section "Checks par sequence" dans le prompt utilisateur

### `PlanCoherenceCriticStep`
- Appel à `enrichPlanJson` avant de construire l'input
- Suppression du `planChecksPerSequence`

## Résultat
Le plan JSON reçu par PlanCoherenceCritic contient désormais, pour chaque séquence concernée,
les champs `checks`, `contraintes`, `focus` et/ou `lore` co-localisés avec les beats.
Exemple pour la séquence 1 de une-rencontre :
```json
{
  "sequence": 1,
  "beats": [...],
  "sensoriels": "...",
  "ton_et_rythme": "...",
  "checks": ["Vérifie qu'il n'y a pas de contact ou discussion entre les personnages"],
  "contraintes": "Pas de contact, pas de discussion, aucun regard direct entre les personnages."
}
```
