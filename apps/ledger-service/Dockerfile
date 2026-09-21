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

COPY apps/ledger-service/pom.xml .
RUN mvn -q -DskipTests dependency:resolve
COPY apps/ledger-service/src ./src
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
  -XX:MaxRAMPercentage=75 \
"

EXPOSE 8086 58086
#ENTRYPOINT ["java", "-jar", "/app/app.jar"]
# Captura o IP interno da Task do ECS e injeta na variável que o Spring lê
#ENTRYPOINT ["/bin/sh", "-c", "export INSTANCIA_IP=$(hostname -i) && java $JAVA_TOOL_OPTIONS -jar /app/app.jar"]
# O segredo para funcionar em qualquer lugar (Docker local ou ECS):
ENTRYPOINT ["/bin/sh", "-c", "export INSTANCIA_IP=$(hostname -i | awk '{print $1}') && java $JAVA_TOOL_OPTIONS -jar /app/app.jar"]