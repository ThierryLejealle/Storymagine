package storymagine.testllm.coeur.domaine;

import java.util.List;

/**
 * Les trois passes de benchmark standard : haiku (petit), moyen, grand.
 */
public final class Passes {

    private Passes() {}

    public static final PasseBench HAIKU = new PasseBench(
        "haiku",
        "Tu es un poète. Réponds uniquement avec le poème — sans titre, sans explication, sans commentaire.",
        "Écris un haïku sur la neige."
    );

    public static final PasseBench MOYEN = new PasseBench(
        "moyen",
        """
        Tu es le planificateur de scènes d'un roman. Un plan détaillé contient exactement :
        - des étapes d'action (3 à 5, numérotées) et une taille cible en mots
        - des éléments sensoriels et de focus à développer
        - des conseils sur le ton et le rythme

        Produis ce plan pour la séquence donnée. En français.""",
        """
        ### Objectif du livre
        # L'As du Ciel

        Juin 1944. Pierre Moreau, 24 ans, pilote de la FAFL (Forces Aériennes Françaises Libres),
        vole sur Spitfire depuis la base de Thorney Island, sur la Manche. Pendant sept jours il
        combat, survit, aime ses camarades en silence. Le huitième jour il ne rentre pas.

        ## Personnages
        Pierre Moreau — pilote, grand, maigre, mains de pêcheur, yeux gris-vert.
        Commandant Bertrand — chef d'escadrille, économe de mots, regard qui jauge.
        Jules Meca — mécanicien, petit, trapu, moustache improbable, blagueur.

        ## Séquence à planifier
        Pierre arrive à la base à l'aube du 6 juin. Le bruit des moteurs Merlin résonne au loin.
        Sur le tarmac, les Spitfires sont alignés dans la brume, hélices immobiles.
        Il pose son sac et regarde le ciel.

        Planifie cette séquence en détail."""
    );

    public static final PasseBench GRAND = new PasseBench(
        "grand",
        """
        Tu es un écrivain littéraire. Tu exécutes fidèlement le plan fourni — chaque point du plan
        DOIT apparaître dans le texte, dans l'ordre indiqué. Tu ne prends aucune décision narrative :
        ton seul rôle est de transformer le plan en prose française de haute qualité.
        Tu ne produis QUE le texte narratif — aucun commentaire, aucun méta-texte.
        Chaque séquence fait au minimum 1 000 mots — développe les scènes avec profondeur et précision sensorielles.
        La première phrase commence par une image sensorielle ou l'environnement — jamais par le prénom
        d'un personnage ni un pronom ('Il', 'Elle', 'Ils').""",
        """
        ### Personnages présents
        Pierre Moreau — pilote, grand, maigre, mains de pêcheur héritées d'un père jamais vu.
        Yeux gris-vert, voix grave, accent du Sud-Ouest atténué qui revient sous le stress.
        Tendresse immense pour les choses ordinaires. La guerre rend cette sensibilité plus aiguë.
        Quand il attend : regarde ses mains.

        Jules Meca — mécanicien, petit, trapu, moustache improbable.
        Toujours une blague de trop. Il considère le Grey Ghost comme son œuvre.

        ### Plan de la séquence
        1. L'arrivée et l'atmosphère. Pierre arrive sur le tarmac dans la brume matinale.
           Le grondement sourd des moteurs Merlin au loin établit une présence sonore oppressante.
        2. Le décor en données. Il s'arrête et commence son observation systémique.
           La ligne de Spitfires immobiles, l'orientation des hélices, les reflets froids sur le métal.
        3. Le calme forcé. Il dépose son sac avec un geste précis.
        4. Ancrage dans le présent. Son regard se fixe sur la Manche. Pause d'observation analytique.

        ### Taille cible
        1 200 mots

        ### Séquence à écrire
        Pierre arrive à la base à l'aube du 6 juin. Le bruit des moteurs Merlin résonne au loin.
        Sur le tarmac, les Spitfires sont alignés dans la brume, hélices immobiles.
        Il pose son sac et regarde le ciel.

        Écris le texte de cette séquence."""
    );

    public static final List<PasseBench> TOUTES = List.of(HAIKU, MOYEN, GRAND);
}
