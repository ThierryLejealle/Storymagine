# chap_1_s1_motifs_filter — appel 10

## EN-TÊTE
- Démarré  : 2026-06-18 10:12:37
- Statut   : ✅ OK
- Sys      : ~163 tok
- Usr      : ~294 tok
- Réponse  : ~125 tok
- Durée    : 5,2s

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
- Transition sonore marquée par le passage d'un calme absolu à une agitation mécanique.
- Submersion du personnage principal face à l'échelle écrasante et désolée de son environnement.
- La machine est traitée comme un organisme vivant possédant une tension interne palpable.
- L'observation technique détaillée précède systématiquement toute interaction verbale entre les personnages.
- Le transfert d'une force physique intense se traduit directement par la perturbation de l'équilibre du personnage.

Retourne uniquement les expressions à bannir (hors leitmotivs protégés).

---

## RÉPONSE

- Transition sonore marquée par le passage d'un calme absolu à une agitation mécanique.
- Submersion du personnage principal face à l'échelle écrasante et désolée de son environnement.
- La machine est traitée comme un organisme vivant possédant une tension interne palpable.
- L'observation technique détaillée précède systématiquement toute interaction verbale entre les personnages.
- Le transfert d'une force physique intense se traduit directement par la perturbation de l'équilibre du personnage.