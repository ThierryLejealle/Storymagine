# 2026-07-13 18h15 - Redacteur : fix de la degenerescence de PhraseExtractor (SequencePhraseExtractor)

## Demande

L'utilisateur a remonte un log ou l'agent `SequencePhraseExtractor` (PhraseExtractor) a genere
644s / 32139 tokens sans jamais produire de reponse exploitable (ni `PASSAGE:` ni `NOT_FOUND`),
et ce cas s'est retrouve silencieusement traite comme un simple "phrase non trouvee" sans laisser
de trace distincte dans les logs. Demande : "Corrige cet agent et teste le !"

## Ce qui a ete touche

- Diagnostic : l'agent avait `thinks()=true` (raisonnement actif) sur une tache de recherche
  floue ouverte, sans plafond de generation (`num_predict` illimite) — combinaison propice a une
  derive du modele en boucle de raisonnement sans jamais conclure.
- `commun/coeur/ports/GenerationOptions.java` (nouveau) : record `(stopSequences, maxTokens, topK,
  topP)` pour parametrer un appel LLM au cas par cas, en plus des reglages par defaut du modele.
- `commun/coeur/ports/ModelCallPort.java` : nouvelle surcharge `generate(..., GenerationOptions)` ;
  l'ancienne surcharge `stopSequences`-only delegue desormais a la nouvelle.
- `commun/infra/OllamaAdapter.java` : `GenerationOptions` chaine jusqu'a `buildOllamaRequest`
  (options `top_k`/`top_p`/`num_predict`/`stop` prennent la valeur de l'appel si fournie, sinon le
  defaut du modele).
- `redacteur/.../agent/sequence/phraseextractor/PhraseExtractor.java` : `thinks()` passe a `false`
  (tache mecanique/fermee, coherent avec la convention documentee dans `Agent.thinks()`) ; ajout
  d'un plafond de securite `MAX_TOKENS=300` via `GenerationOptions.maxTokens(...)` ; ajout d'un
  `log.warn(...)` explicite quand la reponse n'est ni un `PASSAGE:` valide ni un `NOT_FOUND:`
  authentique, pour distinguer un echec degenere d'un "non trouve" legitime.
- `PhraseExtractorTest.java` : 4 nouveaux tests (`thinksIsFalse_...`,
  `degenerateResponseWithoutPassageOrNotFound_logsAWarningAndReturnsNotFound`,
  `genuineNotFound_doesNotLogAWarning`, `passageFound_doesNotLogAWarning`) via
  `MockModelCallPort`/`CapturingLogPort`.

## Resultat

`mvn test` (module redacteur) au vert, 10 tests sur `PhraseExtractorTest`. Un futur cas degenere
similaire remontera desormais un `[WARN]` explicite au lieu de se fondre dans les "non trouve"
normaux.
