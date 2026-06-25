package storymagine.redacteur.infra.scenario.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScenarioConfigDto {
    public String  title;
    public String  language;
    public int     default_sequence_words;
    public int     context_window;
    public String  writing_example;
    public String  stitch;
    public Integer planner_effort_in_lines;
}
