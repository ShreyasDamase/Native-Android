# P0-15 — Kubernetes & Scaling Overview: When VPS is Enough, When K8s is Needed

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> **Context**: The most expensive engineering mistake a startup can make is over-engineering infrastructure before it's needed. The second most expensive is under-engineering it after it IS needed. This chapter gives you a clear decision framework for when to use what — based on what actually kills startups versus what actually killed Netflix in its early days.

---

## The Scaling Journey — A Timeline, Not a Checklist

```
Stage 0: Idea Validation
  → SQLite or PostgreSQL on your laptop
  → No users, no scaling needed
  → Focus: ship features, not infrastructure

Stage 1: First Users (0 → 1,000 DAU)
  → Single VPS (DigitalOcean Droplet, Hetzner)
  → $6–$20/month
  → Everything on one machine: app + DB + Redis
  → Deploy: git pull + systemd restart

Stage 2: Growing (1,000 → 10,000 DAU)
  → Separate DB server (managed DB)
  → Bigger VPS for the app
  → Add NGINX reverse proxy
  → Maybe: Docker Compose for easier deployments
  → Deploy: Docker Compose pull + restart

Stage 3: Scaling (10,000 → 100,000 DAU)
  → Multiple app instances behind NGINX/HAProxy
  → Stateless application requirement kicks in
  → Managed DB with read replicas
  → Redis cluster for session/cache
  → Deploy: still Docker Compose per node, or first K8s exploration

Stage 4: Production Scale (100,000+ DAU)
  → Kubernetes (managed: EKS/GKE/DigitalOcean K8s)
  → Multiple services, microservices
  → HPA, rolling updates, self-healing
  → CI/CD pipelines with GitOps
  → Deploy: Helm + ArgoCD

Stage 5: Hyperscale (millions of DAU)
  → Multi-region K8s
  → Service mesh (Istio/Linkerd)
  → Custom operators
  → Dedicated platform team (5–10 engineers just for infra)
```

> [!IMPORTANT]
> **Zepto went from zero to $800M valuation in 2 years. Their early tech stack was not Kubernetes.** It was a fast-moving monolith on a few VMs. K8s was added when they needed the operational capabilities it provides — not on day 1. Premature K8s adoption is a startup killer because it diverts engineering bandwidth from the product.

---

## When a Single VPS is Enough

### The VPS is underrated

A DigitalOcean Droplet or Hetzner server (preferred for European users — better value) can handle more than most founders realize:

**Hetzner CX41 — €14/month:**
- 4 vCPUs, 16GB RAM, 160GB SSD
- Easily handles: 500-1,000 concurrent users, ~200 req/sec

**Hetzner CCX23 — €45/month (dedicated vCPUs):**
- 4 dedicated vCPUs, 16GB RAM, 360GB NVMe
- Handles: 2,000-5,000 concurrent users, ~1,000 req/sec

**What 1,000 req/sec means for a startup:**
- 86.4 million requests per day
- If average session is 20 requests: 4.3 million sessions per day
- That's ~4.3 million daily active users if each visits once

A well-optimized Spring Boot app with connection pooling, Redis cache, and efficient SQL can serve **4+ million DAU** from a single powerful VPS.

### When does a single VPS stop working?

```
✗ You need > 100% uptime (single VPS has zero redundancy)
✗ Peak traffic exceeds vertical scaling ceiling (~4 vCPUs, 32GB RAM)
✗ You need zero-downtime deployments
✗ Your DB and app on the same machine create resource contention
✗ A single regional failure would kill your entire business
✗ You need to scale individual services independently
```

### The VPS Setup

```bash
# A well-configured single-VPS setup
# App: Spring Boot JAR via systemd
# DB: Managed PostgreSQL (DigitalOcean, Supabase)
# Cache: Redis on the same VPS
# Reverse Proxy: NGINX
# Process Manager: systemd
# SSL: Let's Encrypt via Certbot
# Monitoring: Grafana + Prometheus (Docker Compose on same VPS or separate small instance)
```

