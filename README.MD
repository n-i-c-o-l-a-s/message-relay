# <span align="center"><img src="/doc/logo.svg" width="3%"/></span><span> message-relay <img src="https://circleci.com/gh/n-i-c-o-l-a-s/message-relay.svg?style=shield"/>

## Présentation générale
> *messagerelay* est une application impliquée dans l'implémentation du *Pattern* d'architecture nommé *[Transactional Outbox](https://microservices.io/patterns/data/transactional-outbox.html)*. Volontairement indépendante du Métier de l'entreprise, l'application *messagerelay* est en charge de lire par intervalles de temps (*polling*) les lignes de la table *outbox* pour ensuite les relayer dans un topic Kafka. Ces lignes ont été précedemment insérées par les applications Métiers (microservices) en exploitant la même transaction base-de-données que celle qui a permis de persister la mutation de son état. En effet, plutôt que d'envoyer eux-mêmes le message à destination du Topic Kafka, les microservices écrivent la clé et le message Kafka dans une table *outbox*.

> *messagerelay* considère la clé et la valeur du message à relayer comme des tableaux d'octets (byte []), lui permettant ainsi de pouvoir relayer tous formats de message (JSON, AVRO, XML, etc.) avec n'importe quel *encoding*: le contenu est simplement relayé, et jamais décodé.

> *messagerelay* n'est pas (encore?) scalable : plusieurs instances de cette application ne peuvent pas se partager les travaux de relais d'une même base de de données.

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
>- supprime de la table *outbox_id* toutes les lignes dont la valeur du champ *id* est inférieure à la valeur du dernier *outbox_id* traité avec succès.

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

### Paramétrages (fichier src/main/resources/application.properties
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

## Variables d'environnement à positionner

``` properties 
KAFKA_BOOTSTRAP_SERVERS # l'hote et le port hébergeant le broker Kafka, par exemple 10.34.2.2:9092 ou broker:29092
```
``` properties
KAFKA_GROUP_ID # le nom du groupe de consommateur Kafka que messagerelay rejoindra
```
``` properties
KAFKA_GROUP_ID # le nom du Topic Kafka dansa le quel messagerelay va consommer et produire des messages
```
``` properties
KAFKA_GROUP_ID # le nom de l'hote hébergeant le serveur Postgresql, par exemple 10.45.33.121 ou postgresql.mydomain.com
```
``` properties
POSTGRESQL_PORT # le port de l'hote hébergeant le serveur Postgresql, par exemple 5432
```
``` properties
POSTGRESQL_DB # le nom de la base-de-données hébergeant la table outbox, par exemple CLIENT_DB ou ACCOUNT_DB
```
``` properties
POSTGRESQL_USER # le nom d'utilisateur pour se connecter à la base-de-données
```
``` properties
POSTGRESQL_PWD # le mot de passe de connexion à la base-de-données
```
## Compilation Maven et création de l'image Docker (standard)

``` sh
mvn clean package
```
``` sh
docker build --build-arg VERSION=<VERSION> -t xxxxxxxxxx/messagerelay .
```

## Démonstration d'une exécution dans un environnement complet conteneurisé

``` sh
cd src/test/containers
```
``` sh
docker-compose up -d
```
``` sh
Creating network "containers_default" with the default driver
Creating zookeeper ... done
Creating postgres  ... done
Creating pgadmin4  ... done
Creating broker    ... done
Creating kafdrop      ... done
Creating messagerelay ... done
```
``` sh
docker-compose down --volumes
```
``` sh
Stopping messagerelay ... done
Stopping kafdrop      ... done
Stopping broker       ... done
Stopping postgres     ... done
Stopping pgadmin4     ... done
Stopping zookeeper    ... done
Removing messagerelay ... done
Removing kafdrop      ... done
Removing broker       ... done
Removing postgres     ... done
Removing pgadmin4     ... done
Removing zookeeper    ... done
Removing network containers_default
```

## Traces applicatives au démarrage (*docker logs -f messagerelay*)
```properties

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.3.2.RELEASE)

2020-08-05 19:31:13.999  INFO 1 --- [           main] c.s.a.m.MessageRelayApplication          : Starting MessageRelayApplication v1.0.0-SNAPSHOT on d73f21dcf42d with
PID 1 (/app.jar started by spring in /)
2020-08-05 19:31:14.016  INFO 1 --- [           main] c.s.a.m.MessageRelayApplication          : No active profile set, falling back to default profiles: default
2020-08-05 19:31:17.240  INFO 1 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFERRED mode.
2020-08-05 19:31:17.488  INFO 1 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 202ms. Found 1 JPA reposit
ory interfaces.
2020-08-05 19:31:19.961  INFO 1 --- [           main] o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService 'applicationTaskExecutor'
2020-08-05 19:31:20.037  INFO 1 --- [           main] o.s.s.c.ThreadPoolTaskScheduler          : Initializing ExecutorService 'taskScheduler'
2020-08-05 19:31:21.039  INFO 1 --- [         task-1] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2020-08-05 19:31:21.879  INFO 1 --- [           main] DeferredRepositoryInitializationListener : Triggering deferred initialization of Spring Data repositories…
2020-08-05 19:31:22.723  INFO 1 --- [         task-1] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 5.4.18.Final
2020-08-05 19:31:23.149  INFO 1 --- [         task-1] o.hibernate.annotations.common.Version   : HCANN000001: Hibernate Commons Annotations {5.1.0.Final}
2020-08-05 19:31:23.939  INFO 1 --- [         task-1] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2020-08-05 19:31:24.326  INFO 1 --- [         task-1] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2020-08-05 19:31:24.894  INFO 1 --- [         task-1] org.hibernate.dialect.Dialect            : HHH000400: Using dialect: org.hibernate.dialect.PostgreSQL10Dialect
2020-08-05 19:31:28.813  INFO 1 --- [         task-1] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000490: Using JtaPlatform implementation: [org.hibernate.engine.tra
nsaction.jta.platform.internal.NoJtaPlatform]
2020-08-05 19:31:28.852  INFO 1 --- [         task-1] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2020-08-05 19:31:29.527  INFO 1 --- [           main] DeferredRepositoryInitializationListener : Spring Data repositories initialized!
2020-08-05 19:31:29.626  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Finding the last commited offset from Kafka topic 'messagerelay-groupi
d'
2020-08-05 19:31:29.630  INFO 1 --- [           main] c.s.a.m.MessageRelayApplication          : Started MessageRelayApplication in 18.029 seconds (JVM running for 20.
788)
2020-08-05 19:31:29.772  INFO 1 --- [   scheduling-1] o.a.k.clients.consumer.ConsumerConfig    : ConsumerConfig values:
        allow.auto.create.topics = true
        auto.commit.interval.ms = 5000
        auto.offset.reset = earliest
        bootstrap.servers = [broker:29092]
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
        group.id = messagerelay-groupid
        group.instance.id = null
        heartbeat.interval.ms = 3000
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

2020-08-05 19:31:30.419  INFO 1 --- [   scheduling-1] o.a.kafka.common.utils.AppInfoParser     : Kafka version: 2.5.0
2020-08-05 19:31:30.425  INFO 1 --- [   scheduling-1] o.a.kafka.common.utils.AppInfoParser     : Kafka commitId: 66563e712b0b9f84
2020-08-05 19:31:30.425  INFO 1 --- [   scheduling-1] o.a.kafka.common.utils.AppInfoParser     : Kafka startTimeMs: 1596655890412
2020-08-05 19:31:30.432  INFO 1 --- [   scheduling-1] o.a.k.clients.consumer.KafkaConsumer     : [Consumer clientId=consumer-messagerelay-groupid-1, groupId=messagerel
ay-groupid] Subscribed to topic(s): messagerelay-groupid
2020-08-05 19:31:34.444  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : No record found in topic messagerelay-groupid
2020-08-05 19:31:34.473  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Next id to poll from database is 1
2020-08-05 19:31:34.676  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Outbox [id=1, key=[49], value=[49]] has been polled
2020-08-05 19:31:34.687  INFO 1 --- [   scheduling-1] o.a.k.clients.producer.ProducerConfig    : ProducerConfig values:
        acks = 1
        batch.size = 16384
        bootstrap.servers = [broker:29092]
        buffer.memory = 33554432
        client.dns.lookup = default
        client.id = producer-1
        compression.type = none
        connections.max.idle.ms = 540000
        delivery.timeout.ms = 120000
        enable.idempotence = false
        interceptor.classes = []
        key.serializer = class org.apache.kafka.common.serialization.ByteArraySerializer
        linger.ms = 0
        max.block.ms = 60000
        max.in.flight.requests.per.connection = 5
        max.request.size = 1048576
        metadata.max.age.ms = 300000
        metadata.max.idle.ms = 300000
        metric.reporters = []
        metrics.num.samples = 2
        metrics.recording.level = INFO
        metrics.sample.window.ms = 30000
        partitioner.class = class org.apache.kafka.clients.producer.internals.DefaultPartitioner
        receive.buffer.bytes = 32768
        reconnect.backoff.max.ms = 1000
        reconnect.backoff.ms = 50
        request.timeout.ms = 30000
        retries = 2147483647
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
        transaction.timeout.ms = 60000
        transactional.id = null
        value.serializer = class org.apache.kafka.common.serialization.ByteArraySerializer

2020-08-05 19:31:34.756  INFO 1 --- [   scheduling-1] o.a.kafka.common.utils.AppInfoParser     : Kafka version: 2.5.0
2020-08-05 19:31:34.756  INFO 1 --- [   scheduling-1] o.a.kafka.common.utils.AppInfoParser     : Kafka commitId: 66563e712b0b9f84
2020-08-05 19:31:34.756  INFO 1 --- [   scheduling-1] o.a.kafka.common.utils.AppInfoParser     : Kafka startTimeMs: 1596655894756
2020-08-05 19:31:34.788  INFO 1 --- [ad | producer-1] org.apache.kafka.clients.Metadata        : [Producer clientId=producer-1] Cluster ID: WteaqqB3Rci_XsyXrfi5Ww
2020-08-05 19:31:34.823  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : id 1 has been successfully relayed to topic messagerelay-groupid
2020-08-05 19:31:34.837  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Outbox [id=2, key=[50], value=[50]] has been polled
2020-08-05 19:31:34.849  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : id 2 has been successfully relayed to topic messagerelay-groupid
2020-08-05 19:31:34.864  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Outbox [id=3, key=[51], value=[51]] has been polled
2020-08-05 19:31:34.865  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : id 3 has been successfully relayed to topic messagerelay-groupid
2020-08-05 19:31:34.879  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Outbox [id=4, key=[52], value=[52]] has been polled
2020-08-05 19:31:34.880  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : id 4 has been successfully relayed to topic messagerelay-groupid
2020-08-05 19:31:34.896  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Outbox [id=5, key=[53], value=[53]] has been polled
2020-08-05 19:31:34.902  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : id 5 has been successfully relayed to topic messagerelay-groupid
2020-08-05 19:31:34.918  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Outbox [id=6, key=[54], value=[54]] has been polled
2020-08-05 19:31:34.919  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : id 6 has been successfully relayed to topic messagerelay-groupid
2020-08-05 19:31:34.932  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Outbox [id=7, key=[55], value=[55]] has been polled
2020-08-05 19:31:34.933  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : id 7 has been successfully relayed to topic messagerelay-groupid
2020-08-05 19:31:34.947  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Outbox [id=8, key=[56], value=[56]] has been polled
2020-08-05 19:31:34.948  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : id 8 has been successfully relayed to topic messagerelay-groupid
2020-08-05 19:31:34.961  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Outbox [id=9, key=[57], value=[57]] has been polled
2020-08-05 19:31:34.961  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : id 9 has been successfully relayed to topic messagerelay-groupid
2020-08-05 19:31:34.974  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Outbox [id=10, key=[49, 48], value=[49, 48]] has been polled
2020-08-05 19:31:34.974  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : id 10 has been successfully relayed to topic messagerelay-groupid
2020-08-05 19:31:34.987  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Outbox [id=11, key=[49, 49], value=[49, 49]] has been polled
2020-08-05 19:31:34.987  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : id 11 has been successfully relayed to topic messagerelay-groupid
2020-08-05 19:31:35.001  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Outbox [id=12, key=[49, 50], value=[49, 50]] has been polled
2020-08-05 19:31:35.001  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : id 12 has been successfully relayed to topic messagerelay-groupid
2020-08-05 19:31:35.014  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Outbox [id=13, key=[49, 51], value=[49, 51]] has been polled
2020-08-05 19:31:35.015  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : id 13 has been successfully relayed to topic messagerelay-groupid
2020-08-05 19:31:35.028  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Outbox [id=14, key=[49, 52], value=[49, 52]] has been polled
2020-08-05 19:31:35.029  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : id 14 has been successfully relayed to topic messagerelay-groupid
2020-08-05 19:31:35.042  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Outbox [id=15, key=[49, 53], value=[49, 53]] has been polled
2020-08-05 19:31:35.042  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : id 15 has been successfully relayed to topic messagerelay-groupid
2020-08-05 19:31:35.059  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : Outbox [id=16, key=[49, 54], value=[49, 54]] has been polled
2020-08-05 19:31:35.060  INFO 1 --- [   scheduling-1] c.s.a.messagerelay.OutboxPoller          : id 16 has been successfully relayed to topic messagerelay-groupid
```


## Prochaines évolutions à adresser et planifier
>- Mettre en place le plugin maven [jib](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin) pour générer l'image Docker et la publier sur dockerhub,
>- Ecrire les tests automatisés avec postgresql et kafka embarqués,
>- Utiliser *jasypt* pour ne plus écrire le mot de passe *postgres* en clair dans un fichier appartenant au repository des sources.
