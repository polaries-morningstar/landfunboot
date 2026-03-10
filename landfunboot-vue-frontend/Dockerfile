# Stage 1: Build
FROM node:20-alpine AS build
WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm install

# Copy source code and build
COPY . .
RUN npm run build

# 将构建产物复制到 /built，启动时再复制到 volume /app/dist 供 nginx 使用
FROM alpine:latest
WORKDIR /app
COPY --from=build /app/dist ./built
RUN mkdir -p /app/dist
