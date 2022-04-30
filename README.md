# OBD Metrics Demo

## About

`OBD Metrics Demo` is a simple project that demonstrates usage of the [OBD Metrics](https://github.com/tzebrowski/OBDMetrics "OBD Metrics")  java framework.



## Med17_5_5Test


```java

var connection = BluetoothConnection.openConnection();
var collector = new DataCollector();

final Pids pids = Pids
        .builder()
        .resource(Thread.currentThread().getContextClassLoader().getResource("extra.json"))
        .resource(Thread.currentThread().getContextClassLoader().getResource("mode01.json"))
        .resource(Thread.currentThread().getContextClassLoader().getResource("alfa.json")).build();

final Init init = Init.builder()
        .delay(1000)
        .header(Header.builder().mode("22").header("DA10F1").build())
        .header(Header.builder().mode("01").header("7DF").build())
        .protocol(Protocol.CAN_11)
        .sequence(DefaultCommandGroup.INIT).build();

final Workflow workflow = Workflow
        .instance()
        .observer(collector)
        .pids(pids)
        .initialize();

final Query query = Query.builder()
        .pid(13l) // Engine RPM
        .pid(12l) // Boost
        .pid(18l) // Throttle position
        .pid(14l) // Vehicle speed
        .pid(5l) //  Engine load
        .pid(7l)  // Short fuel trim
        .build();

int commandFrequency = 10;
final Adjustments optional = Adjustments
        .builder()
        .cacheConfig(
                CacheConfig.builder()
                        .storeResultCacheOnDisk(Boolean.TRUE)
                        .resultCacheFilePath("./result_cache.json")
                        .resultCacheEnabled(Boolean.TRUE).build())
        .adaptiveTiming(AdaptiveTimeoutPolicy
                .builder()
                .enabled(Boolean.TRUE)
                .checkInterval(2000)
                .commandFrequency(9)
                .build())
        .producerPolicy(ProducerPolicy.builder()
                .priorityQueueEnabled(Boolean.TRUE)
                .lowPriorityCommandFrequencyDelay(2000).build())
        .batchEnabled(true)
        .build();



workflow.start(connection, query, init, optional);


WorkflowFinalizer.finalizeAfter(workflow, 15000);

final PidDefinitionRegistry pidRegistry = workflow.getPidRegistry();
final PidDefinition rpm = pidRegistry.findBy(13l);
final Diagnostics diagnostics = workflow.getDiagnostics();
final Histogram rpmHist = diagnostics.histogram().findBy(rpm);
Assertions.assertThat(rpmHist.getMin()).isGreaterThan(500);


final double ratePerSec = diagnostics.rate().findBy(RateType.MEAN, rpm).get().getValue();

log.info("Rate:{}  ->  {}", rpm, ratePerSec);

Assertions.assertThat(ratePerSec).isGreaterThanOrEqualTo(commandFrequency);
```