```nginx
# /etc/nginx/sites-available/myapp.conf
upstream springboot {
    server 127.0.0.1:8080;
    keepalive 32;  # Keep connections alive between NGINX and Spring Boot
}

server {
    listen 80;
    server_name api.yourcompany.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.yourcompany.com;

    ssl_certificate /etc/letsencrypt/live/api.yourcompany.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.yourcompany.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;

    # Rate limiting (see SD-3 chapter)
    limit_req zone=api burst=50 nodelay;

    location / {
        proxy_pass http://springboot;
        proxy_http_version 1.1;
        proxy_set_header Connection "";  # For keepalive
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Timeouts
        proxy_connect_timeout 5s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }
}
```

---

## Vertical Scaling — Bigger Machine

**The simplest form of scaling**: buy a bigger server.

| Resource | When to Scale | How to Detect |
|----------|-------------|---------------|
| CPU | Consistently >70% over 5 min | `top`, Grafana CPU metric |
| RAM | >80% used, swap being used | `free -h`, JVM heap metrics |
| Disk | >75% full | `df -h`, disk I/O wait |
| Network | >60% of bandwidth cap | `iftop`, cloud provider metrics |
| DB connections | Pool exhaustion errors | Spring Boot actuator `/metrics/hikaricp.connections.pending` |

**When vertical scaling stops working:**
- Your cloud provider has no bigger instance size (physical ceiling)
- Bigger machines get disproportionately expensive (non-linear price/performance)
- JVM stop-the-world GC pauses get worse with very large heaps (>32GB)
- Single region failure risk grows as you put more eggs in one basket

---

## Horizontal Scaling — Multiple Instances

When you run 3 Spring Boot instances behind a load balancer, you need to ensure they all serve the same experience regardless of which instance handles a request.

### Statelessness Requirement

> [!CAUTION]
> **Session state stored in-memory (HttpSession) is the most common reason horizontal scaling breaks.** If User A's session is on Instance 1, but their next request goes to Instance 2 (because load balancer round-robins), they're logged out. Every Spring Boot instance must be stateless — ALL state must be in external stores (Redis, PostgreSQL).

```kotlin
// WRONG: Storing session in-memory
@GetMapping("/cart")
fun getCart(session: HttpSession): Cart {
    return session.getAttribute("cart") as Cart  // Only on this instance!
}

// RIGHT: Store session in Redis
// application.yml:
// spring.session.store-type: redis

@GetMapping("/cart")  
fun getCart(session: HttpSession): Cart {
    // Session backed by Redis — works across all instances
    return session.getAttribute("cart") as Cart
}
```

```yaml
# application.yml — Redis-backed sessions
spring:
  session:
    store-type: redis
    timeout: 30m
    redis:
      namespace: "myapp:session"

  data:
    redis:
      host: ${REDIS_HOST}
      port: 6379
```

### NGINX Load Balancing

```nginx
upstream springboot_cluster {
    # Round-robin (default)
    server 10.0.0.1:8080;
    server 10.0.0.2:8080;
    server 10.0.0.3:8080;

    # Health checking — remove down instances automatically
    keepalive 64;
}

# For sticky sessions (if you must — prefer stateless):
upstream springboot_sticky {
    ip_hash;  # Same IP always goes to same server
    server 10.0.0.1:8080;
    server 10.0.0.2:8080;
}
```

---

## Why Kubernetes — What It Actually Solves

K8s is not just "Docker at scale." It solves specific operational problems:

### 1. Self-Healing
```yaml
# If a pod crashes, K8s restarts it automatically.
# With a VPS + systemd: you configure systemctl restart-on-failure
# With K8s: built-in, across any node in the cluster
```

### 2. Automated Rolling Updates
```
Old: ssh to server, stop app, deploy new JAR, start app → 30-60 seconds downtime
K8s: kubectl rollout → new pods start, old ones stop after health checks pass → 0 downtime
```

