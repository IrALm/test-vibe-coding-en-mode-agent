# syntax=docker/dockerfile:1

# Image runtime uniquement : le jar est construit en amont par le stage CI
# `build` (mvn package -DskipTests) et fourni dans le contexte de build via les
# artifacts GitLab (cf. .gitlab-ci.yml, build-api-image → needs: build). Pas de
# recompilation ici — elle a déjà eu lieu, la refaire serait redondant.
#
# Pour un build local sans passer par la CI : `mvn package -DskipTests` d'abord,
# puis `docker build .`.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
