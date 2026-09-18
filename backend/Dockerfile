# ============================================================
# Stage 1: Build the JAR with Maven
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Cache Maven dependencies first (layer-caching optimization)
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B --no-transfer-progress -q

# Copy source and build the production JAR (skip tests — already ran in CI)
COPY src ./src
RUN ./mvnw package -DskipTests -B --no-transfer-progress -q -Pprod

# ============================================================
# Stage 2: Minimal JRE 21 runtime image
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

# Copy only the built fat-JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Railway injects $PORT dynamically; default to 8080 if not set
ENV SERVER_PORT=${PORT:-8080}
ENV SPRING_PROFILES_ACTIVE=prod

# JVM flags: optimized for containerized low-memory environments
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dserver.port=${PORT:-8080}", \
  "-jar", "app.jar"]

EXPOSE 8080
