package ca.gradfinder.processing;

import java.math.BigDecimal;

/** "100.00%" -> 100% */
public final class PercentNormalizer extends TextNormalizer {
    @Override public NormalizedValue normalize(String raw) {
        String text = clean(raw);
        if (!text.matches("\\d{1,3}(\\.\\d+)? ?%")) return NormalizedValue.unclear(text, "not a percentage");
        BigDecimal value = new BigDecimal(text.replaceAll("[ %]", ""));
        if (value.compareTo(BigDecimal.valueOf(100)) > 0) return NormalizedValue.unclear(text, "more than 100%");
        return NormalizedValue.of(value.stripTrailingZeros().toPlainString() + "%");
    }
}
