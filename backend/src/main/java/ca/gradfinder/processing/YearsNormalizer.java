package ca.gradfinder.processing;

import java.util.*;
import java.util.regex.*;

/** "1 years" -> "1 year". Labels are kept: "4 years (thesis-based); 6 years (course-based)". */
public final class YearsNormalizer extends TextNormalizer {
    private static final Pattern PART = Pattern.compile("(\\d+(?:\\.\\d+)?)(?: years?)?( \\([^()]+\\))?");

    @Override public NormalizedValue normalize(String raw) {
        String text = clean(raw);
        List<String> out = new ArrayList<>();
        for (String part : text.split(" ?; ?")) {
            Matcher m = PART.matcher(part);
            if (!m.matches()) return NormalizedValue.unclear(text, "not a number of years");
            out.add(m.group(1) + (m.group(1).equals("1") ? " year" : " years") + (m.group(2) == null ? "" : m.group(2)));
        }
        return NormalizedValue.of(String.join("; ", out));
    }
}
