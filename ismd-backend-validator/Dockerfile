### Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Install bash
RUN apk add --no-cache bash

# Copy the entire project
COPY . .

# Make mvnw executable
RUN chmod +x mvnw

# Build the validator module (which will also build common as a dependency)
RUN ./mvnw -pl ismd-backend-validator clean package -DskipTests

### Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Get build arguments
ARG MODULE_VERSION=unknown
ARG GIT_COMMIT=unknown

# Set environment variables
ENV APP_VERSION=${MODULE_VERSION}
ENV GIT_COMMIT=${GIT_COMMIT}

# Set Spring profile
ARG SPRING_PROFILES_ACTIVE=production
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}

# Copy the built JAR from the build stage
COPY --from=build /app/ismd-backend-validator/target/ismd-backend-validator-*.jar app.jar

# Add labels for better image identification
LABEL org.opencontainers.image.title="ISMD Backend Validator"
LABEL org.opencontainers.image.version="${MODULE_VERSION}"

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
