# chap_1_s2_motifs_filter — appel 19

## EN-TÊTE
- Démarré  : 2026-06-18 14:24:02
- Statut   : ✅ OK
- Sys      : ~163 tok
- Usr      : ~259 tok
- Réponse  : ~90 tok
- Durée    : 25,0s

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
- Le bruit sec des bottes sur le parquet du bâtiment claquait dans la pénombre
- Un espace où le temps semblait s’être figé entre les piles de dossiers militaires et les cartes froissées
- L'atmosphère lourde d’un mélange âcre du papier ancien mouillé par l'humidité ambiante
- Le visage fermé et la mâchoire serrée trahissaient le poids constant du commandement

Retourne uniquement les expressions à bannir (hors leitmotivs protégés).

---

## RÉPONSE

- Le bruit sec des bottes sur le parquet du bâtiment claquait dans la pénombre
- Un espace où le temps semblait s’être figé entre les piles de dossiers militaires et les cartes froissées
- L'atmosphère lourde d’un mélange âcre du papier ancien mouillé par l'humidité ambiante
- Le visage fermé et la mâchoire serrée trahissaient le poids constant du commandement