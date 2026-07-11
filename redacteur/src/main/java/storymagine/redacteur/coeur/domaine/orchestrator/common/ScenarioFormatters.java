package storymagine.redacteur.coeur.domaine.orchestrator.common;

import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Requirement;
import storymagine.redacteur.coeur.domaine.scenario.RequirementList;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.scenario.Sequence;
import storymagine.redacteur.coeur.domaine.scenario.focus.FocusElement;
import storymagine.redacteur.coeur.domaine.scenario.focus.FocusInline;
import storymagine.redacteur.coeur.domaine.scenario.focus.FocusItem;
import storymagine.redacteur.coeur.domaine.scenario.focus.FocusRef;
import storymagine.redacteur.coeur.domaine.scenario.lore.LoreElement;
import storymagine.redacteur.coeur.domaine.scenario.lore.LoreInline;
import storymagine.redacteur.coeur.domaine.scenario.lore.LoreItem;
import storymagine.redacteur.coeur.domaine.scenario.lore.LoreRef;
import storymagine.redacteur.coeur.domaine.scenario.personnage.Personnage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Static helpers: Scenario domain objects → LLM-ready strings. */
public final class ScenarioFormatters {

    private ScenarioFormatters() {}

    /** Returns goal.md content, falling back to the scenario title if absent. */
    public static String bookGoal(Scenario scenario) {
        String goal = scenario.bookGoal();
        return (goal != null && !goal.isBlank()) ? goal : scenario.config().title();
    }

    /** Returns keep_phrases.md content — null if absent or empty. */
    public static String keepPhrases(Scenario scenario) {
        return scenario.keepPhrases();
    }

    private static String joinContent(String global, String specific) {
        boolean hasGlobal   = global   != null && !global.isBlank();
        boolean hasSpecific = specific != null && !specific.isBlank();
        if (hasGlobal && hasSpecific) return global + "\n" + specific;
        if (hasGlobal)   return global;
        if (hasSpecific) return specific;
        return "";
    }

