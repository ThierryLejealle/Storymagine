# 2026-11-03 - Tableau des tirages (🎲) : une section par itération de `repeat`, morceaux et éléments distingués

## Évolution demandée
Le tableau 🎲 était illisible pour un `repeat` :
1. Le `repeat` formait une ligne unique avec tous ses éléments, et les `pick` / `cast` écrits dans le bloc étaient des
   lignes séparées sans lien avec la copie qui les avait tirés : impossible de voir ce qui constitue chaque itération.
2. Les paragraphes étaient joints par un simple retour à la ligne, et les lignes `-` entre les morceaux d'un élément
   (`item1`, `item2`, `item3`) ressemblaient au séparateur entre éléments : `a3` et `b1` semblaient collés.

Décisions prises avec l'auteur : une ligne de titre par copie (`defis 2/3`) puis, dessous et dans l'ordre, l'élément
du bloc, les `pick` et les `cast` de cette copie ; les morceaux d'un élément nommés `item1`, `item2`… un par ligne ;
les paragraphes d'un `{pick 3 x}` numérotés et séparés par un filet.

## Ce qui a été touché
- Domaine `pick` : `PickScope` (nouveau : bloc, libellé, copie, nombre de copies), `PickDraw` (+ `scope`, constructeur
  à trois arguments conservé), `RepeatExpansion` (porte un `CopySpan` par copie au lieu d'une liste de tirages ; sa
  méthode `draws()` donne l'élément de chaque copie), `RepeatExpander` (**un tirage par copie** au lieu d'un par bloc,
  position de chaque copie dans le texte déplié, **sans rien ajouter au texte**), `PickResolver` (retrouve la copie d'un
  `pick` par la position de sa commande, range les tirages par copie : `inReadingOrder`), `CastResolver` (la ligne interne
  d'ouverture de copie porte la portée, les `cast` de la copie la reprennent).
- Infra : `PickDrawsFile` écrit la portée en troisième mot de l'en-tête (`@@ repeat-random defis 2:defis:3/5`) et relit
  l'ancien format (sans portée = hors copie).
- Web : `ChatWebServer` (`GET /picks` renvoie `group`, `labelled` et `items` découpés en morceaux avec `PickParagraph`),
  `chat.html` (titre par copie, « hors boucle » au retour, éléments numérotés séparés d'un filet, morceaux `item1`…).
- Tests : `PickDrawsGroupingTest` (nouveau : regroupement avec un `pick` et un `cast` dans une boucle, `pick` avant,
  entre et après des blocs, deux blocs jamais confondus, morceaux d'un élément), `PickDrawsFileTest` (portée aller-retour,
  ancien fichier), `ChatWebServerEndpointsTest` (le JSON de `/picks`), et quatre tests passés de « un tirage par bloc »
  à « un tirage par copie » (`RepeatExpanderTest` x2, `ChatFileStoragePicksTest`, `CastScenarioLoadingTest`).
  Le rendu HTML a été vérifié avec Node sur des lignes d'exemple (2 itérations de 3 morceaux, un pick à 2 paragraphes,
  une ligne hors boucle).
- Documentation : guide §2.6 et §2.6 bis (description du tableau) ; `specs/chat.md`.
- Aucun prompt d'agent modifié.

## Résultat
578 tests verts. Le texte déplié, donc ce que lit le jeu, est identique à avant : seules l'organisation et l'affichage
des tirages changent. Une partie commencée avant cette fonction se relit : ses tirages s'affichent comme avant (sans
titres de copie, le `repeat` en une ligne).
**Non vérifié à la main** : l'aspect dans le navigateur (couleurs, filets). La page est lue dans les ressources compilées :
il faut reconstruire et relancer le chat pour la voir.
**Limite connue** : dans une copie, les `cast` viennent après les `pick` même si le `cast` est écrit avant (les casts
s'exécutent après les picks) ; les `cast` hors boucle sont en fin de tableau.
