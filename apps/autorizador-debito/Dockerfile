# --- STAGE 1: protos-common (dependência compartilhada, ainda sem registry
# próprio - instala no cache Maven da imagem, do mesmo jeito que aconteceria
# localmente via ~/.m2). Build context é a raiz do monorepo. ---
FROM eclipse-temurin:25-jdk AS protos-common
WORKDIR /build
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
COPY protos-common ./protos-common
RUN mvn -q -f protos-common/pom.xml -DskipTests clean install

# --- STAGE 2: BUILD ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
COPY --from=protos-common /root/.m2 /root/.m2

COPY apps/autorizador-debito/pom.xml .
RUN mvn -q -DskipTests dependency:resolve
COPY apps/autorizador-debito/src ./src
RUN mvn -q -DskipTests clean package

# --- STAGE 3: RUNTIME ---
FROM eclipse-temurin:25-jre
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

ENV JAVA_TOOL_OPTIONS="\
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:+UseStringDeduplication \
  -XX:+DisableExplicitGC \
  -XX:InitialRAMPercentage=20 \
  -XX:MaxRAMPercentage=65 \
"

EXPOSE 8081 58081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]