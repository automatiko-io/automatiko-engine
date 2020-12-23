
package io.automatiko.engine.workflow.process.core.node;

import java.io.Serializable;

public class Transformation implements Serializable {

	private static final long serialVersionUID = 1641905060375832661L;

	private String source;
	private String language;
	private String expression;
	private Object compiledExpression;

	public Transformation(String lang, String expression) {
		this.language = lang;
		this.expression = expression;
	}

	public Transformation(String lang, String expression, String source) {
		this.language = lang;
		this.expression = expression;
		this.source = source;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public Object getCompiledExpression() {
		return compiledExpression;
	}

	public void setCompiledExpression(Object compliedExpression) {
		this.compiledExpression = compliedExpression;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

}
