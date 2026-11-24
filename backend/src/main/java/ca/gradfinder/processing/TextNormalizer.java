package ca.gradfinder.processing;

import java.text.Normalizer;

/** One kind of space, one kind of apostrophe, and characters written the same way. */
public class TextNormalizer implements FieldNormalizer {
    @Override public NormalizedValue normalize(String raw) {
        String text = clean(raw);
        return text.indexOf('\uFFFD') >= 0 ? NormalizedValue.unclear(text, "contains damaged characters") : NormalizedValue.of(text);
    }

    protected final String clean(String raw) {
        return Normalizer.normalize(raw, Normalizer.Form.NFC).replaceAll("[\\s\\u00A0\\u202F]+", " ").replace('\u2019', '\'').strip();
    }
}
