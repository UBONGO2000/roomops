# Build context is the repo root (not backend/) because the Maven build needs
# ../api-contract/openapi.yaml, a sibling directory, for code generation.

# ---- Build stage ----
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

COPY backend/.mvn/ backend/.mvn/
COPY backend/mvnw backend/pom.xml backend/
COPY api-contract/ api-contract/
WORKDIR /app/backend
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY backend/src ./src
RUN ./mvnw -B package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app

COPY --from=build /app/backend/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
