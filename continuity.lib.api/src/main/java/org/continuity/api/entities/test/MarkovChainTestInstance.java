package org.continuity.api.entities.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

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
		try (InputStream in = getClass().getResourceAsStream(path)) {
			return new BufferedReader(new InputStreamReader(in)).lines().map(l -> l.split(SEPARATOR)).collect(Collectors.toList()).toArray(new String[0][]);
		}
	}

}
