# 在线考试系统 - 构建架构设计

## 构建概述

基于系统设计文档分析，设计企业级构建架构，支持微服务多环境部署、自动化CI/CD流水线、容器化编排、监控与安全集成。

### 核心构建原则
- **云原生架构**: 容器化部署、Kubernetes编排
- **多环境支持**: 开发、测试、预生产、生产环境隔离
- **自动化流水线**: GitOps工作流、零停机部署
- **安全内建**: 镜像扫描、密钥管理、合规检查
- **可观测性**: 构建监控、性能追踪、错误报告

---

## 1. 项目结构设计

### 1.1 整体项目架构

```
exam-system/
├── services/                    # 微服务目录
│   ├── auth-service/           # 认证服务
│   │   ├── src/
│   │   ├── tests/
│   │   ├── Dockerfile
│   │   ├── package.json
│   │   └── .dockerignore
│   ├── exam-service/           # 考试服务
│   ├── question-service/       # 题库服务
│   ├── proctoring-service/     # 监考服务
│   ├── analytics-service/      # 分析服务
│   └── notification-service/   # 通知服务
├── frontend/                   # 前端应用
│   ├── src/
│   ├── public/
│   ├── Dockerfile
│   ├── package.json
│   └── nginx.conf
├── infrastructure/             # 基础设施代码
│   ├── docker-compose/
│   ├── kubernetes/
│   ├── terraform/
│   └── helm/
├── ci-cd/                      # CI/CD配置
│   ├── .github/workflows/
│   ├── .gitlab-ci.yml
│   ├── jenkins/
│   └── scripts/
├── monitoring/                 # 监控配置
│   ├── prometheus/
│   ├── grafana/
│   └── alertmanager/
├── docs/                       # 文档
├── scripts/                    # 构建脚本
│   ├── build.sh
│   ├── deploy.sh
│   ├── test.sh
│   └── setup-env.sh
├── Makefile                    # 统一构建入口
├── docker-compose.yml          # 本地开发环境
├── docker-compose.prod.yml     # 生产环境配置
└── README.md
```

### 1.2 微服务构建配置

#### 认证服务 (Node.js)
```json
{
  "name": "@exam-system/auth-service",
  "version": "1.0.0",
  "description": "Authentication and authorization service",
  "main": "src/index.js",
  "scripts": {
    "dev": "nodemon src/index.js",
    "start": "node src/index.js",
    "build": "webpack --mode production",
    "test": "jest --coverage",
    "test:integration": "jest --config jest.integration.config.js",
    "lint": "eslint src/ --fix",
    "docker:build": "docker build -t exam-system/auth-service:${VERSION:-latest} .",
    "docker:push": "docker push exam-system/auth-service:${VERSION:-latest}",
    "k8s:deploy": "kubectl apply -f kubernetes/auth-service.yaml"
  },
  "dependencies": {
    "express": "^4.18.2",
    "jsonwebtoken": "^9.0.2",
    "bcryptjs": "^2.4.3",
    "mongoose": "^7.5.0",
    "redis": "^4.6.8",
    "helmet": "^7.0.0",
    "cors": "^2.8.5",
    "express-rate-limit": "^6.8.1"
  },
  "devDependencies": {
    "nodemon": "^3.0.1",
    "jest": "^29.6.2",
    "supertest": "^6.3.3",
    "eslint": "^8.47.0",
    "webpack": "^5.88.2",
    "webpack-cli": "^5.1.4"
  },
  "engines": {
    "node": ">=18.0.0",
    "npm": ">=9.0.0"
  }
}
```

#### 考试服务 (Python)
```python
# requirements.txt
fastapi==0.103.0
uvicorn[standard]==0.23.2
pydantic==2.3.0
sqlalchemy==2.0.20
alembic==1.11.3
psycopg2-binary==2.9.7
redis==4.6.0
celery==5.3.1
pytest==7.4.0
pytest-asyncio==0.21.1
httpx==0.24.1
python-multipart==0.0.6
python-jose[cryptography]==3.3.0
passlib[bcrypt]==1.7.4
```

```python
# pyproject.toml
[build-system]
requires = ["poetry-core>=1.0.0"]
build-backend = "poetry.core.masonry.api"

[tool.poetry]
name = "exam-service"
version = "1.0.0"
description = "Exam management service"
authors = ["Exam System Team"]

[tool.poetry.scripts]
start = "exam_service.main:app"
test = "pytest"
lint = "black . && isort . && flake8"

[tool.poetry.dependencies]
python = "^3.11"
fastapi = "^0.103.0"
uvicorn = {extras = ["standard"], version = "^0.23.2"}
sqlalchemy = "^2.0.20"
alembic = "^1.11.3"

[tool.poetry.group.dev.dependencies]
pytest = "^7.4.0"
pytest-asyncio = "^0.21.1"
black = "^23.7.0"
isort = "^5.12.0"
flake8 = "^6.0.0"
```

#### 前端应用 (React)
```json
{
  "name": "@exam-system/frontend",
  "version": "1.0.0",
  "description": "Exam system frontend application",
  "private": true,
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "build:staging": "vite build --mode staging",
    "build:production": "vite build --mode production",
    "preview": "vite preview",
    "test": "vitest",
    "test:e2e": "playwright test",
    "lint": "eslint src --ext ts,tsx --report-unused-disable-directives --max-warnings 0",
    "type-check": "tsc --noEmit",
    "docker:build": "docker build -t exam-system/frontend:${VERSION:-latest} .",
    "analyze": "npx vite-bundle-analyzer"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.15.0",
    "zustand": "^4.4.1",
    "react-query": "^3.39.3",
    "@radix-ui/react-dialog": "^1.0.4",
    "@radix-ui/react-dropdown-menu": "^2.0.5",
    "framer-motion": "^10.16.1",
    "lucide-react": "^0.263.1",
    "tailwindcss": "^3.3.3"
  },
  "devDependencies": {
    "@types/react": "^18.2.15",
    "@types/react-dom": "^18.2.7",
    "@typescript-eslint/eslint-plugin": "^6.0.0",
    "@typescript-eslint/parser": "^6.0.0",
    "@vitejs/plugin-react": "^4.0.3",
    "eslint": "^8.45.0",
    "eslint-plugin-react-hooks": "^4.6.0",
    "eslint-plugin-react-refresh": "^0.4.3",
    "typescript": "^5.0.2",
    "vite": "^4.4.5",
    "vitest": "^0.34.0",
    "@playwright/test": "^1.37.1",
    "vite-bundle-analyzer": "^0.7.0"
  },
  "engines": {
    "node": ">=18.0.0",
    "npm": ">=9.0.0"
  }
}
```

