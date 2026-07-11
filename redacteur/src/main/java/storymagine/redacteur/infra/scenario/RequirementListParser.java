package storymagine.redacteur.infra.scenario;

import storymagine.redacteur.coeur.domaine.scenario.Requirement;
import storymagine.redacteur.coeur.domaine.scenario.RequirementList;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses requirements.md.
 * Sections: ## PLAN / ## RÉDACTION (h2 headers).
 * Bullets before any section header are GLOBAL — injected into both plan and writer lists.
 * Each bullet is parsed as a Requirement (see Requirement.parse for the "constraint | check" syntax).
 */
class RequirementListParser {

    enum Section { GLOBAL, PLAN, WRITER }

    static RequirementList parse(String content) {
        var plan   = new ArrayList<String>();
        var writer = new ArrayList<String>();
        parseLines(content, plan, writer);
        return new RequirementList(plan.stream().map(Requirement::parse).toList(),
                                   writer.stream().map(Requirement::parse).toList());
    }

    private static void parseLines(String content, List<String> plan, List<String> writer) {
        content = content.replaceAll("(?s)<!--.*?-->", "");
        Section       section = Section.GLOBAL;
        List<String>  global  = new ArrayList<>();
        StringBuilder current = null;

        for (String raw : content.split("\n")) {
            String line  = raw.strip();
            String upper = line.toUpperCase();

            if (upper.startsWith("## COMMON")) { section = Section.GLOBAL; current = null; continue; }
            if (upper.startsWith("## PLAN"))   { section = Section.PLAN;   current = null; continue; }
            if (upper.startsWith("## R"))      { section = Section.WRITER; current = null; continue; }
            if (line.startsWith("#"))          { continue; }

            List<String> target = switch (section) {
                case GLOBAL -> global;
                case PLAN   -> plan;
                case WRITER -> writer;
            };

            if (line.startsWith("- ")) {
                current = new StringBuilder(line.substring(2).strip());
                target.add(null);
            } else if (!line.isBlank() && current != null) {
                current.append(' ').append(line);
            }

            if (current != null && !target.isEmpty()) {
                target.set(target.size() - 1, current.toString());
            }
        }

        global.removeIf(s -> s == null || s.isBlank());
        plan.addAll(0, global);
        writer.addAll(0, global);
        plan.removeIf(s -> s == null || s.isBlank());
        writer.removeIf(s -> s == null || s.isBlank());
    }
}
