package storymagine.redacteur.infra.scenario.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class ChapterDefaultsDto {
    public List<String> character_sheets;
    @JsonDeserialize(using = FocusListDeserializer.class)
    public List<String> focus;
    @JsonDeserialize(using = LoreStringDeserializer.class)
    public String lore;
    @JsonDeserialize(using = PlanWriterListDeserializer.class)
    public PlanWriterListDto checks;
    @JsonDeserialize(using = PlanWriterListDeserializer.class)
    public PlanWriterListDto constraints;
    public Integer planner_effort_in_lines;
    public Integer sequence_min_words;
}
