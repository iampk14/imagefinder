# Stage 1: Use Maven to build the project with Java 8
FROM maven:3.8.5-openjdk-8-slim AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Use Jetty to run the .war file
FROM jetty:9.4.50-jre8
COPY --from=build /app/target/imagefinder-0.1.0-SNAPSHOT.war /var/lib/jetty/webapps/root.war


EXPOSE 8080
CMD ["java", "-jar", "/usr/local/jetty/start.jar"]
