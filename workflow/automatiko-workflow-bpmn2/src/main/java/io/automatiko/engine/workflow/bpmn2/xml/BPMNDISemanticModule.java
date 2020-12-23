
package io.automatiko.engine.workflow.bpmn2.xml;

import io.automatiko.engine.workflow.bpmn2.xml.di.BPMNEdgeHandler;
import io.automatiko.engine.workflow.bpmn2.xml.di.BPMNPlaneHandler;
import io.automatiko.engine.workflow.bpmn2.xml.di.BPMNShapeHandler;
import io.automatiko.engine.workflow.compiler.xml.DefaultSemanticModule;

public class BPMNDISemanticModule extends DefaultSemanticModule {

	public BPMNDISemanticModule() {
		super("http://www.omg.org/spec/BPMN/20100524/DI");

		addHandler("BPMNPlane", new BPMNPlaneHandler());
		addHandler("BPMNShape", new BPMNShapeHandler());
		addHandler("BPMNEdge", new BPMNEdgeHandler());
	}

}
