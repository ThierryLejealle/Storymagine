# 2026-10-30 - Chat : niveaux d'activité des personnages (active, present, passive, absent)

## 1. Évolution demandée

Pouvoir dire, dans le scénario, qui parle dans une scène, avec une logique d'héritage entre titres
(`#`, `##`, `###`), et laisser le joueur le forcer en cliquant les vignettes. Quatre niveaux, qui
remplacent les deux cases 🔊/🔇 (présence) et 💬 (éligible à l'interjection) :

- **active** 😎 parle à chaque échange ;
- **present** 🙂 (défaut) répond quand il est nommé ; peut être tiré au sort et interjecter, avec une chance double ;
- **passive** 🤨 peut être tiré au sort et interjecter, sans répondre parce qu'on le nomme ;
- **absent** 😴 ne parle jamais et n'est pas dans la scène.

Commandes `{npc-active|present|passive|absent noms}` et `{npc-reset}`. Une commande sans nom vaut pour tous
les PNJ dont le niveau n'est pas encore explicite. Chaque PNJ a un niveau et un statut explicite ou
implicite ; le plus profond gagne ; le clic du joueur force un niveau (point rouge), accroché au titre parent
de l'acte courant, qui disparaît quand l'histoire en sort. Aucun prompt n'est modifié.

## 2. Ce qui a été touché

- Nouveau domaine `coeur/domaine/activity` : `NpcActivity`, `ActivitySource`, `NpcActivityState`,
  `ActivityDirective`, `ActivityDirectiveMarker`, `ActivityResolver`, `CastActivity`, `ForcedActivity`.
- `ScenarioAct` porte `activityPath` (les commandes de chaque titre, de la racine à la feuille) ;
  `ChatScenario.activityAt(act)` calcule l'activité, jamais stockée, et refuse au chargement un nom inconnu,
  un PNJ nommé deux fois ou une scène sans personne. `ScenarioOutlineParser` extrait les commandes (retirées du
  texte de l'acte, interdites dans la prémisse). `ScenarioValidator` connaît les cinq commandes.
- `ChatSession` : les listes `present` / `interjecting` sont remplacées par les niveaux forcés ; `activity()`,
  `scenarioActivity()`, `cycleActivity(id)` (cinq positions, un cran qui viderait la scène est sauté),
  `presentNpcIds()` devient dérivé. Un niveau forcé est abandonné à la sortie de son titre parent
  (changement d'acte, retour arrière, chargement, rechargement).
- `SpeakerSelector` réécrit : actifs, mentions (message du joueur puis réponses du tour précédent), continuité,
  tirage pondéré present 2 / passive 1 ; interjection avec chance doublée pour present.
- `ChatService` / `ChatServiceImpl` : `cycleNpcActivity` remplace `setNpcPresent` et `setNpcInterjecting`.
- `RunState` / `ChatFileStorageAdapter` : clé `forced=id:niveau@ancre;...` dans `state.txt` ; les anciennes clés
  `present` / `interjecting` sont encore lues (un PNJ absent de `present` rouvre forcé absent).
- Web : `POST /cycle-activity` remplace `/set-present` et `/set-interjecting` ; `NpcView(id, label, level,
  source, scenarioLevel)` ; `chat.html` : pastille sombre avec emoji et point d'état (blanc explicite, gris
  implicite, rouge forcé), tooltip, clic qui fait défiler.
- Tests : `ActivityResolverTest`, `ActivityDirectiveMarkerTest`, `ActivityScenarioLoadingTest`, `RunStateTest`,
  `SpeakerSelectorTest` (réécrit), `ChatSessionTest`, `ChatServiceImplTest` (deux tests de bout en bout),
  `ChatFileStorageAdapterTest`, `ChatWebServerEndpointsTest` adaptés ou complétés.
- Documentation : guide `chatscenarios/guide-redaction-chat.md` (§2.9, nouveau §2.11, §4.1 à §4.3, §4.8, §6.5),
  `specs/chat.md`, raccourci `.claude/skills/chat-scenario-writing/SKILL.md`.

## 3. Résultat

`mvn test` depuis la racine : 450 tests, tous verts. Comportements à connaître :

- un PNJ **passive** que le joueur nomme ne répond pas pour autant (c'est ce qui le distingue de present) ;
- la combinaison « présent mais ne réagit que s'il est nommé » (ancien 💬 décoché) n'existe plus ;
- un PNJ absent qui revient lit tout l'historique : l'idée de l'en tenir ignorant a été abandonnée ;
- les réglages 🔊/💬 faits à la main dans une partie existante ne sont pas repris (sauf la liste des absents).

Non essayé avec un vrai modèle : l'effet de l'interjection doublée pour present sur un petit modèle.