### 3. Horizontal Pod Autoscaling (HPA)
```
3 AM: 10 pods running (low traffic)
8 AM: K8s sees CPU > 70% → scales to 30 pods automatically
11 AM: traffic drops → scales back to 10 pods
No manual intervention, no over-provisioning
```

### 4. Service Discovery
```
With VMs: you hard-code IP addresses of services (or use Consul/Zookeeper)
K8s: services get stable DNS names (order-service.default.svc.cluster.local)
New pods register automatically. Dead pods deregister automatically.
```

### 5. Resource Isolation
```
K8s resource limits prevent one service from starving others.
Without limits: a spike in the recommendation service kills the checkout service
With limits: each service gets its guaranteed slice, excess is capped
```

---

## Kubernetes Primitives — The Essential Concepts

### Pod
The smallest deployable unit. Usually 1 container, sometimes 2 (app + sidecar).

```yaml
# You never create Pods directly — use Deployments instead
# Pods are mortal — they can die and are replaced by K8s
# A pod gets its own IP address within the cluster
```

### Deployment
Manages a set of identical Pods. Handles rolling updates, rollback.

### Service
Stable network endpoint for a set of Pods. The Pods behind it can come and go, the Service DNS name stays the same.

```
Types:
- ClusterIP: Only accessible within the cluster (service-to-service communication)
- NodePort: Accessible on each node's IP at a static port (development only)
- LoadBalancer: Creates a cloud load balancer (expensive, use Ingress instead for HTTP)
```

### Ingress
HTTP routing layer. One external load balancer routes to many internal services.

```
Without Ingress: 10 services = 10 cloud load balancers = 10 × $20/month = $200/month just for LBs
With Ingress:    10 services = 1 cloud load balancer + NGINX Ingress controller = $20/month
```

---

## Complete Spring Boot Kubernetes YAMLs

### Deployment

```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: production
  labels:
    app: order-service
    version: "1.0.0"
spec:
  replicas: 3
  selector:
    matchLabels:
      app: order-service
  
  strategy:
    type: RollingUpdate
    rollingUpdate:
      # At most 1 pod unavailable during update (maintains N-1 capacity)
      maxUnavailable: 1
      # At most 1 extra pod during update (cost control)
      maxSurge: 1

  template:
    metadata:
      labels:
        app: order-service
        version: "1.0.0"
      annotations:
        # Force pod restart when ConfigMap changes (Reloader handles this)
        configmap.reloader.stakater.com/reload: "order-service-config"
    
    spec:
      # Grace period: K8s waits this long for in-flight requests to complete
      # before force-killing the pod. Match to your longest request duration.
      terminationGracePeriodSeconds: 30
      
      # Spread pods across nodes for high availability
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: kubernetes.io/hostname
          whenUnsatisfiable: DoNotSchedule
          labelSelector:
            matchLabels:
              app: order-service
      
      containers:
        - name: order-service
          image: ghcr.io/yourcompany/order-service:1.0.0
          imagePullPolicy: Always
          
          ports:
            - containerPort: 8080
              name: http
          
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "production"
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: order-service-secrets
                  key: db-password
            - name: REDIS_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: order-service-secrets
                  key: redis-password
            - name: JAVA_OPTS
              value: >-
                -XX:MaxRAMPercentage=75.0
                -XX:+UseG1GC
                -XX:+UseContainerSupport
                -Djava.security.egd=file:/dev/./urandom
          
          envFrom:
            - configMapRef:
                name: order-service-config
          
          # CRITICAL: Resource requests and limits.
          # Without these, one pod can consume all node resources.
          # requests: what K8s guarantees this pod
          # limits: the hard ceiling
          resources:
            requests:
              memory: "512Mi"   # K8s schedules based on this
              cpu: "250m"       # 250 millicores = 0.25 vCPU
            limits:
              memory: "1Gi"     # If exceeded: OOMKilled (pod restarted)
              cpu: "1000m"      # If exceeded: throttled (NOT killed)
          
          # Liveness probe: Is the pod alive? If no: restart it.
          # NEVER make liveness probe depend on external services (DB, Redis).
          # If DB is down, you don't want ALL pods restarting simultaneously.
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60    # Wait for JVM warmup
            periodSeconds: 10
            failureThreshold: 3
            timeoutSeconds: 5
          
          # Readiness probe: Is the pod ready to receive traffic?
          # CAN depend on external services. Pod removed from Service endpoints if failing.
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
            failureThreshold: 3
            timeoutSeconds: 3
          
          # Startup probe: Handles slow startup (JVM, DB migrations).
          # Overrides liveness probe until startup completes.
          # Prevents premature liveness kill during Flyway migration.
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            failureThreshold: 30   # 30 × 10s = 5 minutes max startup time
            periodSeconds: 10
          
          volumeMounts:
            - name: tmp-volume
              mountPath: /tmp
      
      volumes:
        - name: tmp-volume
          emptyDir: {}
      
      # Prefer to not schedule on nodes already running this service
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchLabels:
                    app: order-service
                topologyKey: kubernetes.io/hostname
```

