# chap_2_s2_motifs_filter — appel 3

## EN-TÊTE
- Démarré  : 2026-06-18 16:15:31
- Statut   : ✅ OK
- Sys      : ~163 tok
- Usr      : ~231 tok
- Réponse  : ~53 tok
- Durée    : 11,6s

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
- Chaleur plus lourde et un peu étouffée du mess
- L'immensité grise du ciel matinal
- Doigts épais aux phalanges tachées par le gras permanent
- Atmosphère décontractée et pesante
- Un sourire trop large pour être sincère
- Mélodie jazzy et feutrée

Retourne uniquement les expressions à bannir (hors leitmotivs protégés).

---

## RÉPONSE

- Chaleur plus lourde et un peu étouffée du mess
- Doigts épais aux phalanges tachées par le gras permanent
- Atmosphère décontractée et pesante
- Un sourire trop large pour être sincère
- Mélodie jazzy et feutrée