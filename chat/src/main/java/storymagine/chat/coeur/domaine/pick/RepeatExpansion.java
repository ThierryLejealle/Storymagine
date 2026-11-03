package storymagine.chat.coeur.domaine.pick;

import java.util.List;

/**
 * What unfolding the "{repeat}" blocks of a scenario's texts produced : the texts, and one CopySpan per
 * copy made (where it sits in its text, which copy it is, and what its item was).
 */
public record RepeatExpansion(List<String> texts, List<CopySpan> spans) {

    /** One copy of a block : its characters [start, end) in the unfolded text number textIndex ; itemDraw is null for "{repeat N}". */
    public record CopySpan(int textIndex, int start, int end, PickScope scope, PickDraw itemDraw) {

        public boolean contains(int textIndex, int position) {
            return this.textIndex == textIndex && position >= start && position < end;
        }
    }

    public RepeatExpansion {
        texts = List.copyOf(texts);
        spans = List.copyOf(spans);
    }

    /** The item of every copy, in play order. */
    public List<PickDraw> draws() {
        return spans.stream().map(CopySpan::itemDraw).filter(draw -> draw != null).toList();
    }
}
