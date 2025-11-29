FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# Select the repackaged Spring Boot jar and move it to build/target/app.jar
RUN set -eux; \
  for f in target/*.jar; do \
    [ -f "$f" ] || continue; \
    # A Spring Boot runnable jar contains BOOT-INF/ entries
    if unzip -l "$f" 2>/dev/null | grep -q "BOOT-INF/"; then \
      mv "$f" target/app.jar; \
      echo "Selected $f as app.jar (contains BOOT-INF)"; \
      break; \
    fi; \
  done; \
  if [ ! -f target/app.jar ]; then echo "No runnable Spring Boot jar found in target/*.jar" >&2; ls -l target || true; exit 1; fi

FROM eclipse-temurin:17-jre
# Copy the runnable jar to image root
COPY --from=build /build/target/app.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
