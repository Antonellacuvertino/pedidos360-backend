FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl ms-clientes -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/ms-clientes/target/ms-clientes-*.jar /app/app.jar
USER spring
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
