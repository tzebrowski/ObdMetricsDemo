# OBD Metrics Demo

![CI](https://github.com/tzebrowski/ObdMetricsDemo/workflows/Build/badge.svg?branch=main)


## About


`OBD Metrics Demo` is a simple project that demonstrates usage of the [OBD Metrics](https://github.com/tzebrowski/OBDMetrics "OBD Metrics")  java framework.



## Giulia_2_0_GME_Test


```java

var connection = BluetoothConnection.openConnection();
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
        .adaptiveTiming(AdaptiveTimeoutPolicy
                .builder()
                .enabled(Boolean.TRUE)
                .checkInterval(5000)
                .commandFrequency(commandFrequency)
                .build())
        .producerPolicy(ProducerPolicy.builder()
                .priorityQueueEnabled(Boolean.TRUE)
                .build())
        .cacheConfig(CacheConfig.builder().resultCacheEnabled(Boolean.FALSE).build())
        .batchEnabled(Boolean.FALSE)
        .build();

final Init init = Init.builder()
        .delay(1000)
        .header(Header.builder().mode("22").header("DA10F1").build())
		.header(Header.builder().mode("01").header("DB33F1").build())
        .protocol(Protocol.CAN_29)
        .fetchDeviceProperties(Boolean.TRUE)
        .fetchSupportedPids(Boolean.TRUE)	
        .sequence(DefaultCommandGroup.INIT).build();

workflow.start(connection, query, init, optional);

WorkflowFinalizer.finalizeAfter500ms(workflow);

final PidDefinitionRegistry rpm = workflow.getPidRegistry();

PidDefinition measuredPID = rpm.findBy(13l);
double ratePerSec = workflow.getDiagnostics().rate().findBy(RateType.MEAN, measuredPID).get().getValue();

log.info("Rate:{}  ->  {}", measuredPID, ratePerSec);

Assertions.assertThat(ratePerSec).isGreaterThanOrEqualTo(commandFrequency);
```
