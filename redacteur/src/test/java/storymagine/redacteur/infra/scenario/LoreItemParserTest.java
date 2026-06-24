package storymagine.redacteur.infra.scenario;

import org.junit.jupiter.api.Test;
import storymagine.redacteur.coeur.domaine.scenario.lore.LoreElement;
import storymagine.redacteur.coeur.domaine.scenario.lore.LoreInline;
import storymagine.redacteur.coeur.domaine.scenario.lore.LorePool;
import storymagine.redacteur.coeur.domaine.scenario.lore.LoreRef;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoreItemParserTest {

    private static final LorePool POOL = new LorePool(List.of(
            new LoreElement("TRAIN", "global train", null, null),
            new LoreElement("GARE",  "global gare",  null, null)
    ));

    @Test
    void array_notation_with_quotes_resolves_as_ref() {
        // LoreStringDeserializer converts YAML [TRAIN] → ["TRAIN"]
        var items = LoreItemParser.parse("[\"TRAIN\"]", POOL);
        assertEquals(1, items.size());
        assertInstanceOf(LoreRef.class, items.get(0));
        var ref = (LoreRef) items.get(0);
        assertEquals("TRAIN", ref.tag());
        assertNotNull(ref.resolved());
    }

    @Test
    void array_notation_without_quotes_resolves_as_ref() {
        var items = LoreItemParser.parse("[GARE]", POOL);
        assertEquals(1, items.size());
        assertInstanceOf(LoreRef.class, items.get(0));
        assertEquals("GARE", ((LoreRef) items.get(0)).tag());
    }

    @Test
    void multiple_refs_on_separate_lines() {
        var items = LoreItemParser.parse("[\"TRAIN\"]\n[\"GARE\"]", POOL);
        assertEquals(2, items.size());
        assertInstanceOf(LoreRef.class, items.get(0));
        assertInstanceOf(LoreRef.class, items.get(1));
    }

    @Test
    void inline_text_becomes_lore_inline() {
        var items = LoreItemParser.parse("Texte libre sans balise", POOL);
        assertEquals(1, items.size());
        assertInstanceOf(LoreInline.class, items.get(0));
        assertEquals("Texte libre sans balise", ((LoreInline) items.get(0)).text());
    }

    @Test
    void unknown_tag_resolves_to_null() {
        var items = LoreItemParser.parse("[INCONNU]", POOL);
        assertEquals(1, items.size());
        var ref = (LoreRef) items.get(0);
        assertEquals("INCONNU", ref.tag());
        assertNull(ref.resolved());
    }
}
