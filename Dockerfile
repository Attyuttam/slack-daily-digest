FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY . .
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests package

CMD ["java", "-jar", "target/*.jar"]
