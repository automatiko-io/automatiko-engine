package io.automatiko.engine.workflow.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.workflow.base.instance.impl.util.VariableUtil;

public class DotNotationChangeTest {

    @Test
    public void testPojoDotNotation() {

        String dotnotation = "person.name";

        String expression = VariableUtil.transformDotNotation(dotnotation, "event");
        assertEquals("person.setName(event)", expression);

        String var = VariableUtil.nameFromDotNotation(dotnotation);
        assertEquals("person", var);
    }

    @Test
    public void testPojo2ndLevelDotNotation() {

        String dotnotation = "person.name.firstName";

        String expression = VariableUtil.transformDotNotation(dotnotation, "event");
        assertEquals("person.getName().setFirstName(event)", expression);
    }

    @Test
    public void testPojoCollectionItemDotNotation() {

        String dotnotation = "person.stringList[]";

        String expression = VariableUtil.transformDotNotation(dotnotation, "event");
        assertEquals("person.getStringList().add(event)", expression);
    }

    @Test
    public void testPojo2ndLevelCollectionItemDotNotation() {

        String dotnotation = "person.name.firstName[]";

        String expression = VariableUtil.transformDotNotation(dotnotation, "event");
        assertEquals("person.getName().add(event)", expression);
    }

    @Test
    public void testCollectionItemDotNotation() {

        String dotnotation = "firstName[]";

        String expression = VariableUtil.transformDotNotation(dotnotation, "event");
        assertEquals("firstName.add(event)", expression);
    }

    @Test
    public void testPojo2ndLevelCollectionItemDotNotationPlus() {

        String dotnotation = "person.name.firstName[+]";

        String expression = VariableUtil.transformDotNotation(dotnotation, "event");
        assertEquals("person.getName().add(event)", expression);
    }

    @Test
    public void testCollectionItemDotNotationPlus() {

        String dotnotation = "firstName[+]";

        String expression = VariableUtil.transformDotNotation(dotnotation, "event");
        assertEquals("firstName.add(event)", expression);

        String var = VariableUtil.nameFromDotNotation(dotnotation);
        assertEquals("firstName", var);
    }

    @Test
    public void testPojo2ndLevelCollectionItemDotNotationMinus() {

        String dotnotation = "person.name.firstName[-]";

        String expression = VariableUtil.transformDotNotation(dotnotation, "event");
        assertEquals("person.getName().remove(event)", expression);

        String var = VariableUtil.nameFromDotNotation(dotnotation);
        assertEquals("person", var);
    }

    @Test
    public void testCollectionItemDotNotationMinus() {

        String dotnotation = "firstName[-]";

        String expression = VariableUtil.transformDotNotation(dotnotation, "event");
        assertEquals("firstName.remove(event)", expression);
    }
}
