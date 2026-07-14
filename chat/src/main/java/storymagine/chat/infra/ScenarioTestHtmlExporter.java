package storymagine.chat.infra;

import storymagine.chat.coeur.domaine.scenariotester.ActTestResult;
import storymagine.chat.coeur.domaine.scenariotester.ScenarioTestReport;

import java.util.List;

/**
 * Builds a standalone HTML report from a ScenarioTestReport — one section per act, three blocks
 * each (continuity issues, clarity issues, suggestions). No I/O — pure transformation, same theme
 * and structure as redacteur's HtmlExporter (CSS duplicated on purpose : modules don't depend on
 * each other outside `commun`).
 */
public final class ScenarioTestHtmlExporter {

    private ScenarioTestHtmlExporter() {}

    public static String generate(ScenarioTestReport report) {
        StringBuilder sb = new StringBuilder(8192);
        String title = "Test de scénario : " + report.scenarioName();
        sb.append(htmlHead(title));
        sb.append("<h1 class=\"book-title\">").append(esc(title)).append("</h1>\n");
        appendToc(sb, report.actResults());
        sb.append("<hr class=\"sep\">\n");
        for (ActTestResult act : report.actResults()) {
            appendAct(sb, act);
        }
        sb.append("</main>\n</body>\n</html>\n");
        return sb.toString();
    }

    private static String slug(ActTestResult act) {
        return "act" + act.actNumber().display().replace('.', '-');
    }

    private static void appendToc(StringBuilder sb, List<ActTestResult> acts) {
        sb.append("<nav class=\"toc\" id=\"toc\">\n<ul>\n");
        for (ActTestResult act : acts) {
            sb.append("  <li><span class=\"toc-num\">").append(esc(act.actNumber().display())).append("</span> ")
              .append("<a href=\"#").append(slug(act)).append("\">")
              .append(esc(act.actTitle())).append("</a></li>\n");
        }
        sb.append("</ul>\n</nav>\n");
    }

    private static void appendAct(StringBuilder sb, ActTestResult act) {
        sb.append("<section class=\"chapter\" id=\"").append(slug(act)).append("\">\n")
          .append("<h2>").append(esc(act.actNumber().display())).append(" — ").append(esc(act.actTitle())).append("</h2>\n")
          .append(appendBlock("Incohérences (continuité)", act.continuityIssues()))
          .append(appendBlock("Clarté / utilisabilité", act.clarityIssues()))
          .append(appendBlock("Suggestions", act.suggestions()))
          .append("<p class=\"back\"><a href=\"#toc\">&#8593; Sommaire</a></p>\n")
          .append("</section>\n");
    }

    private static String appendBlock(String heading, List<String> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>").append(esc(heading)).append("</h3>\n");
        if (items.isEmpty()) {
            sb.append("<p class=\"none\">Rien à signaler.</p>\n");
        } else {
            sb.append("<ul>\n");
            for (String item : items) {
                sb.append("  <li>").append(esc(item)).append("</li>\n");
            }
            sb.append("</ul>\n");
        }
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String htmlHead(String title) {
        return "<!DOCTYPE html>\n<html lang=\"fr\">\n<head>\n"
            + "<meta charset=\"UTF-8\">\n"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "<title>" + esc(title) + "</title>\n"
            + "<style>\n" + CSS + "</style>\n"
            + "</head>\n<body>\n<main id=\"top\">\n";
    }

    private static final String CSS =
        "*,*::before,*::after{box-sizing:border-box;margin:0;padding:0}\n"
        + "body{"
        +   "font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",system-ui,sans-serif;"
        +   "background:#fff;color:#111827;line-height:1.6;padding:2.5rem 1.25rem"
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
        + ".toc-num{color:#6b7280;min-width:2.4rem;display:inline-block}\n"
        + ".toc a{color:#2563eb;text-decoration:none;font-weight:500}\n"
        + ".toc a:hover{text-decoration:underline}\n"
        + ".sep{border:none;border-top:1px solid #e5e7eb;margin:2rem 0}\n"
        + ".chapter{margin-bottom:3rem;scroll-margin-top:1.5rem}\n"
        + ".chapter h2{"
        +   "font-size:1.35rem;font-weight:700;color:#111827;"
        +   "margin-bottom:1.25rem;padding-bottom:.4rem;"
        +   "border-bottom:1px solid #f3f4f6"
        + "}\n"
        + ".chapter h3{font-size:.95rem;font-weight:600;color:#374151;margin:1.1rem 0 .4rem}\n"
        + "ul{margin-left:1.25rem;margin-bottom:.5rem}\n"
        + "li{margin-bottom:.3rem}\n"
        + "p.none{color:#9ca3af;font-style:italic;margin-bottom:.5rem}\n"
        + "hr{border:none;border-top:1px solid #e5e7eb;margin:1.5rem 0}\n"
        + ".back{text-align:right;margin-top:1.5rem;font-size:.82rem}\n"
        + ".back a{color:#9ca3af;text-decoration:none}\n"
        + ".back a:hover{color:#2563eb}\n";
}
