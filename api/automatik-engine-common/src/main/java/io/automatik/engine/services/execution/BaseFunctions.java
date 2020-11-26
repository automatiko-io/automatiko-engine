package io.automatik.engine.services.execution;

import java.util.Objects;

public class BaseFunctions {

    public static boolean isEqual(Object o1, Object o2) {
        return Objects.equals(o1, o2);
    }

    public static boolean isNumber(Object o) {

        if (o instanceof Number) {
            return true;
        }
        return false;
    }
}
