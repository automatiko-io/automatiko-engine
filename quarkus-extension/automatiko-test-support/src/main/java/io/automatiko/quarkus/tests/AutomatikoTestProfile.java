package io.automatiko.quarkus.tests;

import java.util.HashSet;
import java.util.Set;

import io.automatiko.quarkus.tests.jobs.TestJobService;
import io.quarkus.test.junit.QuarkusTestProfile;

public class AutomatikoTestProfile implements QuarkusTestProfile {

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        Set<Class<?>> alternatives = new HashSet<>();

        alternatives.add(TestJobService.class);

        return alternatives;
    }

}
