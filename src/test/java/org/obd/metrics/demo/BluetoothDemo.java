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
import org.obd.metrics.api.model.Init.Header;
import org.obd.metrics.api.model.Init.Protocol;
import org.obd.metrics.api.model.Pids;
import org.obd.metrics.api.model.ProducerPolicy;
import org.obd.metrics.api.model.Query;
import org.obd.metrics.command.group.DefaultCommandGroup;
import org.obd.metrics.diagnostic.RateType;
import org.obd.metrics.test.WorkflowFinalizer;

public class BluetoothDemo {
	
	@Test
	public void test() throws IOException, InterruptedException, ExecutionException {
		var connection = BluetoothConnection.openConnection("OBDII");
		var collector = new DataCollector();

		final Pids pids = Pids
		        .builder()
		        .resource(Thread.currentThread().getContextClassLoader().getResource("giulia_2.0_gme.json"))
		        .resource(Thread.currentThread().getContextClassLoader().getResource("extra.json"))
		        .resource(Thread.currentThread().getContextClassLoader().getResource("mode01.json"))
				.build();
		
		int commandFrequency = 6;
		var workflow = Workflow
		        .instance()
		        .pids(pids)
		        .observer(collector)
		        .initialize();

		var query = Query.builder()
				.pid(7005l) //Intake Pressure
				.pid(7006l) 
		        .pid(7007l) 
		        .pid(7008l) 
				.build();

		var optional = Adjustments
				.builder()
				.vehicleCapabilitiesReadingEnabled(Boolean.TRUE)
		        .vehicleMetadataReadingEnabled(Boolean.TRUE)
				.adaptiveTimeoutPolicy(AdaptiveTimeoutPolicy
		                .builder()
		                .enabled(Boolean.TRUE)
		                .checkInterval(5000)
		                .commandFrequency(commandFrequency)
		                .build())
		        .producerPolicy(ProducerPolicy.builder()
		                .priorityQueueEnabled(Boolean.TRUE)
		                .build())
		        .cachePolicy(CachePolicy.builder().resultCacheEnabled(Boolean.FALSE).build())
		        .batchPolicy(
		        		BatchPolicy
		        		.builder()
		        		.responseLengthEnabled(Boolean.FALSE)
		        		.enabled(Boolean.FALSE).build())
		        .build();

		var init = Init.builder()
		        .delayAfterInit(1000)
		        .header(Header.builder().mode("22").header("DA10F1").build())
				.header(Header.builder().mode("01").header("DB33F1").build())
		        .protocol(Protocol.CAN_29)
		        .sequence(DefaultCommandGroup.INIT).build();
		
		workflow.start(connection, query, init, optional);

		WorkflowFinalizer.finalizeAfter(workflow,25000);

		var registry = workflow.getPidRegistry();

		var intakePressure = registry.findBy(7005l);
		double ratePerSec = workflow.getDiagnostics().rate().findBy(RateType.MEAN, intakePressure).get().getValue();

		Assertions.assertThat(ratePerSec).isGreaterThanOrEqualTo(commandFrequency);
	}
}
