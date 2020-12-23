
package io.automatiko.engine.api.workflow;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroupedNamedDataType {

	private final Map<String, Set<NamedDataType>> groupedDataTypes = new HashMap<>();

	public void add(String name, List<NamedDataType> types) {
		Set<NamedDataType> dataTypes = this.groupedDataTypes.getOrDefault(name, new LinkedHashSet<>());
		dataTypes.addAll(types);
		this.groupedDataTypes.put(name, dataTypes);
	}

	public Set<NamedDataType> getTypesByName(String name) {
		return this.groupedDataTypes.getOrDefault(name, new LinkedHashSet<>());
	}

	@Override
	public String toString() {
		return "GroupedNamedDataType [groupedDataTypes=" + groupedDataTypes + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + groupedDataTypes.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GroupedNamedDataType other = (GroupedNamedDataType) obj;
		if (!groupedDataTypes.equals(other.groupedDataTypes))
			return false;
		return true;
	}
}
