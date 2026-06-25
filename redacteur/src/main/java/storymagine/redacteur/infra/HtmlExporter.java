package storymagine.redacteur.infra;

import storymagine.redacteur.coeur.domaine.story.WrittenChapter;

import java.util.List;
import java.util.regex.Pattern;

/** Builds a standalone HTML document from written chapters. No I/O — pure transformation. */
public final class HtmlExporter {

    private HtmlExporter() {}

    private record ChapterEntry(String title, String slug, String body) {}

    public static String generate(String bookTitle, List<WrittenChapter> chapters) {
        List<ChapterEntry> entries = toEntries(chapters);
        StringBuilder sb = new StringBuilder(16384);
        sb.append(htmlHead(bookTitle));
        sb.append("<h1 class=\"book-title\">").append(esc(bookTitle)).append("</h1>\n");
        appendToc(sb, entries);
        sb.append("<hr class=\"sep\">\n");
        for (ChapterEntry e : entries) {
            appendChapter(sb, e);
        }
        sb.append("</main>\n</body>\n</html>\n");
        return sb.toString();
    }

    // ── Domain → entries ─────────────────────────────────────────────────────

    private static List<ChapterEntry> toEntries(List<WrittenChapter> chapters) {
        int idx = 0;
        java.util.List<ChapterEntry> result = new java.util.ArrayList<>();
        for (WrittenChapter wc : chapters) {
            String title = wc.id().value();
            String body  = wc.fullText();
            result.add(new ChapterEntry(title, slugify(title, idx++), body == null ? "" : body));
        }
        return result;
    }

    private static String slugify(String title, int index) {
        String s = title.toLowerCase()
            .replace('à', 'a').replace('â', 'a').replace('á', 'a')
            .replace('è', 'e').replace('é', 'e').replace('ê', 'e').replace('ë', 'e')
            .replace('î', 'i').replace('ï', 'i')
            .replace('ô', 'o').replace('ö', 'o')
            .replace('ù', 'u').replace('û', 'u').replace('ü', 'u')
            .replace('ç', 'c').replace('ñ', 'n')
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+|-+$", "");
        return "ch" + (index + 1) + (s.isEmpty() ? "" : "-" + s);
    }

    // ── HTML assembly ─────────────────────────────────────────────────────────

    private static void appendToc(StringBuilder sb, List<ChapterEntry> entries) {
        sb.append("<nav class=\"toc\" id=\"toc\">\n<ul>\n");
        int num = 1;
        for (ChapterEntry e : entries) {
            sb.append("  <li><span class=\"toc-num\">").append(num++).append(".</span> ")
              .append("<a href=\"#").append(e.slug()).append("\">")
              .append(esc(e.title())).append("</a></li>\n");
        }
        sb.append("</ul>\n</nav>\n");
    }

    private static void appendChapter(StringBuilder sb, ChapterEntry e) {
        sb.append("<section class=\"chapter\" id=\"").append(e.slug()).append("\">\n")
          .append("<h2>").append(esc(e.title())).append("</h2>\n")
          .append(bodyToHtml(e.body()))
          .append("<p class=\"back\"><a href=\"#toc\">&#8593; Sommaire</a></p>\n")
          .append("</section>\n");
    }

    // ── Text → HTML ───────────────────────────────────────────────────────────

    private static String bodyToHtml(String body) {
        if (body == null || body.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        for (String block : body.split("\n{2,}")) {
            String t = block.strip();
            if (t.isEmpty()) continue;
            t = t.replace("\n", " ");
            sb.append("<p>").append(inline(t)).append("</p>\n");
        }
        return sb.toString();
    }

    private static final Pattern BOLD_ITALIC = Pattern.compile("\\*{3}(.+?)\\*{3}");
    private static final Pattern BOLD        = Pattern.compile("\\*{2}(.+?)\\*{2}");
    private static final Pattern ITALIC      = Pattern.compile("\\*(.+?)\\*");

    private static String inline(String text) {
        text = esc(text);
        text = BOLD_ITALIC.matcher(text).replaceAll("<strong><em>$1</em></strong>");
        text = BOLD       .matcher(text).replaceAll("<strong>$1</strong>");
        text = ITALIC     .matcher(text).replaceAll("<em>$1</em>");
        return text;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // ── HTML head + CSS ───────────────────────────────────────────────────────

    private static String htmlHead(String bookTitle) {
        return "<!DOCTYPE html>\n<html lang=\"fr\">\n<head>\n"
            + "<meta charset=\"UTF-8\">\n"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "<title>" + esc(bookTitle) + "</title>\n"
            + "<style>\n" + CSS + "</style>\n"
            + "</head>\n<body>\n<main id=\"top\">\n";
    }

    private static final String CSS =
        "*,*::before,*::after{box-sizing:border-box;margin:0;padding:0}\n"
        + "body{"
        +   "font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",system-ui,sans-serif;"
        +   "background:#fff;color:#111827;line-height:1.78;padding:2.5rem 1.25rem"
        + "}\n"
        + "main{max-width:760px;margin:0 auto}\n"
        + ".book-title{"
        +   "font-size:2rem;font-weight:700;color:#111827;"
        +   "margin-bottom:1.75rem;padding-bottom:.75rem;"
        +   "border-bottom:2px solid #e5e7eb"
        + "}\n"
        + ".toc{"
        +   "background:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;"
        +   "padding:1.25rem 1.75rem;margin-bottom:1.5rem"
        + "}\n"
        + ".toc ul{list-style:none}\n"
        + ".toc li{padding:.3rem 0;font-size:.97rem}\n"
        + ".toc-num{color:#6b7280;min-width:1.8rem;display:inline-block}\n"
        + ".toc a{color:#2563eb;text-decoration:none;font-weight:500}\n"
        + ".toc a:hover{text-decoration:underline}\n"
        + ".sep{border:none;border-top:1px solid #e5e7eb;margin:2rem 0}\n"
        + ".chapter{margin-bottom:4rem;scroll-margin-top:1.5rem}\n"
        + ".chapter h2{"
        +   "font-size:1.35rem;font-weight:700;color:#111827;"
        +   "margin-bottom:1.5rem;padding-bottom:.4rem;"
        +   "border-bottom:1px solid #f3f4f6"
        + "}\n"
        + "p{margin-bottom:1.1rem;text-align:justify;hyphens:auto}\n"
        + "strong{font-weight:600}\n"
        + "em{font-style:italic}\n"
        + "hr{border:none;border-top:1px solid #e5e7eb;margin:1.5rem 0}\n"
        + ".back{text-align:right;margin-top:2.5rem;font-size:.82rem}\n"
        + ".back a{color:#9ca3af;text-decoration:none}\n"
        + ".back a:hover{color:#2563eb}\n";
}