---

## 2. 容器化构建

### 2.1 Docker构建策略

#### 多阶段构建 - 前端应用
```dockerfile
# frontend/Dockerfile
# Stage 1: Build stage
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./
COPY tsconfig.json ./
COPY vite.config.ts ./

# Install dependencies
RUN npm ci --only=production && npm cache clean --force

# Copy source code
COPY src/ ./src/
COPY public/ ./public/
COPY index.html ./

# Build application
RUN npm run build

# Stage 2: Production stage
FROM nginx:alpine AS production

# Install security updates
RUN apk update && apk upgrade && apk add --no-cache curl

# Copy built assets
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Create non-root user
RUN addgroup -g 1001 -S nginx && \
    adduser -S nginx -u 1001

# Set ownership
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    chown -R nginx:nginx /etc/nginx/conf.d

# Switch to non-root user
USER nginx

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost/ || exit 1

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

#### 优化构建 - Node.js服务
```dockerfile
# services/auth-service/Dockerfile
FROM node:18-alpine AS base

# Install security updates and dependencies
RUN apk update && apk upgrade && \
    apk add --no-cache dumb-init && \
    rm -rf /var/cache/apk/*

# Create app directory
WORKDIR /app

# Copy package files
COPY package*.json ./

# Stage 1: Dependencies
FROM base AS dependencies
RUN npm ci --only=production && npm cache clean --force

# Stage 2: Build
FROM base AS build
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build && npm prune --production

# Stage 3: Production
FROM base AS production

# Create non-root user
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nodejs -u 1001

# Copy built application
COPY --from=build --chown=nodejs:nodejs /app/dist ./dist
COPY --from=build --chown=nodejs:nodejs /app/node_modules ./node_modules
COPY --from=build --chown=nodejs:nodejs /app/package.json ./

# Switch to non-root user
USER nodejs

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD node healthcheck.js

EXPOSE 3000

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]
CMD ["node", "dist/index.js"]
```

#### 高性能构建 - Python服务
```dockerfile
# services/exam-service/Dockerfile
FROM python:3.11-slim AS base

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    libc6-dev \
    libpq-dev \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set environment variables
ENV PYTHONUNBUFFERED=1 \
    PYTHONHASHSEED=random \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

WORKDIR /app

# Stage 1: Dependencies
FROM base AS dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Stage 2: Production
FROM base AS production

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copy installed packages
COPY --from=dependencies /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY --from=dependencies /usr/local/bin /usr/local/bin

# Copy application code
COPY --chown=appuser:appuser . .

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8000/health || exit 1

EXPOSE 8000

# Use uvicorn with production settings
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "4"]
```

### 2.2 Docker Compose配置

#### 开发环境
```yaml
# docker-compose.yml
version: '3.8'

services:
  # Databases
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: exam_system
      POSTGRES_USER: exam_user
      POSTGRES_PASSWORD: dev_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U exam_user -d exam_system"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes --requirepass dev_password
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Message Queue
  rabbitmq:
    image: rabbitmq:3-management-alpine
    environment:
      RABBITMQ_DEFAULT_USER: exam_user
      RABBITMQ_DEFAULT_PASS: dev_password
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"

  # Services
  auth-service:
    build:
      context: ./services/auth-service
      dockerfile: Dockerfile
      target: development
    environment:
      NODE_ENV: development
      DATABASE_URL: postgresql://exam_user:dev_password@postgres:5432/exam_system
      REDIS_URL: redis://:dev_password@redis:6379/0
      JWT_SECRET: dev-jwt-secret
    volumes:
      - ./services/auth-service:/app
      - /app/node_modules
    ports:
      - "3001:3000"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy

  exam-service:
    build:
      context: ./services/exam-service
      dockerfile: Dockerfile
      target: development
    environment:
      DATABASE_URL: postgresql://exam_user:dev_password@postgres:5432/exam_system
      REDIS_URL: redis://:dev_password@redis:6379/1
      RABBITMQ_URL: amqp://exam_user:dev_password@rabbitmq:5672/
    volumes:
      - ./services/exam-service:/app
    ports:
      - "3002:8000"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_started

  # Frontend
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
      target: development
    environment:
      VITE_API_BASE_URL: http://localhost:8000/api/v1
      VITE_WS_URL: ws://localhost:8000/ws
    volumes:
      - ./frontend:/app
      - /app/node_modules
    ports:
      - "3000:3000"

volumes:
  postgres_data:
  redis_data:
  rabbitmq_data:

networks:
  default:
    name: exam-system-dev
```

#### 生产环境
```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  # Load Balancer
  nginx:
    image: nginx:alpine
    volumes:
      - ./infrastructure/nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./infrastructure/nginx/ssl:/etc/nginx/ssl
    ports:
      - "80:80"
      - "443:443"
    depends_on:
      - frontend
      - auth-service
      - exam-service

  # Services
  auth-service:
    image: exam-system/auth-service:${VERSION}
    environment:
      NODE_ENV: production
      DATABASE_URL: ${DATABASE_URL}
      REDIS_URL: ${REDIS_URL}
      JWT_SECRET: ${JWT_SECRET}
    deploy:
      replicas: 3
      resources:
        limits:
          memory: 512M
          cpus: '0.5'
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3000/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  exam-service:
    image: exam-system/exam-service:${VERSION}
    environment:
      DATABASE_URL: ${DATABASE_URL}
      REDIS_URL: ${REDIS_URL}
      RABBITMQ_URL: ${RABBITMQ_URL}
    deploy:
      replicas: 5
      resources:
        limits:
          memory: 1G
          cpus: '1.0'

  frontend:
    image: exam-system/frontend:${VERSION}
    deploy:
      replicas: 2
      resources:
        limits:
          memory: 128M
          cpus: '0.25'

networks:
  default:
    external: true
    name: exam-system-prod
```

---

## 3. Kubernetes部署配置

### 3.1 命名空间和配置

```yaml
# infrastructure/kubernetes/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: exam-system
  labels:
    name: exam-system
    version: v1.0.0

---
# ConfigMap for application configuration  
apiVersion: v1
kind: ConfigMap
metadata:
  name: exam-config
  namespace: exam-system
data:
  # Database configuration
  POSTGRES_HOST: "postgres-service"
  POSTGRES_PORT: "5432"
  POSTGRES_DB: "exam_system"
  
  # Redis configuration
  REDIS_HOST: "redis-service"
  REDIS_PORT: "6379"
  
  # Application configuration
  LOG_LEVEL: "INFO"
  MAX_UPLOAD_SIZE: "100MB"
  EXAM_TIMEOUT_MINUTES: "180"
  
  # Feature flags
  ENABLE_MONITORING: "true"
  ENABLE_PROCTORING: "true"
  ENABLE_ANALYTICS: "true"

---
# Secrets for sensitive data
apiVersion: v1
kind: Secret
metadata:
  name: exam-secrets
  namespace: exam-system
type: Opaque
stringData:
  # Database credentials
  POSTGRES_USER: "exam_user"
  POSTGRES_PASSWORD: "secure_production_password"
  
  # JWT secrets
  JWT_SECRET: "production-jwt-secret-key-256-bit"
  JWT_REFRESH_SECRET: "production-refresh-secret-key-256-bit"
  
  # Redis password
  REDIS_PASSWORD: "redis_production_password"
  
  # External services
  AWS_ACCESS_KEY_ID: "AKIA..."
  AWS_SECRET_ACCESS_KEY: "..."
  
  # Monitoring
  GRAFANA_ADMIN_PASSWORD: "grafana_admin_password"
```

### 3.2 数据库部署

```yaml
# infrastructure/kubernetes/database.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: exam-system
spec:
  serviceName: postgres-service
  replicas: 3
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      securityContext:
        fsGroup: 999
      containers:
      - name: postgres
        image: postgres:15-alpine
        ports:
        - containerPort: 5432
          name: postgres
        env:
        - name: POSTGRES_DB
          valueFrom:
            configMapKeyRef:
              name: exam-config
              key: POSTGRES_DB
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: exam-secrets
              key: POSTGRES_USER
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: exam-secrets
              key: POSTGRES_PASSWORD
        - name: PGDATA
          value: /var/lib/postgresql/data/pgdata
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          exec:
            command:
              - pg_isready
              - -U
              - $(POSTGRES_USER)
              - -d
              - $(POSTGRES_DB)
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
              - pg_isready
              - -U
              - $(POSTGRES_USER)
              - -d
              - $(POSTGRES_DB)
          initialDelaySeconds: 5
          periodSeconds: 5
  volumeClaimTemplates:
  - metadata:
      name: postgres-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: fast-ssd
      resources:
        requests:
          storage: 100Gi

---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: exam-system
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
  type: ClusterIP
```

### 3.3 应用服务部署

```yaml
# infrastructure/kubernetes/auth-service.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: exam-system
  labels:
    app: auth-service
    version: v1.0.0
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
        version: v1.0.0
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "3000"
        prometheus.io/path: "/metrics"
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 1001
        fsGroup: 1001
      containers:
      - name: auth-service
        image: exam-system/auth-service:1.0.0
        imagePullPolicy: Always
        ports:
        - containerPort: 3000
          name: http
        env:
        - name: NODE_ENV
          value: "production"
        - name: PORT
          value: "3000"
        - name: DATABASE_URL
          value: "postgresql://$(POSTGRES_USER):$(POSTGRES_PASSWORD)@postgres-service:5432/$(POSTGRES_DB)"
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: exam-secrets
              key: POSTGRES_USER
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: exam-secrets
              key: POSTGRES_PASSWORD
        - name: POSTGRES_DB
          valueFrom:
            configMapKeyRef:
              name: exam-config
              key: POSTGRES_DB
        - name: REDIS_URL
          value: "redis://:$(REDIS_PASSWORD)@redis-service:6379/0"
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: exam-secrets
              key: REDIS_PASSWORD
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: exam-secrets
              key: JWT_SECRET
        envFrom:
        - configMapRef:
            name: exam-config
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /health
            port: 3000
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /ready
            port: 3000
          initialDelaySeconds: 5
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        securityContext:
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
          capabilities:
            drop:
            - ALL

---
apiVersion: v1
kind: Service
metadata:
  name: auth-service
  namespace: exam-system
  labels:
    app: auth-service
spec:
  selector:
    app: auth-service
  ports:
  - port: 80
    targetPort: 3000
    protocol: TCP
    name: http
  type: ClusterIP

---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: auth-service-hpa
  namespace: exam-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: auth-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
```

### 3.4 Ingress配置

```yaml
# infrastructure/kubernetes/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: exam-system-ingress
  namespace: exam-system
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "100m"
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
    nginx.ingress.kubernetes.io/enable-cors: "true"
    nginx.ingress.kubernetes.io/cors-allow-origin: "https://exam.yourdomain.com"
    nginx.ingress.kubernetes.io/cors-allow-methods: "GET, POST, PUT, DELETE, OPTIONS"
    nginx.ingress.kubernetes.io/cors-allow-headers: "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization"
spec:
  tls:
  - hosts:
    - api.exam.yourdomain.com
    - app.exam.yourdomain.com
    secretName: exam-system-tls
  rules:
  - host: api.exam.yourdomain.com
    http:
      paths:
      - path: /api/v1/auth
        pathType: Prefix
        backend:
          service:
            name: auth-service
            port:
              number: 80
      - path: /api/v1/exams
        pathType: Prefix
        backend:
          service:
            name: exam-service
            port:
              number: 80
      - path: /api/v1/questions
        pathType: Prefix
        backend:
          service:
            name: question-service
            port:
              number: 80
      - path: /api/v1/proctoring
        pathType: Prefix
        backend:
          service:
            name: proctoring-service
            port:
              number: 80
  - host: app.exam.yourdomain.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend-service
            port:
              number: 80
```

---

## 4. CI/CD流水线

### 4.1 GitHub Actions配置

```yaml
# .github/workflows/ci-cd.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
  release:
    types: [published]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: exam-system

jobs:
  # 代码质量检查
  quality-check:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [auth-service, exam-service, question-service, proctoring-service, frontend]
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Setup Node.js
      if: contains(matrix.service, 'service') && matrix.service != 'exam-service'
      uses: actions/setup-node@v4
      with:
        node-version: '18'
        cache: 'npm'
        cache-dependency-path: services/${{ matrix.service }}/package-lock.json
        
    - name: Setup Python
      if: matrix.service == 'exam-service'
      uses: actions/setup-python@v4
      with:
        python-version: '3.11'
        
    - name: Setup Node.js for Frontend
      if: matrix.service == 'frontend'
      uses: actions/setup-node@v4
      with:
        node-version: '18'
        cache: 'npm'
        cache-dependency-path: frontend/package-lock.json

    - name: Install dependencies (Node.js services)
      if: contains(matrix.service, 'service') && matrix.service != 'exam-service'
      working-directory: services/${{ matrix.service }}
      run: npm ci

    - name: Install dependencies (Python service)
      if: matrix.service == 'exam-service'
      working-directory: services/${{ matrix.service }}
      run: |
        python -m pip install --upgrade pip
        pip install -r requirements.txt
        pip install -r requirements-dev.txt

    - name: Install dependencies (Frontend)
      if: matrix.service == 'frontend'
      working-directory: frontend
      run: npm ci

    - name: Lint (Node.js)
      if: contains(matrix.service, 'service') && matrix.service != 'exam-service'
      working-directory: services/${{ matrix.service }}
      run: npm run lint

    - name: Lint (Python)
      if: matrix.service == 'exam-service'
      working-directory: services/${{ matrix.service }}
      run: |
        black --check .
        isort --check-only .
        flake8 .

    - name: Lint (Frontend)
      if: matrix.service == 'frontend'
      working-directory: frontend
      run: npm run lint

    - name: Type check
      if: matrix.service == 'frontend'
      working-directory: frontend
      run: npm run type-check

    - name: Run tests (Node.js)
      if: contains(matrix.service, 'service') && matrix.service != 'exam-service'
      working-directory: services/${{ matrix.service }}
      run: npm test -- --coverage

    - name: Run tests (Python)
      if: matrix.service == 'exam-service'
      working-directory: services/${{ matrix.service }}
      run: pytest --cov=. --cov-report=xml

    - name: Run tests (Frontend)
      if: matrix.service == 'frontend'
      working-directory: frontend
      run: npm test

    - name: Upload coverage reports
      uses: codecov/codecov-action@v3
      with:
        file: |
          services/${{ matrix.service }}/coverage/lcov.info
          services/${{ matrix.service }}/coverage.xml
          frontend/coverage/lcov.info

  # 安全扫描
  security-scan:
    runs-on: ubuntu-latest
    needs: quality-check
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Run Trivy vulnerability scanner
      uses: aquasecurity/trivy-action@master
      with:
        scan-type: 'fs'
        scan-ref: '.'
        format: 'sarif'
        output: 'trivy-results.sarif'
        
    - name: Upload Trivy scan results
      uses: github/codeql-action/upload-sarif@v2
      with:
        sarif_file: 'trivy-results.sarif'

    - name: Run Snyk security scan
      uses: snyk/actions/node@master
      env:
        SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
      with:
        args: --all-projects --severity-threshold=high

  # 构建和推送镜像
  build-and-push:
    runs-on: ubuntu-latest
    needs: [quality-check, security-scan]
    if: github.event_name == 'push' || github.event_name == 'release'
    
    strategy:
      matrix:
        service: [auth-service, exam-service, question-service, proctoring-service, frontend]
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v3
      
    - name: Log in to Container Registry
      uses: docker/login-action@v3
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
        
    - name: Extract metadata
      id: meta
      uses: docker/metadata-action@v5
      with:
        images: ${{ env.REGISTRY }}/${{ github.repository }}/${{ matrix.service }}
        tags: |
          type=ref,event=branch
          type=ref,event=pr
          type=sha,prefix={{branch}}-
          type=semver,pattern={{version}}
          type=semver,pattern={{major}}.{{minor}}

    - name: Build and push Docker image
      uses: docker/build-push-action@v5
      with:
        context: ${{ matrix.service == 'frontend' && './frontend' || format('./services/{0}', matrix.service) }}
        platforms: linux/amd64,linux/arm64
        push: true
        tags: ${{ steps.meta.outputs.tags }}
        labels: ${{ steps.meta.outputs.labels }}
        cache-from: type=gha
        cache-to: type=gha,mode=max
        build-args: |
          VERSION=${{ steps.meta.outputs.version }}
          BUILD_DATE=${{ steps.meta.outputs.created }}
          VCS_REF=${{ github.sha }}

  # 部署到测试环境
  deploy-staging:
    runs-on: ubuntu-latest
    needs: build-and-push
    if: github.ref == 'refs/heads/develop'
    environment: staging
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Configure kubectl
      uses: azure/k8s-set-context@v3
      with:
        method: kubeconfig
        kubeconfig: ${{ secrets.KUBE_CONFIG_STAGING }}
        
    - name: Deploy to staging
      run: |
        envsubst < infrastructure/kubernetes/staging/kustomization.yaml | kubectl apply -f -
        kubectl rollout status deployment/auth-service -n exam-system-staging
        kubectl rollout status deployment/exam-service -n exam-system-staging
        kubectl rollout status deployment/frontend -n exam-system-staging

    - name: Run smoke tests
      run: |
        kubectl wait --for=condition=ready pod -l app=auth-service -n exam-system-staging --timeout=300s
        kubectl wait --for=condition=ready pod -l app=exam-service -n exam-system-staging --timeout=300s
        npm run test:smoke -- --env=staging

  # E2E测试
  e2e-tests:
    runs-on: ubuntu-latest
    needs: deploy-staging
    if: github.ref == 'refs/heads/develop'
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Setup Node.js
      uses: actions/setup-node@v4
      with:
        node-version: '18'
        cache: 'npm'
        cache-dependency-path: frontend/package-lock.json
        
    - name: Install Playwright
      working-directory: frontend
      run: |
        npm ci
        npx playwright install --with-deps
        
    - name: Run E2E tests
      working-directory: frontend
      env:
        BASE_URL: https://staging.exam.yourdomain.com
      run: npx playwright test
      
    - name: Upload test results
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: playwright-report
        path: frontend/playwright-report/

  # 部署到生产环境
  deploy-production:
    runs-on: ubuntu-latest
    needs: [build-and-push, e2e-tests]
    if: github.event_name == 'release'
    environment: production
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Configure kubectl
      uses: azure/k8s-set-context@v3
      with:
        method: kubeconfig
        kubeconfig: ${{ secrets.KUBE_CONFIG_PRODUCTION }}
        
    - name: Deploy to production
      run: |
        envsubst < infrastructure/kubernetes/production/kustomization.yaml | kubectl apply -f -
        kubectl rollout status deployment/auth-service -n exam-system --timeout=600s
        kubectl rollout status deployment/exam-service -n exam-system --timeout=600s
        kubectl rollout status deployment/frontend -n exam-system --timeout=600s

    - name: Verify deployment
      run: |
        kubectl get pods -n exam-system
        kubectl get services -n exam-system
        kubectl get ingress -n exam-system

    - name: Run production health checks
      run: |
        curl -f https://api.exam.yourdomain.com/health
        curl -f https://app.exam.yourdomain.com/
```

### 4.2 构建脚本

```bash
#!/bin/bash
# scripts/build.sh

set -euo pipefail

# 配置变量
PROJECT_NAME="exam-system"
REGISTRY="${REGISTRY:-ghcr.io/yourusername}"
VERSION="${VERSION:-$(git rev-parse --short HEAD)}"
ENVIRONMENT="${ENVIRONMENT:-development}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
    exit 1
}

# 检查依赖
check_dependencies() {
    log "Checking dependencies..."
    
    command -v docker >/dev/null 2>&1 || error "Docker is required but not installed"
    command -v docker-compose >/dev/null 2>&1 || error "Docker Compose is required but not installed"
    command -v kubectl >/dev/null 2>&1 || warn "kubectl not found - Kubernetes deployment will not be available"
    
    log "Dependencies check completed"
}

# 构建单个服务
build_service() {
    local service=$1
    local context_path=$2
    
    log "Building $service..."
    
    # 构建Docker镜像
    docker build \
        --build-arg VERSION="$VERSION" \
        --build-arg BUILD_DATE="$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
        --build-arg VCS_REF="$(git rev-parse HEAD)" \
        --target production \
        -t "$REGISTRY/$PROJECT_NAME/$service:$VERSION" \
        -t "$REGISTRY/$PROJECT_NAME/$service:latest" \
        "$context_path"
    
    # 安全扫描
    if command -v trivy >/dev/null 2>&1; then
        log "Running security scan for $service..."
        trivy image --exit-code 0 --severity HIGH,CRITICAL "$REGISTRY/$PROJECT_NAME/$service:$VERSION"
    fi
    
    log "$service build completed"
}

# 构建所有服务
build_all_services() {
    log "Building all services..."
    
    # 构建微服务
    build_service "auth-service" "./services/auth-service"
    build_service "exam-service" "./services/exam-service"
    build_service "question-service" "./services/question-service"
    build_service "proctoring-service" "./services/proctoring-service"
    build_service "analytics-service" "./services/analytics-service"
    build_service "notification-service" "./services/notification-service"
    
    # 构建前端
    build_service "frontend" "./frontend"
    
    log "All services built successfully"
}

# 推送镜像
push_images() {
    if [[ "$ENVIRONMENT" != "development" ]]; then
        log "Pushing images to registry..."
        
        local services=("auth-service" "exam-service" "question-service" "proctoring-service" "analytics-service" "notification-service" "frontend")
        
        for service in "${services[@]}"; do
            docker push "$REGISTRY/$PROJECT_NAME/$service:$VERSION"
            docker push "$REGISTRY/$PROJECT_NAME/$service:latest"
        done
        
        log "Images pushed successfully"
    else
        warn "Skipping image push in development environment"
    fi
}

# 运行测试
run_tests() {
    log "Running tests..."
    
    # 运行单元测试
    docker-compose -f docker-compose.test.yml up --build --abort-on-container-exit --exit-code-from test-runner
    
    # 清理测试容器
    docker-compose -f docker-compose.test.yml down -v
    
    log "Tests completed successfully"
}

# 部署到Kubernetes
deploy_k8s() {
    if command -v kubectl >/dev/null 2>&1; then
        log "Deploying to Kubernetes..."
        
        # 应用Kubernetes配置
        kubectl apply -k "infrastructure/kubernetes/overlays/$ENVIRONMENT"
        
        # 等待部署完成
        kubectl rollout status deployment/auth-service -n exam-system-$ENVIRONMENT
        kubectl rollout status deployment/exam-service -n exam-system-$ENVIRONMENT
        kubectl rollout status deployment/frontend -n exam-system-$ENVIRONMENT
        
        log "Kubernetes deployment completed"
    else
        warn "kubectl not available - skipping Kubernetes deployment"
    fi
}

# 主函数
main() {
    log "Starting build process for $PROJECT_NAME v$VERSION"
    
    check_dependencies
    
    case "${1:-all}" in
        "test")
            run_tests
            ;;
        "build")
            build_all_services
            ;;
        "push")
            push_images
            ;;
        "deploy")
            deploy_k8s
            ;;
        "all")
            run_tests
            build_all_services
            push_images
            deploy_k8s
            ;;
        *)
            error "Unknown command: $1. Available commands: test, build, push, deploy, all"
            ;;
    esac
    
    log "Build process completed successfully"
}

# 执行主函数
main "$@"
```

---

## 5. 监控和可观测性

### 5.1 Prometheus配置

```yaml
# monitoring/prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: 'exam-system-prod'
    environment: 'production'

rule_files:
  - "rules/*.yml"

alerting:
  alertmanagers:
  - static_configs:
    - targets:
      - alertmanager:9093

scrape_configs:
  # Kubernetes API server
  - job_name: 'kubernetes-apiservers'
    kubernetes_sd_configs:
    - role: endpoints
    scheme: https
    tls_config:
      ca_file: /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
    bearer_token_file: /var/run/secrets/kubernetes.io/serviceaccount/token
    relabel_configs:
    - source_labels: [__meta_kubernetes_namespace, __meta_kubernetes_service_name, __meta_kubernetes_endpoint_port_name]
      action: keep
      regex: default;kubernetes;https

  # Kubernetes nodes
  - job_name: 'kubernetes-nodes'
    kubernetes_sd_configs:
    - role: node
    scheme: https
    tls_config:
      ca_file: /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
    bearer_token_file: /var/run/secrets/kubernetes.io/serviceaccount/token
    relabel_configs:
    - action: labelmap
      regex: __meta_kubernetes_node_label_(.+)

  # Application services
  - job_name: 'exam-services'
    kubernetes_sd_configs:
    - role: endpoints
      namespaces:
        names:
        - exam-system
    relabel_configs:
    - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_scrape]
      action: keep
      regex: true
    - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_path]
      action: replace
      target_label: __metrics_path__
      regex: (.+)
    - source_labels: [__address__, __meta_kubernetes_service_annotation_prometheus_io_port]
      action: replace
      regex: ([^:]+)(?::\d+)?;(\d+)
      replacement: $1:$2
      target_label: __address__
    - action: labelmap
      regex: __meta_kubernetes_service_label_(.+)
    - source_labels: [__meta_kubernetes_namespace]
      action: replace
      target_label: kubernetes_namespace
    - source_labels: [__meta_kubernetes_service_name]
      action: replace
      target_label: kubernetes_name

  # Database monitoring
  - job_name: 'postgres-exporter'
    static_configs:
    - targets: ['postgres-exporter:9187']
    
  - job_name: 'redis-exporter'
    static_configs:
    - targets: ['redis-exporter:9121']

  # Infrastructure monitoring
  - job_name: 'node-exporter'
    kubernetes_sd_configs:
    - role: endpoints
    relabel_configs:
    - source_labels: [__meta_kubernetes_endpoints_name]
      action: keep
      regex: node-exporter
```

### 5.2 告警规则

```yaml
# monitoring/prometheus/rules/exam-system.yml
groups:
- name: exam-system-alerts
  rules:
  # Application alerts
  - alert: HighErrorRate
    expr: |
      (
        sum(rate(http_requests_total{status=~"5.."}[5m])) by (service)
        /
        sum(rate(http_requests_total[5m])) by (service)
      ) * 100 > 5
    for: 2m
    labels:
      severity: critical
      team: backend
    annotations:
      summary: "High error rate detected for {{ $labels.service }}"
      description: "Error rate is {{ $value | humanizePercentage }} for service {{ $labels.service }}"
      runbook_url: "https://runbooks.exam.yourdomain.com/high-error-rate"

  - alert: ResponseTimeHigh
    expr: |
      histogram_quantile(0.95,
        sum(rate(http_request_duration_seconds_bucket[5m])) by (le, service)
      ) > 2
    for: 5m
    labels:
      severity: warning
      team: backend
    annotations:
      summary: "High response time for {{ $labels.service }}"
      description: "95th percentile response time is {{ $value }}s for {{ $labels.service }}"

  - alert: DatabaseConnectionsHigh
    expr: pg_stat_activity_count > 80
    for: 2m
    labels:
      severity: warning
      team: dba
    annotations:
      summary: "High database connections"
      description: "Database has {{ $value }} active connections"

  - alert: RedisMemoryUsageHigh
    expr: |
      (redis_memory_used_bytes / redis_memory_max_bytes) * 100 > 90
    for: 5m
    labels:
      severity: critical
      team: infrastructure
    annotations:
      summary: "Redis memory usage is high"
      description: "Redis memory usage is {{ $value | humanizePercentage }}"

  # Infrastructure alerts
  - alert: PodCrashLooping
    expr: |
      rate(kube_pod_container_status_restarts_total[15m]) * 60 * 15 > 0
    for: 0m
    labels:
      severity: critical
      team: infrastructure
    annotations:
      summary: "Pod {{ $labels.pod }} is crash looping"
      description: "Pod {{ $labels.pod }} in namespace {{ $labels.namespace }} is restarting frequently"

  - alert: PodNotReady
    expr: |
      kube_pod_status_ready{condition="false"} == 1
    for: 15m
    labels:
      severity: warning
      team: infrastructure
    annotations:
      summary: "Pod {{ $labels.pod }} not ready"
      description: "Pod {{ $labels.pod }} in namespace {{ $labels.namespace }} has been not ready for more than 15 minutes"

  - alert: DeploymentReplicasMismatch
    expr: |
      kube_deployment_spec_replicas != kube_deployment_status_available_replicas
    for: 15m
    labels:
      severity: warning
      team: infrastructure
    annotations:
      summary: "Deployment replicas mismatch"
      description: "Deployment {{ $labels.deployment }} has {{ $value }} available replicas, expected {{ $labels.spec_replicas }}"

  # Business logic alerts
  - alert: ExamCompletionRateLow
    expr: |
      (
        sum(increase(exam_attempts_completed_total[1h]))
        /
        sum(increase(exam_attempts_started_total[1h]))
      ) * 100 < 80
    for: 30m
    labels:
      severity: warning
      team: product
    annotations:
      summary: "Low exam completion rate"
      description: "Exam completion rate is {{ $value | humanizePercentage }} in the last hour"

  - alert: ProctoringViolationsHigh
    expr: |
      sum(increase(proctoring_violations_total[1h])) > 100
    for: 15m
    labels:
      severity: warning
      team: product
    annotations:
      summary: "High number of proctoring violations"
      description: "{{ $value }} proctoring violations detected in the last hour"

  - alert: AuthenticationFailuresHigh
    expr: |
      sum(increase(auth_failures_total[10m])) > 50
    for: 5m
    labels:
      severity: critical
      team: security
    annotations:
      summary: "High authentication failure rate"
      description: "{{ $value }} authentication failures in the last 10 minutes - possible attack"
```

### 5.3 Grafana Dashboard配置

```json
{
  "dashboard": {
    "id": null,
    "title": "Exam System Overview",
    "tags": ["exam-system", "overview"],
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "Service Status",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=\"exam-services\"}",
            "legendFormat": "{{ service }}"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "color": {
              "mode": "thresholds"
            },
            "thresholds": {
              "steps": [
                {"color": "red", "value": 0},
                {"color": "green", "value": 1}
              ]
            }
          }
        }
      },
      {
        "id": 2,
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total[5m])) by (service)",
            "legendFormat": "{{ service }}"
          }
        ],
        "yAxes": [
          {
            "label": "Requests/sec"
          }
        ]
      },
      {
        "id": 3,
        "title": "Response Time (95th percentile)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le, service))",
            "legendFormat": "{{ service }}"
          }
        ],
        "yAxes": [
          {
            "label": "Seconds"
          }
        ]
      },
      {
        "id": 4,
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{status=~\"5..\"}[5m])) by (service) / sum(rate(http_requests_total[5m])) by (service) * 100",
            "legendFormat": "{{ service }}"
          }
        ],
        "yAxes": [
          {
            "label": "Percentage",
            "max": 100
          }
        ]
      },
      {
        "id": 5,
        "title": "Active Users",
        "type": "singlestat",
        "targets": [
          {
            "expr": "active_users_current",
            "legendFormat": "Current Active Users"
          }
        ]
      },
      {
        "id": 6,
        "title": "Exam Metrics",
        "type": "table",
        "targets": [
          {
            "expr": "sum(exam_attempts_started_total)",
            "legendFormat": "Started"
          },
          {
            "expr": "sum(exam_attempts_completed_total)",
            "legendFormat": "Completed"
          },
          {
            "expr": "sum(exam_attempts_started_total) - sum(exam_attempts_completed_total)",
            "legendFormat": "In Progress"
          }
        ]
      }
    ],
    "time": {
      "from": "now-6h",
      "to": "now"
    },
    "refresh": "30s"
  }
}
```

---

## 6. 构建优化策略

### 6.1 多阶段构建优化

```dockerfile
# 优化的多阶段构建示例
FROM node:18-alpine AS base
RUN apk add --no-cache libc6-compat
WORKDIR /app
COPY package*.json ./

# Dependencies stage - 依赖安装优化
FROM base AS deps
RUN npm ci --only=production && npm cache clean --force

# Build stage - 构建优化
FROM base AS builder
RUN npm ci
COPY . .
ENV NODE_ENV=production
RUN npm run build

# Runtime stage - 运行时优化
FROM node:18-alpine AS runner
RUN apk add --no-cache dumb-init
WORKDIR /app

ENV NODE_ENV=production
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

# Copy built application
COPY --from=builder --chown=nextjs:nodejs /app/dist ./dist
COPY --from=deps --chown=nextjs:nodejs /app/node_modules ./node_modules
COPY --from=builder --chown=nextjs:nodejs /app/package.json ./package.json

USER nextjs

EXPOSE 3000
ENV PORT 3000

ENTRYPOINT ["dumb-init", "--"]
CMD ["node", "dist/index.js"]
```

### 6.2 缓存策略优化

```yaml
# .github/workflows/cache-optimization.yml
- name: Cache Node.js dependencies
  uses: actions/cache@v3
  with:
    path: |
      ~/.npm
      **/node_modules
    key: ${{ runner.os }}-node-${{ hashFiles('**/package-lock.json') }}
    restore-keys: |
      ${{ runner.os }}-node-