### Service

```yaml
# k8s/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: order-service
  namespace: production
  labels:
    app: order-service
spec:
  type: ClusterIP  # Internal only — Ingress handles external traffic
  selector:
    app: order-service  # Routes to pods with this label
  ports:
    - port: 80          # Port the Service listens on (within cluster)
      targetPort: 8080  # Port the Pod listens on
      name: http
  sessionAffinity: None  # Stateless — no sticky sessions needed
```

### Horizontal Pod Autoscaler

```yaml
# k8s/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service-hpa
  namespace: production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service
  
  minReplicas: 3    # Never go below 3 (HA requirement — survive 1 node failure)
  maxReplicas: 20   # Cost ceiling — scale out to 20 at most
  
  metrics:
    # CPU-based scaling (most common)
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70  # Scale when avg CPU > 70%
    
    # Memory-based scaling (useful for stateful apps)
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
    
    # Custom metric: requests per second from Prometheus
    # Requires Prometheus Adapter installed in cluster
    - type: Pods
      pods:
        metric:
          name: http_requests_per_second
        target:
          type: AverageValue
          averageValue: "200"  # Scale when avg RPS per pod > 200
  
  behavior:
    # Scale up quickly when load spikes
    scaleUp:
      stabilizationWindowSeconds: 30   # Wait 30s before scaling up again
      policies:
        - type: Pods
          value: 4          # Add up to 4 pods at a time
          periodSeconds: 60
    
    # Scale down slowly to avoid flapping
    scaleDown:
      stabilizationWindowSeconds: 300  # Wait 5 minutes before scaling down
      policies:
        - type: Pods
          value: 2          # Remove at most 2 pods at a time
          periodSeconds: 120
```

### Ingress

```yaml
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: api-ingress
  namespace: production
  annotations:
    kubernetes.io/ingress.class: nginx
    # TLS: Let's Encrypt via cert-manager
    cert-manager.io/cluster-issuer: letsencrypt-prod
    # Rate limiting (see SD-3)
    nginx.ingress.kubernetes.io/limit-rps: "100"
    # Request size limit
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
    # Timeouts
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "5"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "30"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "30"
    # CORS
    nginx.ingress.kubernetes.io/enable-cors: "true"
    nginx.ingress.kubernetes.io/cors-allow-origin: "https://yourcompany.com"

spec:
  tls:
    - hosts:
        - api.yourcompany.com
      secretName: api-tls-secret  # cert-manager populates this
  
  rules:
    - host: api.yourcompany.com
      http:
        paths:
          - path: /api/orders
            pathType: Prefix
            backend:
              service:
                name: order-service
                port:
                  number: 80
          - path: /api/inventory
            pathType: Prefix
            backend:
              service:
                name: inventory-service
                port:
                  number: 80
          - path: /api/tracking
            pathType: Prefix
            backend:
              service:
                name: tracking-service
                port:
                  number: 80
```

