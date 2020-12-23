
package io.automatiko.engine.codegen.process.config;

import static io.automatiko.engine.codegen.CodegenUtils.extractOptionalInjection;
import static io.automatiko.engine.codegen.CodegenUtils.genericType;
import static io.automatiko.engine.codegen.CodegenUtils.method;
import static io.automatiko.engine.codegen.CodegenUtils.newObject;
import static io.automatiko.engine.codegen.ConfigGenerator.callMerge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatiko.engine.api.event.process.ProcessEventListener;
import io.automatiko.engine.api.jobs.JobsService;
import io.automatiko.engine.api.uow.UnitOfWorkManager;
import io.automatiko.engine.api.workflow.ProcessEventListenerConfig;
import io.automatiko.engine.api.workflow.VariableInitializer;
import io.automatiko.engine.api.workflow.WorkItemHandlerConfig;
import io.automatiko.engine.codegen.di.DependencyInjectionAnnotator;
import io.automatiko.engine.services.uow.CollectingUnitOfWorkFactory;
import io.automatiko.engine.services.uow.DefaultUnitOfWorkManager;
import io.automatiko.engine.workflow.CachedProcessEventListenerConfig;
import io.automatiko.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatiko.engine.workflow.DefaultWorkItemHandlerConfig;
import io.automatiko.engine.workflow.StaticProcessConfig;
import io.automatiko.engine.workflow.base.instance.context.variable.DefaultVariableInitializer;

public class ProcessConfigGenerator {

	private static final String METHOD_EXTRACT_JOBS_SERVICE = "extract_jobsService";
	private static final String METHOD_EXTRACT_PROCESS_EVENT_LISTENER_CONFIG = "extract_processEventListenerConfig";
	private static final String METHOD_EXTRACT_UNIT_OF_WORK_MANAGER = "extract_unitOfWorkManager";
	private static final String METHOD_EXTRACT_WORK_ITEM_HANDLER_CONFIG = "extract_workItemHandlerConfig";
	private static final String METHOD_EXTRACT_VARIABLE_INITIALIZER = "extract_variableInitializer";
	private static final String METHOD_MERGE_PROCESS_EVENT_LISTENER_CONFIG = "merge_processEventListenerConfig";
	private static final String VAR_DEFAULT_JOBS_SEVICE = "defaultJobsService";
	private static final String VAR_DEFAULT_PROCESS_EVENT_LISTENER_CONFIG = "defaultProcessEventListenerConfig";
	private static final String VAR_DEFAULT_UNIT_OF_WORK_MANAGER = "defaultUnitOfWorkManager";
	private static final String VAR_DEFAULT_VARIABLE_INITIALIZER = "defaultVariableInitializer";
	private static final String VAR_DEFAULT_WORK_ITEM_HANDLER_CONFIG = "defaultWorkItemHandlerConfig";
	private static final String VAR_JOBS_SERVICE = "jobsService";
	private static final String VAR_PROCESS_EVENT_LISTENER_CONFIGS = "processEventListenerConfigs";
	private static final String VAR_PROCESS_EVENT_LISTENERS = "processEventListeners";
	private static final String VAR_UNIT_OF_WORK_MANAGER = "unitOfWorkManager";
	private static final String VAR_VARIABLE_INITIALIZER = "variableInitializer";
	private static final String VAR_WORK_ITEM_HANDLER_CONFIG = "workItemHandlerConfig";

	private DependencyInjectionAnnotator annotator;

	private List<BodyDeclaration<?>> members = new ArrayList<>();

