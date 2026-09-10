# 2026-09-09 - Diagnostic ponctuel MTP (draft_num_predict)

## Demande
Mesurer l'effet du paramètre Ollama `draft_num_predict` (Multi-Token Prediction) sur la vitesse
de génération. Un seul appel haïku par valeur de MTP testée (aucun, puis 1 à 16), tableau de
résultats MTP / tokens / tok-s, avec le même menu de sélection GPU que testLLM et chat.
Modèle visé initialement : `hf.co/HauhauCS/Qwen3.8-27B-Uncensored-HauhauCS-Aggressive-MTP-GGUF:IQ4_XS`,
fixé en dur — puis revu en cours de séance : choix interactif du modèle (comme testLLM), ce modèle
ajouté aux favoris pour rester facile d'accès.

## Touché
- Nouvelle classe `storymagine.testllm.ui.cli.TestMtpCli` (module testllm, `ui/cli`) — renommée
  en cours de séance depuis `TestMtpQwen38Cli` (le nom référençait encore Qwen alors que le modèle
  n'est plus fixé en dur) :
  - menu GPU repris de `TestLlmCli.resolveGpuMode` (options 1-4, sans le mode "complet") ;
  - choix du modèle repris de `OllamaSetupCli.chooseModel` (favoris en tête, marqués `*`,
    Entrée = premier de la liste) — plus de modèle fixé en dur ;
  - 17 appels HTTP directs `/api/chat` (non-stream) construits à la main avec Jackson — sans
    passer par `OllamaAdapter`/`GenerationOptions` pour l'appel lui-même (pas d'option arbitraire
    type `draft_num_predict`), mais `OllamaAdapter.listModels` est réutilisé pour la liste des
    modèles. Réutilise le prompt de la passe `haiku` existante (`Passes.HAIKU`).
- Nouveau `test-mtp.bat` à la racine (renommé depuis `test-mtp-qwen3-8.bat`), calqué sur `test-llm.bat`.
- `storymagine.properties` : `favoris.7=hf.co/HauhauCS/Qwen3.8-27B-Uncensored-HauhauCS-Aggressive-MTP-GGUF:IQ4_XS`.
- **Bug corrigé** `testllm/pom.xml` : le plugin `exec-maven-plugin` déclarait
  `<mainClass>storymagine.testllm.ui.cli.TestLlmCli</mainClass>` en valeur littérale — un
  `-Dexec.mainClass=...` passé en ligne de commande ne peut jamais surcharger une valeur littérale
  du POM, seulement une propriété `${...}`. Résultat observé : `test-mtp-qwen3-8.bat` lançait
  silencieusement `TestLlmCli` (d'où le menu "choisir un modèle" inattendu vu par l'utilisateur) au
  lieu de `TestMtpQwen38Cli`. Corrigé en introduisant une propriété `exec.mainClass` (défaut
  `TestLlmCli`, comportement de `test-llm.bat` inchangé) référencée par `${exec.mainClass}` dans le
  plugin, désormais réellement surchargeable en ligne de commande.

- **Bug de mesure corrigé** dans `TestMtpCli.appel()` : le corps de requête n'envoyait jamais le
  champ `think`, donc Ollama appliquait son propre défaut (réflexion activée pour les modèles qui
  la supportent) — observé concrètement sur `Qwen3.8-27B-...-MTP` : 1601 tokens et ~80s pour un
  haïku censé faire quelques lignes, contre 58 caractères / 1s sur le bench standard qui envoie
  `think:false` explicitement. Ajout de `body.put("think", false)` pour une comparaison MTP propre
  (génération déterministe, sans bruit de réflexion variable d'un appel à l'autre).
- Ajout d'un aperçu du texte généré (`message.content`, tronqué à 70 caractères) dans la ligne de
  résultat de chaque appel — le script ne vérifiait jusque-là que les compteurs de tokens, jamais
  le texte réellement produit par Ollama.
- `commun/NvidiaSmiSnapshot.capture()` : ajout d'une surcharge `capture(boolean verbose)` — les
  deux lignes `[GPU] interrogation nvidia-smi...` / `[GPU] nvidia-smi termine (...)` étaient bruyantes,
  surtout via `HardwarePoller` (appelé en boucle pendant chaque génération). `capture()` (sans
  argument) garde son comportement d'origine — inchangé pour `OllamaSetupCli`/chat/redacteur — et
  les 4 appels du module testllm (`TestLlmCli`, `TestMtpCli`, `HardwarePoller`, `ConsoleRunLogger`)
  passent désormais à `capture(false)`, silencieux.

## Résultat
Compilation du module testllm vérifiée (`mvn -q -pl testllm -am compile`) après chaque étape, et
compilation complète du projet (`mvn -q compile`) vérifiée après le changement dans `commun`
(surcharge ajoutée, signature existante inchangée — chat et redacteur non impactés). Test
manuel du bat corrigé : `TestMtpCli` se lance bien (et non plus `TestLlmCli`), liste les modèles
avec les favoris marqués, et enchaîne les appels Ollama réels sans erreur. Vérification du fix
`think:false` en attente d'une nouvelle exécution complète côté utilisateur. Aucun changement au
cœur (service/domaine/ports) : script de diagnostic isolé, sans concept métier introduit.
