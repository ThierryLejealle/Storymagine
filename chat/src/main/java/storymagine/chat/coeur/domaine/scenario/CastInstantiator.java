package storymagine.chat.coeur.domaine.scenario;

import storymagine.chat.coeur.domaine.activity.ArrivalSpec;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Builds the Cast of a scenario from the character sheets found on disk and the "{npc-arrives ...}"
 * texts written in it : a sheet used as "{npc-arrives goblin Grik, Snak}" is a template, not a
 * character, and each name creates one character from it (id from the name, the sheet's "# Title" line
 * replaced by the name, every "%NPC%" in the sheet replaced by the name). The same name written again
 * for the same template is the same character. Pure : no file is read here.
 */
public final class CastInstantiator {

    public static final String NAME_MARKER = "%NPC%";

    private CastInstantiator() {}

    /** @throws IllegalArgumentException a name clashing with another character, a template also used as a character, or a name given to two templates */
    public static Cast assemble(List<Npc> sheets, List<String> arrivalTexts) {
        Cast sheetCast = new Cast(sheets);
        Map<String, Npc> templates = new LinkedHashMap<>();
        Map<String, String> templateOfName = new LinkedHashMap<>();
        List<String[]> creations = new ArrayList<>(); // {templateId, name}, in order written
        for (String text : arrivalTexts) {
            if (sheetCast.findByName(text.strip()).isPresent()) continue; // plain arrival of a character
            for (Npc sheet : sheets) {
                if (!ArrivalSpec.isTemplateForm(text, sheet.id())) continue;
                templates.putIfAbsent(sheet.id(), sheet);
                for (String name : ArrivalSpec.namesAfterTemplate(text)) {
                    String key = name.toLowerCase(Locale.ROOT);
                    String known = templateOfName.putIfAbsent(key, sheet.id());
                    if (known != null && !known.equals(sheet.id())) {
                        throw new IllegalArgumentException("{npc-arrives} : le nom " + name + " est donne a deux modeles ("
                            + known + " et " + sheet.id() + ").");
                    }
                    if (known == null) creations.add(new String[] {sheet.id(), name});
                }
                break;
            }
        }
        for (String text : arrivalTexts) {
            String plain = text.strip();
            if (templates.containsKey(plain)) {
                throw new IllegalArgumentException("{npc-arrives " + plain + "} : " + plain + " est utilise comme modele ailleurs "
                    + "(avec un nom) : un fichier est soit un personnage, soit un modele.");
            }
        }
        List<Npc> npcs = new ArrayList<>();
        for (Npc sheet : sheets) if (!templates.containsKey(sheet.id())) npcs.add(sheet);
        Cast characters = new Cast(npcs);
        Set<String> usedIds = new LinkedHashSet<>();
        npcs.forEach(n -> usedIds.add(n.id()));
        for (String[] creation : creations) {
            Npc instance = instantiate(templates.get(creation[0]), creation[1]);
            if (characters.findByName(creation[1]).isPresent() || !usedIds.add(instance.id())) {
                throw new IllegalArgumentException("{npc-arrives " + creation[0] + " " + creation[1] + "} : le nom " + creation[1]
                    + " est deja celui d'un autre personnage.");
            }
            npcs.add(instance);
        }
        return new Cast(npcs, templates.keySet());
    }

    static Npc instantiate(Npc template, String name) {
        List<StoryTag> tags = template.tags().stream()
            .map(t -> new StoryTag(t.name(), t.visibility(), named(t.text(), name))).toList();
        return new Npc(idOf(name), name, named(retitled(template.publicInfo(), name), name),
            named(template.secretInfo(), name), tags);
    }

    private static String named(String text, String name) {
        return text.replace(NAME_MARKER, name);
    }

    /** The sheet's first "# Title" line becomes "# Name" (the title of a template is only a label). */
    private static String retitled(String publicInfo, String name) {
        int newline = publicInfo.indexOf('\n');
        String first = newline < 0 ? publicInfo : publicInfo.substring(0, newline);
        if (!first.stripLeading().startsWith("# ")) return publicInfo;
        return "# " + name + (newline < 0 ? "" : publicInfo.substring(newline));
    }

    /** Lowercase ascii letters and digits, the rest folded into single dashes : "Grik le Borgne" gives "grik-le-borgne". */
    static String idOf(String name) {
        String folded = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        String id = folded.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (id.isEmpty()) throw new IllegalArgumentException("{npc-arrives} : le nom \"" + name + "\" ne donne aucun identifiant.");
        return id;
    }
}
