<div align="center">
  <img src="https://github.com/sultan99/react-on-lambda/raw/gh-pages/logo.svg?sanitize=true" width="110" height="110"/>
</div>

<img src="/doc/logo.svg" width="10%" />

# MESSAGE-RELAY


## Présentation générale
> *messagerelay* est une application impliquée dans l'implémentation du Pattern d'architecture nommé *[Transactional Outbox](https://microservices.io/patterns/data/transactional-outbox.html)*. *messagerelay* est chargée de lire (poll) les lignes de la table *outbox* et de le relayer dans un topic Kafka. Ces lignes ont été précedemment insérées par les applications (microservices) dans la meme transaction de base-de-données qui a permis de changer et muter leur état.


## Stack technologique exploitée
>- Spring Boot,
>- Spring Kafka, pour la consommation et la production de messages Kafka, 
>- PostgreSQL, pour la base-de-données hébergeant l'état du microservice,
>- Kafka pour le Message-Broker,
>- PMD, Findbug, CheckStyle, Jacoco, pour la qualimétrie,
>- Maven 3.6.3, pour le *build* (3.5.0 minimum requis),
>- Docker, pour la conteneurisation.


## Capacités générales

### Au *startup* de l'application, *messagerelay*
>- se connecte au cluster Kafka puis souscrit, en tant que consommateur, au topic de destination,
>- se positionne, pour chaque partition du topic, sur le dernier *offset* commité,
>- extrait le message localisé à cette *offset* pour en extraire le header nommé *outbox_id* et enfin y ajouter +1,
>- considère cette valeur comme le prochain *outbox_id* à lire depuis la base-de-données.

---
### Toutes les N millisecondes, *messagerelay*

>-  lit la ligne de base-de-donnéee dont le champ *id* est le prochain *outbox_id* à relayer vers le topic Kafka,
>- crée un nouveau message à partir de ces valeurs récupérées, 
>- y ajoute un header nommé *outobx_id*,
>- envoie ce message dans le topic.


### Paramétrages
```properties
# below, properties related to messagerelay as kafka consumer
messagerelay.kafka.bootstrapservers=localhost:9092
messagerelay.kafka.groupid=client-messagerelay-groupid
messagerelay.kafka.topic=client-topic-env-local
messagerelay.kafka.auto-offset-reset=earliest
# properties related to messagerelay as kafka producer
spring.kafka.producer.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.ByteArraySerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.ByteArraySerializer
# below, properties related to message database connection
messagerelay.db.poll.delay.in.ms=10
spring.datasource.hikari.connectionTimeout=10000
spring.datasource.hikari.maximumPoolSize=5
spring.datasource.url=jdbc:postgresql://localhost:5432/admin
spring.datasource.username=messagerelay
spring.datasource.password=01EEJMXE4VXFNBGV86VVJS8DV3
spring.jpa.generate-ddl=false
spring.jpa.hibernate.ddl-auto=none
```

### Quelques détails

>- *messagerelay* considère la clé et la valeur du message à relayer comme des tableaux de bytes (byte []), lui permettant ainsi de pouvoir relayer tous formats de message (JSON, AVRO, XML, etc.),

---
## Execution sur poste local sans Docker
>- Les variables d'environnement suivantes sont nécessaires ```KAFKA_BOOTSTRAP_SERVER=localhost:9092``` et ```KAFKA_SCHEMA_REGISTRY_SERVER=http://localhost:8081```,
>- L'application est ensuite construite et packagée dans un JAR via la commande ```mvn clean package```,
>- Puis, après avoir démarré Zookeeper et Kafka sur votre poste local, l'application se lance via la ligne de commande ```java -Duser.timezone=UTC -jar target/pds-1.5.0-SNAPSHOT.jar```.

---
## Execution sur poste local avec Docker
>- ```docker build --build-arg VERSION=1.5.0-SNAPSHOT -t pds .```
>- ```docker run -e 'KAFKA_BOOTSTRAP_SERVER=localhost:9092' -e 'KAFKA_SCHEMA_REGISTRY_SERVER=http://localhost:8081' pds```


## Traces applicatives au démarrage
```properties

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.3.2.RELEASE)

2020-08-05 21:24:08.672  INFO 39720 --- [           main] c.s.a.m.MessageRelayApplication          : Starting MessageRelayApplication on LAPTOP-4CN8F2PR with PID 39720 (C:\dev\source\message-relay\target\classes started by nicol in C:\dev\source\message-relay)
2020-08-05 21:24:08.677  INFO 39720 --- [           main] c.s.a.m.MessageRelayApplication          : No active profile set, falling back to default profiles: default
2020-08-05 21:24:09.578  INFO 39720 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFERRED mode.
2020-08-05 21:24:09.675  INFO 39720 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 80ms. Found 1 JPA repository interfaces.
2020-08-05 21:24:10.232  INFO 39720 --- [           main] o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService 'applicationTaskExecutor'
2020-08-05 21:24:10.243  INFO 39720 --- [           main] o.s.s.c.ThreadPoolTaskScheduler          : Initializing ExecutorService 'taskScheduler'
2020-08-05 21:24:10.300  INFO 39720 --- [         task-1] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2020-08-05 21:24:10.382  INFO 39720 --- [         task-1] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 5.4.18.Final
2020-08-05 21:24:10.608  INFO 39720 --- [           main] DeferredRepositoryInitializationListener : Triggering deferred initialization of Spring Data repositories…
2020-08-05 21:24:10.683  INFO 39720 --- [         task-1] o.hibernate.annotations.common.Version   : HCANN000001: Hibernate Commons Annotations {5.1.0.Final}
2020-08-05 21:24:11.145  INFO 39720 --- [         task-1] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2020-08-05 21:24:11.280  INFO 39720 --- [         task-1] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2020-08-05 21:24:11.324  INFO 39720 --- [         task-1] org.hibernate.dialect.Dialect            : HHH000400: Using dialect: org.hibernate.dialect.PostgreSQL10Dialect
2020-08-05 21:24:12.089  INFO 39720 --- [         task-1] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000490: Using JtaPlatform implementation: [org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform]
2020-08-05 21:24:12.099  INFO 39720 --- [         task-1] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2020-08-05 21:24:12.339  INFO 39720 --- [           main] DeferredRepositoryInitializationListener : Spring Data repositories initialized!
2020-08-05 21:24:12.347  INFO 39720 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Finding the last commited offset from Kafka topic 'entity-X'
2020-08-05 21:24:12.350  INFO 39720 --- [           main] c.s.a.m.MessageRelayApplication          : Started MessageRelayApplication in 4.304 seconds (JVM running for 5.074)
2020-08-05 21:24:12.377  INFO 39720 --- [   scheduling-1] o.a.k.clients.consumer.ConsumerConfig    : ConsumerConfig values: 
	allow.auto.create.topics = true
	auto.commit.interval.ms = 5000
	auto.offset.reset = earliest
	bootstrap.servers = [localhost:9092]
	check.crcs = true
	client.dns.lookup = default
	client.id = 
	client.rack = 
	connections.max.idle.ms = 540000
	default.api.timeout.ms = 60000
	enable.auto.commit = true
	exclude.internal.topics = true
	fetch.max.bytes = 52428800
	fetch.max.wait.ms = 500
	fetch.min.bytes = 1
	group.id = entity-X-messagerelay
	group.instance.id = null
	heartbeat.interval.ms = 1000
	interceptor.classes = []
	internal.leave.group.on.close = true
	isolation.level = read_uncommitted
	key.deserializer = class org.apache.kafka.common.serialization.ByteArrayDeserializer
	max.partition.fetch.bytes = 1048576
	max.poll.interval.ms = 300000
	max.poll.records = 500
	metadata.max.age.ms = 300000
	metric.reporters = []
	metrics.num.samples = 2
	metrics.recording.level = INFO
	metrics.sample.window.ms = 30000
	partition.assignment.strategy = [class org.apache.kafka.clients.consumer.RangeAssignor]
	receive.buffer.bytes = 65536
	reconnect.backoff.max.ms = 1000
	reconnect.backoff.ms = 50
	request.timeout.ms = 30000
	retry.backoff.ms = 100
	sasl.client.callback.handler.class = null
	sasl.jaas.config = null
	sasl.kerberos.kinit.cmd = /usr/bin/kinit
	sasl.kerberos.min.time.before.relogin = 60000
	sasl.kerberos.service.name = null
	sasl.kerberos.ticket.renew.jitter = 0.05
	sasl.kerberos.ticket.renew.window.factor = 0.8
	sasl.login.callback.handler.class = null
	sasl.login.class = null
	sasl.login.refresh.buffer.seconds = 300
	sasl.login.refresh.min.period.seconds = 60
	sasl.login.refresh.window.factor = 0.8
	sasl.login.refresh.window.jitter = 0.05
	sasl.mechanism = GSSAPI
	security.protocol = PLAINTEXT
	security.providers = null
	send.buffer.bytes = 131072
	session.timeout.ms = 10000
	ssl.cipher.suites = null
	ssl.enabled.protocols = [TLSv1.2]
	ssl.endpoint.identification.algorithm = https
	ssl.key.password = null
	ssl.keymanager.algorithm = SunX509
	ssl.keystore.location = null
	ssl.keystore.password = null
	ssl.keystore.type = JKS
	ssl.protocol = TLSv1.2
	ssl.provider = null
	ssl.secure.random.implementation = null
	ssl.trustmanager.algorithm = PKIX
	ssl.truststore.location = null
	ssl.truststore.password = null
	ssl.truststore.type = JKS
	value.deserializer = class org.apache.kafka.common.serialization.ByteArrayDeserializer

2020-08-05 21:24:12.663  INFO 39720 --- [   scheduling-1] o.a.kafka.common.utils.AppInfoParser     : Kafka version: 2.5.0
2020-08-05 21:24:12.663  INFO 39720 --- [   scheduling-1] o.a.kafka.common.utils.AppInfoParser     : Kafka commitId: 66563e712b0b9f84
2020-08-05 21:24:12.663  INFO 39720 --- [   scheduling-1] o.a.kafka.common.utils.AppInfoParser     : Kafka startTimeMs: 1596655452662
2020-08-05 21:24:12.666  INFO 39720 --- [   scheduling-1] o.a.k.clients.consumer.KafkaConsumer     : [Consumer clientId=consumer-entity-X-messagerelay-1, groupId=entity-X-messagerelay] Subscribed to topic(s): entity-X
2020-08-05 21:24:12.906  INFO 39720 --- [   scheduling-1] org.apache.kafka.clients.Metadata        : [Consumer clientId=consumer-entity-X-messagerelay-1, groupId=entity-X-messagerelay] Cluster ID: rR4uGeZAT-ygUIYZKjIQ0g
2020-08-05 21:24:12.907  INFO 39720 --- [   scheduling-1] o.a.k.c.c.internals.AbstractCoordinator  : [Consumer clientId=consumer-entity-X-messagerelay-1, groupId=entity-X-messagerelay] Discovered group coordinator localhost:9092 (id: 2147483646 rack: null)
2020-08-05 21:24:12.910  INFO 39720 --- [   scheduling-1] o.a.k.c.c.internals.AbstractCoordinator  : [Consumer clientId=consumer-entity-X-messagerelay-1, groupId=entity-X-messagerelay] (Re-)joining group
2020-08-05 21:24:12.921  INFO 39720 --- [   scheduling-1] o.a.k.c.c.internals.AbstractCoordinator  : [Consumer clientId=consumer-entity-X-messagerelay-1, groupId=entity-X-messagerelay] Join group failed with org.apache.kafka.common.errors.MemberIdRequiredException: The group member needs to have a valid member id before actually entering a consumer group
2020-08-05 21:24:12.921  INFO 39720 --- [   scheduling-1] o.a.k.c.c.internals.AbstractCoordinator  : [Consumer clientId=consumer-entity-X-messagerelay-1, groupId=entity-X-messagerelay] (Re-)joining group
2020-08-05 21:24:12.927  INFO 39720 --- [   scheduling-1] o.a.k.c.c.internals.ConsumerCoordinator  : [Consumer clientId=consumer-entity-X-messagerelay-1, groupId=entity-X-messagerelay] Finished assignment for group at generation 9: {consumer-entity-X-messagerelay-1-5bf1ccff-b762-43c6-a3c3-33d5a22df639=Assignment(partitions=[entity-X-0, entity-X-1, entity-X-2])}
2020-08-05 21:24:12.932  INFO 39720 --- [   scheduling-1] o.a.k.c.c.internals.AbstractCoordinator  : [Consumer clientId=consumer-entity-X-messagerelay-1, groupId=entity-X-messagerelay] Successfully joined group with generation 9
2020-08-05 21:24:12.935  INFO 39720 --- [   scheduling-1] o.a.k.c.c.internals.ConsumerCoordinator  : [Consumer clientId=consumer-entity-X-messagerelay-1, groupId=entity-X-messagerelay] Adding newly assigned partitions: entity-X-2, entity-X-1, entity-X-0
2020-08-05 21:24:12.935  INFO 39720 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Following partition(s) has been assigned [entity-X-2, entity-X-1, entity-X-0]
2020-08-05 21:24:12.947  INFO 39720 --- [   scheduling-1] o.a.k.c.c.internals.ConsumerCoordinator  : [Consumer clientId=consumer-entity-X-messagerelay-1, groupId=entity-X-messagerelay] Setting offset for partition entity-X-2 to the committed offset FetchPosition{offset=3, offsetEpoch=Optional[0], currentLeader=LeaderAndEpoch{leader=Optional[localhost:9092 (id: 1 rack: null)], epoch=0}}
2020-08-05 21:24:12.948  INFO 39720 --- [   scheduling-1] o.a.k.c.c.internals.ConsumerCoordinator  : [Consumer clientId=consumer-entity-X-messagerelay-1, groupId=entity-X-messagerelay] Setting offset for partition entity-X-1 to the committed offset FetchPosition{offset=4, offsetEpoch=Optional[0], currentLeader=LeaderAndEpoch{leader=Optional[localhost:9092 (id: 1 rack: null)], epoch=0}}
2020-08-05 21:24:12.948  INFO 39720 --- [   scheduling-1] o.a.k.c.c.internals.ConsumerCoordinator  : [Consumer clientId=consumer-entity-X-messagerelay-1, groupId=entity-X-messagerelay] Setting offset for partition entity-X-0 to the committed offset FetchPosition{offset=5, offsetEpoch=Optional[0], currentLeader=LeaderAndEpoch{leader=Optional[localhost:9092 (id: 1 rack: null)], epoch=0}}
2020-08-05 21:24:14.672  INFO 39720 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Next offset to fetch in partition 0 would be 5
2020-08-05 21:24:14.673  INFO 39720 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Inspecting the offset before last (4) of partition 0 in order to extract 'outbox_id' header
2020-08-05 21:24:14.673  INFO 39720 --- [   scheduling-1] o.a.k.clients.consumer.KafkaConsumer     : [Consumer clientId=consumer-entity-X-messagerelay-1, groupId=entity-X-messagerelay] Seeking to offset 4 for partition entity-X-0
2020-08-05 21:24:14.674  INFO 39720 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Next offset to fetch in partition 1 would be 4
2020-08-05 21:24:14.675  INFO 39720 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Inspecting the offset before last (3) of partition 1 in order to extract 'outbox_id' header
2020-08-05 21:24:14.675  INFO 39720 --- [   scheduling-1] o.a.k.clients.consumer.KafkaConsumer     : [Consumer clientId=consumer-entity-X-messagerelay-1, groupId=entity-X-messagerelay] Seeking to offset 3 for partition entity-X-1
2020-08-05 21:24:14.675  INFO 39720 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Next offset to fetch in partition 2 would be 3
2020-08-05 21:24:14.676  INFO 39720 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Inspecting the offset before last (2) of partition 2 in order to extract 'outbox_id' header
2020-08-05 21:24:14.676  INFO 39720 --- [   scheduling-1] o.a.k.clients.consumer.KafkaConsumer     : [Consumer clientId=consumer-entity-X-messagerelay-1, groupId=entity-X-messagerelay] Seeking to offset 2 for partition entity-X-2
2020-08-05 21:24:15.215  INFO 39720 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Record found: topic='entity-X', partition='2', offset='2' 
2020-08-05 21:24:15.218  INFO 39720 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Extracted 'outbox_id' header: 9
2020-08-05 21:24:15.219  INFO 39720 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Record found: topic='entity-X', partition='1', offset='3' 
2020-08-05 21:24:15.219  INFO 39720 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Extracted 'outbox_id' header: 12
2020-08-05 21:24:15.219  INFO 39720 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Record found: topic='entity-X', partition='0', offset='4' 
2020-08-05 21:24:15.219  INFO 39720 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Extracted 'outbox_id' header: 11
2020-08-05 21:24:15.234  INFO 39720 --- [   scheduling-1] o.a.k.c.c.internals.ConsumerCoordinator  : [Consumer clientId=consumer-entity-X-messagerelay-1, groupId=entity-X-messagerelay] Revoke previously assigned partitions entity-X-2, entity-X-1, entity-X-0
2020-08-05 21:24:15.235  INFO 39720 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Following partition(s) has been revoked [entity-X-2, entity-X-1, entity-X-0]
2020-08-05 21:24:15.235  INFO 39720 --- [   scheduling-1] o.a.k.c.c.internals.AbstractCoordinator  : [Consumer clientId=consumer-entity-X-messagerelay-1, groupId=entity-X-messagerelay] Member consumer-entity-X-messagerelay-1-5bf1ccff-b762-43c6-a3c3-33d5a22df639 sending LeaveGroup request to coordinator localhost:9092 (id: 2147483646 rack: null) due to the consumer is being closed
2020-08-05 21:24:15.267  INFO 39720 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Next id to poll from database is 13

```

---
## Prochaines évolutions à adresser et planifier
>- Exploiter le Schema Registry,
>- *State Rebuilder*: au démarrage, l'application reconstitue ses états internes en traitant les messages à partir de l'offset 0 et jusqu'au prochain message précédemment non traitée, sans n'émettre aucun message dans les topics *sink*, 
>- *Graceful/clean shutdown*: l'application tente de terminer les traitements commencés, avant de s'éteindre gracieusement,
>- Mettre en place le plugin maven [jib](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin) pour générer l'image Docker et la publier dans azure docker registry,
>- La structure des tables SQL dynamiques est automatiquement déduite des schéma AVRO associés à chacun des topics *source*,
>- Exploiter les indicateurs qualité produits, pour mettre en place des quality-gate et un reporting,
>- Ecrire les tests automatisés,
>- Définir le schéma et la documentation du fichier de config YAML et le documenter,
>- Garantir l'exactly-once processing,
>- Ajouter des tirets aux ULID reçus en entrée pour les transformer en UUID, et disposer dans PostgreSQL d'une colonne de type UUID (actuellement, c'est un VARCHAR),
>- Les schémas de données contenant des éléments de type complexes (avec des relations 0,N) ne sont pas gérées: il faudrait le prévoir car les entités "plates" restent assez rares,
