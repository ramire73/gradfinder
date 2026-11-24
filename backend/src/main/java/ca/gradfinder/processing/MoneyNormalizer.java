package ca.gradfinder.processing;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.*;

/** "$7,600.00 (2026-27 Fall/Winter)" -> 7600.00, with "2026-27 Fall/Winter" kept as a note. Canadian dollars only. */
public final class MoneyNormalizer extends TextNormalizer {
    private static final Pattern PLAIN = Pattern.compile("\\d[\\d,]*(?:\\.\\d{1,2})?");
    private static final Pattern AMOUNT = Pattern.compile("\\$ ?((?>\\d[\\d,]*(?:\\.\\d+)?))(?! ?(?:thousand|million|billion|k\\b))");
    private static final Pattern FOREIGN = Pattern.compile("(?i)US\\$|\\bUSD\\b|\u20AC|\\bEUR\\b|\u00A3");

    @Override public NormalizedValue normalize(String raw) {
        String text = clean(raw);
        if (PLAIN.matcher(text).matches()) return NormalizedValue.of(number(text));
        if (FOREIGN.matcher(text).find()) return NormalizedValue.unclear(text, "not in Canadian dollars");
        List<MatchResult> amounts = AMOUNT.matcher(text).results().toList();
        if (amounts.size() != 1 || text.indexOf('$') != text.lastIndexOf('$'))
            return NormalizedValue.unclear(text, amounts.isEmpty() ? "no amount found" : "more than one amount");
        MatchResult m = amounts.getFirst();
        if (!PLAIN.matcher(m.group(1)).matches()) return NormalizedValue.unclear(text, "more than two decimals");
        String rest =(text.substring(0, m.start()) + " " + text.substring(m.end())).replaceAll("(?i)\\b(CAD|CA)\\b", "").strip();
        if (rest.matches("\\([^()]*\\)")) rest = rest.substring(1, rest.length() - 1);
        return new NormalizedValue(number(m.group(1)), clean(rest), "");
    }

    private String number(String amount) { return new BigDecimal(amount.replace(",", "")).setScale(2).toPlainString(); }
}
