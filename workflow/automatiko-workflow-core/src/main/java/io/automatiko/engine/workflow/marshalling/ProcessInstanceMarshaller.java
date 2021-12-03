
package io.automatiko.engine.workflow.marshalling;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.jobs.ExpirationTime;
import io.automatiko.engine.api.jobs.JobsService;
import io.automatiko.engine.api.jobs.ProcessInstanceJobDescription;
import io.automatiko.engine.api.marshalling.ObjectMarshallingStrategy;
import io.automatiko.engine.api.runtime.EnvironmentName;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.workflow.ExportedProcessInstance;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.workflow.AbstractProcess;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.StringExportedProcessInstance;
import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.marshalling.impl.AutomatikoMessages;
import io.automatiko.engine.workflow.marshalling.impl.ClassObjectMarshallingStrategyAcceptor;
import io.automatiko.engine.workflow.marshalling.impl.MarshallerReaderContext;
import io.automatiko.engine.workflow.marshalling.impl.PersisterHelper;
import io.automatiko.engine.workflow.marshalling.impl.ProcessMarshallerRegistry;
import io.automatiko.engine.workflow.marshalling.impl.ProtobufRuleFlowProcessInstanceMarshaller;
import io.automatiko.engine.workflow.marshalling.impl.strategies.SerializablePlaceholderResolverStrategy;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceImpl;
import io.automatiko.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;
import io.automatiko.engine.workflow.process.instance.node.StateBasedNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.TimerNodeInstance;

public class ProcessInstanceMarshaller {

    private Map<String, Object> env = new HashMap<String, Object>();

    public ProcessInstanceMarshaller(ObjectMarshallingStrategy... strategies) {
        ObjectMarshallingStrategy[] strats = null;
        if (strategies == null) {
            strats = new ObjectMarshallingStrategy[] {
                    new SerializablePlaceholderResolverStrategy(ClassObjectMarshallingStrategyAcceptor.DEFAULT) };
        } else {
            strats = new ObjectMarshallingStrategy[strategies.length + 1];
            int i = 0;
            for (ObjectMarshallingStrategy strategy : strategies) {
                strats[i] = strategy;
                i++;
            }
            strats[i] = new SerializablePlaceholderResolverStrategy(ClassObjectMarshallingStrategyAcceptor.DEFAULT);
        }

        env.put(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES, strats);
    }

    public void addToEnvironment(String name, Object value) {
        env.put(name, value);

    }

    public byte[] marhsallProcessInstance(ProcessInstance<?> processInstance) {

        io.automatiko.engine.api.runtime.process.ProcessInstance pi = ((AbstractProcessInstance<?>) processInstance)
                .internalGetProcessInstance();

        if (pi == null) {
            return null;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            ProcessMarshallerWriteContext context = new ProcessMarshallerWriteContext(baos,
                    ((io.automatiko.engine.workflow.base.instance.ProcessInstance) pi).getProcessRuntime(), null, env);
            context.setProcessInstanceId(pi.getId());
            context.setState(pi.getState());

            String processType = pi.getProcess().getType();
            context.stream.writeUTF(processType);

            io.automatiko.engine.workflow.marshalling.impl.ProcessInstanceMarshaller marshaller = ProcessMarshallerRegistry.INSTANCE
                    .getMarshaller(processType);

            Object result = marshaller.writeProcessInstance(context, pi);
            if (marshaller instanceof ProtobufRuleFlowProcessInstanceMarshaller && result != null) {
                AutomatikoMessages.ProcessInstance _instance = (AutomatikoMessages.ProcessInstance) result;
                PersisterHelper.writeToStreamWithHeader(context, _instance);
            }
            context.close();
            ((WorkflowProcessInstanceImpl) pi).disconnect();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error while marshalling process instance", e);
        }
    }

