package org.continuity.api.entities.artifact.markovbehavior;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.io.Files;

public enum MarkovChainTestInstance {

	SOCK_SHOP("/markov-chains/sock-shop.csv"), SOCK_SHOP_WO_CART("/markov-chains/sock-shop-wo-cartUsingGET.csv"), SIMPLE("/markov-chains/simple.csv"), SIMPLE_WO_A(
			"/markov-chains/simple-wo-a.csv"), SIMPLE_INSERT(
					"/markov-chains/simple-insert.csv"), SIMPLE_WITH_INSERT("/markov-chains/simple-with-insert.csv"), SPECIAL_NUMBERS("/markov-chains/special-numbers.csv");

	private static final String SEPARATOR = ",";

	private final String path;

	private MarkovChainTestInstance(String path) {
		this.path = path;
	}

	public String[][] getCsv() throws IOException {
		List<String> lines = Files.readLines(new File(getClass().getResource(path).getFile()), Charset.defaultCharset());

		return lines.stream().map(l -> l.split(SEPARATOR)).collect(Collectors.toList()).toArray(new String[lines.size()][]);
	}

}