	public ObjectCreationExpr newInstance() {
		if (annotator != null) {
			return new ObjectCreationExpr().setType(StaticProcessConfig.class.getCanonicalName())
					.addArgument(new MethodCallExpr(METHOD_EXTRACT_WORK_ITEM_HANDLER_CONFIG))
					.addArgument(new MethodCallExpr(METHOD_EXTRACT_PROCESS_EVENT_LISTENER_CONFIG))
					.addArgument(new MethodCallExpr(METHOD_EXTRACT_UNIT_OF_WORK_MANAGER))
					.addArgument(new MethodCallExpr(METHOD_EXTRACT_JOBS_SERVICE))
					.addArgument(new MethodCallExpr(METHOD_EXTRACT_VARIABLE_INITIALIZER));
		} else {
			return new ObjectCreationExpr().setType(StaticProcessConfig.class.getCanonicalName())
					.addArgument(new NameExpr(VAR_DEFAULT_WORK_ITEM_HANDLER_CONFIG))
					.addArgument(new NameExpr(VAR_DEFAULT_PROCESS_EVENT_LISTENER_CONFIG))
					.addArgument(new NameExpr(VAR_DEFAULT_UNIT_OF_WORK_MANAGER))
					.addArgument(new NameExpr(VAR_DEFAULT_JOBS_SEVICE))
					.addArgument(new NameExpr(VAR_DEFAULT_VARIABLE_INITIALIZER));
		}
	}

	public List<BodyDeclaration<?>> members() {

		FieldDeclaration defaultWihcFieldDeclaration = new FieldDeclaration().setModifiers(Modifier.Keyword.PRIVATE)
				.addVariable(new VariableDeclarator(
						new ClassOrInterfaceType(null, WorkItemHandlerConfig.class.getCanonicalName()),
						VAR_DEFAULT_WORK_ITEM_HANDLER_CONFIG, newObject(DefaultWorkItemHandlerConfig.class)));
		members.add(defaultWihcFieldDeclaration);

		FieldDeclaration defaultUowFieldDeclaration = new FieldDeclaration().setModifiers(Modifier.Keyword.PRIVATE)
				.addVariable(new VariableDeclarator(
						new ClassOrInterfaceType(null, UnitOfWorkManager.class.getCanonicalName()),
						VAR_DEFAULT_UNIT_OF_WORK_MANAGER,
						newObject(DefaultUnitOfWorkManager.class, newObject(CollectingUnitOfWorkFactory.class))));
		members.add(defaultUowFieldDeclaration);

		FieldDeclaration defaultJobsServiceFieldDeclaration = new FieldDeclaration()
				.setModifiers(Modifier.Keyword.PRIVATE).addVariable(
						new VariableDeclarator(new ClassOrInterfaceType(null, JobsService.class.getCanonicalName()),
								VAR_DEFAULT_JOBS_SEVICE, new NullLiteralExpr()));
		members.add(defaultJobsServiceFieldDeclaration);

		FieldDeclaration defaultVariableInitializerFieldDeclaration = new FieldDeclaration()
				.setModifiers(Modifier.Keyword.PRIVATE)
				.addVariable(new VariableDeclarator(
						new ClassOrInterfaceType(null, VariableInitializer.class.getCanonicalName()),
						VAR_DEFAULT_VARIABLE_INITIALIZER, newObject(DefaultVariableInitializer.class)));
		members.add(defaultVariableInitializerFieldDeclaration);

		if (annotator != null) {
			FieldDeclaration wihcFieldDeclaration = annotator
					.withInjection(new FieldDeclaration().addVariable(new VariableDeclarator(
							genericType(annotator.optionalInstanceInjectionType(), WorkItemHandlerConfig.class),
							VAR_WORK_ITEM_HANDLER_CONFIG)));
			members.add(wihcFieldDeclaration);

			FieldDeclaration uowmFieldDeclaration = annotator
					.withInjection(new FieldDeclaration().addVariable(new VariableDeclarator(
							genericType(annotator.optionalInstanceInjectionType(), UnitOfWorkManager.class),
							VAR_UNIT_OF_WORK_MANAGER)));
			members.add(uowmFieldDeclaration);

			FieldDeclaration jobsServiceFieldDeclaration = annotator.withInjection(new FieldDeclaration().addVariable(
					new VariableDeclarator(genericType(annotator.optionalInstanceInjectionType(), JobsService.class),
							VAR_JOBS_SERVICE)));
			members.add(jobsServiceFieldDeclaration);

			FieldDeclaration viFieldDeclaration = annotator
					.withInjection(new FieldDeclaration().addVariable(new VariableDeclarator(
							genericType(annotator.optionalInstanceInjectionType(), VariableInitializer.class),
							VAR_VARIABLE_INITIALIZER)));
			members.add(viFieldDeclaration);

			FieldDeclaration pelcFieldDeclaration = annotator
					.withOptionalInjection(new FieldDeclaration().addVariable(new VariableDeclarator(
							genericType(annotator.multiInstanceInjectionType(), ProcessEventListenerConfig.class),
							VAR_PROCESS_EVENT_LISTENER_CONFIGS)));
			members.add(pelcFieldDeclaration);

			FieldDeclaration pelFieldDeclaration = annotator
					.withOptionalInjection(new FieldDeclaration().addVariable(new VariableDeclarator(
							genericType(annotator.multiInstanceInjectionType(), ProcessEventListener.class),
							VAR_PROCESS_EVENT_LISTENERS)));
			members.add(pelFieldDeclaration);

			members.add(extractOptionalInjection(WorkItemHandlerConfig.class.getCanonicalName(),
					VAR_WORK_ITEM_HANDLER_CONFIG, VAR_DEFAULT_WORK_ITEM_HANDLER_CONFIG, annotator));
			members.add(extractOptionalInjection(UnitOfWorkManager.class.getCanonicalName(), VAR_UNIT_OF_WORK_MANAGER,
					VAR_DEFAULT_UNIT_OF_WORK_MANAGER, annotator));
			members.add(extractOptionalInjection(JobsService.class.getCanonicalName(), VAR_JOBS_SERVICE,
					VAR_DEFAULT_JOBS_SEVICE, annotator));
			members.add(extractOptionalInjection(VariableInitializer.class.getCanonicalName(), VAR_VARIABLE_INITIALIZER,
					VAR_DEFAULT_VARIABLE_INITIALIZER, annotator));

			members.add(generateExtractEventListenerConfigMethod());
			members.add(generateMergeEventListenerConfigMethod());
		} else {
			FieldDeclaration defaultPelcFieldDeclaration = new FieldDeclaration().setModifiers(Modifier.Keyword.PRIVATE)
					.addVariable(new VariableDeclarator(
							new ClassOrInterfaceType(null, ProcessEventListenerConfig.class.getCanonicalName()),
							VAR_DEFAULT_PROCESS_EVENT_LISTENER_CONFIG,
							newObject(DefaultProcessEventListenerConfig.class)));
			members.add(defaultPelcFieldDeclaration);
		}

		return members;
	}

