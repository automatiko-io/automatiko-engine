
package io.automatik.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.automatik.engine.api.Application;
import io.automatik.engine.api.Model;
import io.automatik.engine.api.auth.AccessDeniedException;
import io.automatik.engine.api.auth.IdentityProvider;
import io.automatik.engine.api.auth.SecurityPolicy;
import io.automatik.engine.api.auth.TrustedIdentityProvider;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.codegen.AbstractCodegenTest;
import io.automatik.engine.services.identity.StaticIdentityProvider;
import io.automatik.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatik.engine.workflow.Sig;
import io.automatik.engine.workflow.compiler.util.NodeLeftCountDownProcessEventListener;

public class AccessPolicyTest extends AbstractCodegenTest {

    private SecurityPolicy securityPolicy = SecurityPolicy.of(new StaticIdentityProvider("john"));

    @Test
    public void testAssignInitiatorFromIdentity() throws Exception {
        IdentityProvider.set(securityPolicy.value());
        Application app = generateCodeProcessesOnly("access-policy/UserTasksProcessWithAccessPolicy.bpmn2");
        assertThat(app).isNotNull();
        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.initiator()).hasValue("john");

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        // set identity to other user than initiator or any of the assigned human task actors
        IdentityProvider.set(new StaticIdentityProvider("mike"));

        // not initiator so can't
        // update process instance model
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> processInstance.updateVariables(null));

        // abort process instance
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> processInstance.abort());

        // signal process instance
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> processInstance.send(Sig.of("test")));
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        // set identity to one of the assigned human task actors but other than initiator
        IdentityProvider.set(new StaticIdentityProvider("mary"));
        processInstance.send(Sig.of("test"));

        // set identity to trusted identity to verify system actions can go through
        IdentityProvider.set(new TrustedIdentityProvider("System<test>"));
        processInstance.send(Sig.of("test"));

        // go back to initiator as identity that is allowed to perform operations
        IdentityProvider.set(securityPolicy.value());
        processInstance.abort();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testAssignInitiatorFromVariable() throws Exception {

        Application app = generateCodeProcessesOnly("access-policy/UserTasksProcessWithAccessPolicyVar.bpmn2");
        assertThat(app).isNotNull();
        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "john");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        IdentityProvider.set(securityPolicy.value());
        assertThat(processInstance.initiator()).hasValue("john");

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        // set identity to other user than initiator or any of the assigned human task actors
        IdentityProvider.set(new StaticIdentityProvider("mike"));

        // not initiator so can't
        // update process instance model
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> processInstance.updateVariables(null));

        // abort process instance
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> processInstance.abort());

        // signal process instance
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> processInstance.send(Sig.of("test")));
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        // set identity to one of the assigned human task actors but other than initiator
        IdentityProvider.set(new StaticIdentityProvider("mary"));
        processInstance.send(Sig.of("test"));

        // go back to initiator as identity that is allowed to perform operations
        IdentityProvider.set(securityPolicy.value());
        processInstance.abort();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testAccessPolicyAllowedBasedOnGroupAssignedUserTask() throws Exception {
        IdentityProvider.set(securityPolicy.value());
        Application app = generateCodeProcessesOnly("access-policy/UserTasksProcessWithAccessPolicyGroup.bpmn2");
        assertThat(app).isNotNull();
        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.initiator()).hasValue("john");

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        // set identity to other user than initiator or any of the assigned human task actors
        IdentityProvider.set(new StaticIdentityProvider("mike"));

        // not initiator so can't
        // update process instance model
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> processInstance.updateVariables(null));

        // abort process instance
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> processInstance.abort());

        // signal process instance
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> processInstance.send(Sig.of("test")));
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        // set identity to one of the assigned human task group assigned actors but other than initiator
        IdentityProvider.set(new StaticIdentityProvider("mary", Arrays.asList("managers")));
        processInstance.send(Sig.of("test"));

        // go back to initiator as identity that is allowed to perform operations
        IdentityProvider.set(securityPolicy.value());
        processInstance.abort();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testIntermediateCycleTimerEvent() throws Exception {
        IdentityProvider.set(securityPolicy.value());
        Application app = generateCodeProcessesOnly("access-policy/IntermediateCatchEventTimerCycleISOAccessPolicy.bpmn2");
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("timer", 3);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("IntermediateCatchEvent");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.initiator()).hasValue("john");

        boolean completed = listener.waitTillCompleted(5000);
        assertThat(completed).isTrue();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        processInstance.abort();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }
}
