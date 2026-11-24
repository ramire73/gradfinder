package ca.gradfinder.parser;

import java.util.*;
import java.util.regex.*;
import org.jsoup.nodes.*;
import org.springframework.stereotype.Component;

/** HTML parser: turns one University of Alberta program page into a ProgramRecord. */
@Component
public final class AlbertaParser extends HtmlProgramParser {
    public static final String CODE = "ualberta";
    public static final String UNIVERSITY = "University of Alberta";
    private static final Pattern PROGRAM_EMAIL = Pattern.compile(
            "Program-specific inquiries(?:(?!General inquiries).)*?([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})", Pattern.DOTALL);

    private static final Pattern BRACKETS = Pattern.compile("\\(([^()]+)\\)\\s*$");
    private static final Pattern APPLICATION_FEE = Pattern.compile("(?i)application fee[^$:]*:\\s*(\\$[\\d,]+(?:\\.\\d{2})?)");
    private static final List<String> NUMBERS =
            List.of("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten");
    private static final String N_YEARS = "(" + String.join("|", NUMBERS) + "|\\d+)(?: consecutive)?(?: calendar)? years?";
    private static final Pattern MIN_TIME = Pattern.compile("(?i)minimum of " + N_YEARS);
    private static final Pattern MAX_TIME = Pattern.compile("(?i)(?:maximum time to complet.*? is|must not exceed) " + N_YEARS);

    private static final Pattern ADMISSION = Pattern.compile("\\b((?:Fall|Winter|Spring|Summer) \\d{4}) Admission\\b");
    private static final String DATE = ":\\s*((?:January|February|March|April|May|June|July|August|September|October|November|December) \\d{1,2}, \\d{4})";
    private static final Pattern OPENS = Pattern.compile("(?i)applications? opens?" + DATE);
    private static final Pattern CLOSES = Pattern.compile("(?i)(?:application deadline|applications? closes?)" + DATE);

    @Override public String code() { return CODE; }

    @Override protected String university() { return UNIVERSITY; }

    /** Agreed columns are listed in ProgramFields. */
    @Override protected Map<String, String> extractFields(Element main, String title) {
        Map<String, String> f = new LinkedHashMap<>();
        String type = label(main, "Program Type"), length = afterHeading(main, "Length of Program");
        String fees = accordion(main, "Tuition and Fees");
        put(f, "specialization", specialization(title));
        put(f, "faculty", label(main, "Faculty"));
        put(f, "academicUnit", inBrackets(title));
        put(f, "deliveryMode", label(main, "Delivery Mode"));
        put(f, "programComponents", components(type, accordion(main, "Program Requirements")));
        Map<String, List<String>> dates = admissionDates(main.text());
        put(f, "intake", String.join("; ", dates.get("intake")));
        put(f, "applicationOpenDate", String.join("; ", dates.get("open")));
        // one deadline, with no separate international date
        put(f, "canadianApplicationDeadline", String.join("; ", dates.get("close")));
        put(f, "internationalApplicationDeadline", String.join("; ", dates.get("close")));
        Matcher fee = APPLICATION_FEE.matcher(fees == null ? "" : fees);
        if (fee.find()) { // one fee, with no separate international amount
            f.put("applicationFeeDomestic", fee.group(1));
            f.put("applicationFeeInternational", fee.group(1));
        }
        boolean twoStreams = type != null && type.contains(",");
        put(f, "minTimeToCompletion", years(length, MIN_TIME, twoStreams));
        put(f, "maxTimeToCompletion", years(length, MAX_TIME, twoStreams));
        // Extra fields, used by the filters and the details view
        put(f, "degreeLevel", label(main, "Degree Level"));
        put(f, "programType", type);
        put(f, "description", afterHeading(main, "Why study this program?"));
        put(f, "entranceRequirements", afterHeading(main, "Entrance Requirements"));
        put(f, "programLength", length);
        put(f, "tuitionAndFees", fees);
        Matcher email = PROGRAM_EMAIL.matcher(main.text());
        if (email.find()) f.put("contactEmail", email.group(1));
        return f;
    }

    /** "Master of Arts (Anthropology)" -> "Anthropology" */
    private String inBrackets(String title) {
        Matcher m = BRACKETS.matcher(title);
        return m.find() ? m.group(1) : null;
    }

