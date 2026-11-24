package ca.gradfinder.processing;

/** Rewrites one kind of value into the format used by every university. Must never invent or drop information. */
public interface FieldNormalizer {
    NormalizedValue normalize(String raw);
}
