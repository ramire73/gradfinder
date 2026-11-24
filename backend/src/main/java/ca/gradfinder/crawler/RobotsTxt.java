package ca.gradfinder.crawler;

import java.util.*;
import java.util.regex.Pattern;

/** Minimal robots.txt reader for the "User-agent: *" group. The longest matching rule wins. */
public final class RobotsTxt {
    private record Rule(boolean allow, String path) {}
    private final List<Rule> rules = new ArrayList<>();

    public RobotsTxt(String content) {
        boolean applies = false, readingAgents = false;
        for (String raw : content.split("\\R")) {
            String[] part = raw.replaceFirst("#.*", "").split(":", 2);
            if (part.length < 2) continue;
            String key = part[0].strip().toLowerCase(Locale.ROOT), value = part[1].strip();
            if (key.equals("user-agent")) {
                if (!readingAgents) applies = false;
                readingAgents = true;
                if (value.equals("*")) applies = true;
            } else {
                readingAgents = false;
                if (applies && !value.isEmpty() && (key.equals("allow") || key.equals("disallow")))
                    rules.add(new Rule(key.equals("allow"), value));
            }
        }
    }

    public boolean isAllowed(String path) {
        return rules.stream().filter(r -> matches(r.path(), path))
                .max(Comparator.<Rule>comparingInt(r -> r.path().length()).thenComparing(Rule::allow))
                .map(Rule::allow).orElse(true);
    }

    private static boolean matches(String rule, String path) {
        boolean end = rule.endsWith("$");
        String body = end ? rule.substring(0, rule.length() - 1) : rule;
        String regex = "\\Q" + body.replace("*", "\\E.*\\Q") + "\\E" + (end ? "" : ".*");
        return Pattern.matches(regex, path);
    }
}
