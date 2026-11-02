FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Dependencies first, so this layer is cached until the pom changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --no-create-home app
USER app

COPY --from=build /workspace/target/credit-engine-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
