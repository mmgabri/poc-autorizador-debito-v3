# --- STAGE 1: debit-authorizer-protos (shared dependency, still without its own
# registry - installed into the image Maven cache, the same way it would happen
# locally via ~/.m2). The build context is the monorepo root. ---
FROM eclipse-temurin:25-jdk AS protos
WORKDIR /build
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
COPY apps/debit-authorizer-protos ./debit-authorizer-protos
RUN mvn -q -f debit-authorizer-protos/pom.xml -DskipTests clean install

# --- STAGE 2: BUILD ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
COPY --from=protos /root/.m2 /root/.m2

COPY apps/account-posting/pom.xml .
RUN mvn -q -DskipTests dependency:resolve
COPY apps/account-posting/src ./src
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
# Captures the internal IP of the ECS Task and injects it into the variable Spring reads
#ENTRYPOINT ["/bin/sh", "-c", "export INSTANCE_IP=$(hostname -i) && java $JAVA_TOOL_OPTIONS -jar /app/app.jar"]
# Works everywhere (local Docker or ECS/EKS):
ENTRYPOINT ["/bin/sh", "-c", "export INSTANCE_IP=$(hostname -i | awk '{print $1}') && java $JAVA_TOOL_OPTIONS -jar /app/app.jar"]