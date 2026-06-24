# chap_1_s3_motifs_filter — appel 14

## EN-TÊTE
- Démarré  : 2026-06-18 14:08:30
- Statut   : ✅ OK
- Sys      : ~163 tok
- Usr      : ~235 tok
- Réponse  : ~42 tok
- Durée    : 23,9s

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
- Un personnage examine méticuleusement les composants d'un équipement technique.
- La description de la machine est faite par des analogies avec le vivant (humain ou animal).
- Le dialogue sert à transmettre une connaissance fonctionnelle et vitale sur un objet.

Retourne uniquement les expressions à bannir (hors leitmotivs protégés).

---

## RÉPONSE

- Un personnage examine méticuleusement les composants d'un équipement technique.
- Le dialogue sert à transmettre une connaissance fonctionnelle et vitale sur un objet.