# 2026-07-09 23h19 — Dérive de contenu dans la chaîne de correcteurs (Write) : diagnostic, tests et pistes

## Synthèse globale (à lire en premier)

**Le problème** : dans la phase Write, un texte correct produit par le Writer peut ressortir dégradé
(mots perdus, sens inversé) après être passé par la chaîne de 4 correcteurs — observé une fois en
production (144→66 mots, un beat inversé), rattrapé par le critique final mais au prix d'un gaspillage
important (9 appels + relance complète). Cause racine : les correcteurs à critères "ouverts" (Style,
Naturality) se relancent sur leur propre sortie déjà réécrite jusqu'à 3 fois, sans jamais converger, et
les pertes se composent passe après passe (détail en §2).

**Ce qu'on a appris en testant, dans l'ordre des surprises :**
1. Un vérificateur LLM qui relit chaque correction proposée détecte **100% des vraies dérives** — mais
   sans connaître le mandat du correcteur d'origine, il **sur-rejette** aussi les bonnes corrections
   (précision aussi basse que 20% en mode rapide) : il ne sait pas distinguer "on a coupé un enjeu" de
   "on a coupé un adjectif de remplissage voulu". C'est le vrai verrou à lever avant de le déployer (§4.1, §6.3).
2. La réflexion (`think`) du modèle améliore nettement ce jugement (précision ×3) mais coûte 7 à 17×
   plus cher en temps/tokens, et peut ne jamais aboutir dans un budget raisonnable (§4.2).
3. Un bug d'enregistrement Ollama (pas propre à gemma4, lié aux imports `hf.co/` directs) empêchait
   d'activer la réflexion explicitement sur certains modèles — contourné sans appel supplémentaire en
   s'appuyant sur une info déjà disponible au démarrage (§9), plus une règle spécifique pour toujours
   considérer gemma4 comme "thinking" indépendamment de ce que déclare Ollama (§11.1).
4. Regrouper plusieurs vérifications dans un seul appel (par lots de ~5) double la vitesse à précision
   égale, sans perte de fiabilité constatée à cette taille (§11.2).

