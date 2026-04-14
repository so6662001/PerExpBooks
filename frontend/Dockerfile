FROM node:22-alpine AS build
WORKDIR /app
RUN corepack enable && corepack prepare pnpm@latest --activate
COPY package.json pnpm-workspace.yaml ./
COPY pnpm-lock.yaml* ./
COPY packages/shared/package.json packages/shared/
COPY packages/mobile/package.json packages/mobile/
RUN pnpm install --no-frozen-lockfile
COPY packages/shared packages/shared
COPY packages/mobile packages/mobile
RUN pnpm --filter @qianku/mobile build

FROM nginx:alpine
COPY --from=build /app/packages/mobile/dist /usr/share/nginx/html
COPY nginx/mobile.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
