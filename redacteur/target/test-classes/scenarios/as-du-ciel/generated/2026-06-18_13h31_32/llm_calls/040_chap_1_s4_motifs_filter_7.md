# chap_1_s4_motifs_filter — appel 7

## EN-TÊTE
- Démarré  : 2026-06-18 13:52:16
- Statut   : ✅ OK
- Sys      : ~163 tok
- Usr      : ~272 tok
- Réponse  : ~30 tok
- Durée    : 24,6s

---

## PROMPT SYSTÈME

Tu es un éditeur littéraire. On te donne :
1. Une liste de leitmotivs/rituels narratifs intentionnellement récurrents (à protéger).
2. Une liste d'expressions extraites d'un texte, candidates à être bannies.
Ta tâche : retourner uniquement les expressions candidates qui NE sont PAS
sémantiquement proches d'un leitmotiv protégé.
En cas de doute, protège l'expression (ne la mets pas dans la liste retournée).
On préfère autoriser quelques répétitions plutôt qu'interdire un leitmotiv par erreur.
Format de sortie STRICT : une expression par ligne, précédée d'un tiret (-).
Pas de commentaires. Pas d'explication. Uniquement la liste filtrée. En français.

---

## PROMPT UTILISATEUR

### Leitmotivs et rituels protégés (ne pas bannir ces expressions) :
# Expressions et situations autorisées à se répéter

- Le rituel de Jules avec le Grey Ghost — geste affectif récurrent, peut revenir souvent
- Le silence de Bertrand — trait de caractère expressif, peut revenir souvent
- Les cigares ou pipes des officiers — habitude visuelle, modérément récurrent
- Métaphore du ciel comme mer, abîme ou vide — registre stylistique récurrent
- Les formules de bord et appellations militaires (Oberleutnant, Feldwebel…) — jargon structurant, toujours autorisé


### Expressions candidates à bannir :
- La nuit avait glissé sur la base avec une lenteur presque palpable
- Un silence pesant interrompu seulement par des grognements réguliers et monotones
- L’obscurité totale qui enveloppait le paysage militaire
- Le vent nocturne s'engouffra dans l'allée longeant le hangar principal et balaya doucement la poussière sur l'asphalte
- La vaste étendue d'encre noire se transformait lentement en un dégradé étrange

Retourne uniquement les expressions à bannir (hors leitmotivs protégés).

---

## RÉPONSE

- Le vent nocturne s'engouffra dans l'allée longeant le hangar principal et balaya doucement la poussière sur l'asphalte