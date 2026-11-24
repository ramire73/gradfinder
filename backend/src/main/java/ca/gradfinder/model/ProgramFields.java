package ca.gradfinder.model;

import java.util.*;

/**
 * The base format, taken from the University of British Columbia file. Every university is fitted to it.
 * COLUMNS maps the key used in ProgramRecord.fields to the column heading.
 * Program Name and URL are not in it because they are ProgramRecord.title and ProgramRecord.url.
 * A website rarely gives all of these; leave a key out rather than guess its value.
 */
public final class ProgramFields {
    private ProgramFields() {}
    public static final String PROGRAM_NAME = "Program Name";
    public static final String URL = "URL";
    public static final Map<String, String> COLUMNS;
    static {
        Map<String, String> c = new LinkedHashMap<>();
        c.put("specialization", "Specialization");
        c.put("faculty", "Faculty");
        c.put("academicUnit", "Academic Unit");
        c.put("deliveryMode", "Mode of Delivery");
        c.put("programComponents", "Program Components");
        c.put("programId", "Program ID");
        c.put("tuitionDomesticFirstYear", "Tuition Domestic (First Year)");
        c.put("tuitionInternationalFirstYear", "Tuition International (First Year)");
        c.put("medianFunding", "Median Funding");
        c.put("minimumFundingPerYear", "Minimum Funding per Year");
        c.put("enrolledStudents", "Enrolled Students");
        c.put("graduationRate", "Graduation Rate");
        c.put("avgYearsToCompletion", "Avg Years to Completion");
        c.put("intake", "Intake");
        c.put("applicationOpenDate", "Application Open Date");
        c.put("canadianApplicationDeadline", "Canadian Application Deadline");
        c.put("canadianTranscriptDeadline", "Canadian Transcript Deadline");
        c.put("canadianRefereeDeadline", "Canadian Referee Deadline");
        c.put("internationalApplicationDeadline", "International Application Deadline");
        c.put("internationalTranscriptDeadline", "International Transcript Deadline");
        c.put("internationalRefereeDeadline", "International Referee Deadline");
        c.put("applicationFeeDomestic", "Application Fee Domestic");
        c.put("applicationFeeInternational", "Application Fee International");
        c.put("installmentsPerYear", "Installments per Year");
        c.put("tuitionPerYearDomestic", "Tuition per Year Domestic");
        c.put("tuitionPerYearInternational", "Tuition per Year International");
        c.put("tuitionPerCreditDomestic", "Tuition per Credit Domestic");
        c.put("tuitionPerCreditInternational", "Tuition per Credit International");
        c.put("studentFeesYearly", "Student Fees (Yearly)");
        c.put("enrolmentYear", "Enrolment Year");
        c.put("applications", "Applications");
        c.put("offers", "Offers");
        c.put("newRegistrations", "New Registrations");
        c.put("totalEnrolment", "Total Enrolment");
        c.put("minTimeToCompletion", "Min Time to Completion");
        c.put("maxTimeToCompletion", "Max Time to Completion");
        COLUMNS = Collections.unmodifiableMap(c);
    }

    /** All headings of the base format, in order. */
    public static List<String> headings() {
        List<String> out = new ArrayList<>();
        COLUMNS.forEach((key, heading) -> {
            out.add(heading);
            if (key.equals("specialization")) out.add(PROGRAM_NAME);
            if (key.equals("faculty")) out.add(URL);
        });
        return out;
    }
}
