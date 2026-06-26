package storymagine.redacteur.coeur.domaine.agent.writer.deusinmachinacorrector;

import java.util.List;

/** Output of DeusInMachinaCorrector — pairs of (wrong phrase, corrected phrase) for inline patching. */
public record DeusInMachinaCorrectorOutput(List<DeusInMachinaCorrectorFinding> findings) {}