- name: Cache Docker layers
  uses: actions/cache@v3
  with:
    path: /tmp/.buildx-cache
    key: ${{ runner.os }}-buildx-${{ github.sha }}
    restore-keys: |
      ${{ runner.os }}-buildx-

- name: Build with cache
  uses: docker/build-push-action@v4
  with:
    cache-from: type=local,src=/tmp/.buildx-cache
    cache-to: type=local,dest=/tmp/.buildx-cache-new,mode=max
```

### 6.3 构建性能监控

```bash
#!/bin/bash
# scripts/build-metrics.sh

# 构建时间监控
start_time=$(date +%s)

# 记录构建指标
record_metric() {
    local metric_name=$1
    local metric_value=$2
    local timestamp=$(date +%s)
    
    echo "build_metric{name=\"$metric_name\",service=\"$SERVICE\"} $metric_value $timestamp" >> /tmp/build-metrics.txt
}

# 监控Docker构建
docker build --progress=plain -t "$IMAGE_NAME" . 2>&1 | tee build.log

# 计算构建时间
end_time=$(date +%s)
build_duration=$((end_time - start_time))
record_metric "build_duration_seconds" "$build_duration"

# 监控镜像大小
image_size=$(docker images "$IMAGE_NAME" --format "table {{.Size}}" | tail -n 1 | sed 's/MB//' | sed 's/GB/*1000/')
record_metric "image_size_mb" "$image_size"

