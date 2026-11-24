package ca.gradfinder.processing;

/** "2025-26" and "2025/26" -> 2025, the year in which the academic year starts. */
public final class EnrolmentYearNormalizer extends TextNormalizer {
    @Override public NormalizedValue normalize(String raw) {
        String text = clean(raw);
        if (text.matches("\\d{4}")) return NormalizedValue.of(text);
        if (text.matches("\\d{4} ?[-/\\u2013] ?(\\d{2}|\\d{4})")) return new NormalizedValue(text.substring(0, 4), text, "");
        return NormalizedValue.unclear(text, "not a year");
    }
}
