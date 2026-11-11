# demo-scene-style : un style par scène, un détail au hasard

Montre `{style nom}` et `{N%}` dans le texte d'une scène.

- `style.txt` est toujours envoyé ; le style de la scène (`styles/auberge.txt`, `styles/foret-sombre.txt`) s'ajoute après lui, pour cette scène et ses sous-scènes.
- `{30%}` en tête d'une ligne du scénario : la ligne est gardée ou supprimée UNE FOIS, au début de la partie (comme un `pick`) ; tous les PNJ la lisent à chaque tour de la scène. L'écrire comme un état, pas comme une action ponctuelle.
- Les `{N%}` des fichiers de style, eux, sont tirés à chaque réponse.

Guide : §2.10 et §2.10 bis.
