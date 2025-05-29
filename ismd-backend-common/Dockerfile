### Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Install bash
RUN apk add --no-cache bash

# Copy the entire project
COPY . .

# Make mvnw executable
RUN chmod +x mvnw

# Build the common module from the root
RUN ./mvnw -pl ismd-backend-common clean package -DskipTests

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
COPY --from=build /app/ismd-backend-common/target/ismd-backend-common-*.jar app.jar

# Add labels for better image identification
LABEL org.opencontainers.image.title="ISMD Backend Common"
LABEL org.opencontainers.image.version="${MODULE_VERSION}"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
