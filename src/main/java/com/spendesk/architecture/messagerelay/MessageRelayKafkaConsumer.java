//package com.spendesk.architecture.messagerelay;
//
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.Map;
//
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.common.TopicPartition;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
//import org.springframework.kafka.listener.ConsumerSeekAware;
//import org.springframework.kafka.support.KafkaHeaders;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.stereotype.Service;
//
//@Service
//public class MessageRelayKafkaConsumer implements ConsumerSeekAware {
//
//	private static final Logger LOG = LoggerFactory.getLogger(MessageRelayKafkaConsumer.class);
//
//	@Autowired
//	private MyDefaultKafkaConsumerFactory myDefaultKafkaConsumerFactory;
//
//	@Value("${spring.kafka.consumer.topic}")
//	private String topic;
//
//	@KafkaListener(topics = "#{kafkaTopicName}")
//	public void receive(@Payload byte[] data, @Header(KafkaHeaders.OFFSET) Long offset,
//			@Header(KafkaHeaders.RECEIVED_PARTITION_ID) Integer partitionId,
//			@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String messageKey, @Header("id") String idCustomHeader) {
//
//		LOG.info("- - - - - - - - - - - - - - -");
//		LOG.info("received message='{}'", data);
//		LOG.info("topic: {}", topic);
//		LOG.info("partition id: {}", partitionId);
//		LOG.info("offset: {}", offset);
//		LOG.info("message key: {}", messageKey);
//		LOG.info("custom header 'id': {}", idCustomHeader);
//	}
//
//	@Bean
//	public String kafkaTopicName() {
//		return this.topic;
//	}
//
//	@Override
//	public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
//
//		assignments.keySet().stream().sorted((o1, o2) -> o1.toString().compareTo(o2.toString()))
//				.forEach(topicPartition -> {
//					int partitionAssigned = topicPartition.partition();
//					String topicAssigned = topicPartition.topic();
//					LOG.info("Partition {} from topic {} has been assigned", partitionAssigned, topicAssigned);
//					LOG.info("Seeking to last commited offset");
//					callback.seekRelative(topic, partitionAssigned, -1, true);
//				});
//
//	}
//
//	@Override
//	public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
//		LOG.info("Following partition(s) has been revoked {}", partitions);
//	}
//
//	@Override
//	public void onIdleContainer(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
//		LOG.info("idle");
//		LOG.info(assignments.toString());
//		callback.
//	}
//
//	@Bean
//	public MyDefaultKafkaConsumerFactory<byte[], byte[]> consumerFactory() {
//		Map<String, Object> props = new HashMap<>();
//		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
//		props.put(ConsumerConfig.GROUP_ID_CONFIG, "client-messagerelay-groupid");
//		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
//				org.apache.kafka.common.serialization.ByteArrayDeserializer.class);
//		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
//				org.apache.kafka.common.serialization.ByteArrayDeserializer.class);
//		return new MyDefaultKafkaConsumerFactory<byte[], byte[]>(props);
//	}
//
//	@Bean
//	public ConcurrentKafkaListenerContainerFactory<byte[], byte[]> kafkaListenerContainerFactory() {
//		ConcurrentKafkaListenerContainerFactory<byte[], byte[]> factory = new ConcurrentKafkaListenerContainerFactory<byte[], byte[]>();
//		factory.getContainerProperties().setIdleEventInterval(100L);
//		factory.setConsumerFactory(this.consumerFactory());
//		return factory;
//	}
//
//}
