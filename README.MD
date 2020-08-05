# message-relay

---
## Présentation générale
> *messagerelay* est une application impliquée dans l'implémentation du Pattern d'architecture nommé *[Transactional Outbox](https://microservices.io/patterns/data/transactional-outbox.html)*. *messagerelay* est chargée de lire (poll) les lignes de la table *outbox* et de le relayer dans un topic Kafka. Ces lignes ont été précedemment insérées par les applications (microservices) dans la meme transaction de base-de-données qui a permis de changer et muter leur état.

---
## Stack technologique exploitée
>- Spring Boot,
>- Spring Kafka, pour la consommation et la production de messages Kafka, 
>- PostgreSQL, pour la base-de-données hébergeant l'état du microservice,
>- Kafka pour le Message-Broker,
>- PMD, Findbug, CheckStyle, Jacoco, pour la qualimétrie,
>- Maven 3.6.3, pour le *build* (3.5.0 minimum requis),
>- Docker, pour la conteneurisation.

---
## Capacités générales

### Au *startup* de l'application, *messagerelay*
>- se connecte au cluster Kafka puis souscrit, en tant que consommateur, au topic de destination,
>- se positionne, pour chaque partition du topic, sur le dernier *offset* commité,
>- extrait le message localisé à cette *offset* pour en extraire le header nommé *outbox_id* et enfin y ajouter +1,
>- considère cette valeur comme le prochain *outbox_id* à lire depuis la base-de-données.

### Toutes les N millisecondes, *messagerelay*

>-  lit la ligne de base-de-donnéee dont le champ *id* est le prochain *outbox_id* à relayer vers le topic Kafka,
>- crée un nouveau message à partir de ces valeurs récupérées, 
>- y ajoute un header nommé *outobx_id*,
>- envoie ce message dans le topic.

### Toute les N miilisecondes

>- r

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