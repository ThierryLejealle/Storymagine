package storymagine.redacteur.infra.scenario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import storymagine.redacteur.coeur.domaine.scenario.*;
import storymagine.redacteur.coeur.domaine.scenario.focus.*;
import storymagine.redacteur.coeur.domaine.scenario.lore.*;
import storymagine.redacteur.coeur.domaine.scenario.personnage.*;
import storymagine.redacteur.coeur.ports.ScenarioError;
import storymagine.redacteur.coeur.ports.ScenarioReaderPort;
import storymagine.redacteur.infra.scenario.dto.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class ScenarioFileAdapter implements ScenarioReaderPort {

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    @Override
    public List<ScenarioError> validate(Path root) {
        List<ScenarioError> errors = new ArrayList<>();

        if (!Files.exists(root.resolve("scenario.md")))
            errors.add(ScenarioError.missingFile("scenario.md"));
        if (!Files.exists(root.resolve("chapitres")))
            errors.add(ScenarioError.missingFile("chapitres/"));
        if (!errors.isEmpty()) return errors;

        try {
            ScenarioConfigDto cfg = readYaml(root.resolve("scenario.md"), ScenarioConfigDto.class);
            if (cfg.title == null || cfg.title.isBlank())
                errors.add(ScenarioError.invalidFormat("scenario.md", "missing title"));
        } catch (Exception e) {
            errors.add(ScenarioError.invalidFormat("scenario.md", e.getMessage()));
        }

        try (Stream<Path> files = Files.list(root.resolve("chapitres"))) {
            if (files.filter(p -> p.getFileName().toString().endsWith(".yaml")).findAny().isEmpty())
                errors.add(ScenarioError.missingFile("chapitres/*.yaml"));
        } catch (IOException e) {
            errors.add(ScenarioError.invalidFormat("chapitres/", e.getMessage()));
        }

        if (!errors.isEmpty()) return errors;

        // Load pools for reference validation
        FocusPool      focusPool      = new FocusPool(List.of());
        LorePool       lorePool       = LorePool.EMPTY;
        PersonnagePool personnagePool = new PersonnagePool(List.of());

        try { focusPool      = loadFocus(root);      } catch (IOException e) { errors.add(ScenarioError.invalidFormat("focus.md",      e.getMessage())); }
        try { lorePool       = loadLore(root);       } catch (IOException e) { errors.add(ScenarioError.invalidFormat("lore.md",       e.getMessage())); }
        try { personnagePool = loadPersonnages(root);} catch (IOException e) { errors.add(ScenarioError.invalidFormat("characters/",   e.getMessage())); }

        int defaultSequenceWords;
        try {
            defaultSequenceWords = readYaml(root.resolve("scenario.md"), ScenarioConfigDto.class).default_sequence_words;
        } catch (Exception e) {
            defaultSequenceWords = 0;
        }

        validateChapters(root, focusPool, lorePool, personnagePool, defaultSequenceWords, errors);

        return errors;
    }

    private static final int LOW_WORD_COUNT_THRESHOLD = 300;

    private void validateChapters(Path root, FocusPool focus, LorePool lore,
                                   PersonnagePool personnages, int defaultSequenceWords,
                                   List<ScenarioError> errors) {
        List<Path> files;
        try (Stream<Path> stream = Files.list(root.resolve("chapitres"))) {
            files = stream.filter(p -> p.getFileName().toString().endsWith(".yaml"))
                          .sorted().toList();
        } catch (IOException e) {
            return;
        }

        for (Path f : files) {
            String name = "chapitres/" + f.getFileName();
            ChapterDto dto;
            try {
                dto = readYaml(f, ChapterDto.class);
            } catch (Exception e) {
                errors.add(ScenarioError.invalidFormat(name, e.getMessage()));
                continue;
            }

            if (dto.title == null || dto.title.isBlank())
                errors.add(ScenarioError.invalidFormat(name, "missing title"));

            if (dto.sequences == null || dto.sequences.isEmpty())
                errors.add(ScenarioError.invalidFormat(name, "no sequences defined"));

            if (dto.defaults != null)
                validateRefs(dto.defaults.focus, dto.defaults.character_sheets, dto.defaults.lore,
                             focus, lore, personnages, name + " [defaults]", errors);

            Integer chapterMinWords = dto.defaults != null ? dto.defaults.sequence_min_words : null;

            if (dto.sequences != null) {
                for (int i = 0; i < dto.sequences.size(); i++) {
                    SequenceDto seq = dto.sequences.get(i);
                    validateRefs(seq.focus, seq.character_sheets, seq.lore,
                                 focus, lore, personnages, name + " [sequence " + (i + 1) + "]", errors);

                    int effectiveMinWords = seq.min_words != null ? seq.min_words
                            : chapterMinWords != null ? chapterMinWords
                            : defaultSequenceWords;
                    if (effectiveMinWords > 0 && effectiveMinWords < LOW_WORD_COUNT_THRESHOLD)
                        System.out.println("AVERTISSEMENT : " + name + " [sequence " + (i + 1) + "] "
                                + "min_words=" + effectiveMinWords + " est bas (< " + LOW_WORD_COUNT_THRESHOLD
                                + " mots) — risque de derive accrue lors des passes de correction.");
                }
            }
        }
    }

    private void validateRefs(List<String> focusTags, List<String> characterIds, String loreRaw,
                               FocusPool focus, LorePool lore, PersonnagePool personnages,
                               String context, List<ScenarioError> errors) {
        if (focusTags != null) {
            for (String entry : focusTags) {
                String tag = extractTag(entry);
                if (tag != null && focus.find(tag).isEmpty())
                    errors.add(ScenarioError.unresolvedRef(tag, context + " > focus"));
            }
        }
        if (characterIds != null) {
            for (String id : characterIds) {
                if (personnages.find(id).isEmpty())
                    errors.add(ScenarioError.unresolvedRef(id, context + " > character_sheets"));
            }
        }
        if (loreRaw != null && !loreRaw.isBlank()) {
            LoreItemParser.parse(loreRaw, lore).stream()
                    .filter(item -> item instanceof LoreRef ref && ref.resolved() == null)
                    .map(item -> ((LoreRef) item).tag())
                    .forEach(tag -> errors.add(ScenarioError.unresolvedRef(tag, context + " > lore")));
        }
    }

    // -------------------------------------------------------------------------
    // Load
    // -------------------------------------------------------------------------

    @Override
    public Scenario load(Path root) {
        try {
            ScenarioConfigDto cfgDto = readYaml(root.resolve("scenario.md"), ScenarioConfigDto.class);
            ScenarioConfig config = new ScenarioConfig(
                    cfgDto.title, cfgDto.language,
                    cfgDto.default_sequence_words > 0 ? cfgDto.default_sequence_words : 300,
                    cfgDto.context_window > 0 ? cfgDto.context_window : 32768,
                    cfgDto.stitch,
                    cfgDto.planner_effort_in_lines);

            String bookGoal      = readOptionalDirective(root, "goal.md");
            String quality       = readOptionalDirective(root, "quality.md");
            String writingStyle  = readOptionalDirective(root, "style.md");
            String keepPhrases   = readOptionalDirective(root, "keep_phrases.md");
            String writingExample = readOptionalDirective(root, cfgDto.writing_example != null
                    ? cfgDto.writing_example : "example.md");

            PersonnagePool personnages = loadPersonnages(root);
            FocusPool      focus       = loadFocus(root);
            LorePool       lore        = loadLore(root);
            RequirementList requirements = loadRequirements(root);
            List<Chapter>  chapters    = loadChapters(root, personnages, focus, lore);

            return new Scenario(config,
                    bookGoal, quality, writingStyle, keepPhrases,
                    writingExample, personnages, focus, lore,
                    requirements, chapters);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load scenario from " + root, e);
        }
    }

    // -------------------------------------------------------------------------
    // Loaders
    // -------------------------------------------------------------------------

    private PersonnagePool loadPersonnages(Path root) throws IOException {
        Path dir = root.resolve("characters");
        if (!Files.exists(dir)) return new PersonnagePool(List.of());

        List<Personnage> list = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.filter(p -> p.getFileName().toString().endsWith(".md"))
                               .sorted().toList()) {
                String id      = f.getFileName().toString().replace(".md", "");
                String content = Files.readString(f);
                list.add(PersonnageParser.parse(id, content));
            }
        }
        return new PersonnagePool(list);
    }

    private FocusPool loadFocus(Path root) throws IOException {
        Path file = root.resolve("focus.md");
        if (!Files.exists(file)) return new FocusPool(List.of());

        String content = Files.readString(file);
        List<FocusElement> elements = TagElementParser.parse(content).stream()
                .map(b -> new FocusElement(b.tag(), b.globalContent(), b.planContent(), b.writerContent(), b.checkContent()))
                .toList();
        return new FocusPool(elements);
    }

    private LorePool loadLore(Path root) throws IOException {
        Path file = root.resolve("lore.md");
        if (!Files.exists(file)) return LorePool.EMPTY;

        String content = Files.readString(file);
        List<LoreElement> elements = TagElementParser.parse(content).stream()
                .map(b -> new LoreElement(b.tag(), b.globalContent(), b.planContent(), b.writerContent()))
                .toList();
        return new LorePool(elements);
    }

    private RequirementList loadRequirements(Path root) throws IOException {
        Path file = root.resolve("requirements.md");
        if (!Files.exists(file)) return RequirementList.EMPTY;
        return RequirementListParser.parse(Files.readString(file));
    }

    private List<Chapter> loadChapters(Path root, PersonnagePool personnages,
                                       FocusPool focus, LorePool lore) throws IOException {
        Path dir = root.resolve("chapitres");
        if (!Files.exists(dir)) return List.of();

        List<Chapter> chapters = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.filter(p -> p.getFileName().toString().endsWith(".yaml"))
                               .sorted().toList()) {
                ChapterDto dto = readYaml(f, ChapterDto.class);
                chapters.add(mapChapter(dto, personnages, focus, lore));
            }
        }
        return List.copyOf(chapters);
    }

    // -------------------------------------------------------------------------
    // Mapping DTOs → domain
    // -------------------------------------------------------------------------

    private Chapter mapChapter(ChapterDto dto, PersonnagePool personnages,
                               FocusPool focus, LorePool lore) {
        ChapterDefaults defaults = dto.defaults != null
                ? mapDefaults(dto.defaults, personnages, focus, lore)
                : ChapterDefaults.EMPTY;

        List<Sequence> sequences = dto.sequences != null
                ? dto.sequences.stream().map(s -> mapSequence(s, personnages, focus, lore)).toList()
                : List.of();

        return new Chapter(
                dto.comment,
                dto.title,
                parseNarrativeType(dto.type),
                dto.description,
                dto.setting,
                dto.goal,
                defaults,
                sequences);
    }

    private ChapterDefaults mapDefaults(ChapterDefaultsDto dto, PersonnagePool personnages,
                                        FocusPool focus, LorePool lore) {
        return new ChapterDefaults(
                resolvePersonnages(dto.character_sheets, personnages),
                resolveFocusItems(dto.focus, focus),
                LoreItemParser.parse(dto.lore, lore),
                mapRequirementList(dto.requirements),
                dto.planner_effort_in_lines,
                dto.sequence_min_words);
    }

    private Sequence mapSequence(SequenceDto dto, PersonnagePool personnages,
                                 FocusPool focus, LorePool lore) {
        SequenceOverrides overrides = new SequenceOverrides(
                dto.stitch,
                dto.min_words != null ? dto.min_words : 0);

        SequenceAdditions additions = new SequenceAdditions(
                resolvePersonnages(dto.character_sheets, personnages),
                resolveFocusItems(dto.focus, focus),
                LoreItemParser.parse(dto.lore, lore),
                mapRequirementList(dto.requirements));

        return new Sequence(
                parseNarrativeType(dto.type),
                dto.directive,
                Boolean.TRUE.equals(dto.no_transition),
                overrides,
                additions);
    }

    // -------------------------------------------------------------------------
    // Resolution helpers
    // -------------------------------------------------------------------------

    private List<Personnage> resolvePersonnages(List<String> ids, PersonnagePool pool) {
        if (ids == null) return List.of();
        return ids.stream()
                .map(id -> pool.find(id).orElse(new Personnage(id, null, null, null)))
                .toList();
    }

    private List<FocusItem> resolveFocusItems(List<String> entries, FocusPool pool) {
        if (entries == null) return List.of();
        return entries.stream()
                .map(entry -> {
                    String tag = extractTag(entry);
                    if (tag != null) return (FocusItem) new FocusRef(tag, pool.find(tag).orElse(null));
                    return (FocusItem) FocusInline.parse(entry.replaceAll("^\"|\"$", "").strip());
                })
                .toList();
    }

    /** Returns the tag name if entry is [TAG] or ["TAG"], else null. */
    private static String extractTag(String entry) {
        if (entry == null) return null;
        String t = entry.strip();
        if (t.startsWith("[") && t.contains("]")) {
            String inner = t.substring(1, t.indexOf(']')).replaceAll("\"", "").trim();
            if (!inner.isBlank()) return inner;
        }
        return null;
    }

    private RequirementList mapRequirementList(RequirementListDto dto) {
        if (dto == null) return RequirementList.EMPTY;
        List<Requirement> plan   = merge(dto.global, dto.plan).stream().map(Requirement::parse).toList();
        List<Requirement> writer = merge(dto.global, dto.writer).stream().map(Requirement::parse).toList();
        return new RequirementList(plan, writer);
    }

    private List<String> merge(List<String> global, List<String> specific) {
        if (global == null && specific == null) return List.of();
        List<String> result = new ArrayList<>();
        if (global   != null) result.addAll(global);
        if (specific != null) result.addAll(specific);
        return result;
    }

    private NarrativeType parseNarrativeType(String raw) {
        if (raw == null) return NarrativeType.IMPERATIVE;
        return switch (raw.toLowerCase()) {
            case "dream"    -> NarrativeType.DREAM;
            case "what_if"  -> NarrativeType.WHAT_IF;
            default         -> NarrativeType.IMPERATIVE;
        };
    }

    // -------------------------------------------------------------------------
    // I/O helpers
    // -------------------------------------------------------------------------

    private <T> T readYaml(Path path, Class<T> type) throws IOException {
        return yaml.readValue(path.toFile(), type);
    }

    private String readOptionalFile(Path root, String filename) {
        try {
            Path f = root.resolve(filename);
            return Files.exists(f) ? Files.readString(f) : null;
        } catch (IOException e) {
            return null;
        }
    }

    /** Reads a directive file, strips HTML comment blocks, returns null if blank or absent. */
    private String readOptionalDirective(Path root, String filename) {
        String raw = readOptionalFile(root, filename);
        if (raw == null) return null;
        String stripped = raw.replaceAll("(?s)<!--.*?-->", "").strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
