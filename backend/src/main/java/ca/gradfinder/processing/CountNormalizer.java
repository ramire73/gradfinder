package ca.gradfinder.processing;

/** "1,250" -> 1250 */
public final class CountNormalizer extends TextNormalizer {
    @Override public NormalizedValue normalize(String raw) {
        String text = clean(raw);
        return text.matches("\\d{1,3}(,\\d{3})*|\\d+") ? NormalizedValue.of(text.replace(",", "")) : NormalizedValue.unclear(text, "not a whole number");
    }
}
