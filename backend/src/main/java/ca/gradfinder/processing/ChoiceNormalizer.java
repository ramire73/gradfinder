package ca.gradfinder.processing;

import java.util.*;

/** Maps different wordings to fixed choices. Several choices are written in a fixed order, separated by "; ". */
public final class ChoiceNormalizer extends TextNormalizer {
    private final Map<String, String> wordings = new HashMap<>();
    private final List<String> order;
    private final String separators;

    /** @param choices standard name -> other wordings */
    public ChoiceNormalizer(String separators, LinkedHashMap<String, List<String>> choices) {
        this.separators = separators;
        this.order = List.copyOf(choices.keySet());
        choices.forEach((name, others) -> {
            wordings.put(name.toLowerCase(Locale.ROOT), name);
            others.forEach(o -> wordings.put(o.toLowerCase(Locale.ROOT), name));
        });
    }

    @Override public NormalizedValue normalize(String raw) {
        String text = clean(raw);
        Set<String> found = new TreeSet<>(Comparator.comparingInt(order::indexOf));
        for (String part : text.split(separators)) {
            String name = wordings.get(part.strip().toLowerCase(Locale.ROOT));
            if (name == null) return NormalizedValue.unclear(text, "unknown choice: " + part.strip());
            found.add(name);
        }
        return NormalizedValue.of(String.join("; ", found));
    }
}
