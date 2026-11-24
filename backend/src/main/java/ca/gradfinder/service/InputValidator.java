package ca.gradfinder.service;

import ca.gradfinder.model.ProgramRecord;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Data validation with regular expressions, for user input and for crawled records. */
@Component
public class InputValidator {
    private static final Map<String, Pattern> RULES = new LinkedHashMap<>();
    static {
        RULES.put("query", Pattern.compile("(?=.*[\\p{L}\\p{N}])[\\p{L}\\p{N} .,'’&()/+-]{1,100}"));
        RULES.put("word", Pattern.compile("[\\p{L}\\p{N}]{1,40}"));
        RULES.put("url", Pattern.compile("https?://[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+(:\\d{1,5})?(/[^\\s<>\"]*)?"));
        RULES.put("email", Pattern.compile("[A-Za-z0-9_%+-]+(\\.[A-Za-z0-9_%+-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}"));
        RULES.put("phone", Pattern.compile("(\\+?1[ .-]?)?(\\([2-9]\\d{2}\\)|[2-9]\\d{2})[ .-]?[2-9]\\d{2}[ .-]?\\d{4}"));
        RULES.put("postalCode", Pattern.compile("(?i)[ABCEGHJ-NPRSTVXY]\\d[ABCEGHJ-NPRSTV-Z][ -]?\\d[ABCEGHJ-NPRSTV-Z]\\d"));
        RULES.put("date", Pattern.compile("\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])"));
    }

    public Set<String> types() { return Collections.unmodifiableSet(RULES.keySet()); }

    public boolean isValid(String rule, String value) {
        Pattern p = RULES.get(rule);
        if (p == null) throw new IllegalArgumentException("Unknown validation type: " + rule);
        return value != null && value.length() <= 500 && p.matcher(value.strip()).matches();
    }

    public String require(String rule, String value) {
        if (!isValid(rule, value)) throw new IllegalArgumentException("Please enter a valid " + rule + ".");
        return value.strip();
    }

    public List<String> problems(ProgramRecord r) {
        List<String> out = new ArrayList<>();
        if (r.university() == null || r.university().isBlank()) out.add("university is missing");
        if (r.title() == null || r.title().isBlank()) out.add("title is missing");
        if (!isValid("url", r.url())) out.add("url is invalid");
        if (r.text().isBlank()) out.add("text is empty");
        String email = r.fields().get("contactEmail");
        if (email != null && !isValid("email", email)) out.add("contactEmail is invalid");
        return out;
    }
}
