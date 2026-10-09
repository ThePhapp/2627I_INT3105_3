FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
# CI runs verify including Docker-backed tests. Image assembly only packages compiled code.
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system gdrn && useradd --system --gid gdrn gdrn
WORKDIR /app
COPY --from=build --chown=gdrn:gdrn /workspace/target/gdrn-*.jar app.jar
USER gdrn
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=6 \
    CMD curl --fail --silent http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
