package ca.gradfinder.model;

import java.util.Map;

/**
 * One degree in one program. "fields" is a map on purpose: the group can add or rename
 * extracted fields without changing the search features. Missing values are left out.
 */
public record ProgramRecord(String university, String title, String url, String crawledAt,
                            Map<String, String> fields, String text) {
    public ProgramRecord {
        fields = fields == null ? Map.of() : fields;
        text = text == null ? "" : text;
    }
}
