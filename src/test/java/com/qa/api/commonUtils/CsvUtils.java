package com.qa.api.commonUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/** Rows include the header row, if the file has one. Quoted fields are supported. */
public final class CsvUtils {
    private CsvUtils() { }

    public static List<List<String>> read(Path file) {
        TestLogger.info("Reading CSV file: " + file);
        List<List<String>> rows = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.RFC4180.parse(reader)) {
            for (CSVRecord record : parser) {
                List<String> row = new ArrayList<>();
                for (String value : record) {
                    row.add(value);
                }
                rows.add(row);
            }
            TestLogger.info("CSV file read successfully; rows: " + rows.size());
            return rows;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read CSV file: " + file, exception);
        }
    }

    /** Replaces the file, including its header if supplied as the first row. */
    public static void write(Path file, List<List<String>> rows) {
        TestLogger.info("Writing CSV file: " + file);
        try {
            FileUtils.createParentFolder(file);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.RFC4180)) {
                for (List<String> row : rows) {
                    printer.printRecord(row);
                }
            }
            TestLogger.info("CSV file written successfully; rows: " + rows.size());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write CSV file: " + file, exception);
        }
    }
}
