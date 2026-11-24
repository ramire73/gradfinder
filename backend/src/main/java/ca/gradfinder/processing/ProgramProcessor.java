package ca.gradfinder.processing;

import ca.gradfinder.model.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Chooses the normalizer for each field and puts one record into the common format. */
@Component
public class ProgramProcessor {
    private final FieldNormalizer text = new TextNormalizer();
    private final Map<String, FieldNormalizer> normalizers = new HashMap<>();

    public ProgramProcessor() {
        use(new MoneyNormalizer(), "tuitionDomesticFirstYear", "tuitionInternationalFirstYear", "medianFunding", "minimumFundingPerYear",
                "applicationFeeDomestic", "applicationFeeInternational", "tuitionPerYearDomestic", "tuitionPerYearInternational",
                "tuitionPerCreditDomestic", "tuitionPerCreditInternational", "studentFeesYearly");
        use(new DateNormalizer(), "applicationOpenDate", "canadianApplicationDeadline", "canadianTranscriptDeadline", "canadianRefereeDeadline",
                "internationalApplicationDeadline", "internationalTranscriptDeadline", "internationalRefereeDeadline");
        use(new CountNormalizer(), "enrolledStudents", "applications", "offers", "newRegistrations", "totalEnrolment");
        use(new IntakeNormalizer(), "intake");
        use(new ChoiceNormalizer("[,;/]| or | and ", choices("In person", List.of("On Campus", "In-person", "Face to face"),
                "Online", List.of("Distance", "Online only"), "Hybrid", List.of("Blended"))), "deliveryMode");
        use(new ChoiceNormalizer("[,;]| or ", choices("Course Based", List.of("Course-based", "Coursework"),
                "Thesis Based", List.of("Thesis-based", "Thesis"))), "programType");
        use(new ChoiceNormalizer("[,;]", choices("Master's", List.of("Masters", "Master"), "Doctoral", List.of("Doctorate", "PhD"),
                "Certificate", List.of("Graduate Certificate"), "Diploma", List.of("Graduate Diploma"),
                "Embedded Certificate", List.of())), "degreeLevel");
    }

    private void use(FieldNormalizer normalizer, String... keys) { for (String key : keys) normalizers.put(key, normalizer); }

    private LinkedHashMap<String, List<String>> choices(Object... pairs) {
        LinkedHashMap<String, List<String>> out = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2)
            out.put((String) pairs[i], ((List<?>) pairs[i + 1]).stream().map(String.class::cast).toList());
        return out;
    }

    public ProcessedProgram process(ProgramRecord raw) {
        Map<String, String> found = new LinkedHashMap<>(raw.fields());
        if (blank(found.get("degreeLevel"))) found.put("degreeLevel", levelFromTitle(raw.title()));
        Map<String, String> fields = new LinkedHashMap<>(), notes = new LinkedHashMap<>(), problems = new LinkedHashMap<>();
        // The same order for every university: agreed columns first, then extra fields.
        List<String> keys = new ArrayList<>(ProgramFields.COLUMNS.keySet());
        found.keySet().stream().filter(k -> !ProgramFields.COLUMNS.containsKey(k)).sorted().forEach(keys::add);
        for (String key : keys) {
            if (blank(found.get(key))) continue;
            NormalizedValue v = normalizers.getOrDefault(key, text).normalize(found.get(key));
            fields.put(key, v.value());
            if (!v.note().isEmpty()) notes.put(key, v.note());
            if (!v.problem().isEmpty()) problems.put(key, v.problem());
        }
        ProgramRecord program = new ProgramRecord(text.normalize(raw.university()).value(), text.normalize(raw.title()).value(),
                raw.url(), raw.crawledAt(), fields, raw.text());
        return new ProcessedProgram(program, notes, problems);
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }

    private String levelFromTitle(String title) {
        String t = title.toLowerCase(Locale.ROOT);
        if (t.contains("certificate")) return "Certificate";
        if (t.contains("diploma")) return "Diploma";
        if (t.matches("(?s).*\\b(doctor|doctorate|phd)\\b.*")) return t.matches("(?s).*\\bmasters?\\b.*") ? "Master's, Doctoral" : "Doctoral";
        return t.matches("(?s).*\\bmasters?\\b.*") ? "Master's" : "";
    }
}
