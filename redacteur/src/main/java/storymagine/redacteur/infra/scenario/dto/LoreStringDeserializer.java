package storymagine.redacteur.infra.scenario.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Accepts both YAML string and array for the {@code lore} field.
 * Array ["TAG1", "TAG2"] is converted to the string format expected by LoreItemParser:
 * ["TAG1"]\n["TAG2"]
 */
class LoreStringDeserializer extends StdDeserializer<String> {

    LoreStringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        if (p.currentToken() == JsonToken.START_ARRAY) {
            List<String> tags = new ArrayList<>();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                tags.add(p.getText());
            }
            return tags.stream()
                    .map(tag -> "[\"" + tag + "\"]")
                    .collect(Collectors.joining("\n"));
        }
        return p.getText();
    }
}
