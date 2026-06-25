# But du projet
Un outil pour rédiger des histoires complètes à partir d'éléments de scénarios.
L'outil utilisera un modèle LLM local, par exemple via Ollama.
Il mettre en oeuvre un logique multi-agent pour évaluer, critiquer et guider la rédaction
L'outil devra s'adapter aux contraintes des petits modèles, notamment un contexte limité

# Style de codage
Le projet est en Anglais.
Le projet est codé en Java, en architecture hexagonale : privilégier un découpage par modules fonctionnels et des POJO
  - Exemple : un module "commun" (pour l'adapteur ollama), un module "testeur" pour tester les LLM, un module redacteur pour la fonctionnalité de rédaction d'histoire, ..
Le coeur du projet (/coeur) doit être conçu en mode POJO : 
  - Une couche Service qui est le point d'entrée du coeur. Le Service expose une interface et exécute des Commandes (pour éviter des méthodes avec trop d'attributs) : /service
  - Une couche Domaine (/domaine) qui contient des POJO les plus indépendants possibles. Ces objets sont préparés et mis en oeuvre par le Service.
     - Les Agents dans Domaine doivent être des POJO, dans leur package avec une classe explicite pour l'entrée, une pour la sortie, une pour construire le prompt. Chaque agent a un .MD de documentation qui porte son nom et qui explicite son fonctionnement dans le détail et les contraintes propres à l'agent. Chaque classe agent contient une javadoc très courte qui donne le role de l'agent. 
  - Une logique Port/Adapteur. On limite les ports dans le Domaine au strict minimum
  - Le coeur n'a aucune adhérence vers le reste du projet ou vers d'autre modules fonctionnels ou vers autre chose que du java pur (pas de framework). Toute dépendance doit être résolue par la formalisation d'un besoin via un port, sans exception. Par exemple : log, date du jour, repertoire d'execution, infrastructure de la machine, etc.. quel que soit l'appel, il passe par un port : le domain en particulier est purement fonctionnel (on peut le relancer dix fois on aura dix fois le meme résultat avec des adapteurs bouchonnés par exemple)
  - Si une méthode prend plus de 4 paramètres en entrée, il faut étudie l'intérèt de rationnaliser (faire émerger des objets portant des concepts qui regroupent les attributs par exemple)

# Un projet en DDD
On s'inspire des pratiques DDD pour le coeur
Les Beans du Coeur doivent avoir un nom "métier"
Etudier s'il ne faut pas transformer les listes en objet. Ex : liste de personnages s'il y a des traitements ou règles propres à la liste
De même pour toute classe du coeur de + de 5 attributs, il faut se poser la question de faire émerger des concepts qui permettent de mieux structurer. Ex : faire émerger un objet Adresse plutot que de laisser les attributs en vrac dans une classe.
Se poser la question de faire une classe spécifique pour les identifiants métiers des objets du coeur (quand ils en ont un !)

# Structure

/module
  /ui
    /cli 

  /coeur
    /service
    /domaine
    /ports

  /infra

Il y a un module 'commun' et des modules fonctionnels.
1. Tous les modules peuvent importer 'commun' ; Le coeur d'un module fonctionnel a le droit d'utiliser des ports ou objet de /commun/coeur (mais pas /commun/ui ou /commun/infra). Ceci ne viole pas la règle
2. Sinon, le coeur ne dépend de RIEN d'exterieur, dans une logique fonctionelle la plus pure possible.Le coeur est mis en oeuvre via un Service dans /service. Le Service est un sorte d'aggregate en logique service qui met en oeuvre des objets du domaine : ce sont eux qui réalisent les traitements. On n'utilise un port dans domain que s'il n'y a pas moyen de faire 'raisonnablement' autrement. Les ports sont plutot utilisés par Service. 
3. Le coeur expose ses besoins via des interfaces fonctionelles ; chaque classe service expose aussi une interface explicitant les services qu'il expose aux ui. 
Exception autorisée : le coeur peut importer /commun/coeur.
4. Une classe par module assure l'assemble du coeur et de ses adapteurs. 
5. Si un adapteur fait "plus" que ce que demande un port, je veux une interface explicite pour formaliser ces 'capacités' additionelles. Par exemple, l'adapteur Ollama permet de faire plus que simplement appeler un LLM (il permet de lister les modèles, ...). Il faut un port pour cela. L'adapteur implémente l'ensemble des ports. Ne pas faire un super Port qui répond à des besoins différents.
6. Les beans des ports sont /ports avec le port
7. Un fichier propriétes par module à la racine du module et un fichier propriétés à la racine du projet pour les propriétés vraiment communes (pas dans /commun)

# Intégrité inter-modules — OBLIGATOIRE

Tout déplacement ou modification d'une classe publique dans un module DOIT s'accompagner de la mise à jour de TOUS les modules qui l'utilisent dans la même opération.
Avant de valider un changement :
1. Identifier tous les modules qui importent la classe modifiée/déplacée (grep sur le package + nom de classe)
2. Mettre à jour les imports dans chaque module impacté
3. Vérifier la compilation depuis la racine du projet (pas seulement le module modifié) : `mvn compile` sur le parent
Ne jamais considérer un refactoring terminé si un seul module est cassé.

# Règles spécifiques
Claude ne doit JAMAIS modifier un prompt d'un agent sans explicitement demander à l'utilisateur.
Toutes les séances de modification doivent faire l'objet d'une fiche d'évolution dans /evols qui contient 1. Le description de l'évolution demandée 2. Ce qui a été touché. 3. Le résultat. 4. La fiche d'évol est horodatée et contient un mini descript de l'évol dans son texte. Ex : 2026-03-25 14h23 12s - Construction de l'adapteur Ollama
Les spécifications, contenus dans /specs doivent toujours être mises à jour et facile à lire par un humain
Claude ne doit JAMAIS avoir accès au disque en dehors du répertoire contenant le projet
On ne lance jamais de test unitaire avec un vrai agent LLM
On peut mettre des tests d'integration ; ils sont executés uniquement à la demande de l'humain
Privilégier les .bat pour lancer des traitements CLI
Les .bat doivent être STUPIDES : la CLI codée en java dans /cli dans chaque module

# Test
L'hexagonal doit permettre de tester le coeur correctement.
Eviter les tests triviaux : un test doit avoir de la valeur
Si besoin d'un scenario pour tester, il doit etre dans le package test

# STYLE
Privilégie la programmation fonctionelle
Utilise les """ pour les chaines sur plusieurs lignes, comme les prompts