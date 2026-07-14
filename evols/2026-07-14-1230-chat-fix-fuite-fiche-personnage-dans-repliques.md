# 2026-07-14 12h30 - Chat : règle anti-fuite de la fiche personnage dans les répliques

## Demande

"Can you add a phrase in the master prompt to prevent the LLM to let his instruction or character
appears in his speech. Like you describe a character as a spy pretending to be shy. He should not
speak and say 'Since I'm shy, what about we do ...'" — avec consigne explicite de demander l'avis
de Fable sur le prompt maître avant modification.

## Consultation Fable (~29K tokens, confirmé avant envoi)

Confirmé comme un mode d'échec réel et fréquent pour les petits modèles : la fiche personnage,
écrite en langage descriptif à la 3e personne ("Elle est timide"), est recyclée par continuation
statistique dans les répliques du personnage lui-même — d'autant plus grave pour un trait CACHÉ
(une couverture d'espionne qui s'auto-dévoile). Recommandation : une règle en fin de
`SYSTEM_INTRO`, juste avant l'injection de `YOUR CHARACTER:` (proximité = le modèle "cadre" la
lecture de la fiche qui suit immédiatement), plutôt que dans `SYSTEM_FORMATTING` (trop loin de la
fiche) ou une section dynamique dédiée (inutile, la fiche est toujours présente). Vérifié non
redondant avec la règle anti-mirroring joueur/personnage et la règle anti-spoiler scénario/acte
existantes.

## Delta présenté et validé avant écriture

Ajouté en fin de `SYSTEM_INTRO` (`ChatPromptBuilder.java`), rien d'autre modifié :

```
The character sheet below is a private author's note — your character has never
read it. Show its traits and motives through behavior only; never quote, name or
explain them. Example, for a character described as shy — Wrong: "Since I'm shy,
maybe we could stay here." Right: *she hesitates, eyes dropping to the floor*
"Maybe... we could stay here?"
```

## Résultat

`mvn -pl chat test` au vert (aucun test n'assertait le contenu exact de `SYSTEM_INTRO`, donc pas
de casse). Validation réelle en session (le modèle incarne le trait plutôt que de le citer) laissée
à l'utilisateur — nécessite un LLM réel.