### ConfigMap and Secret

```yaml
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: order-service-config
  namespace: production
data:
  SPRING_DATASOURCE_URL: "jdbc:postgresql://db-prod.internal:5432/orders"
  SPRING_DATASOURCE_USERNAME: "order_service"
  REDIS_HOST: "redis-cluster.internal"
  REDIS_PORT: "6379"

---
# k8s/secret.yaml
# NEVER commit secrets to git. Use Vault, AWS Secrets Manager, or Sealed Secrets.
apiVersion: v1
kind: Secret
metadata:
  name: order-service-secrets
  namespace: production
type: Opaque
# In reality, use: kubectl create secret generic order-service-secrets --from-literal=...
# Or use ExternalSecrets operator to pull from AWS Secrets Manager
data:
  db-password: <base64-encoded-password>
  redis-password: <base64-encoded-password>
```

---

## Resource Requests and Limits — Why You MUST Set Them

> [!CAUTION]
> **Deploying to K8s without resource limits is like running a server without swap limits. One memory leak in one pod will OOMKill other pods on the same node. In a cluster with no limits, a single bad deployment can cascade into a full cluster outage.**

### Understanding the difference:

```
requests: The amount K8s guarantees the pod can always get.
          Used for SCHEDULING decisions (which node can host this pod).
          
limits:   The hard cap. Pod can't exceed this.
          Memory limit exceeded → OOMKilled (pod restarted immediately)
          CPU limit exceeded    → Throttled (slower, NOT restarted)

Example:
  requests.cpu: 250m  → K8s won't schedule this pod unless a node has 250m available
  limits.cpu: 1000m   → Pod can burst to 1000m if the node has spare capacity

  requests.memory: 512Mi → Guaranteed 512Mi
  limits.memory: 1Gi     → Cannot exceed 1Gi; if it does, K8s kills and restarts the pod
```

### JVM-specific settings for containers:

```
# Without UseContainerSupport (pre-Java 10): JVM reads host RAM (32GB), not container limit (1GB)
# It allocates a 8GB heap → OOMKilled immediately
# With UseContainerSupport (default in Java 11+): JVM respects container limits

-XX:+UseContainerSupport          # Should be default on JDK 11+, set explicitly to be safe
-XX:MaxRAMPercentage=75.0         # Use 75% of container memory limit for heap
                                   # For 1Gi limit → 768Mi heap
                                   # Leaves 25% for non-heap (Metaspace, threads, native)
-XX:+UseG1GC                      # Best general-purpose GC for microservices
-XX:MaxGCPauseMillis=100          # Target max GC pause of 100ms
```

> [!WARNING]
> **Setting MaxRAMPercentage too high causes OOMKills that look random.** The JVM heap is not the only thing using memory. Stack frames per thread (~512KB each × 200 threads = 100MB), Metaspace (50-200MB), native code, and OS overhead all consume memory outside the heap. At 75% MaxRAMPercentage, a 1Gi limit → 768Mi heap → 256Mi for everything else. That's enough for typical services but can be tight under load.

---

## Managed K8s vs Self-Hosted — The Real Comparison

| Factor | Self-hosted K8s (kubeadm) | Managed (EKS/GKE/DO K8s) |
|--------|--------------------------|--------------------------|
| Setup time | 2-3 days minimum | 30 minutes |
| Control plane | You manage it | Managed, HA by default |
| Upgrades | Manual, risky, 2-4 hrs | Click a button |
| etcd backups | Your responsibility | Automatic |
| Monthly cost | $50-100/month (master nodes) | $70-100/month (managed) |
| Engineering time | 20-40 hrs/month for a small cluster | Near zero |
| Suitable for | Learning, on-premise requirements | Production startups |

**For a startup: always use managed K8s.** The operational cost of running your own control plane is not worth it until you have a dedicated platform team (10+ engineers).

### Managed K8s Options

