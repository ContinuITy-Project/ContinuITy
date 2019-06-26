package org.continuity.commons.storage;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.univocity.parsers.common.processor.BeanListProcessor;
import com.univocity.parsers.common.processor.BeanWriterProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;

public class CsvFileStorage<T> extends FileStorage<List<T>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(CsvFileStorage.class);

	private static final String FILE_EXT = ".csv";

	private final Class<T> recordType;

	public CsvFileStorage(Path storagePath, List<T> emptyEntity, Class<T> recordType) {
		super(storagePath, emptyEntity);
		this.recordType = recordType;
	}

	@Override
	protected void write(Path dirPath, String id, List<T> entity) throws IOException {
		CsvWriterSettings settings = new CsvWriterSettings();
		settings.setRowWriterProcessor(new BeanWriterProcessor<>(recordType));

		CsvWriter writer = new CsvWriter(toPath(dirPath, id).toFile(), settings);

		writer.writeHeaders();
		writer.processRecordsAndClose(entity);
	}

	@Override
	protected List<T> read(Path dirPath, String id) throws IOException {
		BeanListProcessor<T> rowProcessor = new BeanListProcessor<>(recordType);

		CsvParserSettings settings = new CsvParserSettings();
		settings.setProcessor(rowProcessor);
		settings.setHeaderExtractionEnabled(true);
		settings.setDelimiterDetectionEnabled(true, ',', ';');

		CsvParser parser = new CsvParser(settings);

		try {
			parser.parse(new FileReader(toPath(dirPath, id).toFile()));
		} catch (FileNotFoundException e) {
			LOGGER.error("Cannot read CSV file: File not found!", e);

			return null;
		}

		return rowProcessor.getBeans();
	}

	@Override
	protected boolean remove(Path dirPath, String id) throws IOException {
		return Files.deleteIfExists(toPath(dirPath, id));
	}

	private Path toPath(Path dirPath, String id) {
		return dirPath.resolve(id + FILE_EXT);
	}

}