    public static String personnages(List<Personnage> personnages, boolean forWriter) {
        if (personnages == null || personnages.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Personnage p : personnages) {
            String specific = forWriter ? p.writerContent() : p.planContent();
            String content  = joinContent(p.globalContent(), specific);
            if (content != null && !content.isBlank()) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append("#### ").append(formatPersonnageName(p.id())).append("\n");
                sb.append(shiftHeadings(content));
            }
        }
        return sb.toString();
    }

    private static String formatPersonnageName(String id) {
        if (id == null || id.isBlank()) return id;
        return Arrays.stream(id.split("[_\\-]"))
                .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(Collectors.joining(" "));
    }

    private static String shiftHeadings(String content) {
        return content.replace("## ", "##### ");
    }

    public static String focusText(List<FocusItem> items, boolean forWriter) {
        if (items == null || items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (FocusItem item : items) {
            String content = switch (item) {
                case FocusRef ref -> {
                    FocusElement el = ref.resolved();
                    String specific = forWriter ? el.writerContent() : el.planContent();
                    yield joinContent(el.globalContent(), specific);
                }
                case FocusInline inline -> inline.text();
            };
            if (content != null && !content.isBlank()) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(content);
            }
        }
        return sb.toString();
    }

    /**
     * Check side of each focus item, for Critics verifying that focus elements were actually
     * used. Optional — items without a check (no "# CHECK" section, no "|" in inline text)
     * are simply skipped. No plan/writer split: one check applies to both phases.
     */
    public static List<String> focusChecks(List<FocusItem> items) {
        if (items == null || items.isEmpty()) return List.of();
        List<String> result = new ArrayList<>();
        for (FocusItem item : items) {
            String check = switch (item) {
                case FocusRef ref -> ref.resolved() != null ? ref.resolved().checkContent() : null;
                case FocusInline inline -> inline.check();
            };
            if (check != null && !check.isBlank()) result.add(check);
        }
        return result;
    }

    public static String loreText(List<LoreItem> items, boolean forWriter) {
        if (items == null || items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (LoreItem item : items) {
            String content = switch (item) {
                case LoreRef ref -> {
                    LoreElement el = ref.resolved();
                    if (el == null) yield "";
                    String specific = forWriter ? el.writerContent() : el.planContent();
                    yield joinContent(el.globalContent(), specific);
                }
                case LoreInline inline -> inline.text();
            };
            if (content != null && !content.isBlank()) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(content);
            }
        }
        return sb.toString();
    }

    public static String planConstraints(RequirementList requirements) {
        return requirements.planRequirements().stream()
                .map(Requirement::constraint)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.joining("\n"));
    }

    public static String writerConstraints(RequirementList requirements) {
        return requirements.writerRequirements().stream()
                .map(Requirement::constraint)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.joining("\n"));
    }

    public static List<String> planChecks(RequirementList requirements) {
        return requirements.planRequirements().stream()
                .map(Requirement::check)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());
    }

    public static List<String> writerChecks(RequirementList requirements) {
        return requirements.writerRequirements().stream()
                .map(Requirement::check)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());
    }

    // ── Merged accessors: scenario + chapter (plan phase) ────────────────────

    public static String planConstraints(Scenario scenario, Chapter chapter) {
        return Stream.of(scenario.requirements(), chapter.defaults().requirements())
                .flatMap(rl -> rl.planRequirements().stream())
                .map(Requirement::constraint)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.joining("\n"));
    }

    public static List<String> planChecks(Scenario scenario, Chapter chapter) {
        return Stream.of(scenario.requirements(), chapter.defaults().requirements())
                .flatMap(rl -> rl.planRequirements().stream())
                .map(Requirement::check)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());
    }

    // ── Merged accessors: scenario + chapter (chapter-level write phase) ─────

    public static String writerConstraints(Scenario scenario, Chapter chapter) {
        return Stream.of(scenario.requirements(), chapter.defaults().requirements())
                .flatMap(rl -> rl.writerRequirements().stream())
                .map(Requirement::constraint)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.joining("\n"));
    }

    public static List<String> writerChecks(Scenario scenario, Chapter chapter) {
        return Stream.of(scenario.requirements(), chapter.defaults().requirements())
                .flatMap(rl -> rl.writerRequirements().stream())
                .map(Requirement::check)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());
    }

    // ── Merged accessors: scenario + chapter + sequence (sequence-level) ─────

    public static String writerConstraints(Scenario scenario, Chapter chapter, Sequence sequence) {
        return Stream.of(scenario.requirements(), chapter.defaults().requirements(),
                         sequence.additions().requirements())
                .flatMap(rl -> rl.writerRequirements().stream())
                .map(Requirement::constraint)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.joining("\n"));
    }

    public static List<String> writerChecks(Scenario scenario, Chapter chapter, Sequence sequence) {
        return Stream.of(scenario.requirements(), chapter.defaults().requirements(),
                         sequence.additions().requirements())
                .flatMap(rl -> rl.writerRequirements().stream())
                .map(Requirement::check)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * Injects per-sequence metadata (points à vérifier, focus, lore) as extra JSON fields
     * inside each sequence object of the plan JSON, co-located with beats/sensoriels/ton_et_rythme.
     * Uses the check() side of Requirement — this JSON is read by PlanCoherenceCritic, a verifier.
     * Characters are not injected — they are shared globally across sequences.
     */
    public static String enrichPlanJson(String planJson, List<Sequence> sequences) {
        if (planJson == null || planJson.isBlank()) return planJson != null ? planJson : "";
        if (sequences == null || sequences.isEmpty()) return planJson;

        int arrayStart = planJson.indexOf('[');
        int arrayEnd   = planJson.lastIndexOf(']');
        if (arrayStart < 0 || arrayEnd <= arrayStart) return planJson;

        String json = planJson.substring(arrayStart, arrayEnd + 1);
        List<String> enrichedObjects = new ArrayList<>();
        int depth = 0;
        int objStart = -1;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    String obj    = json.substring(objStart, i + 1);
                    int    seqNum = extractSeqNum(obj);
                    String annot  = (seqNum >= 1 && seqNum <= sequences.size())
                        ? buildSeqAnnotation(sequences.get(seqNum - 1))
                        : "";
                    enrichedObjects.add(annot.isBlank()
                        ? obj
                        : obj.substring(0, obj.length() - 1) + ",\n" + annot + "\n}");
                    objStart = -1;
                }
            }
        }
        return "[\n" + String.join(",\n", enrichedObjects) + "\n]";
    }

    private static String buildSeqAnnotation(Sequence seq) {
        List<String> parts = new ArrayList<>();
        List<String> checks = planChecks(seq.additions().requirements());
        if (!checks.isEmpty()) {
            String arr = checks.stream()
                .map(ch -> "\"" + jsonEscape(ch) + "\"")
                .collect(Collectors.joining(", "));
            parts.add("  \"points_a_verifier\": [" + arr + "]");
        }
        String focus = focusText(seq.additions().focus(), false);
        if (!focus.isBlank()) parts.add("  \"focus\": \"" + jsonEscape(focus) + "\"");
        String lore = loreText(seq.additions().lore(), false);
        if (!lore.isBlank()) parts.add("  \"lore\": \"" + jsonEscape(lore) + "\"");
        return String.join(",\n", parts);
    }

    private static int extractSeqNum(String obj) {
        String pattern = "\"sequence\"";
        int ki = obj.indexOf(pattern);
        if (ki < 0) return 0;
        int colon = obj.indexOf(':', ki + pattern.length());
        if (colon < 0) return 0;
        int s = colon + 1;
        while (s < obj.length() && Character.isWhitespace(obj.charAt(s))) s++;
        int e = s;
        while (e < obj.length() && Character.isDigit(obj.charAt(e))) e++;
        try { return Integer.parseInt(obj.substring(s, e)); } catch (NumberFormatException ex) { return 0; }
    }

    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    /**
     * Merges character sheets from chapter defaults + all sequences (dedup by id, defaults first).
     * Returns formatted text for the plan phase (global + planContent), or empty string if none.
     */
    public static String planCharactersText(Chapter chapter) {
        LinkedHashMap<String, Personnage> seen = new LinkedHashMap<>();
        chapter.defaults().characters().forEach(p -> seen.put(p.id(), p));
        chapter.sequences().forEach(seq ->
                seq.additions().characters().forEach(p -> seen.putIfAbsent(p.id(), p)));
        return personnages(new ArrayList<>(seen.values()), false);
    }

    /** Description of a single sequence for the planner (directive + focus/lore/characters/constraints). */
    public static String singleSequenceDescription(Sequence seq) {
        StringBuilder sb = new StringBuilder(seq.directive() != null ? seq.directive().trim() : "");
        String f = focusText(seq.additions().focus(), false);
        if (!f.isBlank()) sb.append("\nÉléments à utiliser (focus) : ").append(f);
        String l = loreText(seq.additions().lore(), false);
        if (!l.isBlank()) sb.append("\nInformations utiles (lore) : ").append(l);
        String c = personnages(seq.additions().characters(), false);
        if (!c.isBlank()) sb.append("\nPersonnages présents : ").append(c);
        String cons = planConstraints(seq.additions().requirements());
        if (!cons.isBlank()) sb.append("\nContraintes : ").append(cons);
        return sb.toString();
    }

    public static List<String> sequenceDescriptions(List<Sequence> sequences) {
        return sequences.stream()
                .map(ScenarioFormatters::singleSequenceDescription)
                .filter(d -> !d.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * Numbered per-sequence directives (raw author text, not the enriched planner description
     * with focus/lore/constraints folded in) — for critics verifying beat-level fidelity to
     * what the author literally asked for in each sequence.
     */
    public static String sequenceDirectivesBlock(List<Sequence> sequences) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sequences.size(); i++) {
            String directive = sequences.get(i).directive();
            if (directive == null || directive.isBlank()) continue;
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("Sequence ").append(i + 1).append(": ").append(directive.trim());
        }
        return sb.toString();
    }
}
