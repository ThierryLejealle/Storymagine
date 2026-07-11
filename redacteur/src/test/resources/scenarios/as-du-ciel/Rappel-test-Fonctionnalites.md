# Rappel — Test des fonctionnalités dans As du Ciel

Ce fichier liste chaque fonctionnalité du moteur testée manuellement via le scénario As du Ciel.
Pour chaque fonctionnalité : où elle est placée, ce qu'on attend, et comment vérifier dans le texte généré.

---

## 1. `plot_directives` — Faits d'intrigue déclaratifs

**Localisation dans le scénario**

| Chapitre | Ce qui est fait |
|---|---|
| `j2_j8` (Jours 2 à 8) | Ajout du fait : moteur Spitfire de Pierre claque en piqué |
| `j7` (Jour 7 — La Dernière Soirée) | Suppression du fait : Jules a réparé le moteur |

**Ce qu'on a ajouté**

```yaml
# Chapitre j2_j8
plot_directives:
  - "Moteur Spitfire Pierre → claquement sourd en piqué, légère perte de puissance à l'accélération (Jules au courant, en attente de pièce)"

# Chapitre j7
plot_directives:
  - "-Moteur Spitfire Pierre"
```

**Ce qu'on attend**

- Dans les séquences de `j2_j8` : le planificateur et le rédacteur reçoivent `[Faits d'intrigue] Moteur Spitfire Pierre : claquement sourd…` dans leur contexte. Le texte peut évoquer la nervosité de Pierre sur son moteur, le geste de Jules, le bruit en piqué.
- Dans `j7` : le fait a disparu de la mémoire. Le texte ne doit plus y faire allusion. Jules peut inspecter le moteur « en bon état ».
- Dans `j8` : aucune trace du problème moteur — il a été résolu la veille.

**Comment vérifier dans le texte généré**

1. Lire les séquences de `j2_j8` : chercher une allusion au moteur (bruit, perte de puissance, souci mécanique). Si le LLM n'en parle jamais sur 7 itérations, la directive n'a pas été lue.
2. Lire `j7` : aucune mention de problème moteur ne doit apparaître. Jules peut « contrôler le Merlin » mais sans signaler d'anomalie.
3. Lire `j8` : idem, le moteur est sain — pas de claquement.

---

## 2. `setting` — Cadre spatio-temporel

**Localisation dans le scénario**

| Chapitre | Ce qui est fait |
|---|---|
| `j1` (Jour 1 — Thorney Island) | `setting:` au niveau chapitre |

**Ce qu'on a ajouté**

```yaml
setting: "Aube du 6 juin 1944. Tarmac de la base de Thorney Island, Hampshire — brume légère, odeur d'huile et de carburant."
```

**Ce qu'on attend**

- Le planificateur (`ScenarioPlannerContext`) reçoit la ligne `Cadre : Aube du 6 juin 1944…` dans le bloc `### Chapitre à planifier`, avant la description.
- Le rédacteur (`WriterContext`) reçoit le bloc `[Cadre de la scène]` dans le user prompt, avant `### Séquence à écrire`.
- Si `plan_mode` ou `sequencePlanMode` est activé, le planificateur de séquence (`SequencePlannerContext`) reçoit aussi `Cadre : …` avant `### Séquence à planifier`.

**Comment vérifier dans le texte généré**

1. Le texte du chapitre `j1` doit démarrer dans un cadre crépusculaire/matinal sur le tarmac — brume, lumière rasante, odeur de carburant. Ces éléments doivent être présents sans que le LLM ait été explicitement guidé par la `description:` seule.
2. Ouvrir le fichier de log du planificateur (`j1_planner`) : vérifier que la ligne `Cadre :` y apparaît avant `Description :`.
3. Ouvrir le fichier de log du rédacteur (`j1_writer`) : vérifier que le bloc `[Cadre de la scène]` y apparaît.
4. Dans les logs `llm_calls/` : ouvrir un appel writer de `j2_j8` et chercher `[Faits d'intrigue]` dans le prompt — la directive doit y figurer. Ouvrir un appel de `j7` ou `j8` — la section doit être vide ou absente.

---

## 3. `type: foreach` — Boucle exhaustive sur un pool de personnages

Mécanisme : le chapitre itère sur **tous** les membres d'un pool dans l'ordre, une fois chacun.
Contrairement à `type: loop` (N tirages aléatoires), `foreach` garantit que chaque personnage
est traité exactement une fois. Les personnages suivants voient le journal des précédents.

**Localisation dans le scénario**

