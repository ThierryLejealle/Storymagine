package storymagine.chat.coeur.domaine.scenario;

import java.util.List;

/**
 * Static roleplay setup read from chatscenarios/{name}/ : who the LLM plays, the (evergreen)
 * premise, and an optional ordered list of progressive acts. When acts are present, only the
 * current one is sent to the LLM alongside the premise — never the whole list — both to keep the
 * prompt small and to make later acts physically absent from the model's context (a stronger
 * anti-spoiler guarantee than asking it not to act on what it can already see). characterName
 * comes from an optional "# Name" first line in character.txt (same convention as ScenarioAct's
 * title) — kept verbatim in characterSheet (not stripped, unlike an act's title) since the model
 * should still read it there ; used elsewhere (transcript labels, prefill) to address the
 * character by name instead of the generic "Character". Empty when character.txt has no such line.
 */
public record ChatScenario(String name, String characterSheet, String premise, List<ScenarioAct> acts,
                            String characterName) {
}
