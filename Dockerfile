FROM eclipse-temurin:26-jdk AS builder

WORKDIR /build

# Las dependencias se resuelven en su propia capa, de modo que un cambio que solo toque el código
# fuente no invalide la caché de Maven en cada reconstrucción.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests clean package

FROM eclipse-temurin:26-jre

WORKDIR /app

# La aplicación nunca necesita privilegios de root en ejecución.
RUN groupadd --system mineops && useradd --system --gid mineops mineops

COPY --from=builder --chown=mineops:mineops /build/target/*.jar app.jar

USER mineops
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
