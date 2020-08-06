package io.automatik.engine.workflow.util;

import java.util.HashMap;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExpressionCompiler;

import io.automatik.engine.services.utils.StringUtils;

public class WidMVELEvaluator {

	public static ParserContext WID_PARSER_CONTEXT;
	// change this if data types change location
	public static final String DATA_TYPE_PACKAGE = "org.jbpm.process.core.datatype.impl.type";

	static {
		WID_PARSER_CONTEXT = new ParserContext();
		WID_PARSER_CONTEXT.addPackageImport(DATA_TYPE_PACKAGE);
		WID_PARSER_CONTEXT.setRetainParserState(false);
	}

	public static Object eval(final String expression) {
		ExpressionCompiler compiler = new ExpressionCompiler(getRevisedExpression(expression), WID_PARSER_CONTEXT);
		return MVEL.executeExpression(compiler.compile(), new HashMap());

	}

	private static String getRevisedExpression(String expression) {
		if (StringUtils.isEmpty(expression)) {
			return expression;
		}
		return expression;
	}
}
