FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl kafka-admin-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/kafka-admin-service/target/kafka-admin-service-*.jar /app/app.jar
USER spring
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
