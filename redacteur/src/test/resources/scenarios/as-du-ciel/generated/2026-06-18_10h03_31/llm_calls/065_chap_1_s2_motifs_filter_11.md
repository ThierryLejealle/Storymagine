# chap_1_s2_motifs_filter — appel 11

## EN-TÊTE
- Démarré  : 2026-06-18 10:13:47
- Statut   : ✅ OK
- Sys      : ~163 tok
- Usr      : ~289 tok
- Réponse  : ~96 tok
- Durée    : 6,6s

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
- La queue de l'appareil pointée vers Thorney Island, les arbres encore noirs sur le ciel blanchissant
- Ce qu'il ressentait là n'était pas ça. C'était plus calme. Plus ancien. Quelque chose qui ressemblait à de la faim
- Il devient un tonnerre qui traverse le métal et les os
- Cette sensation de puissance brute installée entre l'exaltation et la nausée sature la poitrine de Pierre
- Le Grey Ghost devenait un organisme vivant dont les vibrations font trembler tout l’habitacle

Retourne uniquement les expressions à bannir (hors leitmotivs protégés).

---

## RÉPONSE

- La queue de l'appareil pointée vers Thorney Island, les arbres encore noirs sur le ciel blanchissant
- Ce qu'il ressentait là n'était pas ça. C'était plus calme. Plus ancien. Quelque chose qui ressemblait à de la faim
- Il devient un tonnerre qui traverse le métal et les os
- Cette sensation de puissance brute installée entre l'exaltation et la nausée sature la poitrine de Pierre