# demo-lib : le marché

Montre une bibliothèque **commune à plusieurs scénarios** : `{pick random /lib/evenements}` lit le fichier
`lib/evenements.txt`, dans le dossier `lib/` **à côté des dossiers de scénarios** (ici `chatscenarios/demos/lib/`).

- `/lib/nom` remplace `nom` partout où une bibliothèque est attendue : `{pick …}` et `{repeat …}`. Un sous-dossier est
  permis (`/lib/lieux/ville`). Les noms ne contiennent que lettres, chiffres, `_` et `-`.
- Le fichier a le format d'un fichier de `picks/` : des paragraphes séparés par `---`.
- N'importe quel scénario du même dossier peut utiliser le même fichier. Le dossier `lib/` n'apparaît pas dans la liste des
  scénarios.
- Le tirage est figé pour la partie, comme un `pick` ordinaire : modifier `lib/evenements.txt` ne change pas une partie en
  cours (« Recommencer » pour retirer).
- Un scénario envoyé à quelqu'un doit être accompagné du dossier `lib/` : sans lui, le chargement échoue en nommant le fichier
  manquant.

À regarder : le tableau des tirages (🎲) montre la bibliothèque sous le nom `/lib/evenements`.
Guide : §2.6.