**Ce qui est réellement codé à ce stade** (tout compile, rien n'est câblé sur les agents existants) :
- `LlmCallContext.think` + `OllamaAdapter` : un agent peut demander une préférence de réflexion par appel (§8).
- `OllamaAdapter.resolveThink()` : neutralise automatiquement un `think=true` que le modèle rejetterait,
  sans appel supplémentaire (§9).
- `OllamaAdapter.supportsThinking()` : toujours vrai pour la famille gemma4, quoi qu'en dise Ollama (§11.1).
- Un warning (non bloquant) à la validation du scénario si `min_words` d'une séquence est trop bas.

**Ce qui n'est PAS fait, à trancher demain** (voir §7 et la liste de reprise en fin de fiche) : le câblage
`think` par agent, le garde-fou textuel dans les 4 prompts (delta à valider), le vérificateur de paires
lui-même (nom du concept, mandat par correcteur), les préfiltres mécaniques, et surtout — nouveau chantier
proposé pour la suite — une **analyse correcteur par correcteur** (et plus largement agent par agent) pour
décider ce qui s'applique à qui.

## 1. Le problème observé

En analysant un log réel (`SequencePlanFidelityCritic`, séquence "les ruelles de Collioure", run du
2026-07-09 21h17), le texte évalué par le critique final ne correspondait plus à ce que le Writer avait
produit :

- **Writer (appel 14)** : texte correct, ~144 mots, couvrant fidèlement les 7 beats du plan.
- **Texte reçu par le critique final**, après 9 passes de correcteurs (3× DeusInMachinaCorrector,
  3× NaturalityCorrector, 3× StyleCorrector, 1× ProofreaderCorrector) : ~66 mots (**-54 %**), et un
  contresens introduit — le beat *"Thierry fixe Christelle avec une attention nouvelle"* était devenu
  *"Christelle le regarda"* (sujet et objet inversés). Le détail *"un détail architectural"* avait
  également disparu.

Heureusement, `PlanFidelityCritic` a détecté les deux anomalies et déclenché une relance complète du
Writer (`SequenceWriter appel 15`) — le système s'est auto-corrigé, mais au prix de 9 appels LLM
gaspillés + une relance intégrale.

## 2. Diagnostic — cause racine

`WriteWorkflow.applyCorrectorsPhase()` (lignes 273-358) relance chaque correcteur jusqu'à 3 fois sur
**sa propre sortie déjà patchée** si le ratio `corrections/mots` dépasse un seuil fixe
(`CorrectorConfig.defaults()` : 0.010 corr/mot, soit 1 correction pour 100 mots, `minCorrectionsForRetry=7`,
`maxRetryPasses=3`).

Deux familles de correcteurs se comportent très différemment face à ce mécanisme :

- **Critères fermés** (ProofreaderCorrector, en partie DeusInMachinaCorrector) : l'ensemble des fautes
  est fini. La relance a du sens — on doit voir une décroissance du nombre de corrections à chaque passe.
- **Critères ouverts** (StyleCorrector, NaturalityCorrector) : leurs consignes ("tout ce qui peut être
  amélioré, **même mineur**") définissent un puits sans fond par construction. Rien ne converge jamais.
  Chaque passe réécrit le texte déjà réécrit par la précédente → les pertes se composent.

Un garde-fou textuel existe déjà dans NaturalityCorrector (*"conserve le sens, le rythme et les
informations"*) mais n'a pas empêché la dérive : ce n'est pas un problème de formulation, c'est
structurel — le même appel qui vient de décider "ceci est un défaut" a peu de recul pour s'auto-auditer
dans la foulée.

## 3. Impacts

### Impacts du problème tel quel (statu quo)

- **Qualité silencieuse** : le filet de sécurité (`PlanFidelityCritic` + critiques de séquence) n'a
  fonctionné qu'une fois sur ce cas — rien ne garantit qu'il attrape systématiquement une dérive de
  *nuance* (contrairement à une suppression totale de beat, un glissement de sens fin peut passer sous
  le radar d'un critique qui raisonne au niveau des beats, pas des phrases).
- **Coût gaspillé** : 9 appels de correcteurs + 1 relance complète du Writer pour un résultat qui aurait
  dû être bon du premier coup. Sur un livre complet (~25+ séquences), ce gaspillage se multiplie.
- **Risque en usage réel** : le cas analysé utilisait gemma 4B ("E2B", modèle de test de la mécanique).
  Un modèle plus gros (cible réelle ~12-26B) réduit probablement la fréquence des erreurs de fond, mais
  ne supprime pas le mécanisme de boucle non convergente pour les critères ouverts (Style/Naturality) —
  voir §5.

### Impacts des pistes de correction envisagées (résumé, détail en §6)

- **Limiter les relances** : gratuit, réduit le nombre d'appels, mais un correcteur en passe unique peut
  laisser passer une vraie faute que 2-3 passes auraient trouvée pour les critères fermés (Proofreader) —
  d'où la distinction fermé/ouvert plutôt qu'une règle uniforme.
- **Garde-fou textuel dans les prompts** : quasi gratuit, mais c'est une modification de prompt LLM →
  soumise à validation explicite (règle projet). Risque : une formulation mal choisie peut contredire la
  mission même du correcteur (ex. "ne réduit pas la richesse des détails" contredirait StyleCorrector,
  dont le rôle est justement de couper les adjectifs de remplissage).
- **Vérificateur LLM par paire** : voir §4, impact mesuré empiriquement — coût et fiabilité réels
  documentés, avec une découverte majeure sur la précision.
- **Couper/paramétrer la réflexion (`think`)** : impact direct sur la latence et le coût token, mais
  impact **négatif sur la précision** observé empiriquement (voir §4) — pas un gain pur et simple.
- **Impact architectural commun à toutes les pistes "verrou par agent"** : `OllamaAdapter`/`OllamaConfig`
  fixent aujourd'hui `think` une fois pour tout le pipeline (un seul adaptateur partagé). Le faire varier
  par agent demande de faire remonter un paramètre par appel (ex. via `LlmCallContext`) plutôt qu'à la
  construction de l'adaptateur — changement scopé mais réel, à traiter comme une évolution de port/capacité
  au sens du hexagonal (cf. règle projet "un port explicite pour les capacités additionnelles").

## 4. Ce qu'on a appris en testant (empirique, gemma-4-26B Q6_K via Ollama)

### 4.1 Le vérificateur de paires FAUX/JUSTE — proof of concept

Prompt testé : donner UNE paire (phrase originale, phrase corrigée) et demander un verdict
`ACCEPTE`/`REJETTE` + motif court, sans connaître le correcteur d'origine ni son mandat.

**Rappel (détection des vraies dérives) : 10/10 = 100%** sur l'échantillon risqué (inversions
sujet/objet, pertes d'enjeu, compressions de type "liste narrativisée", inversions de sens). Les
motifs retournés étaient précis et correctement argumentés à chaque fois.

**Précision (ne pas bloquer les bonnes corrections) : très dépendante du mode de réflexion** — voir 4.2.

### 4.2 `think=true` vs `think=false` — résultat contre-intuitif

Benchmark : 20 paires (10 sûres attendant `ACCEPTE`, 10 risquées attendant `REJETTE`) × 2 modes = 40
appels réels sur `gemma-4-26B-A4B-it-heretic-i1-GGUF:Q6_K` (Ollama natif `/api/chat`, température 0.2).

| Mode | Résultat |
|---|---|
| `think=true` | **Erreur systématique (HTTP 400)** : `"...Q6_K" does not support thinking`. Ce build communautaire n'est pas tagué "thinking" dans les métadonnées Ollama — le paramètre est purement et simplement rejeté. |
| `think=false` (20/20 réussis) | 100% de rappel (10/10 cas risqués), mais **20% de précision seulement (2/10 cas sûrs)** — 8 corrections légitimes rejetées à tort, dont l'exemple donné dans le prompt même de NaturalityCorrector comme bonne pratique ("Elle noua son foulard dans un geste protecteur et deliberement visible" → "Elle noua son foulard"). Rapide (~1080ms, ~33 tokens de réponse) et fiable (zéro troncature). |
| Réflexion "naturelle" (paramètre `think` absent, testé plus tôt via MCP) | Précision bien meilleure sur les cas complétés (3/5 = 60%), mais coût 10-30× supérieur en tokens (150-900+), et **troncature fréquente** — plusieurs réponses n'ont jamais atteint le verdict formaté même à 600-900 tokens de budget. |

**Conclusion contre-intuitive et importante** : couper la réflexion rend le vérificateur rapide et fiable
à exécuter, mais nettement **moins bon juge** — sans le raisonnement, le modèle applique un critère
maximaliste "rien ne doit jamais disparaître", incompatible avec la mission même des correcteurs de
style/naturel. La réflexion, quand elle aboutit, améliore nettement le jugement — mais son coût et son
imprévisibilité (troncature) la rendent difficile à budgétiser.

### 4.3 Mécanisme exact du `think` côté Ollama (clarifié après investigation complémentaire)

Point clarifié après-coup : Ollama sépare bien la réflexion dans un champ dédié (`message.thinking` sur
l'endpoint natif `/api/chat`, `message.reasoning` sur l'endpoint compatible OpenAI `/v1/chat/completions`)
— elle n'est **pas mélangée dans `content`**. Le comportement observé plus tôt (blocs `<think>...</think>`
visibles dans la sortie testée via l'outil MCP) vient très probablement de la concaténation faite par cet
outil de test (`thinking` + `content` affichés ensemble), pas d'un dérapage du modèle.

Testé et confirmé, sur les **deux** endpoints Ollama (natif et compatible OpenAI) :

| Requête | Natif `/api/chat` | Compatible OpenAI `/v1/chat/completions` |
|---|---|---|
| Rien précisé (défaut) | Réflexion activée (`message.thinking` rempli, ~150-230 tokens) | Idem (`message.reasoning` rempli) |
| Désactiver (`think:false` / `reasoning_effort:"none"`) | ✅ Fonctionne — réponse propre, ~10 tokens | ✅ Fonctionne, résultat identique |
| Activer/graduer explicitement (`think:true` / `reasoning_effort:"high"`) | ❌ HTTP 400 : `"...does not support thinking"` | ❌ **Erreur strictement identique** |

Le fait que l'erreur soit mot pour mot identique sur les deux endpoints confirme qu'il s'agit du même
verrou interne Ollama (métadonnées de capacité "thinking" absentes pour ce modèle importé via `hf.co/...`),
pas d'un problème d'endpoint ni d'un tag de prompt à ajouter (hypothèse testée explicitement — un tag
`<|think|>` inséré manuellement en tête du system prompt n'a produit aucune différence mesurable, sortie
strictement identique avec ou sans).

**Conséquence pratique, plutôt positive** : ce verrou ne bloque aucun besoin identifié au §6.4 — le
comportement par défaut (rien précisé) EST "réflexion activée", et "désactiver explicitement" fonctionne
très bien sur les deux endpoints. Le seul cas non couvrable avec ce modèle précis serait un réglage
**gradué** (`"low"/"medium"`) — non requis par les recommandations actuelles. Si un besoin de gradation
apparaît plus tard, il faudra ré-importer le modèle dans Ollama avec les métadonnées de capacité
correctement renseignées (ou utiliser un modèle packagé nativement, pas via `hf.co/...` brut).

**Précision importante après test croisé sur plusieurs variantes** : le verrou n'est **pas propre à
gemma4 en tant que famille**, ni à ce build Q6_K précis — c'est l'**import via `hf.co/...`** qui semble
sauter l'enregistrement de la capacité "thinking" dans Ollama, quel que soit le modèle. Testé sur 5
variantes :

| Modèle | Import | `think=true` |
|---|---|---|
| `gemma4:e2b` | local (pull direct) | ✅ OK |
| `gemma4-4B-100k:latest` | local | ✅ OK |
| `gemma4-64k:latest` | local | ✅ OK |
| `igorls/gemma-4-12B-it-qat-q4_0-unquantized-heretic:latest` | local (sans préfixe `hf.co/`) | ✅ OK |
| `hf.co/unsloth/gemma-4-E2B-it-qat-GGUF:UD-Q4_K_XL` | `hf.co/` direct | ❌ même erreur |
| `hf.co/mradermacher/gemma-4-26B-...-Q6_K` (testé initialement) | `hf.co/` direct | ❌ même erreur |

**Comparatif de coût `think=true` vs `false`, mesuré proprement (appels isolés, hors contention) sur deux
familles de modèle correctement enregistrées :**

| Modèle | `think=true` (régime stable) | `think=false` | Facteur |
|---|---|---|---|
| `qwen3.5:9b` | ~5 160 ms, 335 tokens réponse | ~300 ms, 4 tokens | ~17× |
| `gemma-4-12B` (`igorls/...heretic`) | ~3 770 ms, ~160 tokens réponse | ~520 ms, 10 tokens | ~7× |

Confirme, sur un gemma4 correctement enregistré, le même schéma de surcoût massif de la réflexion que
sur Qwen — la conclusion du §4.2 (rappel excellent mais précision fragile sans réflexion, coût élevé
avec) n'est donc pas un artefact du build cassé, elle tient pour gemma4 en général.

### 4.4 Découverte annexe : le levier `think` existe déjà dans le code

`OllamaAdapter`/`OllamaRequest.java:16` (`public Boolean think`) sait déjà envoyer ce paramètre à
Ollama — mais il est fixé **une fois pour tout le pipeline** (un seul adaptateur partagé par tous les
agents), pas par agent. Rien n'empêche de l'étendre par agent : au vu du §4.3, un simple booléen
(`true`/absent = activé, `false` = désactivé) suffit à couvrir tous les cas identifiés au §6.4, sans
attendre un réglage à niveaux.

## 5. Pistes envisagées (synthèse des consultations + tests)

Deux consultations externes (Claude Fable 5) ont été menées en cours de route, en plus des tests
empiriques ci-dessus. Récapitulatif des pistes :

| Piste | Avis | Statut |
|---|---|---|
| 1. Politique de relance différenciée : passe unique pour Style/Naturality (critères ouverts), relance à critère de décroissance pour Proofreader/DeusInMachina (critères fermés) | Confirmé par Fable comme le levier le plus rentable — coupe la cause racine, gratuit en appels | **Mis en TODO** par l'auteur du projet (le rôle anti-"saveur IA" de Naturality est jugé trop important pour le limiter sans plus de recul) |
| 2. Garde-fou textuel générique ("ne change jamais le sens et ne perds aucune information ou action mentionnée") ajouté aux 4 prompts | Validé comme utile ("c'est top") — nécessaire mais pas suffisant seul (cf. §2, limite structurelle de l'auto-audit) | **Approuvé dans son principe**, delta exact à rédiger et valider avant écriture (règle projet) |
| 3. Préfiltres mécaniques gratuits (ratio de perte de mots, changement de l'ensemble des noms propres) | Le ratio de mots est simple en Java ; la détection d'inversion de noms propres est faisable (comparer la séquence des mots à majuscule hors début de phrase) mais heuristique, pas garantie | Le sous-cas "fusion de 4 phrases en 1" (règle 4 DeusInMachina) est mis en **TODO : suppression de cette règle** |
| 4. Vérificateur LLM par paire | Rappel excellent (100% sur l'échantillon), mais précision fragile sans connaître le mandat du correcteur d'origine, et coût/fiabilité très sensibles au mode de réflexion (§4.2) | **Prouvé utile mais pas déployable tel quel** — voir proposition §6 |

## 6. Proposition de solution mixte

Aucune des 4 pistes prises isolément n'est satisfaisante : la relance différenciée seule ne rattrape pas
une dérive de nuance qui survient dès la première passe ; le garde-fou textuel seul a déjà montré ses
limites structurelles ; le vérificateur seul, sans contexte, casse le travail légitime des correcteurs.
Proposition combinée, priorisée par rapport coût/bénéfice :

### 6.1 Garde-fou textuel (tous les correcteurs) — le plus simple
Ajouter aux 4 prompts la formulation validée ("ne change jamais le sens et ne perds aucune information
ou action mentionnée"), adaptée pour ne pas contredire la mission de chaque correcteur (StyleCorrector
en particulier : reformuler en "ne supprime pas d'information factuelle ou d'action", pas "richesse des
détails"). **Delta exact à soumettre en validation avant écriture.**

### 6.2 Relance ciblée, pas juste différenciée
Au-delà du simple "passe unique vs relance classique" (point 1, en TODO), une relance plus fine
consisterait à ne jamais redemander à un correcteur de rescanner tout le texte : ne lui representer que
les phrases déjà touchées lors de la passe précédente (si on relance), pas l'intégralité — réduit le
risque qu'une deuxième passe se remette à retoucher des phrases déjà correctes qu'elle n'avait pas
signalées la première fois. Nécessite de faire remonter, entre deux passes, la liste des phrases déjà
patchées plutôt que de re-soumettre tout le texte brut.

### 6.3 Vérificateur LLM — seulement pour certains agents, et seulement sur les paires suspectes
Ne pas vérifier systématiquement toutes les paires (coût, et sur-rejet démontré en §4.2). Design proposé :
1. **Préfiltre mécanique gratuit d'abord** (§5, piste 3) : ratio de perte de mots + heuristique noms
   propres. La grande majorité des corrections (fautes d'accord, reformulations mineures) passent sans
   jamais toucher un LLM.
2. **Vérificateur LLM seulement sur les paires qui échouent le préfiltre**, et **seulement pour les
   correcteurs à risque intrinsèque élevé** — d'après l'analyse par agent (Fable + révision propre,
   fiche précédente) : **NaturalityCorrector** en priorité (risque "élevé" — ses cibles sont souvent la
   seule occurrence d'une idée), et éventuellement DeusInMachinaCorrector uniquement sur sa règle 4
   (fusion de phrases). Pas de vérificateur sur Proofreader ni Style, dont le risque intrinsèque est
   plus faible et où le coût ne se justifie pas.
3. **Le vérificateur doit connaître le mandat du correcteur d'origine** (ex. "ce correcteur a le droit de
   couper des adjectifs/formulations artificielles, mais jamais une action ou un enjeu") — c'est la
   correction directe de la cause de sur-rejet observée en §4.2, pas encore testée empiriquement.

### 6.4 Sur le paramétrage de la réflexion (`think`) par agent

Concept à garder en tête pour quand un modèle correctement tagué "thinking" sera utilisé (le build
testé ici ne le permet pas). Faute de comparaison `true` vs `false` propre sur ce modèle, le
raisonnement se fait sur la nature de la tâche de chaque agent :

| Agent | Réflexion utile ? | Recommandation |
|---|---|---|
| **ProofreaderCorrector** | Non — tâche fermée et mécanique (grammaire/orthographe), l'intuition de l'auteur du projet est confirmée : réfléchir longuement sur une faute d'accord n'apporte rien et ne fait que gonfler le coût sans gain de qualité attendu | `think=false` (ou effort minimal) |
| **DeusInMachinaCorrector** | Faible à moyen — 5 règles assez mécaniques/pattern-matching, sauf la règle 4 (fusion) qui demande un peu plus de jugement | `think=false`, sauf si on isole un appel dédié pour la règle 4 |
| **StyleCorrector** | Moyen — jugement sur l'intensité/la nuance d'un verbe, mais tâche locale et bornée | `think=false` par défaut, à réévaluer si la précision déçoit en usage réel |
| **NaturalityCorrector** | **Oui, nettement** — c'est précisément le type de jugement (une phrase porte-t-elle une info unique ou est-elle réellement superflue ?) où le §4.2 montre que la réflexion change la précision du simple au triple | `think=true` / effort medium-high si le modèle le permet |
| **PlanFidelityCritic et critiques similaires** (comparaison texte ↔ beats) | Oui — la vérification "chaque beat est-il couvert" bénéficie d'un raisonnement pas-à-pas | `think=true` / effort medium |
| **Vérificateur de paires (§6.3), s'il est implémenté** | Oui, d'après §4.2 — mais avec un raisonnement borné plutôt que libre (ex. consigne "analyse en 1-2 phrases avant de répondre" dans le prompt, plutôt que le mode "thinking" libre du modèle qui peut dériver jusqu'à 900+ tokens) | Réflexion **guidée et bornée dans le prompt**, pas le mode `think` du modèle |

**Prérequis technique avant d'exploiter ce tableau** : faire varier `think` par agent demande de sortir
ce paramètre de la construction unique de `OllamaAdapter` pour le faire porter par l'appel (probablement
via `LlmCallContext`, qui transporte déjà le nom/label de l'agent) — évolution d'architecture à valider
séparément, indépendante du choix de valeurs ci-dessus.

**TODO ajouté** : ce tableau ne couvre que les 4 correcteurs + PlanFidelityCritic. Il reste à se poser la
question pour **tous les agents du pipeline** (Writer, Planner, autres Critics de plan/chapitre/livre,
Extractor, Tracker, Filter...) — est-ce que la réflexion leur apporte quelque chose, ou est-ce juste du
coût ajouté ? Probablement une bonne partie de l'analyse "correcteur par correcteur" prévue pour demain.

## 7. Ce qui reste à trancher avant tout code

- Le delta exact du garde-fou textuel (§6.1) — à présenter avec l'ancien texte, le nouveau texte, et la
  justification, par prompt, avant écriture (règle projet).
- Le nom du concept métier pour le vérificateur de paires (§6.3) — aucun suffixe existant
  (`Critic`/`Corrector`/`Planner`/`Extractor`/`Tracker`/`Filter`) ne correspond exactement à "juge une
  paire avant/après" ; à valider avant la moindre ligne de code, avec le mandat par-correcteur qu'il
  devra recevoir en entrée.
- Le mécanisme de retransmission `think` par agent (§6.4) — extension de `LlmCallContext`/`OllamaAdapter`
  à concevoir comme une évolution de port explicite. Point clarifié en §4.3 : un simple booléen suffit,
  pas besoin de gradation — seul `think=false` (désactivation explicite) demande d'être transmis par
  appel, le cas "activé" correspondant au comportement par défaut du modèle quand rien n'est précisé.

## 8. Réalisé — capacité `think` par appel dans le port/adaptateur (2026-07-09)

Le mécanisme de transport (dernier point du §7) est implémenté et compile proprement
(`mvn test-compile` sur `commun`, `redacteur`, `testllm`) :

- **`LlmCallContext`** (`commun/coeur/ports/LlmCallContext.java`) : nouveau champ `Boolean think`
  (null = pas de préférence, hérite du réglage par défaut de l'adaptateur). Nouvelle méthode
  `withThink(Boolean)` pour qu'un agent demande une préférence explicite sur son appel. Les factories
  `of(...)` existantes sont inchangées côté appelant (think=null par défaut) — aucun des ~35 agents
  existants n'a eu besoin d'être modifié.
- **`OllamaAdapter`** : le paramètre `think` de `LlmCallContext` est propagé à travers
  `generate → generateInternal → sendSync/sendStreaming → buildOllamaRequest` et prime sur le réglage
  par défaut de l'adaptateur quand il est non-null.
- **Bug latent corrigé au passage** : `buildOllamaRequest` faisait `req.think = think ? TRUE : null` —
  le cas "désactivé" au niveau pipeline envoyait un champ absent (`null`), pas `false` explicite. Or
  d'après §4.3, un champ absent égale "réflexion activée par défaut" côté Ollama pour un modèle qui la
  supporte : le réglage pipeline `think=false` n'avait donc jamais réellement désactivé la réflexion.
  Corrigé en `thinkOverride != null ? thinkOverride : (think ? TRUE : FALSE)` — `false` est maintenant
  toujours envoyé explicitement.

**Pas encore fait** : aucun agent n'appelle `withThink(...)` pour l'instant — le tableau de
recommandations du §6.4 (ex. `false` pour ProofreaderCorrector/StyleCorrector/DeusInMachinaCorrector,
laisser par défaut pour NaturalityCorrector) n'est pas câblé. C'est un choix par agent, à valider
séparément avant de modifier chaque classe.

## 9. Réalisé — neutralisation automatique de `think=true` non supporté (2026-07-09)

Suite logique du §8, après une tentative de correction d'import Ollama qui s'est avérée coûteuse et peu
fiable (voir §10) : plutôt que de réparer chaque modèle en amont, `OllamaAdapter` **détecte et s'adapte
tout seul** au démarrage.

`ModelLifecyclePort.probe()` (déjà appelé une fois par génération, `RedacteurCli.java:214`) alimente
déjà `cachedModelInfo.supportsThinking` via `/api/show` → `capabilities[]`. Ce champ s'est révélé fiable
empiriquement pour prédire si `think=true` explicite va échouer (il valait `false` exactement sur le
modèle cassé testé en §4.3-4.5). Nouvelle méthode `OllamaAdapter.resolveThink(Boolean wanted)` :
si un appel demande `TRUE` explicite mais que le modèle probé ne supporte pas l'activation ("thinking"
absent de `capabilities[]`), le champ est silencieusement redescendu à `null` (comportement par défaut,
déjà "activé" pour ce genre de modèle) au lieu de laisser planter la requête en HTTP 400.

**Avantage sur l'approche §10** : zéro appel LLM supplémentaire (réutilise l'info déjà récupérée par
`probe()`), zéro dépendance à un `ollama create` qui s'est montré peu fiable sur un gros modèle, et ça
s'adapte automatiquement à N'IMPORTE QUEL modèle gemma4 (ou autre famille) sans configuration par tag.
Compile proprement (`mvn test-compile` sur `commun`, `redacteur`, `testllm`).

## 10. Abandonné (pour l'instant) — correction d'import via Modelfile custom, gardé en TODO

Piste explorée avant le §9 : réparer l'enregistrement Ollama d'un modèle importé via `hf.co/...` en
recréant un tag local avec un Modelfile correct.

**Diagnostic réalisé** : comparaison de `ollama show --modelfile` entre un import cassé et un import qui
fonctionne. Le modèle qui marche utilise les directives natives `RENDERER gemma4` / `PARSER gemma4` +
`TEMPLATE {{ .Prompt }}`, alors que l'import `hf.co/` cassé se retrouve avec un template Jinja brut extrait
des métadonnées GGUF, sans passer par le moteur natif gemma4 d'Ollama (qui sait gérer nativement la
séparation `thinking`/`content`). `/api/show` confirme : `capabilities: ["tools", "completion"]` sur le
modèle cassé — "thinking" absent.

**Tentative de correctif** : Modelfile avec `FROM <chemin du blob existant>` + `TEMPLATE {{ .Prompt }}` +
`RENDERER gemma4` + `PARSER gemma4`, puis `ollama create fix-<nom-origine> -f Modelfile` — pensé pour ne
dupliquer aucun poids (stockage Ollama adressé par contenu : un nouveau tag ne crée qu'un petit manifeste
+ template, pointant vers le même blob sha256 que l'original ; supprimer le tag d'origine ne libère pas
ce blob tant que le tag corrigé le référence).

**Résultat** : `ollama create` sur le blob de 26 Go (Q6_K) s'est bloqué dans une boucle
"parsing GGUF / verifying conversion" sans jamais aboutir (échec après plusieurs minutes, sans message
d'erreur exploitable dans la sortie capturée). Non concluant — possiblement un souci propre à la taille
du blob ou à la reconstruction des métadonnées GGUF sans le manifeste d'origine.

**TODO à garder** : la piste reste valide en théorie (mécanisme de stockage par contenu confirmé, cause
racine bien identifiée) et pourrait valoir la peine d'être retentée sur un modèle plus petit pour valider
la mécanique avant de s'attaquer à un gros blob, ou en investiguant pourquoi `ollama create` boucle sur
la vérification. Non prioritaire depuis la solution §9, qui couvre le besoin sans cette manip.

## 11. Réalisé — gemma4 toujours considéré "thinking", + test de groupage des vérifications

### 11.1 `supportsThinking()` : gemma4 prime sur la déclaration Ollama

Consigne de l'auteur du projet : peu importe ce que déclare `/api/show`, la famille gemma4 doit toujours
être considérée comme "thinking" (elle réfléchit nativement par défaut, cf. §4.3 — seule l'activation
*explicite* via l'API est parfois cassée sur les imports `hf.co/`). Implémenté :

- `OllamaAdapter.supportsThinking()` (méthode publique, utilisée notamment par l'affichage console de
  testllm) renvoie désormais `true` dès que `cachedModelInfo.family` vaut `"gemma4"` (vérifié
  empiriquement : `details.family = "gemma4"` sur le modèle testé), sans regarder le champ brut.
- **`resolveThink()` n'est pas affecté** — il continue de lire directement `cachedModelInfo.supportsThinking`
  (le champ brut, pas la méthode publique), donc le garde-fou technique anti-HTTP 400 reste correct :
  on ne renvoie jamais `true` littéralement à un import qu'Ollama rejetterait, override de croyance ou pas.
- Confirmé qu'aucune autre partie du code ne consomme `supportsThinking()` de façon comportementale (seul
  `ConsoleRunLogger` l'affiche, à titre informatif) — le changement est donc sans risque de régression.
- Confirmé également, à la demande de l'auteur : faire varier `think` d'un appel à l'autre sur un modèle
  déjà chargé **ne recharge pas le modèle** — `load_duration` reste bas (~150-290 ms, coût de vérification
  de présence, pas un rechargement) sur tous les appels après le premier, qu'on alterne `true`/`false` ou
  non. Le paramètre n'affecte que le template envoyé, pas les poids en mémoire.

Compile proprement (`mvn test-compile`).

### 11.2 Groupage de plusieurs paires dans un seul appel de vérification

Testé (26B Q6_K, `think=false`) : 5 paires FAUX/JUSTE soumises en un seul appel, réponse demandée sous
forme de tableau (`N | VERDICT | motif`), comparé à 5 appels séparés sur les mêmes paires.

| | 5 appels séparés | 1 appel groupé (5 paires) |
|---|---|---|
| Durée totale | ~5,4 s | **~2,2-2,6 s** (hors un pic isolé à 23 s, non reproductible, probablement de la contention passagère) |
| Tokens générés | ~180 | **~85-90** |
| Précision | 3/5 (idem qu'en appels séparés) | **3/5, identique** |
| Fiabilité du format | — | Stable sur 3 essais consécutifs : toujours 5 lignes, bien numérotées, aucune paire perdue ni confondue |

**Conclusion** : grouper par petits lots (~5 paires) est net — environ 2× plus rapide, moitié moins de
tokens, précision inchangée, format fiable à cette taille de lot sur ce modèle (26B). Contredit partiellement
la prudence de Fable ("un appel par paire pour les petits modèles") — mais Fable raisonnait sur des petits
modèles en général, pas sur ce 26B précis, et seulement à N=5 (pas testé à N plus grand, où le risque de
paire oubliée pourrait réapparaître). **Recommandation mise à jour pour le vérificateur (§6.3)** : lots de
5 paires maximum, jamais un appel unique pour toutes les paires d'une séquence.

## 12. Suite prévue — reprise de session

Session interrompue ici pour la journée. Point de reprise et prochaine étape convenue avec l'auteur du
projet : **analyser correcteur par correcteur** (et plus largement agent par agent, cf. TODO ajouté en §6.4)
avant d'implémenter quoi que ce soit. Liste de ce qui attend une décision, dans un ordre de reprise
suggéré :

1. Revue correcteur par correcteur (DeusInMachina, Naturality, Style, Proofreader) : risque intrinsèque,
   utilité de la réflexion, pertinence du vérificateur, formulation du garde-fou — probablement la session
   la plus structurante, dont tout le reste découle.
2. Étendre cette revue à tous les agents du pipeline (pas seulement les correcteurs) pour la question
   `think` spécifiquement (TODO §6.4).
3. Delta exact du garde-fou textuel (§6.1) à valider avant écriture.
4. Nom du concept métier + mandat par correcteur pour le vérificateur de paires (§6.3/§7).
5. Câblage effectif de `withThink(...)` sur les agents concernés, une fois les points 1-2 tranchés.
6. Éventuellement reprendre la piste Modelfile (§10) sur un modèle plus petit, si le besoin de `think=true`
   explicite (plutôt que le défaut) se confirme quelque part.

Rien n'est cassé ni bloquant en l'état : le pipeline actuel tourne avec le comportement corrigé (§8, §9,
§11.1) mais sans aucun agent utilisant encore `withThink(...)` — donc identique au comportement
d'avant-session pour la génération elle-même, seul le socle technique a changé.

## 13. Réalisé (reprise du 2026-07-10) — Split GrammarCorrector/PhrasingCorrector, RetryStrategy par agent

Reprise de session le 2026-07-10. Première itération concrète de l'analyse "correcteur par correcteur"
annoncée au §12 point 1 : ProofreaderCorrector traité en premier (le plus simple), bien qu'il reste en
dernier dans l'ordre d'exécution du pipeline.

### 13.1 Nouveaux concepts métier

- **`RetryStrategy`** (enum, `agent/commun/RetryStrategy.java`) — comment un correcteur décide de se
  relancer sur sa propre sortie déjà patchée :
  - `SINGLE_PASS` — jamais de relance
  - `RATIO_THRESHOLD` — comportement historique (corr/mot ou seuil absolu)
  - `DECREASING` — relance tant que le nombre de fautes baisse d'une passe à l'autre
  - `DECREASING_AND_RATIO_THRESHOLD` — relance seulement si RATIO_THRESHOLD **et** DECREASING sont
    vrais tous les deux (les deux stratégies simples restent disponibles isolément, à la demande de
    l'auteur du projet, même si aucun agent ne les utilise seules aujourd'hui)
- **`AgentCorrector`** (interface, `agent/AgentCorrector.java`, hérite de `Agent`) — expose
  `retryStrategy(): RetryStrategy`. Implémentée par les 4 correcteurs existants.

### 13.2 Split ProofreaderCorrector → GrammarCorrector + PhrasingCorrector (planned)

Décision : séparer l'ancien `ProofreaderCorrector` en deux agents à périmètre strictement borné —
`GrammarCorrector` (accuracy : grammaire/orthographe/accord/conjugaison, critère fermé/vérifiable) et
`PhrasingCorrector` (fluency : mots mal choisis, phrases bancales, calques, syntaxe non idiomatique,
critère plus ouvert — **pas encore créé**, prévu pour une prochaine session).

**GrammarCorrector** — renommage complet de ProofreaderCorrector (classe, package
`proofreadercorrector` → `grammarcorrector`, Input/Output/Step) via `git mv` pour préserver l'historique.

**Delta de prompt** (validé avant écriture, conformément à la règle projet) :

AVANT :
```
Tu analyses ce texte et identifies toutes les fautes de langue :
- fautes de grammaire, d'orthographe, d'accord et de conjugaison
- mots manquants, absents ou mal choisis (emploi sémantiquement inapproprié)
- phrases bancales : calques d'une autre langue, formulations maladroites,
  pléonasmes, syntaxe confuse ou non idiomatique dans la langue du texte
- ou tout autre problème qui sonne faux dans la langue du texte
```

APRÈS :
```
Tu analyses ce texte et identifies les fautes de grammaire, d'orthographe, d'accord et de conjugaison.
Ne signale aucun autre type de faute.
Ne change jamais le sens de la phrase ni les faits qu'elle rapporte : la correction ne porte que sur la forme.
Vérifie que la faute est réelle et que ta correction est elle-même sans faute.
```

Raison : les 3 clauses retirées (mots mal choisis, phrases bancales, catch-all) sont des jugements de
sens/style, pas de forme — c'est la source de risque identifiée au §6.4/analyse Fable pour cet agent,
malgré sa mission affichée "bas risque". La clause catch-all *"ou tout autre problème qui sonne faux"*
est supprimée purement et simplement (ni gardée ici ni déplacée) — chèque en blanc sans limite.

Reste du prompt (format FAUX/JUSTE, exemple, règles de citation) inchangé.

**`retryStrategy()` : `DECREASING_AND_RATIO_THRESHOLD`** — critère fermé, la relance a du sens, mais
seulement tant qu'elle progresse réellement (décroissance) et qu'il reste un volume significatif de
fautes (seuil).

**`think=false`** via `LlmCallContext.of(agentName(), agentLabel()).withThink(Boolean.FALSE)` — tâche
fermée, la réflexion n'apporte rien pour ce domaine, conforme à la recommandation du §6.4.

### 13.3 `WriteWorkflow` — dispatch de la stratégie de relance

`needsRetry(int, String)` devient `needsRetry(RetryStrategy, int count, int previousCount, String text)`,
avec dispatch par `switch` sur la stratégie. `previousCount` initialisé à `Integer.MAX_VALUE` avant
chaque boucle de correcteur, pour que `DECREASING`/`DECREASING_AND_RATIO_THRESHOLD` ne bloquent jamais
la toute première tentative de relance faute de donnée de comparaison (la tendance ne devient
significative qu'à partir de la 2e relance).

Les 3 autres correcteurs (DeusInMachina, Naturality, Style) gardent `RATIO_THRESHOLD` — comportement
strictement inchangé, migration reportée à leur tour dans l'analyse agent par agent.

### 13.4 Fichiers touchés (résumé)

- Nouveaux : `RetryStrategy.java`, `AgentCorrector.java`
- Renommés (`git mv`) : `GrammarCorrector{,Input,Output}.java`, `GrammarCorrectorStep.java`
- Modifiés : `DeusInMachinaCorrector.java`, `NaturalityCorrector.java`, `StyleCorrector.java`
  (implémentent `AgentCorrector`, `retryStrategy() → RATIO_THRESHOLD`) ; leurs 3 `*CorrectorStep.java`
  (méthode `retryStrategy()` déléguée) ; `WriteWorkflow.java` (dispatch + renommage) ;
  `RedacteurModule.java` (câblage) ; `WorkflowLogTest.java` (assertion renommée) ;
  `agent/CLAUDE.md`, `orchestrator/CLAUDE.md`, `specs/retry-rules.md` (doc à jour)

### 13.5 Résultat

`mvn clean test-compile` propre sur `commun`, `redacteur`, `testllm`. Suite de tests `redacteur` :
19 tests, 1 seul échec (`WorkflowLogTest.planOnly_doesNotLogWriteOrEval`) — **pré-existant, sans rapport**
avec ce travail : `StoryOrchestrator.java`/`QualityLevel.java` (logique PLAN_ONLY) étaient déjà modifiés
avant le début de cette session par un autre chantier en cours (refactor requirements/renommage
checker→critic), ni l'un ni l'autre non touché ici. Vérifié via `git diff --stat` sur ces deux fichiers.

### 13.6 Reste à faire

- **`PhrasingCorrector`** (mots mal choisis, phrases bancales) — pas encore créé. Nom validé, périmètre
  défini (§13.2), mais aucun fichier écrit.
- Analyse des 3 autres correcteurs (DeusInMachina, Naturality, Style) — risque, `think`, `retryStrategy`
  cible, garde-fou textuel — reste à faire un par un.
- Le garde-fou textuel générique validé au §6.1 ("ne change jamais le sens...") n'a été appliqué qu'à
  GrammarCorrector pour l'instant (sous une forme adaptée : "vérifie que la faute est réelle et que ta
  correction est sans faute" plutôt que la formulation générique) — pas encore répercuté sur les 3 autres.