**Google GKE:**
- Best Kubernetes support (Google created K8s)
- Autopilot mode: completely managed, pay per Pod resource
- Good: GKE Autopilot eliminates node management entirely

**AWS EKS:**
- Best if you're already deep in AWS
- More manual setup than GKE
- Best ecosystem (ECR, IAM, ALB integration)
- Fargate mode for serverless pods

**DigitalOcean Kubernetes:**
- Simplest setup, best documentation for beginners
- Perfect for teams moving from VPS
- $12/month per node + $0 control plane fee
- Best for: early-stage startup, < $5k/month infra budget

---

## Helm — Parameterized Kubernetes Deployments

Without Helm, you have YAML files with hardcoded values. Changing image versions requires editing multiple files. Helm gives you templates.

```
your-service/
├── Chart.yaml
├── values.yaml           ← default configuration
├── values-staging.yaml   ← override for staging
├── values-prod.yaml      ← override for production
└── templates/
    ├── deployment.yaml
    ├── service.yaml
    ├── hpa.yaml
    ├── ingress.yaml
    └── configmap.yaml
```

```yaml
# Chart.yaml
apiVersion: v2
name: order-service
description: Order management service
version: 0.1.0
appVersion: "1.0.0"

# values.yaml
image:
  repository: ghcr.io/yourcompany/order-service
  tag: latest
  pullPolicy: Always

replicaCount: 3

resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "1000m"

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 20
  targetCPUUtilizationPercentage: 70

ingress:
  enabled: true
  host: api.yourcompany.com

env:
  SPRING_PROFILES_ACTIVE: production
```

```yaml
# templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .Release.Name }}-{{ .Chart.Name }}
spec:
  replicas: {{ .Values.replicaCount }}
  template:
    spec:
      containers:
        - name: {{ .Chart.Name }}
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
          resources:
            requests:
              memory: {{ .Values.resources.requests.memory }}
              cpu: {{ .Values.resources.requests.cpu }}
            limits:
              memory: {{ .Values.resources.limits.memory }}
              cpu: {{ .Values.resources.limits.cpu }}
```

```bash
# Deploy to staging
helm upgrade --install order-service ./order-service \
  -f values-staging.yaml \
  --set image.tag=1.2.3 \
  --namespace staging

# Deploy to production
helm upgrade --install order-service ./order-service \
  -f values-prod.yaml \
  --set image.tag=1.2.3 \
  --namespace production

# Rollback to previous release
helm rollback order-service 1 --namespace production
```

---

## The Hard Truth About K8s

> [!WARNING]
> **Kubernetes is not a silver bullet. It is operational complexity that you trade for operational capabilities.** A 2-person startup running K8s is spending 30-40% of engineering time on infrastructure instead of product. This is frequently lethal.

**What K8s does NOT solve:**
- Slow code → still slow, just on more pods
- Database performance → K8s doesn't touch your SQL
- Bad architecture → distributed bad architecture is worse than monolithic bad architecture
- Security → K8s adds RBAC, network policies, etc. — more attack surface if misconfigured

**The operational overhead list:**
- Cluster upgrades (every 3-4 months, each version supported for ~1 year)
- Node OS patching
- etcd backup verification
- Certificate rotation
- RBAC management
- Network policy maintenance
- Persistent volume management
- Cluster autoscaler tuning
- Node group management

---

## Alternatives — Managed Platforms That Give K8s Benefits Without the Pain

### Railway ($5/month per service minimum)
- Deploy from GitHub → automatic builds, deployments
- Horizontal scaling with one click
- Built-in Postgres, Redis
- Zero infrastructure management
- **Best for**: Pre-product-market-fit startups

### Render ($7/month for web services)
- Docker Compose → Render migration is trivial
- Auto-deploy from GitHub
- Managed Postgres, Redis
- Private networking between services
- **Best for**: Small teams wanting a PaaS experience

