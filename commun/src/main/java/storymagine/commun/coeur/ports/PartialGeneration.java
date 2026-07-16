package storymagine.commun.coeur.ports;

/**
 * One snapshot of an in-progress LLM call (see ModelCallPort.generate(..., Consumer)) : both
 * fields are always the COMPLETE text generated so far, never just the newest fragment — same
 * convention as LlmResult, so a caller can simply replace what it's showing instead of
 * accumulating itself. thinkingSoFar is "" until the model actually starts reasoning (if at all),
 * and stays fixed once textSoFar starts growing — models emit their reasoning before their visible
 * reply, never interleaved.
 */
public record PartialGeneration(String thinkingSoFar, String textSoFar) {
}
