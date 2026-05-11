FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:25-jre

WORKDIR /app

RUN useradd --system --user-group throttlr
COPY --from=build /workspace/target/*.jar /app/throttlr.jar
RUN chown throttlr:throttlr /app/throttlr.jar

USER throttlr

ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/throttlr.jar"]
