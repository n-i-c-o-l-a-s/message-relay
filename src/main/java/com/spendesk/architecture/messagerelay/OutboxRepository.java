package com.spendesk.architecture.messagerelay;

import org.springframework.data.repository.CrudRepository;

public interface OutboxRepository extends CrudRepository<Outbox, Long> {

}
