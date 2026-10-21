# Stage 1: compile the Spring Boot application with the Java version declared in pom.xml.
FROM maven:3.9-eclipse-temurin-21 AS build

# Keep all build files in an isolated directory inside the image.
WORKDIR /build

# Copy the Maven descriptor and source, then create the executable JAR.
COPY pom.xml ./
COPY src ./src

# Cache Maven downloads between image builds without including them in the final image.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package -DskipTests

# Stage 2: run only the packaged application with a smaller Java runtime image.
FROM eclipse-temurin:21-jre-jammy

# Run the API as an unprivileged user instead of root.
RUN groupadd --system spring && useradd --system --gid spring spring
WORKDIR /app

# Copy only the executable JAR from the build stage; Maven and source code are excluded.
COPY --from=build /build/target/*.jar app.jar

USER spring
EXPOSE 8083

# Start Spring Boot. Database settings continue to come from environment variables.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
