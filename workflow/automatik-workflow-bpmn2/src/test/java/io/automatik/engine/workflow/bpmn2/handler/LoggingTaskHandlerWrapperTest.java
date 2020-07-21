package io.automatik.engine.workflow.bpmn2.handler;//

//package org.jbpm.bpmn2.handler;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.jbpm.bpmn2.JbpmBpmn2TestCase;
//import org.jbpm.bpmn2.handler.LoggingTaskHandlerDecorator.InputParameter;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.Test;
//import org.kie.api.KieBase;
//
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//public class LoggingTaskHandlerWrapperTest extends JbpmBpmn2TestCase {
//    
//    @Test
//    public void testLimitExceptionInfoList() throws Exception {
//        KieBase kbase = createKnowledgeBase("BPMN2-ExceptionThrowingServiceProcess.bpmn2");
//        ksession = createKnowledgeSession(kbase);
//        
//        LoggingTaskHandlerDecorator loggingTaskHandlerWrapper = new LoggingTaskHandlerDecorator(ServiceTaskHandler.class, 2);
//        loggingTaskHandlerWrapper.setPrintStackTrace(false);
//        ksession.getWorkItemManager().registerWorkItemHandler("Service Task", loggingTaskHandlerWrapper);
//
//        Map<String, Object> params = new HashMap<String, Object>();
//        params.put("serviceInputItem", "exception message");
//        ksession.startProcess("ServiceProcess", params);
//        ksession.startProcess("ServiceProcess", params);
//        ksession.startProcess("ServiceProcess", params);
//
//        int size = loggingTaskHandlerWrapper.getWorkItemExceptionInfoList().size(); 
//        assertTrue( size == 2, "WorkItemExceptionInfoList is too large: " + size);
//    }
//    
//    @Test
//    public void testFormatLoggingError() throws Exception {
//        KieBase kbase = createKnowledgeBase("BPMN2-ExceptionThrowingServiceProcess.bpmn2");
//        ksession = createKnowledgeSession(kbase);
//        
//        LoggingTaskHandlerDecorator loggingTaskHandlerWrapper = new LoggingTaskHandlerDecorator(ServiceTaskHandler.class, 2);
//        loggingTaskHandlerWrapper.setLoggedMessageFormat("{0} - {1} - {2} - {3}");
//        List<InputParameter> inputParameters = new ArrayList<LoggingTaskHandlerDecorator.InputParameter>();
//        inputParameters.add(InputParameter.EXCEPTION_CLASS);
//        inputParameters.add(InputParameter.WORK_ITEM_ID);
//        inputParameters.add(InputParameter.WORK_ITEM_NAME);
//        inputParameters.add(InputParameter.PROCESS_INSTANCE_ID);
//        
//        loggingTaskHandlerWrapper.setLoggedMessageInput(inputParameters);
//        
//        loggingTaskHandlerWrapper.setPrintStackTrace(false);
//        ksession.getWorkItemManager().registerWorkItemHandler("Service Task", loggingTaskHandlerWrapper);
//
//        Map<String, Object> params = new HashMap<String, Object>();
//        params.put("serviceInputItem", "exception message");
//        ksession.startProcess("ServiceProcess", params);
//        ksession.startProcess("ServiceProcess", params);
//        ksession.startProcess("ServiceProcess", params);
//    }
//
//}
