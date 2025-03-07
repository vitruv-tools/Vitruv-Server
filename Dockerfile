FROM eclipse-temurin:17 AS builder
WORKDIR /app
COPY . .
# only build secserver module
RUN ./mvnw clean verify -pl security-server -am

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/security-server/target/security-server-3.2.0-SNAPSHOT.jar /app/app.jar
COPY --from=builder /app/security-server/target/libs /app/libs
ENTRYPOINT ["java", "-cp", "/app/app.jar:/app/libs/*", "app.VitruvServerApp"]
