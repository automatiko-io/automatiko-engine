//package org.acme.ibmmq;
//
//import jakarta.enterprise.context.ApplicationScoped;
//import jakarta.enterprise.inject.Produces;
//
//import com.ibm.msg.client.jms.JmsConnectionFactory;
//import com.ibm.msg.client.jms.JmsFactoryFactory;
//import com.ibm.msg.client.wmq.WMQConstants;
//
///**
// * Start IBM MQ as docker container with following command
// * docker run --rm -e LICENSE=accept -e MQ_QMGR_NAME=QM1 -e MQ_APP_PASSWORD=passw0rd -p 1414:1414 -p 9443:9443 -d ibmcom/mq
// * 
// * IBM MQ Admin Console can be found at https://localhost:9443
// * login with: admin:passw0rd
// */
//@ApplicationScoped
//public class ConnectionFactory {
//
//    @Produces
//    javax.jms.ConnectionFactory factory() throws Exception {
//
//        JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
//        JmsConnectionFactory cf = ff.createConnectionFactory();
//
//        // Set the properties
//        cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, "localhost");
//        cf.setIntProperty(WMQConstants.WMQ_PORT, 1414);
//        cf.setStringProperty(WMQConstants.WMQ_CHANNEL, "DEV.APP.SVRCONN");
//        cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
//        cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, "QM1");
//        cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "automatiko");
//        cf.setStringProperty(WMQConstants.USERID, "app");
//        cf.setStringProperty(WMQConstants.PASSWORD, "passw0rd");
//
//        return cf;
//    }
//
//}
