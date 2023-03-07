package io.automatiko.engine.addons.usertasks.notification.webhook;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.ConfigProvider;

import io.automatiko.engine.addons.usertasks.notification.NotificationEmitter;
import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.runtime.process.HumanTaskWorkItem;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;

public abstract class WebHookNotificationEmitter<T> implements NotificationEmitter {

    private Map<String, T> webHooks = new ConcurrentHashMap<>();

    private Map<String, String> connections = new ConcurrentHashMap<>();

    private Engine engine;

    private String serviceUrl;

    public WebHookNotificationEmitter() {
    }

    public WebHookNotificationEmitter(Engine engine, Optional<String> serviceUrl) {
        this.engine = engine;
        this.serviceUrl = serviceUrl.orElse("http://localhost:8080");
    }

    protected abstract String defaultTemplate();

    protected abstract String templateSuffix();

    protected String buildTemplate(Map<String, Object> data, WorkItem workItem) {

        HumanTaskWorkItem humanTask = (HumanTaskWorkItem) workItem;

        Template template = getTemplate(humanTask.getProcessInstance().getProcess(), humanTask);
        if (template == null) {
            template = engine.getTemplate(defaultTemplate());
        }

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("name", humanTask.getTaskName());
        templateData.put("description", humanTask.getTaskDescription());
        templateData.put("taskId", humanTask.getId());
        templateData.put("instanceId", humanTask.getProcessInstanceId());
        templateData.put("processId", humanTask.getProcessInstance().getProcessId());
        templateData.put("processName", humanTask.getProcessInstance().getProcessName());
        templateData.put("inputs", humanTask.getParameters());

        String parentProcessInstanceId = humanTask.getProcessInstance().getParentProcessInstanceId();
        if (parentProcessInstanceId != null && !parentProcessInstanceId.isEmpty()) {
            parentProcessInstanceId += ":";
        } else {
            parentProcessInstanceId = "";
        }
        String version = version(humanTask.getProcessInstance().getProcess().getVersion());
        String encoded = Base64.getEncoder().encodeToString((humanTask.getProcessInstance().getProcessId() + version + "|"
                + parentProcessInstanceId + humanTask.getProcessInstance().getId() + "|" + humanTask.getId())
                        .getBytes(StandardCharsets.UTF_8));
        String link = serviceUrl + "/management/tasks/link/" + encoded;
        templateData.put("link", link);
        return template.instance().data(templateData).render();
    }

    protected T webhook(String channel, String configProperty) {

        String urlForChannel = connections.computeIfAbsent(channel,
                k -> ConfigProvider.getConfig().getOptionalValue(configProperty + channel, String.class).orElse(null));

        if (urlForChannel == null) {
            return null;
        }

        return webHooks.computeIfAbsent(channel, k -> buildConnector(urlForChannel));
    }

    /**
     * Retrieve custom template for the slack message in following order
     * <ul>
     * <li>processId.taskname-slack</li>
     * <li>taskname-slack</li>
     * </ul>
     * 
     * @param process id of the process task belongs to
     * @param humanTask an instance of user task
     * @return template for slack message if found otherwise null
     */
    protected Template getTemplate(Process process, HumanTaskWorkItem humanTask) {
        Template template = engine.getTemplate(process.getId() + version(process.getVersion()) + "."
                + (String) humanTask.getParameters().getOrDefault("TaskName",
                        humanTask.getTaskName())
                + templateSuffix());
        if (template == null) {
            template = engine.getTemplate((String) humanTask.getParameters().getOrDefault("TaskName",
                    humanTask.getTaskName()) + templateSuffix());
        }

        return template;
    }

    protected String version(String version) {
        if (version != null && !version.trim().isEmpty()) {
            return "_" + version.replaceAll("\\.", "_");
        }
        return "";
    }

    protected abstract T buildConnector(String webhookUrl);
}
