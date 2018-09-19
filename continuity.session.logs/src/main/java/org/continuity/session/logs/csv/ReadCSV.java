package org.continuity.session.logs.csv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.continuity.session.logs.entities.RowObject;

import com.univocity.parsers.common.processor.BeanListProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

/**
 * 
 * @author Alper Hidiroglu
 *
 */
public class ReadCSV {
	/**
	 * Reads data from CSV and saves the data into list of RowObjects.
	 * 
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public ArrayList<RowObject> readDataFromCSV(String link) {	
		BeanListProcessor<RowObject> rowProcessor = new BeanListProcessor<RowObject>(RowObject.class);

		CsvParserSettings parserSettings = new CsvParserSettings();
		parserSettings.setRowProcessor(rowProcessor);
		parserSettings.setHeaderExtractionEnabled(true);
		parserSettings.setDelimiterDetectionEnabled(true, ';');

		CsvParser parser = new CsvParser(parserSettings);
		try {
			parser.parse(new FileReader(new File(link)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		List<RowObject> beans = rowProcessor.getBeans();
		
		return (ArrayList<RowObject>) beans;
	}
}
