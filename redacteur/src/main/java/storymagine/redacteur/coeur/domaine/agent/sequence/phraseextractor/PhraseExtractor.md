# PhraseExtractor

## Rôle
Nouvel agent (2026-07-12), pas un `Corrector` de la chaîne habituelle : c'est un agent de
secours, appelé uniquement quand `TextPatcher.apply()` échoue à retrouver la citation FAUX d'un
autre Corrector dans le texte (petite erreur de recopie du LLM appelant : accord, ponctuation,
mot déplacé). But : relocaliser le passage réellement visé pour permettre au correctif d'être
tout de même appliqué, au lieu d'abandonner directement.

## Ce qu'il fait
Reçoit le texte complet et la citation qui n'a pas matché. Retrouve dans le texte le passage qui
correspond réellement (mot pour mot, tel qu'il apparaît dans le texte) et le recopie, ou répond
`NOT_FOUND` si rien d'assez proche n'existe. Ne corrige ni ne juge rien — pure localisation.

## Format de sortie
```
PASSAGE: "passage recopié mot pour mot du texte"
```
ou `NOT_FOUND` seul. Format à une ligne, différent du FAUX:/JUSTE: des Correctors (pas une liste
de paires) — parsing dédié dans `PhraseExtractor.parseResponse`, pas de dépendance à
`CorrectionParser`.

## Thinking
`thinks() = true` — contrairement à `PhrasingCorrector` (jugement fermé, catégories et exemples
explicites), la tâche est une recherche floue dans un texte parfois long avec plusieurs passages
proches : comparer les candidats avant de recopier bénéficie d'un raisonnement pas-à-pas. Réserve
connue : `OllamaAdapter.resolveThink()` neutralise déjà silencieusement `think=true` en "n/a"
pour les modèles dont `/api/show` ne déclare pas le support — le comportement réel dépend donc du
modèle utilisé, indépendamment de ce réglage.

## Intégration
Appelé depuis `WriteWorkflow#applyCorrections` : si `TextPatcher.apply()` échoue, on appelle
`PhraseExtractor` avec le texte et la citation en échec ; si un passage est trouvé, on retente
`TextPatcher.apply()` une fois avec ce passage relocalisé. Si cette seconde tentative échoue
aussi (ou si `PhraseExtractor` répond `NOT_FOUND`), le `log.warn()` existant est conservé tel
quel — le rescue ne masque jamais un échec réel, il tente seulement une relocalisation avant.

## Origine
Conçu le 2026-07-12 suite à l'observation de petites erreurs de recopie de `StyleCorrector` en
usage réel (`094_SequenceStyleCorrector_12.md`, scénario 1998) : deux corrections sur cinq
citaient une phrase presque identique à l'originale (accord/répétition non exacte), faisant
échouer silencieusement le patch. Design fait directement (pas de consultation Fable pour ce
prompt), validé explicitement avant écriture.