    public ExportedProcessInstance<?> exportProcessInstance(ProcessInstance<?> processInstance) {

        io.automatiko.engine.api.runtime.process.ProcessInstance pi = ((AbstractProcessInstance<?>) processInstance)
                .internalGetProcessInstance();

        if (pi == null) {
            return null;
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Map<String, Object> localEnv = new HashMap<String, Object>(env);
            localEnv.put("_export_", true);
            ProcessMarshallerWriteContext context = new ProcessMarshallerWriteContext(baos,
                    ((io.automatiko.engine.workflow.base.instance.ProcessInstance) pi).getProcessRuntime(), null, localEnv);
            context.setProcessInstanceId(pi.getId());
            context.setState(pi.getState());

            String processType = pi.getProcess().getType();
            context.stream.writeUTF(processType);

            io.automatiko.engine.workflow.marshalling.impl.ProcessInstanceMarshaller marshaller = ProcessMarshallerRegistry.INSTANCE
                    .getMarshaller(processType);

            Object result = marshaller.writeProcessInstance(context, pi);
            AutomatikoMessages.Header.Builder _header = AutomatikoMessages.Header.newBuilder();
            _header.setVersion(AutomatikoMessages.Version.newBuilder().setVersionMajor(1).setVersionMinor(0)
                    .setVersionRevision(0).build());
            PersisterHelper.writeStrategiesIndex(context, _header);

            String header = JsonFormat.printer().print(_header);

            context.close();

            // collect all information about timers
            Collection<io.automatiko.engine.workflow.process.instance.NodeInstance> nodes = ((WorkflowProcessInstanceImpl) pi)
                    .getNodeInstances(true);
            StringBuilder timers = new StringBuilder("[");
            for (io.automatiko.engine.workflow.process.instance.NodeInstance ni : nodes) {
                String timerId = null;
                if (ni instanceof TimerNodeInstance) {
                    timerId = ((TimerNodeInstance) ni).getTimerId();
                } else if (ni instanceof StateBasedNodeInstance) {
                    if (((StateBasedNodeInstance) ni).getTimerInstances() != null) {
                        ((StateBasedNodeInstance) ni).getTimerInstances().forEach(timer -> {
                            ZonedDateTime scheduledTime = context.processRuntime.getJobsService().getScheduledTime(timer);
                            timers.append("{\"timerId\":\"").append(timer).append("\",\"scheduledAt\":\"")
                                    .append(scheduledTime.toString()).append("\",\"nodeInstanceId\":\"")
                                    .append(ni.getId()).append("\"}");
                        });
                    }
                }

                if (timerId != null) {
                    ZonedDateTime scheduledTime = context.processRuntime.getJobsService().getScheduledTime(timerId);
                    timers.append("{\"timerId\":\"").append(timerId).append("\",\"scheduledAt\":\"")
                            .append(scheduledTime.toString()).append("\",\"nodeInstanceId\":\"")
                            .append(ni.getId()).append("\"}");

                }

                if (((NodeInstanceImpl) ni).getRetryJobId() != null && !((NodeInstanceImpl) ni).getRetryJobId().isEmpty()) {
                    ZonedDateTime scheduledTime = context.processRuntime.getJobsService()
                            .getScheduledTime(((NodeInstanceImpl) ni).getRetryJobId());
                    timers.append("{\"timerId\":\"").append(((NodeInstanceImpl) ni).getRetryJobId())
                            .append("\",\"scheduledAt\":\"")
                            .append(scheduledTime.toString()).append("\",\"nodeInstanceId\":\"")
                            .append(ni.getId()).append("\"}");
                }
            }
            timers.append("]");

            return StringExportedProcessInstance.of(header, JsonFormat.printer().print((MessageOrBuilder) result),
                    timers.toString(), null);
        } catch (Exception e) {
            throw new RuntimeException("Error while marshalling process instance", e);
        }
    }

