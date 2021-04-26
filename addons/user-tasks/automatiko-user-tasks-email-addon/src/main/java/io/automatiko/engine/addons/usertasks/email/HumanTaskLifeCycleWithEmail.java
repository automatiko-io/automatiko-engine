package io.automatiko.engine.addons.usertasks.email;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.runtime.process.HumanTaskWorkItem;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.runtime.process.WorkItemManager;
import io.automatiko.engine.api.workflow.workitem.LifeCyclePhase;
import io.automatiko.engine.api.workflow.workitem.Transition;
import io.automatiko.engine.workflow.base.instance.impl.humantask.BaseHumanTaskLifeCycle;
import io.automatiko.engine.workflow.base.instance.impl.workitem.Active;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;

public class HumanTaskLifeCycleWithEmail extends BaseHumanTaskLifeCycle {

    private static final String DEFAULT_TEMPLATE = "default-usertask-email";
    private static final String TEMPLATE_SUFFIX = "-email";

    private static final Logger LOGGER = LoggerFactory.getLogger(HumanTaskLifeCycleWithEmail.class);

    private Mailer mailer;

    private EmailAddressResolver emailAddressResolver;

    private Engine engine;

    private String serviceUrl;

    public HumanTaskLifeCycleWithEmail(Mailer mailer, EmailAddressResolver emailAddressResolver, Engine engine,
            Optional<String> serviceUrl) {
        this.mailer = mailer;
        this.emailAddressResolver = emailAddressResolver;
        this.engine = engine;
        this.serviceUrl = serviceUrl.orElse("http://localhost:8080");
    }

    @Override
    public Map<String, Object> transitionTo(WorkItem workItem, WorkItemManager manager,
            Transition<Map<String, Object>> transition) {
        Map<String, Object> data = super.transitionTo(workItem, manager, transition);
        LifeCyclePhase targetPhase = phaseById(transition.phase());

        if (targetPhase.id().equals(Active.ID)) {
            sendEmail(workItem);
        }

        return data;
    }

    private void sendEmail(WorkItem workItem) {
        HumanTaskWorkItem humanTask = (HumanTaskWorkItem) workItem;
        List<String> users = new ArrayList<>();

        if (humanTask.getActualOwner() != null) {
            users.add(humanTask.getActualOwner());
        }
        if (humanTask.getPotentialUsers() != null) {
            users.addAll(humanTask.getPotentialUsers());
        }

        Map<String, String> addresses = emailAddressResolver.resolve(users, humanTask.getPotentialGroups());

        if (addresses.isEmpty()) {
            return;
        }

        String subject = "New task has been assigned to you (" + humanTask.getTaskName() + ")";

        Template template = getTemplate(humanTask.getProcessInstance().getProcessId(), humanTask);
        if (template == null) {
            template = engine.getTemplate(DEFAULT_TEMPLATE);
        }
        Mail[] emails = new Mail[addresses.size()];
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("name", humanTask.getTaskName());
        templateData.put("description", humanTask.getTaskDescription());
        templateData.put("taskId", humanTask.getId());
        templateData.put("instanceId", humanTask.getProcessInstanceId());
        templateData.put("processId", humanTask.getProcessInstance().getProcessId());
        templateData.put("inputs", humanTask.getParameters());

        int count = 0;
        for (Entry<String, String> address : addresses.entrySet()) {
            String dedicatedTo = "";

            if (users.contains(address.getKey())) {
                dedicatedTo = address.getKey();
            }
            String parentProcessInstanceId = humanTask.getProcessInstance().getParentProcessInstanceId();
            if (parentProcessInstanceId != null && !parentProcessInstanceId.isEmpty()) {
                parentProcessInstanceId += ":";
            } else {
                parentProcessInstanceId = "";
            }
            String version = version(humanTask.getProcessInstance().getProcess().getVersion());
            String encoded = Base64.getEncoder().encodeToString((humanTask.getProcessInstance().getProcessId() + version + "|"
                    + parentProcessInstanceId + humanTask.getProcessInstance().getId() + "|" + humanTask.getId() + "|"
                    + dedicatedTo)
                            .getBytes(StandardCharsets.UTF_8));
            String link = serviceUrl + "/management/tasks/link/" + encoded;
            templateData.put("link", link);
            String body = template.instance().data(templateData).render();

            emails[count] = Mail.withHtml(address.getValue(), subject, body);
            count++;
        }
        // send emails asynchronously
        CompletableFuture.runAsync(() -> {
            for (String to : addresses.values()) {
                mailer.send(emails);
                LOGGER.debug("Email sent to {} with new assigned task {}", to, humanTask.getName());
            }
        });

    }

    /**
     * Retrieve custom template for the email in following order
     * <ul>
     * <li>processId.taskname-email</li>
     * <li>taskname-email</li>
     * </ul>
     * 
     * @param processId id of the process task belongs to
     * @param humanTask an instance of user task
     * @return template for email if found otherwise null
     */
    protected Template getTemplate(String processId, HumanTaskWorkItem humanTask) {
        Template template = engine.getTemplate(processId + "." + (String) humanTask.getParameters().getOrDefault("TaskName",
                humanTask.getTaskName()) + TEMPLATE_SUFFIX);
        if (template == null) {
            template = engine.getTemplate((String) humanTask.getParameters().getOrDefault("TaskName",
                    humanTask.getTaskName()) + TEMPLATE_SUFFIX);
        }

        return template;
    }

    protected String version(String version) {
        if (version != null && !version.trim().isEmpty()) {
            return "_" + version.replaceAll("\\.", "_");
        }
        return "";
    }
}
