FROM gradle:9.2.1-jdk17 AS build
# Set container working directory to /app
WORKDIR /app
# Copy Gradle configuration files
COPY gradlew /app/
COPY gradle /app/gradle
# Normalize Windows line endings and ensure Gradle wrapper is executable
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew
# Copy build script and source code
COPY build.gradle settings.gradle /app/
COPY src /app/src
# Run tests and coverage report during image build so failures are visible in docker build logs
RUN ./gradlew test jacocoTestReport --no-daemon --max-workers=1 \
    -Dorg.gradle.daemon=false \
    -Dorg.gradle.jvmargs="-Xms256m -Xmx768m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8"
# Build the server
RUN ./gradlew clean bootJar --no-daemon --max-workers=1 -x test \
    -Dorg.gradle.daemon=false \
    -Dorg.gradle.jvmargs="-Xms256m -Xmx768m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8"

# make image smaller by using multi stage build
FROM eclipse-temurin:17-jdk
# Set the env to "production"
ENV SPRING_PROFILES_ACTIVE=production
# get non-root user
RUN groupadd appgroup && \
    useradd -r -g appgroup appuser
USER appuser
# Set container working directory to /app
WORKDIR /app
# copy built artifact from build stage
COPY --from=build /app/build/libs/*.jar /app/soprafs26.jar
# Expose the port on which the server will be running (based on application.properties)
EXPOSE 8080
# start server
CMD ["java", "-jar", "/app/soprafs26.jar"]
