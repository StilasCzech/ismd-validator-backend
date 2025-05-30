### Common build stage for both modules
FROM eclipse-temurin:17-jdk-alpine AS common-build
WORKDIR /app

# Install bash
RUN apk add --no-cache bash

# Copy all files
COPY . .

# Make mvnw executable
RUN chmod +x mvnw

# Build common module
FROM common-build AS common
RUN ./mvnw -pl ismd-backend-common clean package -DskipTests

# Build validator module
FROM common-build AS validator
RUN ./mvnw -pl ismd-backend-validator clean package -DskipTests

# Build all modules
FROM common-build AS all
RUN ./mvnw -pl ismd-backend-common,ismd-backend-validator clean package -DskipTests

# Runtime stage for common module
FROM eclipse-temurin:17-jre-alpine AS common-runtime
WORKDIR /app

# Get build arguments
ARG MODULE_VERSION=unknown

# Set environment variables
ENV APP_VERSION=${MODULE_VERSION}

# Set Spring profile
ARG SPRING_PROFILES_ACTIVE=production
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}

# Copy the built JAR
COPY --from=common /app/ismd-backend-common/target/ismd-backend-common-*.jar app.jar

# Add labels for better image identification
LABEL org.opencontainers.image.title="ISMD Backend Common"
LABEL org.opencontainers.image.version="${MODULE_VERSION}"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

# Runtime stage for validator module
FROM eclipse-temurin:17-jre-alpine AS validator-runtime
WORKDIR /app

# Get build arguments
ARG MODULE_VERSION=unknown

# Set environment variables
ENV APP_VERSION=${MODULE_VERSION}

# Set Spring profile
ARG SPRING_PROFILES_ACTIVE=production
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}

# Copy the built JAR
COPY --from=validator /app/ismd-backend-validator/target/ismd-backend-validator-*.jar app.jar

# Add labels for better image identification
LABEL org.opencontainers.image.title="ISMD Backend Validator"
LABEL org.opencontainers.image.version="${MODULE_VERSION}"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]