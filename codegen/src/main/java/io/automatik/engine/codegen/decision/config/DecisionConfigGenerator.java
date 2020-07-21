
package io.automatik.engine.codegen.decision.config;

import static com.github.javaparser.ast.NodeList.nodeList;
import static io.automatik.engine.codegen.CodegenUtils.genericType;
import static io.automatik.engine.codegen.CodegenUtils.method;
import static io.automatik.engine.codegen.CodegenUtils.newObject;
import static io.automatik.engine.codegen.ConfigGenerator.callMerge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.kie.dmn.api.core.event.DMNRuntimeEventListener;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatik.engine.api.decision.DecisionEventListenerConfig;
import io.automatik.engine.codegen.di.DependencyInjectionAnnotator;

public class DecisionConfigGenerator {

	private static final String METHOD_EXTRACT_DECISION_EVENT_LISTENER_CONFIG = "extract_decisionEventListenerConfig";
	private static final String METHOD_MERGE_DECISION_EVENT_LISTENER_CONFIG = "merge_decisionEventListenerConfig";
	private static final String VAR_DECISION_EVENT_LISTENER_CONFIG = "decisionEventListenerConfigs";
	private static final String VAR_DEFAULT_DECISION_EVENT_LISTENER_CONFIG = "defaultDecisionEventListenerConfig";
	private static final String VAR_DMN_RUNTIME_EVENT_LISTENERS = "dmnRuntimeEventListeners";

	private DependencyInjectionAnnotator annotator;

	private List<BodyDeclaration<?>> members = new ArrayList<>();

	public ObjectCreationExpr newInstance() {
		if (annotator != null) {
			return new ObjectCreationExpr().setType(StaticDecisionConfig.class.getCanonicalName())
					.addArgument(new MethodCallExpr(METHOD_EXTRACT_DECISION_EVENT_LISTENER_CONFIG));
		} else {
			return new ObjectCreationExpr().setType(StaticDecisionConfig.class.getCanonicalName())
					.addArgument(new NameExpr(VAR_DEFAULT_DECISION_EVENT_LISTENER_CONFIG));
		}
	}

	public List<BodyDeclaration<?>> members() {

		if (annotator != null) {
			FieldDeclaration delcFieldDeclaration = annotator
					.withOptionalInjection(new FieldDeclaration().addVariable(new VariableDeclarator(
							genericType(annotator.multiInstanceInjectionType(), DecisionEventListenerConfig.class),
							VAR_DECISION_EVENT_LISTENER_CONFIG)));
			members.add(delcFieldDeclaration);

			FieldDeclaration drelFieldDeclaration = annotator
					.withOptionalInjection(new FieldDeclaration().addVariable(new VariableDeclarator(
							genericType(annotator.multiInstanceInjectionType(), DMNRuntimeEventListener.class),
							VAR_DMN_RUNTIME_EVENT_LISTENERS)));
			members.add(drelFieldDeclaration);

			members.add(generateExtractEventListenerConfigMethod());
			members.add(generateMergeEventListenerConfigMethod());
		} else {
			FieldDeclaration defaultDelcFieldDeclaration = new FieldDeclaration().setModifiers(Modifier.Keyword.PRIVATE)
					.addVariable(new VariableDeclarator(
							new ClassOrInterfaceType(null, DecisionEventListenerConfig.class.getCanonicalName()),
							VAR_DEFAULT_DECISION_EVENT_LISTENER_CONFIG,
							newObject(DefaultDecisionEventListenerConfig.class)));
			members.add(defaultDelcFieldDeclaration);
		}

		return members;
	}

	public DecisionConfigGenerator withDependencyInjection(DependencyInjectionAnnotator annotator) {
		this.annotator = annotator;
		return this;
	}

	private MethodDeclaration generateExtractEventListenerConfigMethod() {
		BlockStmt body = new BlockStmt().addStatement(
				new ReturnStmt(new MethodCallExpr(new ThisExpr(), METHOD_MERGE_DECISION_EVENT_LISTENER_CONFIG,
						nodeList(annotator.getMultiInstance(VAR_DECISION_EVENT_LISTENER_CONFIG),
								annotator.getMultiInstance(VAR_DMN_RUNTIME_EVENT_LISTENERS)))));

		return method(Modifier.Keyword.PRIVATE, DecisionEventListenerConfig.class,
				METHOD_EXTRACT_DECISION_EVENT_LISTENER_CONFIG, body);
	}

	private MethodDeclaration generateMergeEventListenerConfigMethod() {
		BlockStmt body = new BlockStmt().addStatement(new ReturnStmt(
				newObject(CachedDecisionEventListenerConfig.class, callMerge(VAR_DECISION_EVENT_LISTENER_CONFIG,
						DecisionEventListenerConfig.class, "listeners", VAR_DMN_RUNTIME_EVENT_LISTENERS))));

		return method(Modifier.Keyword.PRIVATE, DecisionEventListenerConfig.class,
				METHOD_MERGE_DECISION_EVENT_LISTENER_CONFIG,
				nodeList(
						new Parameter().setType(genericType(Collection.class, DecisionEventListenerConfig.class))
								.setName(VAR_DECISION_EVENT_LISTENER_CONFIG),
						new Parameter().setType(genericType(Collection.class, DMNRuntimeEventListener.class))
								.setName(VAR_DMN_RUNTIME_EVENT_LISTENERS)),
				body);
	}

}
