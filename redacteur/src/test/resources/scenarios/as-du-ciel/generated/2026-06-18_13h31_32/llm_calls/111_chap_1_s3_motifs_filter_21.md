# chap_1_s3_motifs_filter — appel 21

## EN-TÊTE
- Démarré  : 2026-06-18 14:28:59
- Statut   : ✅ OK
- Sys      : ~163 tok
- Usr      : ~244 tok
- Réponse  : ~75 tok
- Durée    : 22,4s

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
- chaque rivet témoignait d'un assemblage méticuleux et éprouvé
- le regard scrutant l'appareil comme on inspectait un patient complexe
- une enclume patinée par la graisse et le travail
- l’olfaction puissante et complexe, mêlant huile brûlée, essence haute octane et un soupçon amer de métal refroidi

Retourne uniquement les expressions à bannir (hors leitmotivs protégés).

---

## RÉPONSE

- chaque rivet témoignait d'un assemblage méticuleux et éprouvé
- le regard scrutant l'appareil comme on inspectait un patient complexe
- une enclume patinée par la graisse et le travail
- l’olfaction puissante et complexe, mêlant huile brûlée, essence haute octane et un soupçon amer de métal refroidi