| Chapitre | Pool | Personnages | Fonctionnalités testées |
|---|---|---|---|
| `debriefing_j4` (Jour 4 — Debriefing) | `pilotes_actifs` | pierre_moreau, henri_leclair | `sequences`, `{{CHARACTER}}`, `character_directives` |

**Ce qu'on a ajouté dans `scenario.yaml`**

```yaml
- id: debriefing_j4
  title: "Jour 4 — Debriefing"
  type: foreach
  foreach:
    pool: "pilotes_actifs"
    context: >
      Compte-rendu oral du jour...
    character_directives:
      pierre_moreau: >
        Pierre est marqué par la mission d'escorte du Hurricane...
      henri_leclair: >
        Henri a passé la journée au sol, cloué par une panne hydraulique...
    sequences:
      - description: >
          {{CHARACTER}} entre dans la salle de debriefing...
```

**Ce qu'on attend**

- **Itération 1 — Pierre** : `{{CHARACTER}}` est remplacé par "pierre moreau" dans la description. La directive `character_directives["pierre_moreau"]` est injectée comme `[Contexte de la séquence]` — Pierre doit paraître plus lent qu'à l'habitude, marqué par l'escorte.
- **Itération 2 — Henri** : même mécanique. Henri doit exprimer (sans forcément le dire) sa frustration d'être resté au sol.
- **Journal inter-personnages** : après Pierre, une entrée de journal est créée. Henri sait donc ce que Pierre a rapporté — son compte-rendu peut s'y articuler implicitement.
- **Ordre garanti** : Pierre toujours avant Henri — pas de tirage aléatoire.

**Comment vérifier dans le texte généré**

