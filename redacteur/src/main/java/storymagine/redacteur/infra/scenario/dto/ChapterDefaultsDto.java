package storymagine.redacteur.infra.scenario.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class ChapterDefaultsDto {
    public List<String> character_sheets;
    public List<String> focus;
    @JsonDeserialize(using = LoreStringDeserializer.class)
    public String lore;
    @JsonDeserialize(using = PlanWriterListDeserializer.class)
    public PlanWriterListDto checks;
    @JsonDeserialize(using = PlanWriterListDeserializer.class)
    public PlanWriterListDto constraints;
}
