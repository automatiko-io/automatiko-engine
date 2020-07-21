
package io.automatik.engine.workflow.base.core.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.runtime.process.DataTransformer;
import io.automatik.engine.workflow.base.core.transformation.JavaScriptingDataTransformer;
import io.automatik.engine.workflow.base.core.transformation.MVELDataTransformer;

/**
 * Registry for all available on runtime <code>DataTransformer</code>s for
 * performing data input and output transformation. Be default it discovers and
 * registers all available JSR 223 compliant scripting languages wrapped with
 * <code>JavaScriptingDataTransformer</code>.<br/>
 * Discovery is based on <code>ScriptEngineManager.getEngineFactories()</code>
 * thus any scripting engines should follow instructions for Java
 * Scripting.<br/>
 * Each Java Scripting engine will get dedicated instance of
 * <code>JavaScriptingDataTransformer</code> which will be registered under one
 * or more key in the registry. The key will be built as follows>
 * <ul>
 * <li>constant prefix for URI like syntax : http://www.java.com/</li>
 * <li>names provided by given ScriptEngineFactory - JavaScript, js,....</li>
 * </ul>
 * For example default JavaScript scripting engine will be registered under
 * following keys:
 * <ul>
 * <li>http://www.java.com/js</li>
 * <li>http://www.java.com/rhino</li>
 * <li>http://www.java.com/JavaScript</li>
 * <li>http://www.java.com/javascript</li>
 * <li>http://www.java.com/ECMAScript</li>
 * <li>http://www.java.com/ecmascript</li>
 * </ul>
 * when defining the language for transformation that of data input/output the
 * complete key should be provided. <code>
 * 	   &lt;dataInputAssociation&gt;<br/>
 * &nbsp;      &lt;sourceRef&gt;s&lt;/sourceRef&gt;<br/>        
 * &nbsp;      &lt;targetRef&gt;_2_param&lt;/targetRef&gt;<br/>
 * &nbsp;      &lt;transformation language="http://www.java.com/javascript"&gt;s.toUpperCase()&lt;/transformation&gt;<br/>
 *     &lt;/dataInputAssociation&gt;<br/>
 * </code> <br/>
 * Besides JSR 223 scripting engine, there is MVEL based transformer available
 * out of the box that is registered under <code>http://www.mvel.org/2.0</code>
 * key. <br/>
 * Custom implementations can be provided and if they are compliant with JSR 223
 * then follows above registration approach otherwise they need to be registered
 * manually with <code>register</code> method.
 * 
 * @see JavaScriptingDataTransformer
 */
public class DataTransformerRegistry {

	private static final Logger logger = LoggerFactory.getLogger(DataTransformerRegistry.class);
	private static final DataTransformerRegistry INSTANCE =
			// do not create ScriptEngineManager on SVM for now
			new DataTransformerRegistry(Collections.emptyList());

	private Map<String, DataTransformer> registry;

	protected DataTransformerRegistry(List<ScriptEngineFactory> factories) {
		this.registry = new ConcurrentHashMap<String, DataTransformer>();
		this.registry.put("http://www.mvel.org/2.0", new MVELDataTransformer());
		for (ScriptEngineFactory factory : factories) {
			DataTransformer transformer = new JavaScriptingDataTransformer(factory);

			for (String name : factory.getNames()) {
				String key = "http://www.java.com/" + name;
				registry.put(key, transformer);
				logger.debug("Registered scripting language {} with instance {}", key, transformer);
			}
		}
	}

	public static DataTransformerRegistry get() {
		return INSTANCE;
	}

	public synchronized void register(String language, DataTransformer transformer) {
		this.registry.put(language, transformer);
		logger.debug("Manual registration of scripting language {} with instance {}", language, transformer);
	}

	public DataTransformer find(String languge) {
		return this.registry.get(languge);
	}
}
