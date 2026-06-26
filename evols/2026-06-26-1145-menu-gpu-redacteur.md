# 2026-06-26 - Menu GPU dans RedacteurCli

## Evolution demandée
Ajouter dans RedacteurCli le meme menu de choix de profil GPU qu'en test-llm :
utiliser Ollama existant / relancer sur GPU 0 / GPU 1 / GPU 0+1.

## Ce qui a été touché

### commun/infra
- Ajout de OllamaLauncher (deplace depuis testllm/infra avec nouveau package)

### testllm
- Suppression de 	estllm/infra/OllamaLauncher.java
- TestLlmCli : import mis a jour vers storymagine.commun.infra.OllamaLauncher

### redacteur
- RedacteurCli : import OllamaLauncher, ajout menu GPU (options 1-4) avant la
  connexion Ollama. Helpers printGpuOption et gpuLabel ajoutes.
  La capture GPU faite en debut de menu est reutilisee pour ModelHardwareDisplay
  (suppression du doublon NvidiaSmiSnapshot.capture() ligne 172).

## Résultat
Compilation OK. Le menu GPU apparait en debut de session redacteur.