	public ProcessConfigGenerator withDependencyInjection(DependencyInjectionAnnotator annotator) {
		this.annotator = annotator;
		return this;
	}

	private MethodDeclaration generateExtractEventListenerConfigMethod() {
		BlockStmt body = new BlockStmt().addStatement(
				new ReturnStmt(new MethodCallExpr(new ThisExpr(), METHOD_MERGE_PROCESS_EVENT_LISTENER_CONFIG,
						NodeList.nodeList(annotator.getMultiInstance(VAR_PROCESS_EVENT_LISTENER_CONFIGS),
								annotator.getMultiInstance(VAR_PROCESS_EVENT_LISTENERS)))));

		return method(Modifier.Keyword.PRIVATE, ProcessEventListenerConfig.class,
				METHOD_EXTRACT_PROCESS_EVENT_LISTENER_CONFIG, body);
	}

	private MethodDeclaration generateMergeEventListenerConfigMethod() {
		BlockStmt body = new BlockStmt().addStatement(new ReturnStmt(
				newObject(CachedProcessEventListenerConfig.class, callMerge(VAR_PROCESS_EVENT_LISTENER_CONFIGS,
						ProcessEventListenerConfig.class, "listeners", VAR_PROCESS_EVENT_LISTENERS))));

		return method(Modifier.Keyword.PRIVATE, ProcessEventListenerConfig.class,
				METHOD_MERGE_PROCESS_EVENT_LISTENER_CONFIG,
				NodeList.nodeList(
						new Parameter().setType(genericType(Collection.class, ProcessEventListenerConfig.class))
								.setName(VAR_PROCESS_EVENT_LISTENER_CONFIGS),
						new Parameter().setType(genericType(Collection.class, ProcessEventListener.class))
								.setName(VAR_PROCESS_EVENT_LISTENERS)),
				body);
	}

}
