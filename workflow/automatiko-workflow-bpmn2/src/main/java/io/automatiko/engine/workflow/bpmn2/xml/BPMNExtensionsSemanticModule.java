
package io.automatiko.engine.workflow.bpmn2.xml;

import io.automatiko.engine.workflow.compiler.xml.DefaultSemanticModule;

public class BPMNExtensionsSemanticModule extends DefaultSemanticModule {

    public static final String BPMN2_EXTENSIONS_URI = "https://automatiko.io";

    public BPMNExtensionsSemanticModule() {
        super(BPMN2_EXTENSIONS_URI);

        addHandler("import", new ImportHandler());
        addHandler("global", new GlobalHandler());
        addHandler("metaData", new MetaDataHandler());
        addHandler("metaValue", new MetaValueHandler());
    }

}
