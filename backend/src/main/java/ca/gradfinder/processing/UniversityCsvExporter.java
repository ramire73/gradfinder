package ca.gradfinder.processing;

import java.io.IOException;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Saves the CSV of one crawled website, with exactly the base columns: data/output/(code)_programs.csv. */
@Component
public class UniversityCsvExporter {
    private final ProgramDataset dataset;
    private final Path dir;
    private final CsvWriter writer = new CsvWriter();

    public UniversityCsvExporter(ProgramDataset dataset, @Value("${gradfinder.data-dir}") String dataDir) {
        this.dataset = dataset;
        this.dir = Path.of(dataDir, "output");
    }

    public Path export(String code, String university) throws IOException {
        Files.createDirectories(dir);
        Path temp = dir.resolve(code + "_programs.csv.tmp");
        Files.writeString(temp, writer.write(dataset.baseRows(university)));
        return Files.move(temp, dir.resolve(code + "_programs.csv"), StandardCopyOption.REPLACE_EXISTING);
    }
}