# 监控构建缓存命中率
cache_hits=$(grep -c "CACHED" build.log || echo 0)
total_steps=$(grep -c "RUN\|COPY\|ADD" Dockerfile || echo 1)
cache_hit_rate=$((cache_hits * 100 / total_steps))
record_metric "cache_hit_rate_percent" "$cache_hit_rate"

# 发送指标到监控系统
if command -v curl >/dev/null 2>&1; then
    curl -X POST "$METRICS_ENDPOINT/metrics" \
         -H "Content-Type: text/plain" \
         --data-binary @/tmp/build-metrics.txt
fi
```

---

## 7. 安全构建配置

### 7.1 容器安全扫描

```yaml
# .github/workflows/security-scan.yml
name: Container Security Scan

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  security-scan:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Build Docker image
      run: docker build -t test-image .
      
    - name: Run Trivy vulnerability scanner
      uses: aquasecurity/trivy-action@master
      with:
        image-ref: 'test-image'
        format: 'sarif'
        output: 'trivy-results.sarif'
        severity: 'CRITICAL,HIGH'
        
    - name: Upload Trivy scan results
      uses: github/codeql-action/upload-sarif@v2
      if: always()
      with:
        sarif_file: 'trivy-results.sarif'
        
    - name: Run Hadolint (Dockerfile linter)
      uses: hadolint/hadolint-action@v3.1.0
      with:
        dockerfile: Dockerfile
        failure-threshold: error
        
    - name: Run Docker Bench Security
      run: |
        docker run --rm --net host --pid host --userns host --cap-add audit_control \
          -e DOCKER_CONTENT_TRUST=$DOCKER_CONTENT_TRUST \
          -v /etc:/etc:ro \
          -v /usr/bin/containerd:/usr/bin/containerd:ro \
          -v /usr/bin/runc:/usr/bin/runc:ro \
          -v /usr/lib/systemd:/usr/lib/systemd:ro \
          -v /var/lib:/var/lib:ro \
          -v /var/run/docker.sock:/var/run/docker.sock:ro \
          --label docker_bench_security \
          docker/docker-bench-security
