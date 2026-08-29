# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml ./
COPY proto-contract/ proto-contract/
COPY movie-service/ movie-service/
COPY user-service/ user-service/
COPY recommendation-service/ recommendation-service/
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre AS base
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd --system app && useradd --system --gid app --create-home app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
WORKDIR /app
USER app

FROM base AS movie-service-runtime
COPY --from=build --chown=app:app /build/movie-service/target/movie-service-*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM base AS user-service-runtime
COPY --from=build --chown=app:app /build/user-service/target/user-service-*.jar /app/app.jar
EXPOSE 9091 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM base AS recommendation-service-runtime
COPY --from=build --chown=app:app /build/recommendation-service/target/recommendation-service-*.jar /app/app.jar
EXPOSE 9093 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
