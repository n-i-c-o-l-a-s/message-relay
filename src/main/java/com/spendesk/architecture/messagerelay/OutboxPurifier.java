package com.spendesk.architecture.messagerelay;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPurifier {

	private static final Logger LOG = LoggerFactory.getLogger(OutboxPurifier.class);

	@Value("${messagerelay.db.purify.delay.in.ms}")
	private String delayBetweenDatabasePurifyAsString;

	@Bean
	public String delayBetweenDatabasePurifies() {
		return this.delayBetweenDatabasePurifyAsString;
	}

	@Autowired
	protected OutboxRepository repository;

	@Autowired
	private OutboxPoller outboxPoller;

	private Long lastOutboxIdDeleted = -1L;

	@Scheduled(fixedDelayString = "#{delayBetweenDatabasePurifies}")
	@Transactional
	public void deleteProcessedLinesFromDatabase() {
		Long greatestIdToDelete = this.outboxPoller.nextIdToPollFromDatabase() - 1;

		if (this.lastOutboxIdDeleted == greatestIdToDelete) {
			LOG.info("No line to delete from 'outbox' table since no line has been recently relayed to Kafka");
		} else {
			LOG.info("Some lines from 'outbox' table, and successfully relayed to Kafka, will be deleted",
					greatestIdToDelete);
			LOG.info("Preparing to delete 'outbox' lines where 'id' is less than or equal to {}", greatestIdToDelete);
			this.repository.deleteByIdLessThanEqual(greatestIdToDelete);
			LOG.info("'outbox' table purified successfully");
			this.lastOutboxIdDeleted = greatestIdToDelete;
		}

	}

}
