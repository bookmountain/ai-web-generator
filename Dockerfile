# syntax=docker/dockerfile:1

FROM node:22-alpine AS frontend-build
WORKDIR /workspace/client
COPY client/package*.json ./
RUN npm ci
COPY client/ ./
ARG VITE_API_BASE_URL=/api
ARG VITE_DEPLOY_DOMAIN=https://ai-web-generator.bookmountain.work
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_DEPLOY_DOMAIN=${VITE_DEPLOY_DOMAIN}
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /workspace
COPY pom.xml ./
COPY tools/mermaid/package*.json tools/mermaid/
RUN mvn -B -DskipTests dependency:go-offline
COPY src/ src/
COPY tools/mermaid/ tools/mermaid/
RUN mvn -B -DskipTests package

FROM nginx:1.29-alpine AS frontend-runtime
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=frontend-build /workspace/client/dist /usr/share/nginx/html
EXPOSE 80

FROM node:22-bookworm-slim AS node-runtime

FROM eclipse-temurin:21-jre-jammy AS backend-runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl fonts-liberation wget \
    && wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
    && apt-get install -y --no-install-recommends ./google-chrome-stable_current_amd64.deb \
    && rm -f google-chrome-stable_current_amd64.deb \
    && rm -rf /var/lib/apt/lists/*
COPY --from=node-runtime /usr/local/ /usr/local/
WORKDIR /app
COPY --from=backend-build /workspace/target/ai-web-generator-*.jar app.jar
COPY --from=backend-build /workspace/tools/mermaid/node_modules tools/mermaid/node_modules
RUN ln -s /app/tools/mermaid/node_modules/.bin/mmdc /usr/local/bin/mmdc \
    && useradd --system --create-home --uid 10001 app \
    && mkdir -p /app/tmp \
    && chown -R app:app /app
USER app
EXPOSE 8123
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