### Fly.io ($0 base, pay for compute)
- Deploy Docker containers globally with `fly deploy`
- Multi-region with one command
- Postgres clusters with auto-failover
- WebSocket support, long-running processes
- **Best for**: Apps needing low-latency globally (gaming, real-time)

### Decision Framework

```
Monthly active users < 50k AND team < 10 engineers:
  → Railway / Render / Fly.io

Monthly active users 50k–500k AND team 5–20 engineers:
  → Managed K8s (DigitalOcean K8s or GKE Autopilot)
  → Start with 1 microservice, not 20

Monthly active users > 500k AND dedicated platform team:
  → Full K8s with GitOps (ArgoCD), service mesh, multi-region

You have on-premise requirements (banking, defense, healthcare):
  → Self-hosted K8s with kubeadm or Rancher
```

---

## Spring Boot Actuator — The Probes K8s Needs

```kotlin
// build.gradle.kts
// implementation("org.springframework.boot:spring-boot-starter-actuator")
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true   # Enables /actuator/health/liveness and /actuator/health/readiness
      group:
        readiness:
          include: db,redis,diskSpace  # Readiness fails if DB or Redis is down
        liveness:
          include: ping                 # Liveness only checks if app is responsive
  
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
```

```kotlin
// Custom readiness indicator
@Component
class DatabaseReadinessIndicator(
    private val dataSource: DataSource
) : HealthIndicator {
    override fun health(): Health {
        return try {
            dataSource.connection.use { conn ->
                conn.prepareStatement("SELECT 1").executeQuery()
            }
            Health.up().withDetail("database", "reachable").build()
        } catch (e: Exception) {
            Health.down().withDetail("error", e.message).build()
        }
    }
}
```

---

## Complete CI/CD Pipeline — GitHub Actions → K8s

```yaml
# .github/workflows/deploy.yml
name: Build and Deploy to Kubernetes

on:
  push:
    branches: [main]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: gradle
      
      - name: Build with Gradle
        run: ./gradlew build -x test
      
      - name: Run Tests
        run: ./gradlew test
      
      - name: Build Docker image
        run: |
          docker build -t ghcr.io/${{ github.repository }}/order-service:${{ github.sha }} .
      
      - name: Push to GitHub Container Registry
        run: |
          echo ${{ secrets.GITHUB_TOKEN }} | docker login ghcr.io -u ${{ github.actor }} --password-stdin
          docker push ghcr.io/${{ github.repository }}/order-service:${{ github.sha }}
      
      - name: Deploy to Kubernetes
        uses: azure/k8s-deploy@v4
        with:
          namespace: production
          manifests: k8s/
          images: |
            ghcr.io/${{ github.repository }}/order-service:${{ github.sha }}
```

```dockerfile
# Dockerfile — Multi-stage build for minimal image size
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
# Cache dependencies layer
RUN ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
```

---

## Production Monitoring Stack on K8s

```yaml
# Deploy kube-prometheus-stack via Helm (Prometheus + Grafana + Alertmanager)
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --set grafana.adminPassword=<secure-password> \
  --set prometheus.prometheusSpec.retention=30d \
  --set prometheus.prometheusSpec.storageSpec.volumeClaimTemplate.spec.resources.requests.storage=50Gi
```

**Key Grafana dashboards to set up:**
1. **JVM Micrometer** — heap usage, GC frequency, thread counts
2. **Spring Boot** — request rate, error rate, latency percentiles
3. **K8s Cluster** — node CPU/RAM, pod count, pending pods
4. **Business Metrics** — orders/minute, payment success rate, inventory alerts

**Critical alerts to configure:**
```yaml
# alerts.yaml (Alertmanager)
groups:
  - name: spring-boot
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Error rate > 5% for 2 minutes"
      
      - alert: PodCrashLooping
        expr: kube_pod_container_status_restarts_total > 5
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Pod {{ $labels.pod }} is crash looping"
      
      - alert: HighMemoryUsage
        expr: container_memory_usage_bytes / container_spec_memory_limit_bytes > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Container memory usage > 90% of limit"
```
