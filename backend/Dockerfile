# Stage 1: Build the application
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Expose the port Render expects
EXPOSE 8080

# The crucial memory limits are baked right into the start command!
ENTRYPOINT ["java", "-Xmx300m", "-jar", "app.jar"]
