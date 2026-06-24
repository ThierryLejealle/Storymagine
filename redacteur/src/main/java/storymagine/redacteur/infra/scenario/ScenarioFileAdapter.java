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

        return errors;
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
                    cfgDto.stitch);

            String bookGoal      = readOptionalDirective(root, "goal.md");
            String quality       = readOptionalDirective(root, "quality.md");
            String writingStyle  = readOptionalDirective(root, "style.md");
            String keepPhrases   = readOptionalDirective(root, "keep_phrases.md");
            String writingExample = readOptionalFile(root, cfgDto.writing_example != null
                    ? cfgDto.writing_example : "example.md");

            PersonnagePool personnages = loadPersonnages(root);
            FocusPool      focus       = loadFocus(root);
            LorePool       lore        = loadLore(root);
            CheckList      checks      = loadChecks(root);
            ConstraintList constraints = loadConstraints(root);
            List<Chapter>  chapters    = loadChapters(root, personnages, focus, lore);

            return new Scenario(config,
                    bookGoal, quality, writingStyle, keepPhrases,
                    writingExample, personnages, focus, lore,
                    checks, constraints, chapters);

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
                .map(b -> new FocusElement(b.tag(), b.globalContent(), b.planContent(), b.writerContent()))
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

    private CheckList loadChecks(Path root) throws IOException {
        Path file = root.resolve("checks.md");
        if (!Files.exists(file)) return CheckList.EMPTY;
        return CheckListParser.parseChecks(Files.readString(file));
    }

    private ConstraintList loadConstraints(Path root) throws IOException {
        Path file = root.resolve("constraints.md");
        if (!Files.exists(file)) return ConstraintList.EMPTY;
        return CheckListParser.parseConstraints(Files.readString(file));
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
                mapCheckList(dto.checks),
                mapConstraintList(dto.constraints));
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
                mapCheckList(dto.checks),
                mapConstraintList(dto.constraints));

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

    private List<FocusItem> resolveFocusItems(List<String> tags, FocusPool pool) {
        if (tags == null) return List.of();
        return tags.stream()
                .map(tag -> (FocusItem) new FocusRef(tag, pool.find(tag).orElse(null)))
                .toList();
    }

    private CheckList mapCheckList(PlanWriterListDto dto) {
        if (dto == null) return CheckList.EMPTY;
        List<Check> plan   = merge(dto.global, dto.plan).stream().map(Check::new).toList();
        List<Check> writer = merge(dto.global, dto.writer).stream().map(Check::new).toList();
        return new CheckList(plan, writer);
    }

    private ConstraintList mapConstraintList(PlanWriterListDto dto) {
        if (dto == null) return ConstraintList.EMPTY;
        List<Constraint> plan   = merge(dto.global, dto.plan).stream().map(Constraint::new).toList();
        List<Constraint> writer = merge(dto.global, dto.writer).stream().map(Constraint::new).toList();
        return new ConstraintList(plan, writer);
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
