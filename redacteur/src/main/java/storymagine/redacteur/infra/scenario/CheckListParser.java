package storymagine.redacteur.infra.scenario;

import storymagine.redacteur.coeur.domaine.scenario.Check;
import storymagine.redacteur.coeur.domaine.scenario.CheckList;
import storymagine.redacteur.coeur.domaine.scenario.Constraint;
import storymagine.redacteur.coeur.domaine.scenario.ConstraintList;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses checks.md and constraints.md.
 * Sections: ## PLAN / ## RÉDACTION (h2 headers).
 * Bullets before any section header are GLOBAL — injected into both plan and writer lists.
 */
class CheckListParser {

    enum Section { GLOBAL, PLAN, WRITER }

    static CheckList parseChecks(String content) {
        var plan   = new ArrayList<String>();
        var writer = new ArrayList<String>();
        parse(content, plan, writer);
        return new CheckList(plan.stream().map(Check::new).toList(),
                             writer.stream().map(Check::new).toList());
    }

    static ConstraintList parseConstraints(String content) {
        var plan   = new ArrayList<String>();
        var writer = new ArrayList<String>();
        parse(content, plan, writer);
        return new ConstraintList(plan.stream().map(Constraint::new).toList(),
                                  writer.stream().map(Constraint::new).toList());
    }

    private static void parse(String content, List<String> plan, List<String> writer) {
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
