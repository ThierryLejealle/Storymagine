package storymagine.redacteur.infra.scenario.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Deserializes the requirements field in chapter and sequence YAML.
 *
 * Flat list  ->  all items go to RequirementListDto.global (applied to both plan and writer).
 * Map        ->  keys "plan" and "writer" route to their respective lists.
 */
class RequirementListDeserializer extends StdDeserializer<RequirementListDto> {

    RequirementListDeserializer() {
        super(RequirementListDto.class);
    }

    @Override
    public RequirementListDto deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        RequirementListDto dto = new RequirementListDto();

        if (p.currentToken() == JsonToken.VALUE_STRING) {
            dto.global = List.of(p.getText());
            return dto;
        }

        if (p.currentToken() == JsonToken.START_ARRAY) {
            dto.global = readStringList(p);
            return dto;
        }

        if (p.currentToken() == JsonToken.START_OBJECT) {
            while (p.nextToken() != JsonToken.END_OBJECT) {
                String key = p.currentName();
                p.nextToken();
                List<String> items = readStringList(p);
                switch (key) {
                    case "common" -> dto.global = items;
                    case "plan"   -> dto.plan   = items;
                    case "writer" -> dto.writer  = items;
                    default       -> { /* ignore unknown keys */ }
                }
            }
            return dto;
        }

        return dto;
    }

    private List<String> readStringList(JsonParser p) throws IOException {
        List<String> list = new ArrayList<>();
        if (p.currentToken() != JsonToken.START_ARRAY) return list;
        while (p.nextToken() != JsonToken.END_ARRAY) {
            list.add(p.getText());
        }
        return list;
    }
}
