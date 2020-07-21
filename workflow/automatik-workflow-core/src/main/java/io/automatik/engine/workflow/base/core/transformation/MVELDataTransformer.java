
package io.automatik.engine.workflow.base.core.transformation;

import java.util.Map;
import java.util.Set;

import org.mvel2.MVEL;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.runtime.process.DataTransformer;

/**
 * MVEL based <code>DataTransformer</code> implementation
 *
 */
public class MVELDataTransformer implements DataTransformer {

	private static final Logger logger = LoggerFactory.getLogger(MVELDataTransformer.class);

	@Override
	public Object compile(String expression, Map<String, Object> parameters) {
		logger.debug("About to compile mvel expression {}", expression);
		ClassLoader classLoader = (ClassLoader) parameters.get("classloader");
		if (classLoader == null) {
			classLoader = this.getClass().getClassLoader();
		}
		ParserConfiguration config = new ParserConfiguration();
		config.setClassLoader(classLoader);
		ParserContext context = new ParserContext(config);
		if (parameters != null) {
			@SuppressWarnings("unchecked")
			Set<String> imports = (Set<String>) parameters.get("imports");
			if (imports != null) {
				for (String clazz : imports) {
					try {
						Class<?> cl = Class.forName(clazz, true, classLoader);
						context.addImport(cl.getSimpleName(), cl);
					} catch (ClassNotFoundException e) {
						logger.warn("Unable to load class {} due to {}", clazz, e.getException());
					}
					;
				}
			}
		}
		return MVEL.compileExpression(expression, context);
	}

	@Override
	public Object transform(Object expression, Map<String, Object> parameters) {
		logger.debug("About to execute mvel expression {} with parameters {}", expression, parameters);
		return MVEL.executeExpression(expression, parameters);
	}

}
