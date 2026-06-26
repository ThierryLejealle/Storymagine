# 2026-06-26 - Alignement NaturalityCorrector sur ProofreaderCorrector + warning replace

## Evolution demandee

Aligner NaturalityCorrector strictement sur la structure de ProofreaderCorrector (FAUX/JUSTE),
vérifier tous les autres Correctors, et afficher un WARNING si un String.replace() echoue
silencieusement dans un Corrector.

## Ce qui a ete touche

### NaturalityCorrector — prompt
- EXTRAIT: + PROBLEME: + SUGGESTION: -> FAUX: / JUSTE: (meme format que ProofreaderCorrector)
- Sentinel [RIEN] -> PAS DE CORRECTION
- PROBLEME supprime : champ inutilise, pertinent pour un Critic pas un Corrector

### NaturalityFinding
- Champs renommes : (citation, probleme, suggestion) -> (wrongPhrase, correctedPhrase)
- Alignes sur la nomenclature de DeusInMachinaCorrectorFinding

### NaturalityCorrector — parser
- Remplacement complet : ancien bloc (split sur EXTRAIT:) -> logique ligne par ligne FAUX:/JUSTE:
  identique a ProofreaderCorrector et DeusInMachinaCorrector

### WriteWorkflow
- applyDeusCorrections, applyNaturalityFixes, applyProofCorrections : static -> instance methods
- Chaque String.replace() journalise log.warn() si le remplacement n'a rien change
- applyNaturalityFixes mis a jour : f.citation() / f.suggestion() -> f.wrongPhrase() / f.correctedPhrase()

### CLAUDE.md /agent
- Section "Corrector Output Format" : EXTRAIT/SUGGESTION -> FAUX/JUSTE, PAS DE CORRECTION
- Section "Reference implementations" : Proofreader -> ProofreaderCorrector
- Ajout section "Corrector - Replace Warning Rule" avec exemple de code

### DeusInMachinaCorrector, ProofreaderCorrector
- Deja alignes : aucune modification requise

## Resultat

Compilation OK. Les trois Correctors utilisent desormais le meme format de sortie LLM
(FAUX/JUSTE/PAS DE CORRECTION) et emettent un warning observable si le LLM cite une
phrase qui ne correspond pas exactement au texte a patcher.