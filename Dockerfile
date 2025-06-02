FROM openjdk:21-slim
WORKDIR /app
COPY target/LB5-1.0-SNAPSHOT.jar /LB5-1.0-SNAPSHOT.jar


# Відкриваємо порт 8080
EXPOSE 8080

# Запускаємо Spring Boot додаток
ENTRYPOINT ["java", "-jar", "/LB5-1.0-SNAPSHOT.jar"]