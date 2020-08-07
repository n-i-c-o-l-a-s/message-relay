# <span align="center"><img src="/doc/logo.svg" width="3%"/></span><span> message-relay <img src="https://circleci.com/gh/n-i-c-o-l-a-s/message-relay.svg?style=shield"/>

## Présentation générale
> *messagerelay* est une application impliquée dans l'implémentation du *Pattern* d'architecture nommé *[Transactional Outbox](https://microservices.io/patterns/data/transactional-outbox.html)*. Volontairement indépendante du Métier de l'entreprise, l'application *messagerelay* est en charge de lire par intervalles de temps (*polling*) les lignes de la table *outbox* pour ensuite les relayer dans un topic Kafka. Ces lignes ont été précedemment insérées par les applications Métiers (microservices) en exploitant la même transaction base-de-données que celle qui a permis de persister la mutation de son état. En effet, plutôt que d'envoyer eux-mêmes le message à destination du Topic Kafka, les microservices écrivent la clé et le message Kafka dans une table *outbox*.

 <span align="center"><img src="/doc/messagerelay-architecture.svg"/></span>

## Stack technologique
>- Spring-Boot,
>- Spring-Data-JPA, pour l'accès à la base de données Postgres qui fait l'objet du *poll*,
>- Spring Kafka, pour la consommation et la production de messages Kafka, 
>- PMD, Findbug, CheckStyle, Jacoco, pour la qualimétrie,
>- Maven 3.6.3, pour le *build* (3.5.0 minimum requis),
>- Docker, pour la conteneurisation.

## Capacités générales

### Au *startup*, *messagerelay*
>- se connecte au cluster Kafka puis souscrit, en tant que consommateur, au topic de destination,
>- positionne son curseur de consommation, pour chaque partition assignée, sur le dernier *offset* commité,
>- lit le message localisé à cet *offset* pour en extraire le header nommé *outbox_id* et y ajouter la valeur 1,
>- considère cette valeur comme le prochain *outbox_id* à lire depuis la base-de-données.

### A intervalles de temps réguliers et configurables, *messagerelay*
>- vérifie la présence de l'arrivée d'une nouvelle de ligne dans la table *outbox*, ne considérant que l'id courant à traiter: *messagerelay* garantie un ordre identique entre l'insertion des lignes dans la table *outbox* et l'insertion dans le Topic Kafka,
>- crée un nouveau message Kafka à partir des valeurs de la ligne lue,  
>- y ajoute un header personnalisé nommé *outobx_id*, dont la valeur est égale au champ *id* de la table *outbox*,
>- relaye ce message vers le topic Kafka,
>- et enfin considère cet *outbox_id* comme le dernier traité avec succès.

### A intervalles de temps réguliers et configurables, *messagerelay*
>- supprime de la table *outbox_id* toutes les lignes dont la valeur du champ *id* est inférieur à la valeur du dernier *outbox_id* traité avec succès.

### Coté base de données Postgres, *messagerelay*
>- doit idéalement faire l'objet de la création d'un user/role Postgres dédié
```sql
CREATE ROLE messagerelay WITH LOGIN NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION ENCRYPTED PASSWORD 'md54cf64e155ec4170d094ff99487216aa0';
```
>- s'attend à travailler avec la structure de table *outbox* suivante 
```sql
CREATE TABLE public.outbox (id bigserial NOT NULL, key bytea NOT NULL, value bytea NOT NULL, CONSTRAINT outbox_pkey PRIMARY KEY (id));
```
>- doit dispoer des droits de lecture et de suppression sur la table *outbox*
```sql
GRANT SELECT, DELETE ON TABLE public.outbox TO messagerelay;
```

### Paramétrages
```properties
# below, properties related to messagerelay as a kafka consumer
messagerelay.kafka.bootstrapservers=${KAFKA_BOOTSTRAP_SERVERS}
messagerelay.kafka.groupid=${GROUP_ID}
messagerelay.kafka.topic=${TOPIC_NAME}
messagerelay.kafka.auto-offset-reset=earliest
# properties related to messagerelay as a kafka producer
spring.kafka.producer.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS}
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.ByteArraySerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.ByteArraySerializer
# below, properties related to messagerelay as a database client
messagerelay.db.poll.delay.in.ms=10
spring.datasource.hikari.connectionTimeout=10000
spring.datasource.hikari.maximumPoolSize=5
spring.datasource.url=jdbc:postgresql://${POSTGRESQL_HOST}:${POSTGRESQL_PORT}/${POSTGRESQL_DB}
spring.datasource.username=${POSTGRESQL_USER}
spring.datasource.password=${POSTGRESQL_PWD}
spring.jpa.generate-ddl=false
spring.jpa.hibernate.ddl-auto=none
```
### Quelques détails techniques

>- *messagerelay* considère la clé et la valeur du message à relayer comme des tableaux de bytes (byte []), lui permettant ainsi de pouvoir relayer tous formats de message (JSON, AVRO, XML, etc.): le contenu est simplement relayé, et jamais décodé,
>- *messagerelay* n'est pas (encore) scalable : plusieurs instances de cette application ne peuvent pas se partager les travaux de relais sur une même base de de données
>-

## Compilation Maven et création de l'image Docker
>- L'application est construite et packagée dans un JAR via la commande ```mvn clean package```
``` sh
mvn clean package
```

>- Elle est ensuite Dockerisée via la commande ```docker build --build-arg VERSION=1.0.0 -t messagrelay .```, en positionnant la version souhaitée,

## Création d'un environnement locale conteneurisé
>- le fichier est disponible pour démontrer l'exécution de messagerelay mais aussi pour effetuer quelques tests manuels,
>- Les variables d'environnement suivantes sont nécessaires: 

``` properties 
KAFKA_BOOTSTRAP_SERVERS # l'hote et le port hébergeant le broker Kafka, par exemple 10.34.2.2:9092 ou broker:29092
```

``` properties
GROUP_ID # le nom du groupe de consommateur Kafak que messagerelay rejoindra, par client-messagerelay ou account-messagerelay
```

``` properties
TOPIC_NAME # le nom du Topic Kafka dansa le quel messagerelay va consommer et produire des messages
```
``` properties
POSTGRESQL_HOST # le nom de l'hote hébergeant le serveur Postgresql, par exemple 10.45.33.121 ou postgresql.mydomain.com
```
``` properties
POSTGRESQL_PORT # le port de l'hote hébergeant le serveur Postgresql, par exemple 5432
```

``` properties
POSTGRESQL_DB # le nom de la base-de-données hébergeant la table outbox, par exemple CLIENT_DB ou ACCOUNT_DB
```

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


## Prochaines évolutions à adresser et planifier
>- Mettre en place le plugin maven [jib](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin) pour générer l'image Docker et la publier dans azure docker registry,
>- Ecrire les tests automatisés avec postgresql et kafka embarqués.
