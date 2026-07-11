package storymagine.commun.coeur.domaine.prompt;

/**
 * Assembles a user prompt from optional titled sections.
 * Knows nothing about the caller's domain: the caller extracts and formats
 * its own content, this class only handles the "title + content, skip if blank,
 * or fall back to a default text" plumbing shared by every agent.
 */
public final class PromptBuilder {

    private final StringBuilder buffer = new StringBuilder();

    private PromptBuilder() {
    }

    public static PromptBuilder create() {
        return new PromptBuilder();
    }

    /**
     * Appends "### title\n" + content, or nothing at all if content is blank.
     */
    public PromptBuilder section(String title, String content) {
        if (content == null || content.isBlank()) {
            return this;
        }
        separateIfNeeded();
        buffer.append("### ").append(title).append("\n").append(content);
        return this;
    }

    /**
     * Same as section(), but falls back to a default text instead of being skipped when content is blank.
     */
    public PromptBuilder sectionOrElse(String title, String content, String fallback) {
        return section(title, (content == null || content.isBlank()) ? fallback : content);
    }

    /**
     * Appends free text with no title, or nothing at all if text is blank.
     */
    public PromptBuilder raw(String text) {
        if (text == null || text.isBlank()) {
            return this;
        }
        separateIfNeeded();
        buffer.append(text);
        return this;
    }

    public String build() {
        return buffer.toString();
    }

    private void separateIfNeeded() {
        if (!buffer.isEmpty()) {
            buffer.append("\n\n");
        }
    }
}
