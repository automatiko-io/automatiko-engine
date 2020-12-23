
package io.automatiko.engine.workflow.bpmn2.core;

import java.io.Serializable;
import java.util.*;

public class Definitions implements Serializable {

	private static final long serialVersionUID = 4L;

	private String targetNamespace;
	private List<DataStore> dataStores;
	private List<Association> associations;
	private List<Error> errors;

	public String getTargetNamespace() {
		return targetNamespace;
	}

	public void setTargetNamespace(String targetNamespace) {
		this.targetNamespace = targetNamespace;
	}

	public void setDataStores(List<DataStore> dataStores) {
		this.dataStores = dataStores;
	}

	public List<DataStore> getDataStores() {
		return this.dataStores;
	}

	public void setAssociations(List<Association> associations) {
		this.associations = associations;
	}

	public List<Association> getAssociations() {
		return this.associations;
	}

	public List<Error> getErrors() {
		return errors;
	}

	public void setErrors(List<Error> errors) {
		this.errors = errors;
	}
}
