FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl ms-pagos -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/ms-pagos/target/ms-pagos-*.jar /app/app.jar
USER spring
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
