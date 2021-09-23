package io.automatiko.engine.services.execution;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.auth.IdentityProvider;

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

    public static String todayDate() {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static String todayYearAndMonth() {
        LocalDate now = LocalDate.now();

        return now.getYear() + "-" + now.getMonthValue();
    }

    public static String todayMonth() {

        return LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
    }

    public static String previousMonth() {

        return LocalDate.now().minusMonths(1).getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
    }

    public static String nextMonth() {

        return LocalDate.now().plusMonths(1).getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
    }

    public static String todayDay() {

        return LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault());
    }

    public static String yesterdayDay() {

        return LocalDate.now().minusDays(1).getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault());
    }

    public static String tomorrowDay() {

        return LocalDate.now().plusDays(1).getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault());
    }

    public static String todayQuarter() {

        int current = LocalDate.now().get(IsoFields.QUARTER_OF_YEAR);
        String value = null;
        switch (current) {
            case 1:
                value = "I";
                break;
            case 2:
                value = "II";
                break;
            case 3:
                value = "III";
                break;
            case 4:
                value = "IV";
                break;

            default:
                break;
        }
        return value;
    }

    public static String previousQuarter() {
        int current = LocalDate.now().get(IsoFields.QUARTER_OF_YEAR);
        String previous = null;
        switch (current) {
            case 1:
                previous = "IV";
                break;
            case 2:
                previous = "I";
                break;
            case 3:
                previous = "II";
                break;
            case 4:
                previous = "III";
                break;

            default:
                break;
        }
        return previous;
    }

    public static String nextQuarter() {

        int current = LocalDate.now().get(IsoFields.QUARTER_OF_YEAR);
        String next = null;
        switch (current) {
            case 1:
                next = "II";
                break;
            case 2:
                next = "III";
                break;
            case 3:
                next = "IV";
                break;
            case 4:
                next = "I";
                break;

            default:
                break;
        }
        return next;
    }

    /*
     * Identity related properties
     */

    /**
     * Retrieves current user name if available otherwise null
     * 
     * @return name of the user who is currently performing operation
     */
    public static String identityName() {
        if (IdentityProvider.isSet()) {
            return IdentityProvider.get().getName();
        }

        return null;
    }

    /**
     * Retrieves current user name if available otherwise <code>whenNotFound</code> as fallback
     * 
     * @param whenNotFound default name to be returned in case there is no user information available
     * @return name of the user who is currently performing operation
     */
    public static String identityName(String whenNotFound) {
        String name = null;
        if (IdentityProvider.isSet()) {
            name = IdentityProvider.get().getName();
        }
        if (name != null) {
            return name;
        }

        return whenNotFound;
    }

    /**
     * Retrieves property associated with user information e.g. email address etc
     * 
     * @param name name of the property to fetch
     * @return returns value of the property or null if not found
     */
    public static Object identityProperty(String name) {
        return identityProperty(name, null);
    }

    /**
     * Retrieves property associated with user information e.g. email address etc in case of missing property it returns default
     * value
     * 
     * @param name name of the property to fetch
     * @param default value to be returned in case property is not found
     * @return returns value of the property or default value given if not found
     */
    public static Object identityProperty(String name, String defaultValue) {
        if (IdentityProvider.isSet()) {
            return IdentityProvider.get().properties().getOrDefault(name, defaultValue);
        }

        return defaultValue;
    }

    /**
     * Verifies if user performing current operation has given role
     * 
     * @param role name of the role user information should have
     * @return true if user information has the role otherwise false (also when there is no user information available)
     */
    public static boolean identityHasRole(String role) {
        if (IdentityProvider.isSet()) {
            return IdentityProvider.get().hasRole(role);
        }

        return false;
    }

    /**
     * Verifies if user performing current operation has any of the given roles
     * 
     * @param roles roles to be checked
     * @return true if any of the given roles exists in user information
     */
    public static boolean identityHasAnyRole(String... roles) {
        if (IdentityProvider.isSet()) {

            return Stream.of(roles).anyMatch(role -> IdentityProvider.get().hasRole(role));
        }

        return false;
    }

    /**
     * Verifies if user performing current operation has all of the given roles
     * 
     * @param roles roles to be checked
     * @return true if all of the given roles exists in user information
     */
    public static boolean identityHasAllRoles(String... roles) {
        if (IdentityProvider.isSet()) {

            return Stream.of(roles).allMatch(role -> IdentityProvider.get().hasRole(role));
        }

        return false;
    }
}
