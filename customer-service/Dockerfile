# syntax=docker/dockerfile:1.7-labs

############################
# STAGE 1: Build Maven
############################
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# Select runnable jar
RUN set -eux; \
  for f in target/*.jar; do \
    [ -f "$f" ] || continue; \
    if unzip -l "$f" 2>/dev/null | grep -q "BOOT-INF/"; then \
      mv "$f" target/app.jar; \
      echo "Selected $f as app.jar (contains BOOT-INF)"; \
      break; \
    fi; \
  done; \
  if [ ! -f target/app.jar ]; then echo "No runnable Spring Boot jar found in target/*.jar" >&2; exit 1; fi

############################
# STAGE 2: Runtime (slim, non-root)
############################
FROM eclipse-temurin:17-jre
COPY --from=build /build/target/app.jar /app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app.jar"]
