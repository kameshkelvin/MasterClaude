# Next.js 前端应用 Dockerfile
# 多阶段构建，优化生产环境镜像

# ====================
# 依赖安装阶段
# ====================
FROM node:18-alpine AS deps

LABEL maintainer="Exam System Team <team@exam-system.com>"
LABEL description="在线考试系统 Next.js 前端应用"

# 安装 libc6-compat 以解决某些 Node.js 包的兼容性问题
RUN apk add --no-cache libc6-compat

WORKDIR /app

# 复制 package 文件
COPY package.json package-lock.json* ./

# 安装依赖
RUN npm ci --only=production --frozen-lockfile && npm cache clean --force

# ====================
# 构建阶段
# ====================
FROM node:18-alpine AS builder

WORKDIR /app

# 从依赖阶段复制 node_modules
COPY --from=deps /app/node_modules ./node_modules

# 复制源代码
COPY . .

# 设置构建环境变量
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

# 构建应用
RUN npm run build

# ====================
# 生产运行阶段
# ====================
FROM node:18-alpine AS production

# 安装必要的系统包
RUN apk add --no-cache \
    curl \
    ca-certificates \
    && rm -rf /var/cache/apk/*

# 创建非 root 用户
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nextjs -u 1001

WORKDIR /app

# 设置生产环境
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000

# 复制构建产物和必要文件
COPY --from=builder /app/public ./public
COPY --from=builder /app/package.json ./package.json

# 复制构建输出
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

# 健康检查脚本
COPY --chown=nextjs:nodejs docker/healthcheck.sh ./healthcheck.sh
RUN chmod +x ./healthcheck.sh

# 切换到非 root 用户
USER nextjs

# 暴露端口
EXPOSE 3000

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD ./healthcheck.sh

# 启动命令
CMD ["node", "server.js"]

# ====================
# 开发环境阶段
# ====================
FROM node:18-alpine AS development

WORKDIR /app

# 安装开发依赖
RUN apk add --no-cache libc6-compat

# 复制 package 文件
COPY package.json package-lock.json* ./

# 安装所有依赖（包括开发依赖）
RUN npm ci

# 复制源代码
COPY . .

# 设置开发环境变量
ENV NODE_ENV=development
ENV NEXT_TELEMETRY_DISABLED=1

# 暴露端口和调试端口
EXPOSE 3000 9229

# 开发模式启动命令
CMD ["npm", "run", "dev"]