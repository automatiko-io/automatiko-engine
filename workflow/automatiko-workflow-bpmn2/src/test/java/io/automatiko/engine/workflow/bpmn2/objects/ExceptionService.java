
package io.automatiko.engine.workflow.bpmn2.objects;

import io.automatiko.engine.api.runtime.process.WorkItem;

public class ExceptionService {

	public static String exceptionParameterName = "my.exception.parameter.name";
	private static ThreadLocal<Object[]> localCaughtEventObjectHolder = new ThreadLocal<Object[]>();

	public String throwException(String message) {
		throw new RuntimeException(message);
	}

	public void handleException(WorkItem workItem) {
		Object[] resultHolder = localCaughtEventObjectHolder.get();
		if (resultHolder != null && resultHolder.length > 0) {
			resultHolder[0] = workItem;
		}
	}

	public void setExceptionParameterName(String exceptionParam) {
		this.exceptionParameterName = exceptionParam;
	}

	public static void setCaughtEventObjectHolder(Object[] testVarArrayHolder) {
		localCaughtEventObjectHolder.set(testVarArrayHolder);
	}
}
