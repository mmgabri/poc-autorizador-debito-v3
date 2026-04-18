# --- STAGE 1: BUILD ---
FROM maven:3.9.9-eclipse-temurin-23 AS build

ENV PORT=8080
ENV GRPC_PORT=58080

WORKDIR /app

# Copia apenas o pom.xml primeiro para caching das dependências
COPY pom.xml .
RUN mvn -q -DskipTests dependency:resolve
# Agora copia o restante do código
COPY src ./src

# Build
RUN mvn -q -DskipTests clean package


# --- STAGE 2: RUNTIME ---
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV JAVA_OPTS="\
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:+UseStringDeduplication \
  -XX:+UseCompressedOops \
  -XX:+AlwaysActAsServerClassMachine \
  -XX:+TieredCompilation \
  -XX:TieredStopAtLevel=1 \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+DisableExplicitGC \
  -XX:+UseContainerSupport \
  -XX:InitialRAMPercentage=40 \
  -XX:MaxRAMPercentage=75 \
  -Djava.security.egd=file:/dev/./urandom \
"

EXPOSE 8080
EXPOSE 58080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
