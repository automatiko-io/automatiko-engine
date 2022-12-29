package io.automatiko.addons.management.error;

import org.eclipse.microprofile.config.ConfigProvider;

import io.automatiko.engine.api.Functions;
import io.automatiko.engine.workflow.base.core.timer.DateTimeUtils;

public class ErrorManagementFunctions implements Functions {

    public static String calculateDelay(String delay) {
        Double incrementFactor = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.automatiko.error-recovery.increment-factor", Double.class).orElse(1.0);

        long millis = DateTimeUtils.parseDuration(delay);

        Double increment = millis * incrementFactor;

        return String.valueOf(millis + increment.intValue()) + "ms";
    }
}
