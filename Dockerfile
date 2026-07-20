FROM eclipse-temurin:26-jdk-alpine AS build
RUN apk add --no-cache nodejs
WORKDIR /workspace
COPY . .
RUN ./mvnw --batch-mode --no-transfer-progress verify

FROM eclipse-temurin:26-jre-alpine
RUN addgroup -S pembana && adduser -S pembana -G pembana
USER pembana
WORKDIR /app
COPY --from=build /workspace/target/pembana-rain-gauge-*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseG1GC", "-jar", "/app/app.jar"]
