package ca.gradfinder.service;

import java.util.*;
import java.util.regex.*;
import org.springframework.stereotype.Component;

/** Finds patterns inside page text with regular expressions. */
@Component
public class PatternFinder {
    private static final Map<String, Pattern> PATTERNS = new LinkedHashMap<>();
    static {
        PATTERNS.put("email", Pattern.compile("\\b[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9-]{1,63}(?:\\.[A-Za-z0-9-]{1,63})*\\.[A-Za-z]{2,}\\b"));
        PATTERNS.put("phone", Pattern.compile("(?<!\\d)(?:\\+?1[ .-])?(?:\\([2-9]\\d{2}\\) ?|[2-9]\\d{2}[ .-])[2-9]\\d{2}[ .-]\\d{4}(?!\\d)"));
        PATTERNS.put("courseCode", Pattern.compile("\\b[A-Z]{2,5}(?: [A-Z])? \\d{3}\\b"));
        PATTERNS.put("money", Pattern.compile("\\$ ?\\d[\\d,]*(?:\\.\\d+)?(?: (?:thousand|million|billion)\\b)?"));
        PATTERNS.put("gpa", Pattern.compile("(?i)\\bGPA[^.\\n]{0,40}?\\d\\.\\d{1,2}\\b|\\b\\d\\.\\d{1,2} GPA\\b"));
        PATTERNS.put("date", Pattern.compile("\\b(?:January|February|March|April|May|June|July|August|September|October|November|December) \\d{1,2}(?:, \\d{4})?\\b"));
        PATTERNS.put("postalCode", Pattern.compile("\\b[ABCEGHJ-NPRSTVXY]\\d[ABCEGHJ-NPRSTV-Z] ?\\d[ABCEGHJ-NPRSTV-Z]\\d\\b"));
        PATTERNS.put("units", Pattern.compile("\\b\\d+(?:\\.\\d+)? units\\b"));
    }

    public Set<String> types() { return Collections.unmodifiableSet(PATTERNS.keySet()); }

    public Map<String, Integer> find(String type, String text) {
        Pattern p = PATTERNS.get(type);
        if (p == null) throw new IllegalArgumentException("Unknown pattern type: " + type);
        Map<String, Integer> out = new TreeMap<>();
        Matcher m = p.matcher(text);
        while (m.find()) out.merge(m.group().strip(), 1, Integer::sum);
        return out;
    }
}
