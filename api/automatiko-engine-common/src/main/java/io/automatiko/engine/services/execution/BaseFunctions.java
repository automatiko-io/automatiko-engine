package io.automatiko.engine.services.execution;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseFunctions {

    private static final Logger LOGGER = LoggerFactory.getLogger("io.automatiko.logger");

    /**
     * Checks two objects for equality
     * 
     * @param o1 first object to check
     * @param o2 second object to check
     * @return return true if given objects are equal otherwise false
     */
    public static boolean isEqual(Object o1, Object o2) {
        return Objects.equals(o1, o2);
    }

    /**
     * Checks if given object is a number
     * 
     * @param o object to check
     * @return return true if given object is a number otherwise false
     */
    public static boolean isNumber(Object o) {

        if (o instanceof Number) {
            return true;
        }
        return false;
    }

    /**
     * Retrieves previous version of given variable based on given versions
     * 
     * @param <T> type of variable
     * @param versions all available versions
     * @return return previous version of the variable if exists otherwise null
     */
    public static <T> T previousVersion(List<T> versions) {

        if (versions == null || versions.isEmpty()) {
            return null;
        }

        return versions.get(versions.size() - 1);
    }

    /**
     * Retrieves selected version of the variable from given versions
     * 
     * @param <T> type of variable
     * @param versions all available versions
     * @param version version number to retrieve
     * @return version of the variable if exists under given version number otherwise null
     */
    public static <T> T variableVersion(List<T> versions, int version) {

        if (versions == null || versions.isEmpty()) {
            return null;
        }
        try {
            return versions.get(version);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public static void log(Object value) {
        LOGGER.info(value == null ? "" : value.toString());
    }

    public static void log(String template, Object... items) {
        LOGGER.info(template, items);
    }

    public static void logWarning(String template, Object... items) {
        LOGGER.warn(template, items);
    }

    public static void logError(String template, Object... items) {
        LOGGER.error(template, items);
    }
}
