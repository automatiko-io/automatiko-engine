
package io.automatik.engine.workflow.bpmn2.xml;

import io.automatik.engine.workflow.bpmn2.xml.di.BPMNEdgeHandler;
import io.automatik.engine.workflow.bpmn2.xml.di.BPMNPlaneHandler;
import io.automatik.engine.workflow.bpmn2.xml.di.BPMNShapeHandler;
import io.automatik.engine.workflow.compiler.xml.DefaultSemanticModule;

public class BPMNDISemanticModule extends DefaultSemanticModule {

	public BPMNDISemanticModule() {
		super("http://www.omg.org/spec/BPMN/20100524/DI");

		addHandler("BPMNPlane", new BPMNPlaneHandler());
		addHandler("BPMNShape", new BPMNShapeHandler());
		addHandler("BPMNEdge", new BPMNEdgeHandler());
	}

}
