# Build stage
FROM node:20-slim AS builder
WORKDIR /app
ENV PNPM_HOME=/root/.local/share/pnpm
ENV PATH=$PNPM_HOME:$PATH
RUN corepack enable
COPY package.json pnpm-lock.yaml .
COPY .env.production ./.env.production
COPY src ./src
COPY public ./public
COPY tsconfig*.json vite.config.ts postcss.config.js tailwind.config.ts .
COPY index.html ./
RUN pnpm install --frozen-lockfile
RUN pnpm build

# Serve stage
FROM nginx:1.27-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
