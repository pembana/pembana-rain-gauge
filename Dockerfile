FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY . .
RUN ./mvnw --batch-mode --no-transfer-progress verify

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S pembana && adduser -S pembana -G pembana
USER pembana
WORKDIR /app
COPY --from=build /workspace/target/pembana-rain-gauge-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]
