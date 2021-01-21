package org.acme.model;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.automatiko.engine.api.workflow.Variable;
import io.automatiko.engine.workflow.base.instance.context.variable.DefaultVariableInitializer;

@ApplicationScoped
public class CustomVariableInitializer extends DefaultVariableInitializer {

    @Inject
    PersonRepository repository;

    @Override
    protected Object defaultValue(String valueExpression, Variable definition, Map<String, Object> data) {

        if (valueExpression.equals("load")) {
            List<Person> list = repository.findByName((String) data.get("name"));

            if (list.isEmpty()) {
                return null;
            }

            return list.get(0);
        }

        return super.defaultValue(valueExpression, definition, data);
    }

}
