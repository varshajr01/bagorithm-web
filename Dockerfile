FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B verify

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/bagorithm-web-1.0.0.jar /app/bagorithm-web.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/bagorithm-web.jar"]
