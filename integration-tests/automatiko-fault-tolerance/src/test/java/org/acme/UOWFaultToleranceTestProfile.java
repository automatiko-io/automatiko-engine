package org.acme;

import java.util.Map;

import io.automatiko.quarkus.tests.FaultToleranceTestProfile;

public class UOWFaultToleranceTestProfile extends FaultToleranceTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> props = super.getConfigOverrides();

        props.put(
                "quarkus.fault-tolerance.\"io.automatiko.addons.fault.tolerance.uow.TimeoutUnitOfWorkManager/execute\".timeout.value",
                "1");

        return props;
    }

}
