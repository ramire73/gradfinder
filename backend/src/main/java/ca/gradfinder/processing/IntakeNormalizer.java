package ca.gradfinder.processing;

import java.util.*;
import java.util.regex.*;

/** "Sep-27" and "September 2027" -> "Fall 2027". Several intakes are separated by "; ". */
public final class IntakeNormalizer extends TextNormalizer {
    private static final Pattern TERM = Pattern.compile("(?i)(fall|autumn|winter|spring|summer) (\\d{4})");
    private static final Pattern MONTH = Pattern.compile("(?i)(sep(?:t(?:ember)?)?|jan(?:uary)?|may)[- ](\\d{4}|\\d{2})");
    private static final Map<String, String> MONTH_TERMS = Map.of("sep", "Fall", "jan", "Winter", "may", "Summer");

    @Override public NormalizedValue normalize(String raw) {
        String text = clean(raw);
        List<String> terms = new ArrayList<>();
        for (String part : text.split(" ?[;,] ?")) {
            Matcher term = TERM.matcher(part), month = MONTH.matcher(part);
            if (term.matches()) {
                String name = term.group(1).toLowerCase();
                terms.add((name.equals("autumn") ? "Fall" : name.substring(0, 1).toUpperCase() + name.substring(1)) + " " + term.group(2));
            } else if (month.matches()) {
                String year = month.group(2);
                terms.add(MONTH_TERMS.get(month.group(1).substring(0, 3).toLowerCase()) + " " + (year.length() == 2 ? "20" + year : year));
            } else return NormalizedValue.unclear(text, "no term and year");
        }
        return NormalizedValue.of(String.join("; ", terms));
    }
}
