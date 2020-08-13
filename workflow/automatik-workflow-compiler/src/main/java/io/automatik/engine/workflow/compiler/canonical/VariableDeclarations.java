
package io.automatik.engine.workflow.compiler.canonical;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.core.datatype.impl.type.ObjectDataType;

public class VariableDeclarations {

	public static VariableDeclarations of(VariableScope vscope) {
		HashMap<String, Variable> vs = new HashMap<>();
		for (Variable variable : vscope.getVariables()) {
			if (variable.hasTag(Variable.INTERNAL_TAG)) {
				continue;
			}

			vs.put(variable.getName(), variable);
		}
		return of(vs);
	}

	public static VariableDeclarations ofInput(VariableScope vscope) {

		return of(vscope, variable -> variable.hasTag(Variable.INTERNAL_TAG) || variable.hasTag(Variable.OUTPUT_TAG));
	}

	public static VariableDeclarations ofOutput(VariableScope vscope) {

		return of(vscope, variable -> variable.hasTag(Variable.INTERNAL_TAG) || variable.hasTag(Variable.INPUT_TAG));
	}

	public static VariableDeclarations of(VariableScope vscope, Predicate<Variable> filterOut) {
		HashMap<String, Variable> vs = new HashMap<>();
		for (Variable variable : vscope.getVariables()) {
			if (filterOut.test(variable)) {
				continue;
			}

			vs.put(variable.getName(), variable);
		}
		return of(vs);
	}

	public static VariableDeclarations of(Map<String, Variable> vscope) {
		return new VariableDeclarations(vscope);
	}

	public static VariableDeclarations ofRawInfo(Map<String, String> vscope) {
		HashMap<String, Variable> vs = new HashMap<>();

		if (vscope != null) {
			for (Entry<String, String> entry : vscope.entrySet()) {
				Variable variable = new Variable();
				variable.setName(entry.getKey());
				variable.setType(new ObjectDataType(constructClass(entry.getValue())));
				vs.put(entry.getKey(), variable);
			}
		}

		return new VariableDeclarations(vs);
	}

	private final Map<String, Variable> vscope;

	public VariableDeclarations(Map<String, Variable> vscope) {
		this.vscope = vscope;
	}

	public String getType(String vname) {
		return vscope.get(vname).getType().getStringType();
	}

	public List<String> getTags(String vname) {
		return vscope.get(vname).getTags();
	}

	public Map<String, Variable> getTypes() {
		return vscope;
	}

	protected static Class<?> constructClass(String name) {
		return constructClass(name, Thread.currentThread().getContextClassLoader());
	}

	protected static Class<?> constructClass(String name, ClassLoader cl) {
		if (name == null) {
			return Object.class;
		}

		switch (name) {
		case "Object":
			return Object.class;
		case "Integer":
			return Integer.class;
		case "Double":
			return Double.class;
		case "Float":
			return Float.class;
		case "Boolean":
			return Boolean.class;
		case "String":
			return String.class;
		case "Date":
			return Date.class;
		default:
			break;
		}

		try {
			return Class.forName(name, true, cl);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Unable to construct variable from type", e);
		}
	}
}
