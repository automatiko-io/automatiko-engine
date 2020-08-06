package io.automatik.engine.workflow.marshalling.impl.strategies;

import java.util.HashSet;
import java.util.Set;

import io.automatik.engine.api.marshalling.ObjectMarshallingStrategy;
import io.automatik.engine.api.marshalling.ObjectMarshallingStrategyStore;
import io.automatik.engine.services.utils.StringUtils;

public class ObjectMarshallingStrategyStoreImpl implements ObjectMarshallingStrategyStore {

	private ObjectMarshallingStrategy[] strategiesList;

	public ObjectMarshallingStrategyStoreImpl(ObjectMarshallingStrategy[] strategiesList) {
		this.strategiesList = strategiesList;
		Set<String> names = new HashSet<String>();
		// Validate received set
		for (ObjectMarshallingStrategy strategy : strategiesList) {
			String name = strategy.getName();

			if (names.contains(name)) {
				throw new RuntimeException(
						"Multiple ObjectMarshallingStrategies with the same name found in environment:" + name);
			} else {
				names.add(name);
			}
		}
		names.clear();
	}

	// Old marshalling algorithm methods
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.kie.api.marshalling.impl.ObjectMarshallingStrategyStore#getStrategy(int)
	 */
	public ObjectMarshallingStrategy getStrategy(int index) {
		return this.strategiesList[index];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.kie.api.marshalling.impl.ObjectMarshallingStrategyStore#getStrategy(java.
	 * lang.Object)
	 */
	public int getStrategy(Object object) {
		for (int i = 0, length = this.strategiesList.length; i < length; i++) {
			if (strategiesList[i].accept(object)) {
				return i;
			}
		}
		throw new RuntimeException(
				"Unable to find PlaceholderResolverStrategy for class : " + object.getClass() + " object : " + object);
	}

	// New marshalling algorithm methods
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.kie.api.marshalling.impl.ObjectMarshallingStrategyStore#getStrategyObject
	 * (java.lang.String)
	 */
	public ObjectMarshallingStrategy getStrategyObject(String strategyName) {
		if (StringUtils.isEmpty(strategyName)) {
			return null;
		}

		for (int i = 0; i < this.strategiesList.length; ++i) {
			if (strategiesList[i].getName().equals(strategyName)) {
				return strategiesList[i];
			}
		}
		throw new RuntimeException("Unable to find PlaceholderResolverStrategy for name : " + strategyName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.kie.api.marshalling.impl.ObjectMarshallingStrategyStore#getStrategyObject
	 * (java.lang.Object)
	 */
	public ObjectMarshallingStrategy getStrategyObject(Object object) {
		for (int i = 0, length = this.strategiesList.length; i < length; i++) {
			if (strategiesList[i].accept(object)) {
				return strategiesList[i];
			}
		}
		throw new RuntimeException(
				"Unable to find PlaceholderResolverStrategy for class : " + object.getClass() + " object : " + object);
	}

}
