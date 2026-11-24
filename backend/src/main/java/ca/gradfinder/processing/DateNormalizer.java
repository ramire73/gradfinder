package ca.gradfinder.processing;

import java.time.*;
import java.util.List;
import java.util.regex.*;

/** Writes every full date as YYYY-MM-DD and keeps labels: "August 15, 2026 (Fall 2026)" -> "2026-08-15 (Fall 2026)". */
public final class DateNormalizer extends TextNormalizer {
    private static final List<String> MONTHS = List.of("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec");
    private static final String MONTH = "(?<month>(?i:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|june?|july?|aug(?:ust)?|sept?(?:ember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?))\\.?";
    private static final String DAY = "(?<day>\\d{1,2})(?:st|nd|rd|th)?";
    private static final List<Pattern> FORMATS = List.of(
            Pattern.compile("\\b" + DAY + "[- ]" + MONTH + "[-, ]+(?<year>\\d{4}|\\d{2})\\b"),
            Pattern.compile("\\b" + MONTH + " " + DAY + ",? (?<year>\\d{4})\\b"));
    private static final Pattern ISO = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b");

    @Override public NormalizedValue normalize(String raw) {
        String original = clean(raw), text = original;
        try {
            for (Pattern format : FORMATS) text = format.matcher(text).replaceAll(this::iso);
        } catch (DateTimeException e) {
            return NormalizedValue.unclear(original, "not a real date");
        }
        return ISO.matcher(text).find() ? NormalizedValue.of(text) : NormalizedValue.unclear(original, "no full date");
    }

    private String iso(MatchResult m) {
        int year = Integer.parseInt(m.group("year"));
        int month = MONTHS.indexOf(m.group("month").substring(0, 3).toLowerCase()) + 1;
        return LocalDate.of(year < 100 ? 2000 + year : year, month, Integer.parseInt(m.group("day"))).toString();
    }
}
