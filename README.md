# OBD Metrics Demo

![CI](https://github.com/tzebrowski/ObdMetricsDemo/workflows/Build/badge.svg?branch=main)


## About


`OBD Metrics Demo` is a simple project that demonstrates usage of the [OBD Metrics](https://github.com/tzebrowski/OBDMetrics "OBD Metrics")  java framework.



## Tcp connector demo
Full example can be found [example](https://github.com/tzebrowski/ObdMetricsDemo/blob/main/src/test/java/org/obd/metrics/demo/TcpDemo.java "example")  


```java
var connection = TcpAdapterConnection.of("192.168.0.10", 35000);
var collector = new DataCollector();

int commandFrequency = 6;
final Workflow workflow = Workflow
        .instance()
        .pids(Pids.DEFAULT)
        .observer(collector)
        .initialize();

final Query query = Query.builder()
        .pid(13l) // Engine RPM
        .pid(16l) // Intake air temperature
        .pid(18l) // Throttle position
        .pid(14l) // Vehicle speed
        .build();

final Adjustments optional = Adjustments
        .builder()
        .adaptiveTiming(AdaptiveTimeoutPolicy
                .builder()
                .enabled(Boolean.TRUE)
                .checkInterval(1)
                .commandFrequency(commandFrequency)
                .build())
        .producerPolicy(ProducerPolicy.builder()
                .priorityQueueEnabled(Boolean.TRUE)
                .build())
        .cacheConfig(CachePolicy.builder().resultCacheEnabled(false).build())
        .batchEnabled(true)
        .build();

workflow.start(connection, query, Init.DEFAULT, optional);

WorkflowFinalizer.finalizeAfter(workflow, 25000);

final PidDefinitionRegistry rpm = workflow.getPidRegistry();

PidDefinition measuredPID = rpm.findBy(13l);
double ratePerSec = workflow.getDiagnostics().rate().findBy(RateType.MEAN, measuredPID).get().getValue();

Assertions.assertThat(ratePerSec).isGreaterThanOrEqualTo(commandFrequency);

```

## Bluetooth connector demo
Full example can be found [example](https://github.com/tzebrowski/ObdMetricsDemo/blob/main/src/test/java/org/obd/metrics/demo/BluetoothDemo.java "example")  



```java

var connection = BluetoothConnection.openConnection("OBDII");
var collector = new DataCollector();

final Pids pids = Pids
        .builder()
        .resource(Thread.currentThread().getContextClassLoader().getResource("giulia_2.0_gme.json"))
        .resource(Thread.currentThread().getContextClassLoader().getResource("extra.json"))
        .resource(Thread.currentThread().getContextClassLoader().getResource("mode01.json"))
		.build();

int commandFrequency = 6;
final Workflow workflow = Workflow
        .instance()
        .pids(pids)
        .observer(collector)
        .initialize();

final Query query = Query.builder()
		.pid(7005l) 
		.pid(7006l) 
	      	.pid(7007l) 
	      	.pid(7008l) 
		.build();

final Adjustments optional = Adjustments
        .builder()
        .vehicleCapabilitiesReadingEnabled(Boolean.TRUE)
        .vehicleMetadataReadingEnabled(Boolean.TRUE)
        .adaptiveTiming(AdaptiveTimeoutPolicy
        	.builder()
        	.enabled(Boolean.TRUE)
        	.checkInterval(5000)
        	.commandFrequency(commandFrequency)
        	.build())
        .producerPolicy(ProducerPolicy.builder()
            .priorityQueueEnabled(Boolean.TRUE)
            .build())
        .cacheConfig(CachePolicy.builder().resultCacheEnabled(Boolean.FALSE).build())
        .batchEnabled(Boolean.FALSE)
        .build();

final Init init = Init.builder()
        .delay(1000)
        .header(Header.builder().mode("22").header("DA10F1").build())
	 .header(Header.builder().mode("01").header("DB33F1").build())
        .protocol(Protocol.CAN_29)
        .sequence(DefaultCommandGroup.INIT).build();

workflow.start(connection, query, init, optional);

WorkflowFinalizer.finalizeAfter500ms(workflow);

final PidDefinitionRegistry rpm = workflow.getPidRegistry();

PidDefinition measuredPID = rpm.findBy(13l);
double ratePerSec = workflow.getDiagnostics().rate().findBy(RateType.MEAN, measuredPID).get().getValue();

log.info("Rate:{}  ->  {}", measuredPID, ratePerSec);

Assertions.assertThat(ratePerSec).isGreaterThanOrEqualTo(commandFrequency);
```
