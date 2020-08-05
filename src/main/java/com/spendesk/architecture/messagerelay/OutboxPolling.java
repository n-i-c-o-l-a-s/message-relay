package com.spendesk.architecture.messagerelay;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPolling {

	private static final Logger LOG = LoggerFactory.getLogger(OutboxPolling.class);

	@Value("${messagerelay.kafka.bootstrapservers}")
	private String kafkaBootstrapServers;

	@Value("${messagerelay.kafka.groupid}")
	private String groupId;

	@Value("${messagerelay.kafka.topic}")
	private String topic;

	@Value("${messagerelay.kafka.auto-offset-reset}")
	private String autoOffetReset;

	@Value("${messagerelay.db.poll.delay.in.ms}")
	private String delayBetweenDatabasePollsAsString;

	@Autowired
	protected OutboxRepository repository;

	@Autowired
	private KafkaTemplate<byte[], byte[]> messageProducer;

	private boolean isFirstRunScheduled = true;
	private Long nextIdToPollFromDatabase = 1L;

	@Bean
	public String delayBetweenDatabasePolls() {
		return this.delayBetweenDatabasePollsAsString;
	}

	@Scheduled(fixedDelayString = "#{delayBetweenDatabasePolls}")
	public void pollOutboxTableFromDatabase() {

		if (isFirstRunScheduled) {
			this.nextIdToPollFromDatabase = this.findTheLastCommittedOffsetFromToKafkaTopic();
			isFirstRunScheduled = false;
		}

		Optional<Outbox> optionalOutbox = repository.findById(nextIdToPollFromDatabase);

		if (optionalOutbox.isPresent()) {
			Outbox outbox = optionalOutbox.get();
			LOG.info("{} has been polled", outbox.toString());
			this.relayMessageToKafkaTopic(outbox.getId(), outbox.getKey(), outbox.getValue());
			nextIdToPollFromDatabase++;
		}
	}

	protected Long findTheLastCommittedOffsetFromToKafkaTopic() {

		Long nextIdToPollFromDatabase = 0L;
		LOG.info("Finding the last commited offset from Kafka topic '{}'", this.topic);

		final Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaBootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, this.groupId);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, this.autoOffetReset);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
				org.apache.kafka.common.serialization.ByteArrayDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
				org.apache.kafka.common.serialization.ByteArrayDeserializer.class);

		final Consumer<byte[], byte[]> consumer = new KafkaConsumer<>(props);
		consumer.subscribe(Arrays.asList(new String[] { topic }), new ConsumerRebalanceListener() {

			@Override
			public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
				LOG.info("Following partition(s) has been revoked {}", partitions);
			}

			@Override
			public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
				LOG.info("Following partition(s) has been assigned {}", partitions);
			}
		});

		ConsumerRecords<byte[], byte[]> consumerRecords = consumer.poll(Duration.ofMillis(1000));

		consumer.assignment().stream().sorted((o1, o2) -> o1.toString().compareTo(o2.toString()))
				.forEach(topicPartition -> {

					long nextOffsetToFetch = consumer.position(topicPartition);
					LOG.info("Next offset to fetch in partition {} would be {}", topicPartition.partition(),
							nextOffsetToFetch);

					if (nextOffsetToFetch > 0) {
						LOG.info(
								"Inspecting the offset before last ({}) of partition {} in order to extract 'outbox_id' header",
								nextOffsetToFetch - 1, topicPartition.partition());
						consumer.seek(topicPartition, nextOffsetToFetch - 1);
					} else {
						LOG.info("Partition {} is considered empty", topicPartition.partition());
					}
				});

		consumerRecords = consumer.poll(Duration.ofMillis(1000));

		if (consumerRecords.count() != 0) {

			for (ConsumerRecord<byte[], byte[]> consumerRecord : consumerRecords) {
				LOG.info("Record found: topic='{}', partition='{}', offset='{}' ", consumerRecord.topic(),
						consumerRecord.partition(), consumerRecord.offset());
				Long idExtractedFromKafkaRecordHeaders = Long.valueOf(consumerRecord.headers() == null ? "0"
						: new String(consumerRecord.headers().headers("id").iterator().next().value()));
				LOG.info("Extracted 'id' header: {}", idExtractedFromKafkaRecordHeaders);
				if (idExtractedFromKafkaRecordHeaders > nextIdToPollFromDatabase) {
					nextIdToPollFromDatabase = idExtractedFromKafkaRecordHeaders;
				}
			}

			consumer.close();

		} else {
			LOG.info("No record found in topic {}", this.topic);
		}

		nextIdToPollFromDatabase++;
		LOG.info("Next id to poll from database is {}", nextIdToPollFromDatabase);
		return nextIdToPollFromDatabase;
	}

	protected void relayMessageToKafkaTopic(Long id, byte[] key, byte[] value) {
		List<Header> headers = new ArrayList<>();
		headers.add(new RecordHeader("outbox_id", id.toString().getBytes()));
		this.messageProducer.send(new ProducerRecord<byte[], byte[]>(this.topic, null, null, key, value, headers));
		LOG.info("id {} has been successfully relayed to topic {}", id, this.topic);
	}

}
