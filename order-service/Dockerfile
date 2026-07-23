FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Agent OpenTelemetry récupéré À LA CONSTRUCTION (aucun jar commité au dépôt).
ARG OTEL_AGENT_VERSION=2.11.0
RUN wget -O /opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY --from=build /opentelemetry-javaagent.jar /opentelemetry-javaagent.jar
EXPOSE 8080
# -javaagent : instrumentation automatique (traces). La config passe par variables
# d'environnement (voir docker-compose.yml). Zéro code applicatif.
ENTRYPOINT ["java", "-javaagent:/opentelemetry-javaagent.jar", "-jar", "app.jar"]
