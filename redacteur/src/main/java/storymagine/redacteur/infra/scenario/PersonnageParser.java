package storymagine.redacteur.infra.scenario;

import storymagine.redacteur.coeur.domaine.scenario.personnage.Personnage;

/**
 * Parses a character .md file into a Personnage.
 * Structure: global content / #PLAN / #WRITER
 * Sub-sections within #PLAN or #WRITER use ## markers (treated as content).
 */
class PersonnageParser {

    static Personnage parse(String id, String content) {
        TagElementParser.TagBlock b = TagElementParser.parseSingleBlock(content);
        return new Personnage(id, b.globalContent(), b.planContent(), b.writerContent());
    }
}
