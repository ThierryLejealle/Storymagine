package storymagine.redacteur.coeur.domaine.agent.commun.planenricher;

import java.util.ArrayList;
import java.util.List;

/**
 * Injects per-sequence directive fields into a plan JSON array under configurable labels.
 * Operates on raw JSON strings without any JSON library dependency.
 */
public final class PlanEnricher {

    private PlanEnricher() {}

    /**
     * Enriches each sequence object in {@code planJson} with fields from {@code directives},
     * using the labels defined in {@code config}.
     * Sequences are matched by their "sequence" integer field (1-based index into directives).
     * Blank values are silently skipped.
     */
    public static String enrich(String planJson, List<SequenceDirective> directives, PlanEnrichmentConfig config) {
        if (planJson == null || planJson.isBlank()) return planJson != null ? planJson : "";
        if (directives == null || directives.isEmpty() || config.mappings().isEmpty()) return planJson;

        int arrayStart = planJson.indexOf('[');
        int arrayEnd   = planJson.lastIndexOf(']');
        if (arrayStart < 0 || arrayEnd <= arrayStart) return planJson;

        String json = planJson.substring(arrayStart, arrayEnd + 1);
        List<String> enrichedObjects = new ArrayList<>();
        int depth    = 0;
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
                    String annot  = (seqNum >= 1 && seqNum <= directives.size())
                        ? buildAnnotation(directives.get(seqNum - 1), config)
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

    private static String buildAnnotation(SequenceDirective directive, PlanEnrichmentConfig config) {
        List<String> parts = new ArrayList<>();
        for (FieldMapping mapping : config.mappings()) {
            String value = switch (mapping.source()) {
                case CHECKS      -> directive.checks();
                case FOCUS       -> directive.focus();
                case LORE        -> directive.lore();
                case CONSTRAINTS -> directive.constraints();
            };
            if (value != null && !value.isBlank())
                parts.add("  \"" + escape(mapping.jsonLabel()) + "\": \"" + escape(value) + "\"");
        }
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

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