```

### 7.2 密钥管理

```bash
#!/bin/bash
# scripts/secret-management.sh

# 使用HashiCorp Vault管理密钥
setup_vault_secrets() {
    local vault_addr="$1"
    local vault_token="$2"
    
    export VAULT_ADDR="$vault_addr"
    export VAULT_TOKEN="$vault_token"
    
    # 创建密钥路径
    vault auth -method=github token="$GITHUB_TOKEN"
    
    # 存储数据库密钥
    vault kv put secret/exam-system/database \
        username="exam_user" \
        password="$(openssl rand -base64 32)"
    
    # 存储JWT密钥
    vault kv put secret/exam-system/jwt \
        secret="$(openssl rand -base64 64)" \
        refresh_secret="$(openssl rand -base64 64)"
    
    # 存储API密钥
    vault kv put secret/exam-system/api \
        aws_access_key="$AWS_ACCESS_KEY_ID" \
        aws_secret_key="$AWS_SECRET_ACCESS_KEY"
}

# 在Kubernetes中使用External Secrets Operator
create_external_secret() {
    cat <<EOF | kubectl apply -f -
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: exam-system-secrets
  namespace: exam-system
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: vault-backend
    kind: SecretStore
  target:
    name: exam-secrets
    creationPolicy: Owner
  data:
  - secretKey: DATABASE_PASSWORD
    remoteRef:
      key: secret/exam-system/database
      property: password
  - secretKey: JWT_SECRET
    remoteRef:
      key: secret/exam-system/jwt
      property: secret
EOF
}
```

---

## 总结

本构建架构设计涵盖了企业级在线考试系统的完整构建流程，包括：

### 核心特性
- **微服务架构**: 支持独立构建、部署和扩展
- **容器化部署**: Docker多阶段构建优化
- **Kubernetes编排**: 生产级容器编排和自动扩展
- **CI/CD自动化**: GitHub Actions完整流水线
- **安全内建**: 容器扫描、密钥管理、合规检查
- **监控可观测**: Prometheus、Grafana完整监控体系

### 构建优势
- **高可用性**: 多副本、自动故障转移
- **高性能**: 缓存优化、并行构建
- **高安全性**: 多层安全防护、零信任架构
- **高可维护性**: 标准化工作流、自动化运维
- **高扩展性**: 水平扩展、弹性伸缩

该构建架构确保了系统能够支持10K+并发用户，实现99.9%可用性目标，为企业级在线考试平台提供了坚实的技术基础。