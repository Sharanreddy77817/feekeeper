# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 feekeeper \
    && useradd --system --uid 10001 --gid feekeeper --home-dir /app --shell /usr/sbin/nologin feekeeper
WORKDIR /app
COPY --from=build --chown=feekeeper:feekeeper /workspace/target/feekeeper-*.jar /app/feekeeper.jar
ENV JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8"
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl --fail --silent --show-error "http://127.0.0.1:${PORT:-8080}/actuator/health/readiness" || exit 1
ENTRYPOINT ["java", "-jar", "/app/feekeeper.jar"]
