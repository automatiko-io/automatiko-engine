
package io.automatik.engine.codegen.data;

import java.util.ArrayList;
import java.util.List;

public class Results {

	private final List<String> results = new ArrayList<>();

	public void add(String s) {
		results.add(s);
	}

	public List<String> getResults() {
		return results;
	}
}
