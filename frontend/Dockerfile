# Build context is frontend/ itself (not the repo root):
#   docker build -t frontend frontend
# Exact tags, checked on Docker Hub (see docs/adr/012-docker-images.md).

FROM node:24.20.0-trixie-slim AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx ng build

# nginx-unprivileged: non-root by default (unlike official nginx), listens on 8080.
FROM nginxinc/nginx-unprivileged:1.30.5-alpine
COPY --from=build /build/dist/mini-erp-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
