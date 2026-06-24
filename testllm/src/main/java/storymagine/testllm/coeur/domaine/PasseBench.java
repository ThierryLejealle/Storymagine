package storymagine.testllm.coeur.domaine;

/**
 * Une passe de benchmark : un nom et les deux prompts à envoyer au LLM.
 */
public record PasseBench(String nom, String systemPrompt, String userPrompt) {}
