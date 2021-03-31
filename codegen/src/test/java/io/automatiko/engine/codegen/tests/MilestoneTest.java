
package io.automatiko.engine.codegen.tests;

import static io.automatiko.engine.api.workflow.flexible.ItemDescription.Status.AVAILABLE;
import static io.automatiko.engine.api.workflow.flexible.ItemDescription.Status.COMPLETED;
import static io.automatiko.engine.codegen.tests.ProcessTestUtils.assertState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.api.workflow.flexible.Milestone;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.process.executable.core.Metadata;
import io.automatiko.engine.workflow.process.executable.instance.ExecutableProcessInstance;

class MilestoneTest extends AbstractCodegenTest {

    @Test
    void testSimpleMilestone() throws Exception {
        Application app = generateCodeProcessesOnly("cases/milestones/SimpleMilestone.bpmn");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("TestCase.SimpleMilestone_1_0");
        ProcessInstance<?> processInstance = p.createInstance(p.createModel());
        assertState(processInstance, ProcessInstance.STATE_PENDING);

        Collection<Milestone> expected = new ArrayList<>();
        expected.add(Milestone.builder().withName("AutoStartMilestone").withStatus(AVAILABLE).build());
        expected.add(Milestone.builder().withName("SimpleMilestone").withStatus(AVAILABLE).build());
        assertMilestones(expected, processInstance.milestones());

        processInstance.start();
        assertState(processInstance, ProcessInstance.STATE_COMPLETED);

        expected = expected.stream()
                .map(m -> Milestone.builder().withId(m.getId()).withName(m.getName()).withStatus(COMPLETED).build())
                .collect(Collectors.toList());
        assertMilestones(expected, processInstance.milestones());

        ExecutableProcessInstance legacyProcessInstance = (ExecutableProcessInstance) ((AbstractProcessInstance<?>) processInstance)
                .processInstance();
        assertThat(legacyProcessInstance.getNodeInstances()).isEmpty();

        Optional<String> milestoneId = Stream.of(legacyProcessInstance.getNodeContainer().getNodes())
                .filter(node -> node.getName().equals("SimpleMilestone"))
                .map(n -> (String) n.getMetaData().get(Metadata.UNIQUE_ID)).findFirst();
        assertTrue(milestoneId.isPresent());
        assertThat(legacyProcessInstance.getCompletedNodeIds()).contains(milestoneId.get());
    }

    @Test
    void testConditionalMilestone() throws Exception {
        Application app = generateCodeProcessesOnly("cases/milestones/ConditionalMilestone.bpmn");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("TestCase.ConditionalMilestone_1_0");
        Model model = p.createModel();
        Map<String, Object> params = new HashMap<>();
        params.put("favouriteColour", "orange");
        model.fromMap(params);
        ProcessInstance<?> processInstance = p.createInstance(model);
        assertState(processInstance, ProcessInstance.STATE_PENDING);

        Collection<Milestone> expected = new ArrayList<>();
        expected.add(Milestone.builder().withName("Milestone").withStatus(AVAILABLE).build());
        assertMilestones(expected, processInstance.milestones());

        processInstance.start();
        assertState(processInstance, ProcessInstance.STATE_ACTIVE);

        expected = expected.stream()
                .map(m -> Milestone.builder().withId(m.getId()).withName(m.getName()).withStatus(AVAILABLE).build())
                .collect(Collectors.toList());
        assertMilestones(expected, processInstance.milestones());

        List<WorkItem> workItems = processInstance.workItems();
        params.put("favouriteColour", "blue");
        processInstance.completeWorkItem(workItems.get(0).getId(), params);

        expected = expected.stream()
                .map(m -> Milestone.builder().withId(m.getId()).withName(m.getName()).withStatus(COMPLETED).build())
                .collect(Collectors.toList());
        assertMilestones(expected, processInstance.milestones());
    }

    private void assertMilestones(Collection<Milestone> expected, Collection<Milestone> milestones) {
        if (expected == null) {
            assertNull(milestones);
        }
        assertNotNull(milestones);
        assertThat(milestones.size()).isEqualTo(expected.size());
        expected.forEach(e -> assertThat(milestones.stream().anyMatch(
                c -> Objects.equals(c.getName(), e.getName()) && Objects.equals(c.getStatus(), e.getStatus())))
                        .withFailMessage("Expected: " + e + " - Not present in: " + milestones).isTrue());
    }

}
