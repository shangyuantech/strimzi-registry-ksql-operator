#FROM openjdk:12-alpine
FROM adoptopenjdk/openjdk11:alpine

ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/operator/schema-registry-operator.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/usr/share/operator/schema-registry-operator.jar"]
