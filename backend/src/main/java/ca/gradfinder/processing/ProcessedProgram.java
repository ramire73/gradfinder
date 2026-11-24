package ca.gradfinder.processing;

import ca.gradfinder.model.ProgramRecord;
import java.util.Map;

/** A record in the common format, with what was set aside (notes) or not understood (problems), both by field key. */
public record ProcessedProgram(ProgramRecord program, Map<String, String> notes, Map<String, String> problems) {}
