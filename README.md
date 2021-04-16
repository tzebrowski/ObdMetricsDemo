# OBD Metrics Demo

## About

`OBD Metrics Demo` is a simple project that demonstrates usage of the [OBD Metrics](https://github.com/tzebrowski/OBDMetrics "OBD Metrics")  java framework.



## Med17_5_5Test


```java

var connection = BluetoothConnection.openConnection();
var collector = new DataCollector();

int commandFrequency = 6;
var workflow = WorkflowFactory
        .mode1()
        .pidSpec(PidSpec
                .builder()
                .initSequence(Mode1CommandGroup.INIT)
                .pidFile(Thread.currentThread().getContextClassLoader().getResource("mode01.json")).build())
        .observer(collector)
        .initialize();

var query = Query.builder()
        .pid(6l) // Engine coolant temperature
        .pid(12l) // Intake manifold absolute pressure
        .pid(13l) // Engine RPM
        .pid(16l) // Intake air temperature
        .pid(18l) // Throttle position
        .pid(14l) // Vehicle speed
        .pid(15l) // Timing advance
        .build();

var optional = Adjustments
        .builder()
        .adaptiveTiming(AdaptiveTimeoutPolicy
                .builder()
                .enabled(Boolean.TRUE)
                .checkInterval(5000)
                .commandFrequency(commandFrequency)
                .build())
        .producerPolicy(ProducerPolicy.builder()
                .priorityQueueEnabled(Boolean.TRUE)
                .lowPriorityCommandFrequencyDelay(2000).build())
        .batchEnabled(true)
        .build();

workflow.start(connection, query, optional);

WorkflowFinalizer.finalizeAfter(workflow, 15000);

var rpm = workflow.getPidRegistry().findBy(13l);
var ratePerSec = workflow.getStatisticsRegistry().getRatePerSec(rpm);

log.info("Rate:{}  ->  {}", rpm, ratePerSec);

Assertions.assertThat(ratePerSec).isGreaterThanOrEqualTo(commandFrequency);
```
