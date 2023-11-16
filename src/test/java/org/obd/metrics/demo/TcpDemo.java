package org.obd.metrics.demo;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.obd.metrics.api.Workflow;
import org.obd.metrics.api.model.AdaptiveTimeoutPolicy;
import org.obd.metrics.api.model.Adjustments;
import org.obd.metrics.api.model.BatchPolicy;
import org.obd.metrics.api.model.CachePolicy;
import org.obd.metrics.api.model.Init;
import org.obd.metrics.api.model.Pids;
import org.obd.metrics.api.model.ProducerPolicy;
import org.obd.metrics.api.model.Query;
import org.obd.metrics.diagnostic.RateType;
import org.obd.metrics.transport.TcpAdapterConnection;

public class TcpDemo {
	
	@Test
	public void test() throws IOException, InterruptedException, ExecutionException {
		var connection = TcpAdapterConnection.of("192.168.0.10", 35000);
		var collector = new DataCollector();

		int commandFrequency = 6;
		var workflow = Workflow
		        .instance()
		        .pids(Pids.DEFAULT)
		        .observer(collector)
		        .initialize();

		var query = Query.builder()
		        .pid(13l) // Engine RPM
		        .pid(16l) // Intake air temperature
		        .pid(18l) // Throttle position
		        .pid(14l) // Vehicle speed
		        .build();

		var optional = Adjustments
		        .builder()
		        .adaptiveTimeoutPolicy(AdaptiveTimeoutPolicy
		                .builder()
		                .enabled(Boolean.TRUE)
		                .checkInterval(1)
		                .commandFrequency(commandFrequency)
		                .build())
		        .producerPolicy(ProducerPolicy.builder()
		                .priorityQueueEnabled(Boolean.TRUE)
		                .build())
		        .cachePolicy(CachePolicy.builder().resultCacheEnabled(false).build())
		        .batchPolicy(
		        		BatchPolicy
		        		.builder()
		        		.responseLengthEnabled(Boolean.FALSE)
		        		.enabled(Boolean.TRUE).build())
		        .build();

		workflow.start(connection, query, Init.DEFAULT, optional);

		WorkflowFinalizer.finalizeAfter(workflow, 25000);

		var registry = workflow.getPidRegistry();

		var rpm = registry.findBy(13l);
		double ratePerSec = workflow.getDiagnostics().rate().findBy(RateType.MEAN, rpm).get().getValue();

		Assertions.assertThat(ratePerSec).isGreaterThanOrEqualTo(commandFrequency);
	}
}
