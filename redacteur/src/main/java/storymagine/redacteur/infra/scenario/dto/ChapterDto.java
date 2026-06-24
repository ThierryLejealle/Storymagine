package storymagine.redacteur.infra.scenario.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChapterDto {
    public String              comment;
    public String              title;
    public String              type;
    public String              description;
    public String              setting;
    public String              goal;
    public ChapterDefaultsDto  defaults;
    public List<SequenceDto>   sequences;
}
