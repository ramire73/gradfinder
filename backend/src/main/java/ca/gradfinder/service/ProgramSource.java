package ca.gradfinder.service;

import ca.gradfinder.model.ProgramRecord;
import java.util.List;

/** A place that programs come from. ProgramDataset combines every source, in the order given by @Order. */
public interface ProgramSource {
    List<ProgramRecord> loadAll();
}
