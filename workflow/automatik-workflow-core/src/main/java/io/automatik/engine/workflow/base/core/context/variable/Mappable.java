
package io.automatik.engine.workflow.base.core.context.variable;

import java.util.List;
import java.util.Map;

import io.automatik.engine.workflow.process.core.node.DataAssociation;

public interface Mappable {

	void addInMapping(String parameterName, String variableName);

	void setInMappings(Map<String, String> inMapping);

	String getInMapping(String parameterName);

	Map<String, String> getInMappings();

	void addInAssociation(DataAssociation dataAssociation);

	List<DataAssociation> getInAssociations();

	void addOutMapping(String parameterName, String variableName);

	void setOutMappings(Map<String, String> outMapping);

	String getOutMapping(String parameterName);

	Map<String, String> getOutMappings();

	void addOutAssociation(DataAssociation dataAssociation);

	List<DataAssociation> getOutAssociations();

}
