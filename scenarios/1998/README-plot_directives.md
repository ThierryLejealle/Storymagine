# Plot directives — faits d'intrigue déclaratifs

Les `plot_directives` permettent d'injecter un fait structurant dans la mémoire de l'histoire
au moment où un chapitre commence. Contrairement aux états extraits automatiquement du texte
(bloc `[État actuel]`), ce sont des faits **déclarés par l'auteur** — ils s'imposent au LLM
et ne sont jamais écrasés par l'extraction automatique.

---

## Syntaxe

Dans `scenario.yaml`, au niveau d'un chapitre :

```yaml
plot_directives:
  - "Entité → description du nouvel état"   # ajoute ou met à jour
  - "-Entité"                                # efface le fait (ex : après résolution)
```

**Règles :**
- `Entité` : nom libre, mais consistant entre chapitres (c'est la clé du dictionnaire).
- `→` : séparateur obligatoire (flèche Unicode U+2192).
- Plusieurs directives sur un même chapitre : liste YAML normale.
- Un même fait peut être mis à jour autant de fois que nécessaire en changeant la valeur.
- Le préfixe `-` (sans espace avant `→`) supprime la clé — le fait disparaît des chapitres
  suivants. À utiliser après résolution (réparation, décès, changement de situation).

---

## Comportement

| Moment        | Ce qui se passe |
|---------------|-----------------|
| Début du chapitre | Les directives sont appliquées AVANT tout appel LLM (planner, writer, critic…) |
| Chapitres suivants | Les faits restent actifs automatiquement jusqu'à suppression explicite |
| Dream / what_if | Les directives s'appliquent mais sont incluses dans le snapshot/restore — un fait ajouté dans un rêve disparaît après la restauration |
| Injection | Visible dans le bloc `[Faits d'intrigue]` de l'état des entités, injecté dans le planificateur et le rédacteur |

---

## Exemples

### Introduire un problème mécanique (chapitre 3)
```yaml
- id: j3
  title: "Turbulences"
  plot_directives:
    - "Moteur Spitfire Pierre → claquement en piqué, perte de puissance à l'accélération"
```

### Faire évoluer l'état (chapitre 5 : le problème empire)
```yaml
- id: j5
  title: "Limite"
  plot_directives:
    - "Moteur Spitfire Pierre → vibrations constantes, fumée blanche intermittente"
```

### Résoudre le fait (chapitre 6 : Jules répare)
```yaml
- id: j6
  title: "La Dernière Soirée"
  plot_directives:
    - "-Moteur Spitfire Pierre"
```

### Blessure d'un personnage
```yaml
- id: j4
  title: "La Chute"
  plot_directives:
    - "Henri Leclair → blessé à l'épaule droite, bras en écharpe"
    - "Spitfire Henri → hors service, réparation 48h"
```

---

## Ce que le LLM voit

Les faits injectés apparaissent dans le bloc **État actuel des entités** transmis au
planificateur et au rédacteur, sous la section `[Faits d'intrigue]` :

```
Pierre : épuisé, 6 sorties en 5 jours
→ Combat violent ce matin

[Faits d'intrigue]
Moteur Spitfire Pierre : claquement en piqué, perte de puissance à l'accélération
```

Le LLM les distingue clairement des états extraits du texte et les traite comme des
contraintes narratives à respecter.
