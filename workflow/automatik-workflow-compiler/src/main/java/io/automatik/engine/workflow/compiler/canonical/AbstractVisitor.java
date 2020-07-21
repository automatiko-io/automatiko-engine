
package io.automatik.engine.workflow.compiler.canonical;

import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.utils.StringEscapeUtils;

import static io.automatik.engine.workflow.process.executable.core.factory.NodeFactory.METHOD_METADATA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public abstract class AbstractVisitor {

	protected static final String FACTORY_FIELD_NAME = "factory";
	protected static final String KCONTEXT_VAR = "kcontext";

	protected MethodCallExpr getFactoryMethod(String object, String methodName, Expression... args) {
		MethodCallExpr variableMethod = new MethodCallExpr(new NameExpr(object), methodName);

		for (Expression arg : args) {
			variableMethod.addArgument(arg);
		}
		return variableMethod;
	}

	protected String getOrDefault(String value, String defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		return value;
	}

	protected Expression getOrNullExpr(String value) {
		if (value == null) {
			return new NullLiteralExpr();
		}
		return new StringLiteralExpr(value);
	}

	protected void visitMetaData(Map<String, Object> metadata, BlockStmt body, String variableName) {
		metadata.forEach((k, v) -> {
			Expression expression = null;
			if (v instanceof Boolean) {
				expression = new BooleanLiteralExpr((Boolean) v);
			} else if (v instanceof Integer) {
				expression = new IntegerLiteralExpr((Integer) v);
			} else if (v instanceof Long) {
				expression = new LongLiteralExpr((Long) v);
			} else if (v instanceof String) {
				expression = new StringLiteralExpr(StringEscapeUtils.escapeJava(v.toString()));
			}
			if (expression != null) {
				body.addStatement(
						getFactoryMethod(variableName, METHOD_METADATA, new StringLiteralExpr(k), expression));
			}
		});
	}
}
