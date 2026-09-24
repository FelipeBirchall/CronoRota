# Multi-stage build: o estágio "build" tem o Maven completo (pesado, ~500MB),
# mas a imagem final só leva o .jar já compilado sobre um JRE enxuto -
# é assim que se evita carregar ferramenta de build pra dentro do contêiner
# que vai rodar em produção.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
# Baixa as dependências antes de copiar o código-fonte: se só o código mudar
# (não o pom.xml), o Docker reaproveita essa camada em cache e não rebaixa
# tudo de novo a cada build - acelera bastante o ciclo de desenvolvimento.
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
