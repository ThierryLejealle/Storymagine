package storymagine.redacteur.infra.scenario.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Accepts a YAML string, a list of strings, or a mixed list for the {@code focus} field.
 * Blank entries are filtered out.
 */
class FocusListDeserializer extends StdDeserializer<List<String>> {

    FocusListDeserializer() {
        super(List.class);
    }

    @Override
    public List<String> deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        List<String> result = new ArrayList<>();
        if (p.currentToken() == JsonToken.START_ARRAY) {
            while (p.nextToken() != JsonToken.END_ARRAY) {
                String val = p.getText();
                if (val != null && !val.isBlank()) result.add(val.trim());
            }
        } else {
            String val = p.getText();
            if (val != null && !val.isBlank()) result.add(val.trim());
        }
        return result;
    }
}