1. **Deux scènes distinctes** : le chapitre doit produire exactement 2 séquences, une par pilote.
2. **Pierre** : chercher une lenteur, une retenue inhabituelle — la trace de l'escorte difficile.
3. **Henri** : chercher la frustration contenue d'un pilote qui n'a pas volé.
4. **`{{CHARACTER}}` résolu** : aucune occurrence littérale de `{{CHARACTER}}` dans le texte final.
5. **Logs `llm_calls/`** : chaque appel writer de `debriefing_j4` doit contenir `[Contexte de la séquence]` avec la directive correspondante (pas l'autre).
6. **Log master** : deux lignes `→ pierre moreau` et `→ henri leclair` avec les mots écrits respectifs.

---

## 5. Directives détaillées prioritaires — contraintes impératives dans la directive

Mécanisme : le Writer reçoit la directive de séquence sous `### Directives détaillées (prioritaires)`
et est explicitement instruit de les traiter comme prioritaires sur la trame narrative (plan LLM).
Les contraintes précises de l'auteur (atmosphère imposée, faits obligatoires, interdits) ne peuvent
plus être effacées par un plan qui les aurait omises ou reformulées.

**Localisation dans le scénario**

| Chapitre | Séquence | Contrainte testée |
|---|---|---|
| `chap_3` (Jour 6) | Attaque surprise au-dessus de Caen | Soleil au zénith — éblouissement impératif |

**Ce qu'on a ajouté dans `chap_3.yaml`**

```yaml
# TEST directives-imperatives
directive: >
  ...
  Il est midi — le soleil est au zénith. La lumière aveugle par instants, venant de face.
  Ce détail du soleil est impératif : il doit être présent et sensible dans la scène.
requirements:
  - "| Le soleil au zénith ou midi est explicitement présent dans la scène"
  - "| L'éblouissement ou la lumière frontale affecte Pierre pendant le combat"
```

**Ce qu'on attend**

- Le texte de la séquence doit mentionner le soleil au zénith ou midi.
- L'éblouissement doit être sensible dans la prose (geste, difficulté à viser, instinct de fermer les yeux).
- Même si le plan LLM généré par ScenarioPlannerContext n'a pas mentionné la lumière, le Writer doit respecter la directive.

**Comment vérifier dans le texte généré**

1. Lire la deuxième séquence du chapitre `chap_3` : chercher "zénith", "midi", "soleil", "aveug" (aveuglant, aveugle).
2. Le `SequenceChecker` doit reporter ✓ sur les deux checks — sinon la contrainte n'a pas été respectée.
3. **Logs `llm_calls/`** : ouvrir le prompt writer de la séquence — vérifier que la section s'appelle `### Directives détaillées (prioritaires)` (et non plus `### Séquence à écrire`).
4. Ouvrir le plan généré (`chap_3_planner`) : si le plan n'y mentionne pas la lumière et que le Writer en parle quand même → la directive a bien eu la priorité.

---

## 4. `## INTÉRIEUR` — État intérieur des personnages (jamais nommé dans le texte)

Mécanisme : section optionnelle dans `characters/[nom].md`, chargée par `CharacterLoader.loadInner()`.
Injectée sous `[État intérieur]` dans le bloc "Personnages présents" du rédacteur et du planificateur.
Consigne LLM : ces traits ne doivent jamais être nommés — ils transparaissent uniquement dans les
gestes, silences et réactions involontaires.

**Personnages concernés dans as-du-ciel**

| Personnage | Traits principaux dans `## INTÉRIEUR` |
|---|---|
| `pierre_moreau` | Déshumanisation progressive (anesthésie face aux pertes) ; lettres à Lucie jamais envoyées ; jambe gauche sous stress |
| `commandant_bertrand` | Attachement inavoué à Pierre ; rituel matinal compulsif (listes de vol) |
| `jules_meca` | Fébrilité mécanique après une perte ; fierté muette sur le Grey Ghost |

**Ce qu'on attend**

- Pierre ne doit **jamais** être décrit comme "insensible" ou "anesthésié" — mais il vérifie son altimètre pendant qu'un ailier s'écrase, il finit son café, il pense à autre chose.
- Bertrand ne doit **jamais** exprimer explicitement son attachement à Pierre — mais son regard s'attarde, il l'observe davantage que les autres.
- Jules ne doit **jamais** que ses blagues sont une fuite — mais après un avion perdu, ses mains ne s'arrêtent plus.

**Comment vérifier dans le texte généré**

1. Chercher dans les scènes de combat : Pierre ne réagit pas à la perte d'un ailier comme on l'attendrait — un geste neutre, une action banale, une pensée déplacée.
2. Chercher dans les scènes avec Bertrand : son regard sur Pierre est noté sans être nommé — une pause, une attention.
3. Chercher Jules après un avion non rentré : il bricole, il inspecte, il repeint. Pas de repos.
4. **Logs `llm_calls/`** : ouvrir un appel planner ou writer avec ces personnages — chercher `[État intérieur]` dans le prompt. Il doit y figurer avec les traits correspondants.

---

## 6. `type: interlude` — Pivot de perspective canonique

Mécanisme : chapitre canonique qui change de point de vue ou de lieu. La mémoire est enrichie normalement.
Un `bridge_back` (ou son équivalent auto-généré) est injecté dans la cohérence du chapitre suivant
pour faciliter le retour à la trame principale.

**Localisation dans le scénario**

| Fichier | Chapitre | Fonctionnalités testées |
|---|---|---|
| `chap_4.yaml` | Jour 6 — L'Autre Côté du Ciel | `type: interlude`, `perspective`, `timeline`, `bridge_back` |

**Ce qu'on attend**

- Le chapitre est rédigé du point de vue de Wolf (côté allemand) — pas de perspective britannique.
- La scène termine sur un état stable (Wolf range son rapport, éteint la lampe).
- Le résumé de l'interlude apparaît dans la mémoire (`storySoFar`) des chapitres suivants.
- Le chapitre suivant (`chap_5` ou `chap_6`) commence sans mentionner les Allemands explicitement.

**Comment vérifier dans le texte généré**

1. Lire `chap_4` : tout le texte doit être du point de vue allemand — aucune perspective britannique.
2. Lire le chapitre qui suit : vérifier l'absence de mention directe de Wolf ou des Allemands.
3. **Log master** : chercher la ligne `─── INTERLUDE — Du côté allemand` puis `↦ Bridge d'interlude injecté` au chapitre suivant.
4. **Log planner du chapitre suivant** : le bloc `État de cohérence` doit contenir `[Transition après interlude]`.

---

## 7. `type: what_if` — Digression alternative non-canonique

Mécanisme : chapitre isolé (snapshot mémoire avant / restore après). Le planificateur est briefé
sur la nature divergente. `freedom` calibre la liberté narrative. `opening_frame` et `closing_frame`
encadrent la digression pour le lecteur.

**Localisation dans le scénario**

| Fichier | Chapitre | Fonctionnalités testées |
|---|---|---|
| `chap_5.yaml` | Et si... La Mitrailleuse | `type: what_if`, `freedom: moderate`, `opening_frame`, `closing_frame` |

**Ce qu'on attend**

- Le texte commence exactement par : *«Et si, ce matin-là, la mitrailleuse de Pierre s'était enraillée ?»*
- Le texte se termine exactement par : *«Mais ce n'est pas ce qui arriva.»*
- Les personnages agissent de façon cohérente avec leurs caractères.
- Les lois physiques restent réalistes (Spitfire MkIX de 1944).
- La mémoire est restaurée après ce chapitre — les faits du what-if n'apparaissent plus dans les chapitres suivants.

**Comment vérifier dans le texte généré**

1. Lire le début de `chap_5` : la première phrase doit correspondre à l'`opening_frame`.
2. Lire la fin de `chap_5` : la dernière phrase doit correspondre au `closing_frame`.
3. Lire `chap_6` (chapitre suivant) : aucune trace du what-if — Pierre n'a pas eu de mitrailleuse enraillée dans la trame principale.
4. **Log master** : chercher `─── WHAT-IF [moderate]` puis `↩ Mémoire restaurée`.
5. **Log writer** des séquences : la première directive doit commencer par `[Entrée dans la digression]` (si `opening_frame` est renseignée, `[CADRAGE D'OUVERTURE — OBLIGATOIRE]`).

---

## 9. `sequence_style_check` — Critique de style opérationnel par séquence

Mécanisme : après chaque séquence, `SequenceStyleCheckerContext` évalue style + qualité prose
avec une grille exigeante (scale 1-10). Si score < seuil, une note stylistique est injectée en
contrainte et le Writer réécrit la séquence. Activé par `sequence_style_check: true`.

**Localisation dans le scénario**

`scenario.md` — paramètre global : `sequence_style_check: true`

Le seuil utilisé est `style_check_threshold` s'il est défini, sinon 7 (défaut moteur).

**Ce qu'on a ajouté**

```yaml
# TEST sequence_style_check
sequence_style_check: true
```

**Ce qu'on attend**

- Après chaque séquence écrite (post-DeusInMachina), le moteur appelle `SequenceStyleCheckerContext`.
- Le log master affiche : `✦ Style seq.N : score/10  ✓ OK` ou `✗ → réécriture style`.
- En cas de score insuffisant : la séquence est réécrite avec une note `[CORRECTIONS STYLISTIQUES : ...]` en contrainte.
- Les défauts typiques signalés : verbes faibles (être/avoir/faire), constructions nominalisées,
  phrases catalogues, clichés, expressions fabriquées.

**Comment vérifier dans le texte généré**

1. **Log master** : chercher les lignes `✦ Style seq.N :` pour chaque séquence de chaque chapitre.
   Vérifier la présence du score et du statut (✓ ou ✗).
2. **En cas de ✗** : chercher deux versions consécutives de la séquence dans les logs — la deuxième
   doit avoir la note `[CORRECTIONS STYLISTIQUES]` dans son prompt writer.
3. **Qualité du texte** : les séquences passées au filtre de style doivent avoir moins de "était",
   de noms déverbaux, et de phrases en liste. Plus de verbes d'action concrets.
4. **Comparaison avec/sans** : désactiver (`sequence_style_check: false`), regénérer le même chapitre
   et comparer les deux textes — le texte avec filtre doit être plus nerveux, plus direct.

---

## 8. `type: dream` — Rêve d'un personnage, non-canonique

Mécanisme : chapitre isolé (snapshot / restore). Contraintes levées. `realism_level` calibre
le critique et les directives. `memory_trace` stocke uniquement le fait de rêver (pas le contenu).

**Localisation dans le scénario**

| Fichier | Chapitre | Fonctionnalités testées |
|---|---|---|
| `chap_7.yaml` | Le Rêve de la Paix | `type: dream`, `realism_level: symbolic`, `memory_trace`, `dreamer` |

**Ce qu'on attend**

- Le texte est onirique et symbolique (pas réaliste, pas surréaliste pur).
- Pierre vole sans appareil — le corps seul dans un ciel impossible.
- Lucie est présente sans dialogue explicite.
- La dernière séquence dissolve progressivement le rêve vers la conscience.
- Après le chapitre : la mémoire est restaurée, SAUF la `memory_trace` — le chapitre suivant peut voir que Pierre a eu une nuit agitée.

**Comment vérifier dans le texte généré**

1. Lire `chap_7` : le texte doit être clairement onirique. Aucun élément mécanique (moteur, cockpit, radio) dans la première séquence.
2. Lire la fin : la dissolution vers le réveil doit être progressive et sensible.
3. Lire `chap_8` (La Chute) : le planificateur et le rédacteur reçoivent la `memory_trace` dans `storySoFar` — le briefing ou les premiers gestes de Pierre peuvent porter une fatigue ou une légèreté inhabituelle.
4. **Log master** : chercher `─── RÊVE [symbolic]`, puis la ligne `↦ Trace mémorielle du rêve : «Pierre a rêvé…»` après le restore.
5. **Critique onirique** : le log critique (`chap_7_critic`) doit évaluer la puissance symbolique — pas la cohérence avec la trame principale.
