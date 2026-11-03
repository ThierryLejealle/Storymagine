# demo-npc-story : le voisin louche

Montre les sections `## tag` d'une fiche et la commande `{npc-story on|off Nom tag}` : un texte de la fiche qui
s'active et se désactive au fil de l'histoire.

- Dans `characters/lea.txt`, `## froide` et `## amicale` sont **publics** (avant `# SECRET`), `## soupcon` et
  `## chatons` sont **secrets** (après). Le premier mot du titre est le tag, le reste de la ligne est un commentaire
  qui n'est jamais envoyé au LLM. Le texte hors tag (dans chaque zone) est permanent.
- Au départ aucun tag n'est actif. Le titre « Le voisin » active `froide` et `soupcon` : Léa est distante et
  soupçonne Marc.
- L'état **se prolonge** : il ne s'arrête pas à la sortie du titre. Dans « Le soir », les `off` retirent la
  méfiance et les `on` mettent la sympathie publique et le secret tendre à la place.
- Revenir en arrière dans l'histoire redonne l'état de l'acte retrouvé.

À regarder : le ton de Léa avant et après la forêt.
Guide : §2.8 bis, §3.
