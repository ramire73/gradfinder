package ca.gradfinder.processing;

/**
 * @param value   the value in the common format, or the original text if it was not understood
 * @param note    details that do not fit the format, such as the year of a fee; empty if none
 * @param problem why the value could not be put in the common format; empty if it could
 */
public record NormalizedValue(String value, String note, String problem) {
    public static NormalizedValue of(String value) { return new NormalizedValue(value, "", ""); }
    public static NormalizedValue unclear(String original, String problem) { return new NormalizedValue(original, "", problem); }
}
