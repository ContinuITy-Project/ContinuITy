package org.continuity.jmeter.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.jmeter.config.CSVDataSet;
import org.apache.jmeter.testbeans.gui.TestBeanGUI;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.CsvColumnInput;
import org.continuity.idpa.annotation.CsvInput;
import org.continuity.idpa.annotation.ListInput;
import org.continuity.idpa.visitor.IdpaByClassSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds {@link CSVDataSet}s based on the {@link CsvInput} of an annotation.
 *
 * @author Henning Schulz
 *
 */
public class CSVDataSetAnnotator {

	private static final Logger LOGGER = LoggerFactory.getLogger(CSVDataSetAnnotator.class);

	private final ApplicationAnnotation annotation;

	public CSVDataSetAnnotator(ApplicationAnnotation annotation) {
		this.annotation = annotation;
	}

	public void addCsvDataSetConfigs(ListedHashTree testPlan) {
		SearchByClass<ThreadGroup> search = new SearchByClass<>(ThreadGroup.class);
		testPlan.traverse(search);

		List<CsvInput> inputs = extractNormalizedCsvInputs();

		for (ThreadGroup threadGroup : search.getSearchResults()) {
			for (CsvInput input : inputs) {
				addCsvDataSetConfigToThreadGroup(input, search.getSubTree(threadGroup).getTree(threadGroup));
			}
		}
	}

	private List<CsvInput> extractNormalizedCsvInputs() {
		List<CsvInput> inputs = new ArrayList<>();
		new IdpaByClassSearcher<>(CsvInput.class, inputs::add).visit(annotation);

		Map<String, Set<CsvInput>> inputsPerAssociated = new HashMap<>();

		inputs.stream().filter(in -> in.getColumn() >= 0).forEach(in -> {
			Set<CsvInput> associated = inputsPerAssociated.get(in.getId());
			Iterator<ListInput> iter = in.getAssociated().iterator();

			while ((associated == null) && iter.hasNext()) {
				associated = inputsPerAssociated.get(iter.next().getId());
			}

			if (associated == null) {
				associated = new HashSet<>();
				inputsPerAssociated.put(in.getId(), associated);
			}

			associated.add(in);

			for (ListInput asso : in.getAssociated()) {
				if (asso instanceof CsvInput) {
					associated.add((CsvInput) asso);
				} else {
					LOGGER.warn("Cannot associate inputs of type {} with CsvInputs! Ignoring {}.", asso.getClass().getSimpleName(), asso.getId());
				}
			}
		});

		List<CsvInput> normalized = new ArrayList<>();
		inputs.stream().filter(in -> in.getColumn() < 0).forEach(normalized::add);
		inputsPerAssociated.values().stream().map(this::merge).forEach(normalized::add);

		return normalized;
	}

	private CsvInput merge(Set<CsvInput> inputs) {
		String filename = extractCommonValue(inputs, andThen(CsvInput::getFilename, String::trim), "filenames");
		Boolean header = extractCommonValue(inputs, andThen(CsvInput::hasHeader, Boolean::new), "header flags");
		String separator = extractCommonValue(inputs, andThen(CsvInput::getSeparator, String::trim), "separators");

		CsvInput merged = new CsvInput();
		merged.setFilename(filename);
		merged.setSeparator(separator);

		if (header != null) {
			merged.setHeader(header);
		}

		merged.setColumns(inputs.stream().map(this::csvToColumn).collect(Collectors.toList()));

		return merged;
	}

	private <T> T extractCommonValue(Set<CsvInput> inputs, Function<CsvInput, T> getter, String field) {
		Set<T> values = inputs.stream().map(getter).collect(Collectors.toSet());

		if (values.size() != 1) {
			LOGGER.warn("Ambiguous {}: found {} different out of {}! Using a random one.", field, values.size(), inputs.stream().map(CsvInput::getId).collect(Collectors.toList()));
		}

		if (values.size() > 0) {
			return values.iterator().next();
		} else {
			return null;
		}
	}

	private CsvColumnInput csvToColumn(CsvInput csv) {
		CsvColumnInput column = new CsvColumnInput();
		column.setId(csv.getId());
		return column;
	}

	private <R, S, T> Function<R, T> andThen(Function<R, S> first, Function<S, T> second) {
		return first.andThen(second);
	}

	private void addCsvDataSetConfigToThreadGroup(CsvInput input, HashTree threadGroupTree) {
		TestBeanGUI testBeanGui = new TestBeanGUI(CSVDataSet.class);
		CSVDataSet csvDataSet = (CSVDataSet) testBeanGui.createTestElement();

		csvDataSet.setName("CsvInput (" + input.getFilename() + ")");
		csvDataSet.setFilename(input.getFilename());
		csvDataSet.setProperty("filename", input.getFilename());
		csvDataSet.setDelimiter(input.getSeparator());
		csvDataSet.setProperty("delimiter", input.getSeparator());
		csvDataSet.setQuotedData(true);
		csvDataSet.setProperty("quotedData", true);
		csvDataSet.setRecycle(true);
		csvDataSet.setProperty("recycle", true);
		csvDataSet.setStopThread(false);
		csvDataSet.setProperty("stopThread", false);
		csvDataSet.setVariableNames(input.getColumns().stream().map(CsvColumnInput::getId).collect(Collectors.joining(",")));
		csvDataSet.setProperty("variableNames", input.getColumns().stream().map(CsvColumnInput::getId).collect(Collectors.joining(",")));

		threadGroupTree.add(csvDataSet);
	}

}
