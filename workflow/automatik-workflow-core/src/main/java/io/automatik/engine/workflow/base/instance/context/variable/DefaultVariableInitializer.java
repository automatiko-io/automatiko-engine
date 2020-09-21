package io.automatik.engine.workflow.base.instance.context.variable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.automatik.engine.api.workflow.VariableInitializer;

public class DefaultVariableInitializer implements VariableInitializer {

	@Override
	public Object initialize(String name, Class<?> clazz) {
		if (List.class.isAssignableFrom(clazz)) {
			return new ArrayList<>();
		} else if (Set.class.isAssignableFrom(clazz)) {
			return new LinkedHashSet<>();
		} else if (Map.class.isAssignableFrom(clazz)) {
			return new HashMap<>();
		} else {
			try {
				return clazz.getConstructor().newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new RuntimeException("Unable to initialize variable of type " + clazz, e);
			}
		}
	}

}
