# 2026-07-14 19h37 - Chat : règle "Turn scope" - pause quand l'action vise directement le joueur

## 1. Évolution demandée
Suite à la fiche `2026-07-14-1858-chat-longueur-reponses-variables.md`, l'utilisateur a observé un
second défaut de rythme narratif : le LLM enchaîne plusieurs événements dans un seul tour (ex. il
ouvre une porte ET décrit ce qu'il y a derrière ET réagit à la scène), sans laisser au joueur la
possibilité de décrire lui-même la suite.

Trois itérations de consultation Fable (même conversation d'agent, poursuivie via SendMessage) ont
affiné le critère :
1. Première proposition : règle stricte "un seul beat par tour, toujours" — rejetée par
   l'utilisateur, qui juge le comportement actuel globalement bon et ne veut pas le casser.
2. Deuxième proposition : exception sur une liste fermée de "seuils" (verrou, porte, question
   répondue, objet révélé) — l'utilisateur précise que ce n'est pas le type d'objet qui compte,
   c'est si l'action vise LUI (le joueur) directement.
3. Version finale retenue : le critère devient "découverte partagée du décor/monde" (le LLM
   continue de narrer librement, comportement inchangé) vs "action dirigée directement vers le
   joueur" (faire entrer le joueur chez soi, lui tendre un objet, lui répondre, lui donner accès à
   quelque chose — là, le LLM s'arrête juste après l'action et laisse le joueur écrire la suite).
   Fable illustre les deux cas avec le même geste (une porte qui s'ouvre) pour que le modèle
   généralise sur le bon axe (qui vit la suite) plutôt que sur l'objet (une porte).

Delta présenté à l'utilisateur (texte exact, raison, objectif) et validé ("Genial. En esperant
que gemma 4 obéisse") avant écriture.

## 2. Ce qui a été touché
- `chat/src/main/java/storymagine/chat/coeur/domaine/session/ChatPromptBuilder.java`,
  constante `SYSTEM_INTRO` : ajout d'un nouveau bloc "Turn scope: ..." avec son exemple (porte
  partagée RIGHT / porte dirigée vers le joueur WRONG-RIGHT), inséré juste avant le bloc
  "Length: ..." existant (règle de longueur, cf. fiche 1858) et après le paragraphe d'ouverture.
  Ordre voulu : la portée narrative (quoi raconter) doit être lue avant la longueur (combien de
  mots), sinon la règle de longueur ("an action sequence can take one or two full paragraphs")
  pourrait être lue comme une licence à empiler plusieurs événements pour remplir le paragraphe.
- Compilation vérifiée (`mvn -pl chat -am compile`) : OK.

## 3. Résultat
Changement de prompt uniquement. Risques identifiés par Fable à surveiller à l'usage (modèle
gemma 4 local, non traités ici) :
- immobilisme / micro-beats si le modèle sur-applique la pause même sur des actions sans
  destinataire direct,
- rupture artificielle de beats composés naturellement liés (un coup porté + son impact),
- tendance à finir chaque tour par une question méta au joueur,
- tension possible avec la règle de longueur sur les scènes d'action dirigées vers le joueur
  (si le modèle tente de compenser la coupure courte par du remplissage).

Pas de test unitaire dédié (contenu de prompt). À valider par l'utilisateur en session de chat
réelle avec gemma 4.
