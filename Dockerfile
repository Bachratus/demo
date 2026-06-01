# ==========================================
# STAGE 1: Build
# ==========================================
FROM gradle:8-jdk21-alpine AS build_stage
WORKDIR /app

COPY build.gradle settings.gradle /app/
COPY gradle /app/gradle
COPY gradlew /app/

RUN ./gradlew dependencies --no-daemon

COPY src /app/src
RUN ./gradlew bootJar --no-daemon

# ==========================================
# STAGE 2: Extracting JAR's into layers
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS extract_stage
WORKDIR /app
COPY --from=build_stage /app/build/libs/*.jar app.jar

RUN java -Djarmode=layertools -jar app.jar extract

# ==========================================
# STAGE 3: Final image
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=extract_stage --chown=spring:spring /app/dependencies/ ./
COPY --from=extract_stage --chown=spring:spring /app/spring-boot-loader/ ./
COPY --from=extract_stage --chown=spring:spring /app/snapshot-dependencies/ ./
COPY --from=extract_stage --chown=spring:spring /app/application/ ./

COPY --chown=spring:spring entrypoint.sh /app/entrypoint.sh

RUN chmod +x /app/entrypoint.sh && sed -i 's/\r$//' /app/entrypoint.sh

USER spring
EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]