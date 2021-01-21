# Automatik with MQTT

## Description

A quickstart project that deals with house climate (temperature and humidity) processing. It illustrates
how easy it is to make a process to work with MQTT Broker and by that IoT sensors that can publish data into the broker.

This example shows

* consuming events from a MQTT topic
* each process instance is expecting a measurement information in JSON format
* events are correlated based on location - property of the measurement event and then handled all events for the same correlation within one instance
* expiration logic is also configured to allow restart of the measurements automatically


<p align="center"><img width=75% height=50% src="docs/images/process.png"></p>

## Infrastructure requirements

This quickstart requires an MQTT to be available and by default expects it to be on default port and localhost.

In addition to that two topics are needed

* home/temperature
* home/humidity


## Build and run

### Prerequisites

You will need:
  - Java 11+ installed
  - Environment variable JAVA_HOME set accordingly
  - Maven 3.6+ installed

When using native image compilation, you will also need:
  - GraalVM 20.1+ installed
  - Environment variable GRAALVM_HOME set accordingly
  - Note that GraalVM native image compilation typically requires other packages (glibc-devel, zlib-devel and gcc) to be installed too, please refer to GraalVM installation documentation for more details.

### Compile and Run in Local Dev Mode

```
mvn clean quarkus:dev    
```

NOTE: With dev mode of Quarkus you can take advantage of hot reload for business assets like processes, decisions and java code. No need to redeploy or restart your running application.


### Compile and Run using Local Native Image
Note that this requires GRAALVM_HOME to point to a valid GraalVM installation

```
mvn clean package -Pnative
```

To run the generated native executable, generated in `target/`, execute

```
./target/automatik-mqtt-{version}-runner
```

### Use the application

To make use of this application it is as simple as putting a message on `home/temperature` topic with following content

```
{
  "timestamp":1, 
  "value" : 18.0, 
  "location":"livingroom"
}

```

this will then trigger new instance of the measument which will be active for 2 minutes. Put another message on `home/humidity` topic with following content

```
{
  "timestamp":1, 
  "value" : 78.0, 
  "location":"livingroom"
}
```
within 2 minutes from the first message and the humidity data will be added to already active instance for the living room

After two minutes of putting the first message (either to temperature or humidity) topic given process instance will end. Another message received after that time will start new instance and so on...

You can also put measurements for any other room/location in the house by setting its name in the message payload, following are examples of doing that for kitchen:

```
{
  "timestamp":1, 
  "value" : 18.0, 
  "location":"kitchen"
}

```

putting above message on `home/temperature` topic will start new instance dedicated for handling measurements for kitchen.