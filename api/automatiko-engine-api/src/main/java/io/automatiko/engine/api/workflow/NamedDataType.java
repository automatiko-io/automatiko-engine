
package io.automatiko.engine.api.workflow;

public class NamedDataType {

	private final String name;

	private final Object dataType;

	public NamedDataType(String name, Object dataType) {
		this.name = name;
		this.dataType = dataType;
	}

	public String getName() {
		return name;
	}

	public Object getDataType() {
		return dataType;
	}

	@Override
	public String toString() {
		return "NamedDataType [name=" + name + ", dataType=" + dataType + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dataType == null) ? 0 : dataType.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		NamedDataType other = (NamedDataType) obj;
		if (dataType == null) {
			if (other.dataType != null)
				return false;
		} else if (!dataType.equals(other.dataType))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
