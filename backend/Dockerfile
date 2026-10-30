FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jre
RUN useradd --system --no-create-home rendezvous
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
USER rendezvous
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
