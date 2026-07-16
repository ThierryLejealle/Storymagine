# 2026-07-15 22h56 - Nouveau scenario : La quête des amazones

## 1. Demande

Scenario d'aventure complet demande par l'utilisateur : joueur = Alex, jeune mage non-combattant
specialise dans la neutralisation de protections magiques, engage par une equipe de 3 amazones
d'Ambaya pour recuperer la Perle d'Aube dans le Temple d'Ilyria et lever une malediction frappant
les enfants du village. Etapes demandees : rencontre, preparatifs, jour du depart, au moins dix
jours de jungle (attaques/pieges/peripeties/combats), plusieurs etapes dans le temple, retour,
adieux au village. Consigne explicite : personnages "fun et interessants", "couleur et piment",
histoire "passionnante" — et une partie secrete par amazone (aspirations/envies/kink, jamais
mechant ni contraire a l'histoire).

## 2. Ce qui a ete cree

`chatscenarios/quete-des-amazones/` :
- `scenario.txt` — 13 chapitres (`#`) / 26 actes-feuilles (`##`), structure identique au motif
  deja valide de `temple-noir-actes` (chapitre = contexte hérité, feuilles = actes reellement
  jouables). Couverture explicite de 10 jours de jungle (chapitres "Jour 1 et 2" a "Jour 9 et 10"),
  puis 3 chapitres temple (entree / profondeurs / Perle d'Aube), retour, adieux.
- `laina.txt`, `mina.txt`, `sheera.txt` — fiche publique + `# SECRET` pour chacune.

## 3. Personnalites et secrets (le "piment" demande)

- **Laina** (cheffe, epeiste) : publique = calme, responsable, cherche la concorde. Secret = epuisee
  d'etre toujours "celle qui decide", reve honteusement d'etre choyee/prise en charge pour une fois
  — remarque que la presence de Mina lui fait cet effet particulierement.
- **Mina** (guerriere-valkyrie, marteau+bouclier) : publique = force herculeenne, esprit brillant,
  douceur inattendue. Secret = tout le monde la croit invulnerable donc personne ne lui offre de
  tendresse ; elle en a besoin comme tout le monde — et remarque la fatigue de Laina, envie
  grandissante (encore timide) de prendre soin d'elle en retour.
- **Sheera** (halfeline scoute, dagues) : publique = bavarde, joueuse, grimpe sur les plus grands
  "comme des montures", terrifiante au combat. Secret = la blague n'en est qu'a moitie une — elle
  aime vraiment etre portee/tenue par quelqu'un de bien plus grand, et vise de plus en plus Mina
  en particulier sans jamais l'admettre serieusement.

Les trois secrets se repondent (Laina veut etre prise en charge, Mina veut prendre soin de
quelqu'un et remarque Laina, Sheera est attiree par Mina) sans jamais se contredire ni forcer une
direction — pure texture optionnelle, le joueur choisit d'y preter attention ou non. Coherent avec
la consigne finale du scenario ("Les adieux") qui laisse explicitement le joueur libre de la
tournure de fin pour chaque relation.

## 4. Verification

Charge via un script Java direct (`ChatFileStorageAdapter.loadScenario` + `ChatSession.fresh`) :
- 3 PNJ charges correctement (laina/mina/sheera).
- 26 actes-feuilles confirmes, numerotation 1.1 a 13.2.
- Tours d'ouverture verifies : etablissent bien qui est Alex, ou il/elle se trouve, qui est present
  (Laina nommee) et le but general — lecon retenue de l'audit d'introduction plus tot dans la
  journee (`evols/2026-07-15-2157-...`), appliquee des la premiere ecriture cette fois plutot que
  corrigee apres coup.

Fichier verifie UTF-8 sans BOM (`#SC` en tete, pas de signature BOM).

## 5. Pas encore fait

Jamais teste avec un vrai LLM (`chat.bat` non relance sur ce scenario). Bon candidat pour tester en
conditions reelles le mecanisme d'interjection (3 PNJ presents simultanement la plupart du temps)
juste implemente.
