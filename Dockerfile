# Stage 1: Build stage com cache de dependências
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copiar apenas pom.xml primeiro para cache de dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fonte e compilar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar JAR compilado
COPY --from=build /app/target/*.jar app.jar

# Variáveis de ambiente opcionais
ENV JAVA_OPTS=""

EXPOSE 8080

# Usar exec form para melhor handling de sinais
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
