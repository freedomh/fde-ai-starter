FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B verify

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/starter-demo-backend-0.1.0.jar app.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "app.jar"]
