package storymagine.redacteur.infra.scenario.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SequenceDto {
    public String       type;
    public String       directive;
    public List<String> character_sheets;
    @JsonDeserialize(using = FocusListDeserializer.class)
    public List<String> focus;
    @JsonDeserialize(using = LoreStringDeserializer.class)
    public String       lore;
    @JsonDeserialize(using = PlanWriterListDeserializer.class)
    public PlanWriterListDto checks;
    @JsonDeserialize(using = PlanWriterListDeserializer.class)
    public PlanWriterListDto constraints;
    public Integer      min_words;
    public String       stitch;
    public Boolean      no_transition;
}