    /** The subject named after "in" (French "en"), otherwise the subject in brackets. */
    private String specialization(String title) {
        String name = BRACKETS.matcher(title).replaceFirst("").strip();
        Matcher m = Pattern.compile("(?i)\\bspecialization in (.+)$").matcher(name);
        if (m.find()) return m.group(1);
        m = Pattern.compile("^.*? (?:in|en) (.+)$").matcher(name);
        return m.find() ? m.group(1) : inBrackets(title);
    }

    /** "Fall 2026 Admission ... Application Deadline: August 15, 2026". Each date is labelled with its term. */
    private Map<String, List<String>> admissionDates(String text) {
        Map<String, List<String>> out = Map.of("intake", new ArrayList<>(), "open", new ArrayList<>(), "close", new ArrayList<>());
        List<MatchResult> terms = ADMISSION.matcher(text).results().toList();
        for (int i = 0; i < terms.size(); i++) {
            String term = terms.get(i).group(1);
            int end = i + 1 < terms.size() ? terms.get(i + 1).start() : text.length();
            String section = text.substring(terms.get(i).end(), Math.min(end, terms.get(i).end() + 250));
            Matcher open = OPENS.matcher(section), close = CLOSES.matcher(section);
            boolean hasOpen = open.find(), hasClose = close.find();
            if (!hasOpen && !hasClose) continue; // the term was only mentioned
            out.get("intake").add(term);
            if (hasOpen) out.get("open").add(open.group(1) + " (" + term + ")");
            if (hasClose) out.get("close").add(close.group(1) + " (" + term + ")");
        }
        return out;
    }

    private String components(String type, String requirements) {
        if (type == null) return null;
        boolean course = type.contains("Course Based"), thesis = type.contains("Thesis Based");
        String req = requirements == null ? "" : requirements.toLowerCase(Locale.ROOT);
        if (course && thesis) return "Coursework OR Thesis";
        // The "Courses" section only says where courses are listed, so it does not show that any are required.
        String required = req.replaceFirst("(?s)(courses )?graduate courses[^.]* can be found.*", "");
        if (thesis) return required.matches("(?s).*\\b(coursework|courses?)\\b.*") ? "Coursework + Thesis" : "Thesis";
        if (course) return req.matches("(?s).*\\b(capstone|capping)\\b.*") ? "Coursework + Capstone" : "Coursework";
        return null;
    }

    /** Time limits stated in years. With two streams, each limit is labelled with its stream. */
    private String years(String length, Pattern pattern, boolean twoStreams) {
        if (length == null) return null;
        Set<String> found = new LinkedHashSet<>();
        for (String paragraph : length.split("\\n")) {
            Matcher m = pattern.matcher(paragraph);
            while (m.find()) {
                int word = NUMBERS.indexOf(m.group(1).toLowerCase(Locale.ROOT));
                String n = word >= 0 ? Integer.toString(word) : m.group(1);
                String value = n + (n.equals("1") ? " year" : " years");
                String before = paragraph.substring(0, m.end()).toLowerCase(Locale.ROOT);
                int thesis = before.lastIndexOf("thesis-based"), course = before.lastIndexOf("course-based");
                if (twoStreams && thesis != course) value += thesis > course ? " (thesis-based)" : " (course-based)";
                found.add(value);
            }
        }
        return found.isEmpty() ? null : String.join("; ", found);
    }

    /** Sidebar markup: <dt>Faculty:</dt> Arts */
    private String label(Element main, String name) {
        for (Element dt : main.select("dt")) {
            if (!dt.text().replace(":", "").strip().equalsIgnoreCase(name)) continue;
            StringBuilder value = new StringBuilder();
            for (Node n = dt.nextSibling(); n != null; n = n.nextSibling()) {
                if (n instanceof Element e && Set.of("dt", "p").contains(e.normalName())) {
                    if (!value.toString().isBlank() || !e.hasText()) break;
                }
                value.append(n instanceof TextNode t ? t.text() : n instanceof Element e ? e.text() : "");
            }
            return value.toString();
        }
        return null;
    }

    private String accordion(Element main, String name) {
        for (Element item : main.select(".accordion-item")) {
            Element button = item.selectFirst("button"), body = item.selectFirst(".accordion-body");
            if (button != null && body != null && button.text().strip().equalsIgnoreCase(name)) return body.text();
        }
        return null;
    }
}
