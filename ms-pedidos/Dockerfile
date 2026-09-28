FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl ms-pedidos -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/ms-pedidos/target/ms-pedidos-*.jar /app/app.jar
USER spring
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
