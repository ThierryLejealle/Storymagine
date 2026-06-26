package storymagine.redacteur.coeur.domaine.agent.writer.stylecorrector;

import java.util.List;

/** Output of StyleCorrector — style corrections to apply inline. */
public record StyleCorrectorOutput(List<StyleCorrectorFinding> findings) {}