    public WorkflowProcessInstance importWorkflowProcessInstance(String header, String data, List<Map<String, String>> timers,
            Process<?> process) {
        Map<String, io.automatiko.engine.api.definition.process.Process> processes = new HashMap<String, io.automatiko.engine.api.definition.process.Process>();
        io.automatiko.engine.api.definition.process.Process p = ((AbstractProcess<?>) process).process();
        processes.put(process.id(), p);// this can include version number in the id
        processes.put(p.getId(), p);// this is raw process id as defined in bpmn or so
        try (ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0])) {
            Map<String, Object> localEnv = new HashMap<String, Object>(env);
            localEnv.put("_import_", true);
            localEnv.put("_services_", ((AbstractProcess<?>) process).services());

            AutomatikoMessages.ProcessInstance.Builder builder = AutomatikoMessages.ProcessInstance.newBuilder();
            JsonFormat.parser().merge(data, builder);

            AutomatikoMessages.Header.Builder headerBuilder = AutomatikoMessages.Header.newBuilder();
            JsonFormat.parser().merge(header, headerBuilder);
            MarshallerReaderContext context = new MarshallerReaderContext(bais, null, processes, localEnv) {

                @Override
                protected void readStreamHeader() throws IOException, StreamCorruptedException {

                }

            };
            context.parameterObject = builder.build();
            PersisterHelper.loadStrategiesIndex(context, headerBuilder.build());

            io.automatiko.engine.workflow.marshalling.impl.ProcessInstanceMarshaller marshaller = ProcessMarshallerRegistry.INSTANCE
                    .getMarshaller(builder.getProcessType());

            WorkflowProcessInstance pi = (WorkflowProcessInstance) marshaller.readProcessInstance(context);

            context.close();

            return pi;
        } catch (Exception e) {
            throw new RuntimeException("Error while unmarshalling process instance", e);
        }
    }

    public WorkflowProcessInstance unmarshallWorkflowProcessInstance(byte[] data, Process<?> process) {
        Map<String, io.automatiko.engine.api.definition.process.Process> processes = new HashMap<String, io.automatiko.engine.api.definition.process.Process>();
        io.automatiko.engine.api.definition.process.Process p = ((AbstractProcess<?>) process).process();
        processes.put(process.id(), p);// this can include version number in the id
        processes.put(p.getId(), p);// this is raw process id as defined in bpmn or so
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            MarshallerReaderContext context = new MarshallerReaderContext(bais, null, processes, this.env);
            ObjectInputStream stream = context.stream;
            String processInstanceType = stream.readUTF();

            io.automatiko.engine.workflow.marshalling.impl.ProcessInstanceMarshaller marshaller = ProcessMarshallerRegistry.INSTANCE
                    .getMarshaller(processInstanceType);

            WorkflowProcessInstance pi = (WorkflowProcessInstance) marshaller.readProcessInstance(context);

            context.close();

            return pi;
        } catch (Exception e) {
            throw new RuntimeException("Error while unmarshalling process instance", e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ProcessInstance unmarshallProcessInstance(byte[] data, Process process, long versionracker) {
        WorkflowProcessInstance wpi = unmarshallWorkflowProcessInstance(data, process);
        Model model = ((AbstractProcess) process).createModel();

        model.fromMap(wpi.getVariables());
        if (wpi.getState() == ProcessInstance.STATE_ACTIVE || wpi.getState() == ProcessInstance.STATE_ERROR) {
            return ((AbstractProcess) process).createInstance(wpi, model, versionracker);
        } else {
            return ((AbstractProcess) process).createReadOnlyInstance(wpi, model);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ProcessInstance unmarshallReadOnlyProcessInstance(byte[] data, Process process) {
        WorkflowProcessInstance wpi = unmarshallWorkflowProcessInstance(data, process);
        Model model = ((AbstractProcess) process).createModel();

        model.fromMap(wpi.getVariables());
        return ((AbstractProcess) process).createReadOnlyInstance(wpi, model);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ProcessInstance importProcessInstance(ExportedProcessInstance<?> instance, Process process) {
        List<Map<String, String>> timers = instance.convertTimers();

        WorkflowProcessInstance wpi = importWorkflowProcessInstance((String) instance.getHeader(),
                (String) instance.getInstance(), timers, process);
        Model model = ((AbstractProcess) process).createModel();

        model.fromMap(wpi.getVariables());
        ProcessInstance processInstance = ((AbstractProcess) process).createInstance(wpi, model, 1);

        if (timers != null && !timers.isEmpty()) {
            String parentProcessInstanceId = wpi.getParentProcessInstanceId();
            if (parentProcessInstanceId != null && !parentProcessInstanceId.isEmpty()) {
                parentProcessInstanceId += ":";
            } else {
                parentProcessInstanceId = "";
            }
            JobsService jobService = ((WorkflowProcessInstanceImpl) wpi).getProcessRuntime().getJobsService();
            Collection<io.automatiko.engine.workflow.process.instance.NodeInstance> nodes = ((WorkflowProcessInstanceImpl) wpi)
                    .getNodeInstances(true);

            // keeps iterator for statebased node instance timers
            Map<String, Iterator<Timer>> nodeInstanceTimers = new LinkedHashMap<>();

            for (Map<String, String> timer : timers) {
                String nodeInstanceId = timer.get("nodeInstanceId");
                for (io.automatiko.engine.workflow.process.instance.NodeInstance ni : nodes) {
                    if (ni.getId().equals(nodeInstanceId)) {
                        ExpirationTime expirationTime = null;
                        long timerNodeId = 0;

                        if (((NodeInstanceImpl) ni).getRetryJobId() != null
                                && ((NodeInstanceImpl) ni).getRetryJobId().equals(timer.get("timerId"))) {

                            jobService
                                    .scheduleProcessInstanceJob(
                                            ProcessInstanceJobDescription.of(timer.get("timerId"), ni.getNodeId(),
                                                    "retry:" + ni.getId(),
                                                    expirationTime, ((NodeInstanceImpl) ni).getProcessInstanceIdWithParent(),
                                                    ni.getProcessInstance().getRootProcessInstanceId(),
                                                    ni.getProcessInstance().getProcessId(),
                                                    ni.getProcessInstance().getProcess().getVersion(),
                                                    ni.getProcessInstance().getRootProcessId()));
                            break;

                        } else if (ni instanceof TimerNodeInstance) {
                            TimerNodeInstance nodeInstance = (TimerNodeInstance) ni;
                            timerNodeId = nodeInstance.getTimerNode().getTimer().getId();
                            expirationTime = nodeInstance
                                    .createTimerInstance(nodeInstance.getTimerNode().getTimer());
                            expirationTime.reset(ZonedDateTime.parse(timer.get("scheduledAt")));
                        } else if (ni instanceof StateBasedNodeInstance) {

                            StateBasedNodeInstance nodeInstance = (StateBasedNodeInstance) ni;

                            if (nodeInstance.getTimerInstances().contains(timer.get("timerId"))) {

                                Iterator<Timer> it = nodeInstanceTimers.computeIfAbsent(nodeInstanceId,
                                        k -> nodeInstance.getEventBasedNode().getTimers().keySet().iterator());

                                expirationTime = nodeInstance.createTimerInstance(it.next());
                                expirationTime.reset(ZonedDateTime.parse(timer.get("scheduledAt")));
                            }
                        }

                        // lastly schedule timer on calculated expiration time                        
                        jobService.scheduleProcessInstanceJob(ProcessInstanceJobDescription.of(timer.get("timerId"),
                                timerNodeId, expirationTime,
                                ((NodeInstanceImpl) ni).getProcessInstanceIdWithParent(),
                                wpi.getRootProcessInstanceId(), wpi.getProcessId(),
                                wpi.getProcess().getVersion(), wpi.getRootProcessId()));
                        break;
                    }
                }
            }
        }

        return processInstance;
    }
}
