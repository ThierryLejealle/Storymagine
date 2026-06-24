# 2026-06-21 17h20 - Tests unitaires BenchmarkRunner

## Description de l'évolution demandée

Ajout de tests unitaires pour la logique de domaine du module testllm, ciblant
les deux zones à risque de bug silencieux : la détection de divergence et la
gestion des erreurs partielles dans runPasse().

## Ce qui a été touché

### `pom.xml` (parent)
- Ajout de `org.junit.jupiter:junit-jupiter:5.10.2` dans `dependencyManagement`
- Ajout de `maven-surefire-plugin:3.2.5` dans `pluginManagement`

### `testllm/pom.xml`
- Ajout de la dépendance `junit-jupiter` en scope `test`

### `testllm/src/test/.../BenchmarkRunnerTest.java` (nouveau)
5 tests sans aucun appel LLM réel :
- `pasDesDivergenceQuandTokensIdentiques` — aucune Divergence si tokens identiques
- `divergenceDetecteeSiUnRunDepasseSeuilDeCinquantePourcent` — Divergence si écart > 50%
- `unSeulRunNeDeclenchePasDivergence` — size < 2 = pas de divergence possible
- `probeFailSkipLeModele` — ProbeFail → pas de RunOk ni PasseComplete
- `erreurPartielleNAccumuleQueLesSuccesDansElapsed` — elapsed.size()=2 si 1 run sur 3 échoue

### `commun/infra/OllamaAdapter.java` (correction)
- Multi-catch `ContextOverflowException | RuntimeException` invalide (sous-classe)
  remplacé par `RuntimeException` seul — ContextOverflowException est capturée car c'est une RuntimeException

## Résultat

`mvn test -pl testllm` : 5/5 tests passent en 0.055s.
