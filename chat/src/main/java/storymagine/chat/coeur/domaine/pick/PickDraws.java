package storymagine.chat.coeur.domaine.pick;

import java.util.List;

/** The draws made for one run, in the order their tags appear in the scenario texts. */
public record PickDraws(List<PickDraw> values) {

    public static final PickDraws NONE = new PickDraws(List.of());

    public PickDraws {
        values = List.copyOf(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
