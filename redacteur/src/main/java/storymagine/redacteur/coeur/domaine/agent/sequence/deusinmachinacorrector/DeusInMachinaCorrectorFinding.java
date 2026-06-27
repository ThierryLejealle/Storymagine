package storymagine.redacteur.coeur.domaine.agent.sequence.deusinmachinacorrector;

/** A single deus-ex-machina correction: the wrong phrase and its corrected version. */
public record DeusInMachinaCorrectorFinding(String wrongPhrase, String correctedPhrase) {}
