# 2026-07-13 18h10 - Chat : fix du mirroring (joueur confondu avec le personnage) + regles de formatting simplifiees

## Demande

Dans le module chat, le LLM de roleplay se mettait regulierement a decrire les sensations/actions
du joueur comme si c'etaient les siennes ("mirroring"), y compris apres plusieurs tentatives de
correction de prompt. L'utilisateur a fourni des extraits de log Ollama reels pour diagnostiquer.

## Ce qui a ete touche

- Diagnostic via deux consultations Fable (avec le prompt reel envoye a Ollama) : le tour du
  joueur en cours d'echange n'etait pas prefixe par `Player:` contrairement au reste du transcript
  historique, ce qui laissait le LLM libre d'interpreter ce texte comme le sien.
- `ChatPromptBuilder.java` : le prompt utilisateur alterne desormais `Player: ...` / `{name}: ...`
  de bout en bout, y compris pour le tour courant, avec un prefill final `{name}:` pour ancrer qui
  doit repondre.
- Regle de formatting simplifiee dans le prompt systeme : `*fait*` = action, texte brut = parole
  (suppression d'un formalisme plus complexe teste puis abandonne sur avis de Fable).
- Ajout d'une regle explicite de "possession" (qui peut decrire quoi) avec un exemple concret
  faux/juste, pour eviter que le LLM decrive les pensees ou sensations privees du joueur.
- `RoleplayNarrator.STOP_SEQUENCES` (`\nPlayer:`, `\nNarration:`) : coupe la generation des que le
  LLM derive vers le tour du joueur ou une narration a sa place.

## Resultat

Le mirroring a cesse d'apparaitre sur les echanges testes apres coup. Regle de formatting validee
explicitement par l'utilisateur ("Non c'est top").
