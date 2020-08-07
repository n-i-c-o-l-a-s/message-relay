FROM openjdk:8
RUN addgroup --system spring 
RUN adduser --system spring
RUN adduser spring spring
USER spring:spring
ARG VERSION
RUN echo "copying target/messagerelay-${VERSION}.jar"
COPY target/messagerelay-${VERSION}.jar app.jar
ENTRYPOINT ["java","-Duser.timezone=UTC","-jar","/app.jar"]