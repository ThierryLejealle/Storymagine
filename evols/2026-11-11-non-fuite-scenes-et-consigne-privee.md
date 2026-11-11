# 2026-11-11 - Non-fuite dans les scènes et consigne privée `{private}`

## Évolution demandée
- Un petit LLM, à qui on avait donné le skill de rédaction, a écrit « Ce qui doit se passer » dans les
  scènes et un résumé de la trame dans la prémisse et les conteneurs. En jeu, les PNJ jouaient en fonction
  de ce futur : ils sortaient du scénario pour le forcer et brûlaient les étapes.
- Décisions : le rédacteur ne décrit que ce qui se passe dans chaque scène jouée (la situation et la consigne
  de chaque PNJ pour cette scène) ; le mobile ultime d'un PNJ peut aller dans son `# SECRET`, écrit comme un
  état et jamais comme un plan ; on complète les secrets en cours d'histoire (`{npc-delta secret}`) ;
  pas de notion de plan global, pas d'avertissements au chargement sur le vocabulaire d'avenir.
- Nouveau vocabulaire commun : **scène jouée** (titre le plus profond d'une branche, porte le `[NEXT ACT]`) et
  **conteneur** (titre qui a des sous-titres).
- Nouvelle commande `{private Nom}` : une consigne que seuls certains PNJ lisent, pour la scène qui l'écrit.
- Nouvelle étape « vérification des fuites » dans les skills.

## Ce qui a été touché
- Concept métier `PrivateLines` (domaine scénario) : `visibleTo(texte, cast, pnj)` filtre le texte pour un
  PNJ, `validate(texte, cast)` contrôle la syntaxe (début de ligne, noms connus, bloc fermé avant le titre
  suivant, pas d'imbrication, ni `[NEXT ACT]` ni ligne `[affichée au joueur]` dedans).
- `ChatPromptBuilder` : la prémisse et le texte de la scène sont filtrés pour le PNJ qui parle (`readBy`).
  Aucun libellé de prompt n'est modifié.
- `ChatScenario` : refuse au chargement une consigne privée mal formée (prémisse et chaque acte).
- `ScenarioValidator` : commandes `private` / `end-private` connues, `checkPrivateLines` (une erreur par fichier,
  avec le numéro de ligne).
- `ScenarioAct` : javadoc, vocabulaire scène jouée / conteneur.
- Tests : `PrivateLinesTest`, `PrivateScenarioLoadingTest`, un test dans `ChatPromptBuilderTest` (Franck lit sa
  consigne, Alex ne la lit pas) et un dans `ScenarioValidatorTest` ; `ScenarioBuilderFormatTest` suit le
  nouveau format de scène (une ligne par PNJ, une consigne privée).
- Documentation d'auteur, mise à jour ensemble : `chatscenarios/guide-redaction-chat.md` (§2.0, §2.3, §2.5,
  nouveau §2.5 bis avec le tableau « quel mécanisme pour quel besoin », §7.1, §7.2, §7.3), skill
  `chat-scenario-writing` (règles 2 et 8, commande, vérification des fuites), `chat/scenario-builder/`
  (`ref-format.md`, `ref-commands.md`, `ref-writing-tips.md` avec la section « Leak check », `skeleton.md`,
  phases 2, 3, 4 et 5) et `specs/chat.md`. Le skill `chat-scenario-builder` n'a pas de contenu propre à changer
  (il renvoie au dossier).
- `What must happen` / `Ce qui doit se passer` disparaît du format enseigné : remplacé par une consigne par
  PNJ. Les scénarios existants ne sont pas migrés (le libellé est libre, rien ne casse).

## Résultat
Compilation et 656 tests du module chat verts. Un PNJ ne lit plus ce qui est réservé à un autre dans son
prompt ; les agents d'analyse et la vue 🐞 voient le texte brut. Aucun prompt d'agent ni de PNJ modifié.
