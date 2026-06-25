package storymagine.redacteur.coeur.ports;

import storymagine.redacteur.coeur.domaine.story.Story;

/** Port for progressive HTML export of the story during generation. */
public interface HtmlExportPort {

    /** Called after each chapter is completed. Writes or overwrites story.html. */
    void exportHtml(String bookTitle, Story story);

    HtmlExportPort NOOP = (title, story) -> {};
}
