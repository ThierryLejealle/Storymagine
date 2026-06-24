package storymagine.commun.coeur.ports;

/**
 * Levée quand le prompt dépasse la fenêtre de contexte du LLM.
 */
public class ContextOverflowException extends RuntimeException {

    private final int promptTokens;
    private final int ctxSize;

    public ContextOverflowException(int promptTokens, int ctxSize) {
        super("Prompt (" + promptTokens + " tokens) dépasse la fenêtre (" + ctxSize + " tokens)");
        this.promptTokens = promptTokens;
        this.ctxSize = ctxSize;
    }

    public ContextOverflowException(String ollamaError, int ctxSize) {
        super("Prompt dépasse la fenêtre (" + ctxSize + " tokens) : " + ollamaError);
        this.promptTokens = -1;
        this.ctxSize = ctxSize;
    }

    public int promptTokens() { return promptTokens; }
    public int ctxSize()      { return ctxSize; }
}
