# P0-6 — Reverse Proxy & Traffic: NGINX, Load Balancing, SSL Termination, DDoS Basics

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> **This is not a DevOps chapter. This is a survival chapter.**
> The moment your Spring Boot service is reachable on port 8080 from the public internet, you have already made a catastrophic mistake. Every production system you will ever build needs a hardened edge layer. NGINX is that layer. Understanding it deeply separates engineers who build systems from engineers who just write code.

---

## 1. What Is a Reverse Proxy and Why Does It Exist

A **reverse proxy** sits in front of your application servers and intercepts all incoming traffic before it reaches your application. The client never talks to your Spring Boot process directly — it talks to NGINX, which then decides where to forward the request.

```
[Client]  →  [NGINX :443]  →  [Spring Boot :8080]
                ↓
          (also routes to)
                ↓
          [Spring Boot :8081]  (second instance)
          [Spring Boot :8082]  (third instance)
```

**Why your Spring Boot app MUST NOT be exposed directly:**

| Risk | If Spring Boot is public | With NGINX in front |
|---|---|---|
| SSL/TLS | You'd need to manage certs in JVM keystore (nightmare) | NGINX handles cert renewal via Certbot |
| DDoS | Tomcat thread pool exhausted in seconds | NGINX async event loop absorbs burst, rate limits before hitting JVM |
| Static files | JVM serves static assets — terrible GC pressure | NGINX serves static files from disk at near-zero CPU cost |
| Slow clients | Client reads 1 byte/sec → Tomcat thread held for minutes | NGINX buffers full response, releases upstream connection immediately |
| Load distribution | Single process = single point of failure | NGINX distributes across N instances |
| Security headers | You'd set them in Spring Security config per endpoint | NGINX injects them globally for every response |
| Port 80/443 | Java can't bind to privileged ports without root (dangerous) | NGINX runs as nginx user, binds 443, forwards to 8080 |

> [!CAUTION]
> Running Spring Boot on port 443 directly as root is one of the most dangerous configurations possible. A single RCE (remote code execution) vulnerability in any dependency gives the attacker root access to your server. NGINX + non-privileged Spring Boot process is the minimum acceptable security posture.

### The Slow Loris Attack — Why Buffering Matters

Slow Loris is an attack where a client opens thousands of connections and sends HTTP headers extremely slowly — one byte every few seconds — keeping connections open without completing requests. Apache (and Spring Boot's embedded Tomcat) is vulnerable because it holds a thread per connection.

NGINX uses an event-driven, non-blocking architecture. It holds slow connections in a kernel buffer, only assigning an upstream connection slot when the full request is received. This means NGINX can absorb Slow Loris attacks while your Spring Boot process never sees the attack at all.

---

## 2. NGINX as Reverse Proxy — Core Configuration

### Installation on Ubuntu/Debian

```bash
sudo apt update
sudo apt install nginx
sudo systemctl enable nginx
sudo systemctl start nginx
```

### Basic Reverse Proxy to Spring Boot

```nginx
# /etc/nginx/sites-available/myapp.conf

server {
    listen 80;
    server_name api.myapp.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;

        # Critical: forward real client IP to Spring Boot
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket support (if needed)
        proxy_set_header Upgrade    $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

> [!IMPORTANT]
> Without `proxy_set_header X-Forwarded-For`, your Spring Boot app sees every request as coming from `127.0.0.1`. This breaks IP-based rate limiting, audit logs, fraud detection, and geo-based routing entirely. In Spring Boot, configure `server.forward-headers-strategy=native` or use `ForwardedHeaderFilter`.

### Spring Boot: Trusting Forwarded Headers

```yaml
# application.yml
server:
  forward-headers-strategy: native  # or 'framework' for Spring Security integration
```

```kotlin
// For Spring Security environments, register this bean:
@Bean
fun forwardedHeaderFilter(): FilterRegistrationBean<ForwardedHeaderFilter> {
    val filter = FilterRegistrationBean(ForwardedHeaderFilter())
    filter.order = Ordered.HIGHEST_PRECEDENCE
    return filter
}
```

### Upstream Block — The Foundation of Load Balancing

```nginx
# /etc/nginx/nginx.conf or included conf

upstream springboot_backend {
    # Default: round robin
    server 127.0.0.1:8080;
    server 127.0.0.1:8081;
    server 127.0.0.1:8082;

    # Keep persistent connections to upstream
    keepalive 32;
}

server {
    listen 443 ssl;
    server_name api.myapp.com;

    location /api/ {
        proxy_pass http://springboot_backend;
        proxy_http_version 1.1;
        proxy_set_header Connection "";  # Required for keepalive upstream
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

> [!NOTE]
> `keepalive 32` means NGINX maintains up to 32 idle persistent connections per worker process to each upstream group. Without this, NGINX opens a new TCP connection for every proxied request. At high traffic (10K req/s), this causes massive TCP connection churn, increasing latency by 10-50ms per request (TCP handshake overhead).

---

## 3. Load Balancing Algorithms

### 3.1 Round Robin (Default)

Requests are distributed sequentially across all upstream servers.

```nginx
upstream backend {
    server 10.0.0.1:8080;
    server 10.0.0.2:8080;
    server 10.0.0.3:8080;
}
```

**When to use:** Stateless services where all instances are identical. Most REST APIs.

**Limitation:** If one server is slower (GC pause, cold start), it still receives the same number of requests, causing a latency spike for 1/N of users.

### 3.2 Least Connections

Sends the next request to the server with the fewest active connections.

```nginx
upstream backend {
    least_conn;
    server 10.0.0.1:8080;
    server 10.0.0.2:8080;
    server 10.0.0.3:8080;
}
```

**When to use:** When request processing times vary significantly — e.g., some requests trigger expensive DB queries, others hit cache. Zepto's order fulfillment service would use this because order-tracking calls (lightweight) and checkout calls (heavy DB + inventory check) go to the same pool.

### 3.3 IP Hash (Sticky Sessions)

The same client IP always goes to the same backend server.

```nginx
upstream backend {
    ip_hash;
    server 10.0.0.1:8080;
    server 10.0.0.2:8080;
    server 10.0.0.3:8080;
}
```

**When to use:** When you have in-memory session state that is NOT stored in a shared cache (Redis). Legacy apps, WebSocket connections where session affinity matters.

> [!WARNING]
> IP Hash is a code smell in modern architecture. If your application requires sticky sessions because session state lives only in-memory on the application server, you have a horizontal scaling problem. The correct fix is to externalize session state to Redis. IP Hash is a band-aid. When a server dies, all "stuck" users lose their session anyway.

### 3.4 Weighted Upstream

Give more traffic to more powerful servers:

```nginx
upstream backend {
    server 10.0.0.1:8080 weight=5;  # Gets 5x more traffic
    server 10.0.0.2:8080 weight=1;  # Smaller/slower instance
}
```

**When to use:** During a rolling deployment where a new instance is warming up (JIT compilation, cache warming). Start it at weight=1, gradually increase.

### 3.5 Backup Server

```nginx
upstream backend {
    server 10.0.0.1:8080;
    server 10.0.0.2:8080;
    server 10.0.0.3:8080 backup;  # Only used if all primary servers are down
}
```

### 3.6 Health Checks and Server Removal

```nginx
upstream backend {
    server 10.0.0.1:8080 max_fails=3 fail_timeout=30s;
    server 10.0.0.2:8080 max_fails=3 fail_timeout=30s;
}
```

`max_fails=3`: After 3 consecutive failures, mark the server as unhealthy.
`fail_timeout=30s`: Stop sending traffic for 30 seconds, then retry.

> [!IMPORTANT]
> NGINX Open Source (free) only does **passive health checks** — it detects failures from actual failed requests. NGINX Plus (paid) supports **active health checks** where NGINX proactively polls a `/health` endpoint. For active health checks in open source NGINX, use the `nginx_upstream_check_module` patch or handle this at the orchestration layer (Kubernetes liveness probes).

---

## 4. SSL/TLS Termination — The Right Way

### Why Terminate SSL at NGINX, Not at Spring Boot

| Factor | SSL at Spring Boot | SSL at NGINX |
|---|---|---|
| Certificate management | Manual keystore updates, app restart needed | Certbot auto-renews, NGINX reload (zero downtime) |
| Performance | JVM SSL handshake = CPU-heavy | NGINX uses OpenSSL (native C), much faster |
| Simplicity | App needs cert config in YAML | App is plain HTTP on localhost only |
| Multiple apps | Each app manages its own cert | One NGINX manages certs for all services |

### Let's Encrypt with Certbot

```bash
# Install Certbot
sudo apt install certbot python3-certbot-nginx

# Obtain and install certificate automatically
sudo certbot --nginx -d api.myapp.com -d myapp.com

# Certbot modifies your nginx.conf automatically
# Verify auto-renewal
sudo certbot renew --dry-run
```

Certbot sets up a cron job: `/etc/cron.d/certbot` that runs twice daily. Certificates are auto-renewed when they have less than 30 days remaining.

### Production SSL Configuration

```nginx
server {
    listen 443 ssl http2;
    server_name api.myapp.com;

    # Certificate paths (managed by Certbot)
    ssl_certificate     /etc/letsencrypt/live/api.myapp.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.myapp.com/privkey.pem;

    # Modern SSL config (TLS 1.2 + 1.3 only — drops IE11, Android 4.x)
    ssl_protocols TLSv1.2 TLSv1.3;

    # Strong cipher suites — prefer server's order
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;  # TLS 1.3 handles this

    # Session resumption — avoid repeated full handshakes
    ssl_session_cache   shared:SSL:10m;
    ssl_session_timeout 1d;
    ssl_session_tickets off;  # Disable for perfect forward secrecy

    # OCSP stapling — avoids client making separate OCSP request
    ssl_stapling on;
    ssl_stapling_verify on;
    resolver 8.8.8.8 8.8.4.4 valid=300s;
    resolver_timeout 5s;

    # HSTS — tell browsers to ALWAYS use HTTPS for this domain
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload" always;

    location / {
        proxy_pass http://springboot_backend;
        # ... proxy headers
    }
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name api.myapp.com;
    return 301 https://$host$request_uri;
}
```

> [!CAUTION]
> **HSTS with `preload` is essentially irreversible.** Once submitted to the browser preload list, all browsers will refuse plain HTTP connections to your domain for up to 2 years — even if you remove the header. Only add `preload` when you are 100% certain your entire domain and all subdomains will always serve HTTPS. Stripe learned this early. You should too.

### Understanding HSTS

`max-age=63072000` = 2 years in seconds. After a browser sees this header once, it will:
1. Never make a plain HTTP request to this domain again (for 2 years)
2. Reject any SSL certificate errors with no "proceed anyway" option

This prevents SSL stripping attacks where a MITM intercepts the HTTP→HTTPS redirect.

### OCSP Stapling — Why It Matters for Latency

Without OCSP stapling:
1. Client connects to your server
2. Client must separately contact the CA's OCSP server to check if cert is revoked
3. This adds 50-200ms latency on first connection, and fails if CA server is down

With OCSP stapling:
1. NGINX periodically fetches the OCSP response from the CA
2. NGINX "staples" it to the TLS handshake
3. Client gets revocation status instantly, zero extra round trip

---

## 5. Rate Limiting — Protecting Your Critical Endpoints

Rate limiting at NGINX is your first line of defense against brute force, credential stuffing, and API abuse.

### `limit_req_zone` — Token Bucket Algorithm

```nginx
http {
    # Define rate limit zones in the http{} block
    # Zone for general API: 10 req/sec per IP, 10MB shared memory
    limit_req_zone $binary_remote_addr zone=api_general:10m rate=10r/s;

    # Stricter zone for login endpoint: 5 req/min per IP
    limit_req_zone $binary_remote_addr zone=login:10m rate=5r/m;

    # Payment endpoints: 2 req/min per IP
    limit_req_zone $binary_remote_addr zone=payment:10m rate=2r/m;

    # By API key instead of IP (for authenticated APIs)
    limit_req_zone $http_x_api_key zone=api_key_limit:10m rate=100r/s;

    server {
        # General API rate limiting with burst
        location /api/ {
            limit_req zone=api_general burst=20 nodelay;
            # burst=20: allow up to 20 queued requests above limit
            # nodelay: don't delay burst requests, reject if queue full
            proxy_pass http://springboot_backend;
        }

        # Login: strict, no burst
        location /api/auth/login {
            limit_req zone=login burst=3 nodelay;
            limit_req_status 429;
            proxy_pass http://springboot_backend;
        }

        # Payment: very strict
        location /api/payments/ {
            limit_req zone=payment burst=1 nodelay;
            limit_req_status 429;
            proxy_pass http://springboot_backend;
        }
    }
}
```

> [!IMPORTANT]
> **The `burst` parameter is critical.** Without burst, a user who sends 2 requests simultaneously to an endpoint with rate=1r/s gets one rejected immediately. With `burst=5`, NGINX queues excess requests (up to 5) and processes them smoothly. Without `nodelay`, those burst requests are artificially delayed (1 per second). With `nodelay`, burst requests are processed immediately but the counter is still decremented, so if the queue is full, new requests are rejected.

### Real-World Rate Limit Strategy

At Zepto/Blinkit scale, rate limiting is critical because:

1. **Login endpoint** — Attackers run credential stuffing attacks (millions of username/password pairs from data breaches). Without rate limiting, they can try 10,000 combinations per second. With 5r/m per IP + Cloudflare bot detection, this becomes computationally infeasible.

2. **OTP endpoints** — SMS OTPs cost money. An unprotected `/api/auth/send-otp` endpoint that costs ₹0.10/SMS can rack up ₹10,000+ in minutes if attacked. Rate limit to 3 per phone number per 10 minutes at minimum.

3. **Search endpoints** — At Uber scale, search is the most expensive operation (geo-search, ranking, personalization). Rate limiting ensures a single abusive client can't monopolize search infrastructure.

### Rate Limiting by Multiple Keys

```nginx
http {
    # Rate limit per IP
    limit_req_zone $binary_remote_addr zone=by_ip:10m rate=30r/m;

    # Rate limit per user ID (from JWT — requires Lua module or custom header)
    limit_req_zone $http_x_user_id zone=by_user:10m rate=60r/m;

    server {
        location /api/search {
            # Both limits apply — whichever is hit first
            limit_req zone=by_ip   burst=10 nodelay;
            limit_req zone=by_user burst=20 nodelay;
            proxy_pass http://springboot_backend;
        }
    }
}
```

### Custom Error Response for Rate Limiting

```nginx
http {
    limit_req_status 429;

    server {
        error_page 429 /rate_limit_error.json;

        location = /rate_limit_error.json {
            internal;
            default_type application/json;
            return 429 '{"error":"rate_limit_exceeded","message":"Too many requests. Please slow down.","retry_after":60}';
        }
    }
}
```

---

## 6. Connection Limits, Timeouts, and Keepalive

These settings prevent resource exhaustion and ensure your NGINX → Spring Boot connection pool is healthy.

### Timeout Configuration

```nginx
http {
    # How long NGINX waits for Spring Boot to send response headers
    proxy_read_timeout 60s;

    # How long NGINX waits to send the request to Spring Boot
    proxy_send_timeout 60s;

    # How long to wait for Spring Boot to accept a connection
    proxy_connect_timeout 10s;

    # How long a client connection can be idle (keep-alive)
    keepalive_timeout 65s;

    # Max requests per keep-alive connection
    keepalive_requests 1000;

    # Client send timeout — close connection if client stops sending
    client_body_timeout 12s;
    client_header_timeout 12s;

    # Send timeout — close if client stops reading response
    send_timeout 10s;
}
```

> [!WARNING]
> **`proxy_read_timeout` must match your slowest legitimate operation.** If you have a report generation endpoint that takes 45 seconds, `proxy_read_timeout 30s` will cause NGINX to close the connection and return a 504 Gateway Timeout to the client, even though Spring Boot eventually succeeded. Tune per-location if needed.

### Per-Location Timeout Override

```nginx
location /api/reports/generate {
    proxy_read_timeout 120s;  # Reports take up to 2 minutes
    proxy_pass http://springboot_backend;
}

location /api/health {
    proxy_read_timeout 5s;   # Health check should be instant
    proxy_pass http://springboot_backend;
}
```

### Connection Limits

```nginx
http {
    # Limit simultaneous connections per IP
    limit_conn_zone $binary_remote_addr zone=conn_limit:10m;

    server {
        location / {
            limit_conn conn_limit 20;  # Max 20 simultaneous connections per IP
            # (legitimate browsers rarely open more than 6)
            proxy_pass http://springboot_backend;
        }
    }
}
```

### Client Body Size Limits

```nginx
http {
    # Default: 1MB. For file upload endpoints, increase per location.
    client_max_body_size 1m;

    server {
        # Profile picture upload
        location /api/users/avatar {
            client_max_body_size 5m;
            proxy_pass http://springboot_backend;
        }

        # Document upload
        location /api/documents/upload {
            client_max_body_size 50m;
            proxy_pass http://springboot_backend;
        }
    }
}
```

---

## 7. DDoS Basics — What NGINX Handles vs What Requires External Tools

### Understanding DDoS Attack Layers

```
OSI Layer 3/4 (Network/Transport):
  - SYN flood: millions of TCP SYN packets, exhausting server connection tables
  - UDP flood: volumetric attack filling network pipes with UDP traffic
  - ICMP flood (ping flood): saturates bandwidth
  - These bypass NGINX entirely — handled at network infrastructure level

OSI Layer 7 (Application):
  - HTTP flood: millions of legitimate-looking GET/POST requests
  - Slow Loris: keep connections open with partial headers
  - Cache-busting attacks: requests with random query params bypass CDN cache
  - These are what NGINX can mitigate
```

### What NGINX Handles

| Attack Type | NGINX Defense |
|---|---|
| HTTP flood | `limit_req_zone` rate limiting |
| Slow Loris | `client_header_timeout`, `client_body_timeout` |
| Connection flood | `limit_conn` per-IP connection limits |
| Large payload abuse | `client_max_body_size` |
| Bad User-Agent bots | `if ($http_user_agent ~ "BadBot")` blocking |
| Direct IP access (bypassing domain) | `default_server` returning 444 |

### What NGINX Cannot Handle

| Attack Type | Required Solution |
|---|---|
| Layer 3/4 volumetric (>1 Gbps) | AWS Shield Advanced, Cloudflare Magic Transit |
| Botnet HTTP flood (millions of IPs) | Cloudflare Bot Management, WAF |
| State-exhaustion attacks (SYN flood) | Cloud provider network-level protection |
| Amplification attacks (DNS, NTP) | Upstream ISP / Cloud provider |

> [!IMPORTANT]
> A 100 Gbps DDoS attack will saturate your server's network interface and physical uplink before a single packet reaches NGINX. No amount of NGINX configuration will help. This is why you need Cloudflare (anycast network absorbs the traffic at their edge) or AWS Shield Advanced (network-level traffic scrubbing). For any startup processing real money, Cloudflare's free tier is non-negotiable as a baseline.

### NGINX Defense Configuration

```nginx
http {
    # Block requests that don't have a Host header (malformed / scanner bots)
    server {
        listen 80 default_server;
        listen 443 ssl default_server;
        return 444;  # 444 = NGINX silently closes connection (no response)
    }

    # Block empty User-Agent (automated scanners)
    map $http_user_agent $bad_agent {
        default         0;
        ""              1;  # Empty UA
        "~*sqlmap"      1;  # SQLMap scanner
        "~*nikto"       1;  # Nikto scanner
        "~*masscan"     1;  # Masscan
        "~*python-requests" 0;  # Allow (your own scripts may use this)
    }

    server {
        if ($bad_agent) {
            return 403;
        }
    }
}
```

---

## 8. Cloudflare Integration

Cloudflare sits in front of NGINX, acting as a global CDN + WAF + DDoS protection layer.

```
[User in Mumbai]
       ↓
[Cloudflare Edge — Mumbai POP]  ← DDoS scrubbing, WAF, rate limiting, bot detection
       ↓
[Your NGINX server — e.g., AWS Mumbai]
       ↓
[Spring Boot :8080]
```

### DNS Proxying ("Orange Cloud")

When you enable Cloudflare's proxy (the orange cloud in DNS settings), your real server IP is hidden. Users connect to Cloudflare's IP, not yours. This is critical because:
1. Attackers cannot bypass Cloudflare by finding your origin IP (if you've never exposed it)
2. Cloudflare's anycast network handles DDoS before it reaches your origin

> [!CAUTION]
> **The "real IP leak" problem.** If you sent any emails from your server, made API calls without going through Cloudflare, or your origin IP ever appeared in SSL cert transparency logs before you enabled Cloudflare, attackers can find your real IP. Always use `iptables` to block all non-Cloudflare traffic to your origin.

### Allowing Only Cloudflare IPs at NGINX

```nginx
# /etc/nginx/conf.d/cloudflare.conf
# Cloudflare IPv4 ranges (update periodically from https://www.cloudflare.com/ips/)
geo $not_cloudflare {
    default          1;

    # Cloudflare IP ranges
    103.21.244.0/22  0;
    103.22.200.0/22  0;
    103.31.4.0/22    0;
    104.16.0.0/13    0;
    104.24.0.0/14    0;
    108.162.192.0/18 0;
    131.0.72.0/22    0;
    141.101.64.0/18  0;
    162.158.0.0/15   0;
    172.64.0.0/13    0;
    173.245.48.0/20  0;
    188.114.96.0/20  0;
    190.93.240.0/20  0;
    197.234.240.0/22 0;
    198.41.128.0/17  0;

    # Allow localhost (for internal health checks)
    127.0.0.1        0;
}

server {
    if ($not_cloudflare) {
        return 403;
    }
}
```

Additionally, use `iptables`:
```bash
# Allow Cloudflare IPs, deny everything else on port 443
iptables -A INPUT -p tcp --dport 443 -s 103.21.244.0/22 -j ACCEPT
iptables -A INPUT -p tcp --dport 443 -j DROP
```

### Restoring Real Client IP from Cloudflare

When Cloudflare proxies traffic, NGINX sees Cloudflare's IP, not the user's. Cloudflare adds the real IP in `CF-Connecting-IP` header.

```nginx
# /etc/nginx/conf.d/cloudflare-realip.conf
real_ip_header CF-Connecting-IP;

# Cloudflare IPs to trust
set_real_ip_from 103.21.244.0/22;
set_real_ip_from 103.22.200.0/22;
# ... (all Cloudflare ranges)
```

Now `$remote_addr` in NGINX contains the actual client IP, which propagates correctly to Spring Boot via `X-Real-IP`.

### Cloudflare WAF Rules

Cloudflare's Web Application Firewall can block:
- OWASP Top 10 attacks (SQL injection, XSS) before they reach your server
- Known bot traffic (scrapers, vulnerability scanners)
- Requests from specific countries (geo-blocking)
- Requests matching custom patterns (e.g., block any request with `../` in URL path)

---

## 9. Gzip Compression

Gzip reduces bandwidth for text-based responses (JSON, HTML, CSS, JS) by 60-80%.

```nginx
http {
    gzip on;
    gzip_vary on;            # Add Vary: Accept-Encoding header
    gzip_proxied any;        # Compress for all proxied requests
    gzip_comp_level 6;       # 1 (fastest) to 9 (best compression). 6 is sweet spot.
    gzip_buffers 16 8k;
    gzip_http_version 1.1;
    gzip_min_length 256;     # Don't compress responses smaller than 256 bytes

    gzip_types
        text/plain
        text/css
        text/xml
        text/javascript
        application/json
        application/javascript
        application/xml
        application/xml+rss
        application/atom+xml
        image/svg+xml;       # SVG is text-based, compresses well
        # Do NOT add: image/jpeg, image/png, image/webp (already compressed)
        # Do NOT add: video/* (already compressed)
}
```

> [!NOTE]
> A typical Spring Boot REST API response of 10KB JSON compresses to ~1.5KB. At 10,000 req/s, this saves 85MB/s of outbound bandwidth. At AWS data transfer pricing (~$0.09/GB), this is ~$220/month in savings — plus your users see faster responses, especially on mobile.

### Brotli — Better Than Gzip

Brotli (developed by Google) achieves 15-25% better compression than gzip for text content. Supported by all modern browsers.

```bash
# Install Brotli module for NGINX
sudo apt install libnginx-mod-http-brotli-filter libnginx-mod-http-brotli-static
```

```nginx
http {
    brotli on;
    brotli_comp_level 6;
    brotli_types text/plain application/json text/css application/javascript;
}
```

---

## 10. Security Headers

Security headers are HTTP response headers that instruct the browser on how to behave. They prevent entire classes of attacks.

```nginx
server {
    # Prevent MIME-type sniffing (browser won't treat text/plain as executable)
    add_header X-Content-Type-Options "nosniff" always;

    # Prevent clickjacking (iframe embedding)
    add_header X-Frame-Options "DENY" always;
    # Or: SAMEORIGIN (allows your own domain to iframe)

    # Enable browser XSS filter (legacy, but harmless)
    add_header X-XSS-Protection "1; mode=block" always;

    # HSTS (already covered in SSL section)
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains" always;

    # Referrer Policy — don't leak URL to third parties
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # Permissions Policy — disable dangerous browser features
    add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;

    # Content Security Policy — most powerful, most complex
    add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'nonce-{random}'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'; connect-src 'self' https://api.myapp.com; frame-ancestors 'none';" always;

    # Remove server identity
    server_tokens off;
    # add_header Server "";  # Some versions support this
}
```

> [!IMPORTANT]
> The `always` flag on `add_header` is critical. Without it, headers are only added to 200/204/301/302 responses. With `always`, they're added to ALL responses including 404, 500, etc. This matters because error responses are often where browsers make security decisions.

### Content-Security-Policy (CSP) Deep Dive

CSP is the most powerful security header. It tells the browser: "Only load resources from these trusted sources." It prevents:
- XSS attacks (injected scripts from untrusted domains can't execute)
- Data injection (inline scripts blocked by default)
- Clickjacking (via `frame-ancestors`)

For a pure REST API (no browser frontend served from same origin), CSP is less relevant. For full-stack apps serving HTML, it's essential.

---

## 11. gzip + Buffer Tuning for High Throughput

```nginx
http {
    # Output buffers — how much to buffer before sending to client
    proxy_buffering on;
    proxy_buffer_size   128k;
    proxy_buffers       4 256k;
    proxy_busy_buffers_size 256k;

    # If response is larger than this, write to temp file instead of memory
    proxy_temp_file_write_size 256k;

    # Don't buffer small responses (< 4KB) — send immediately
    # proxy_buffering off;  # Use this for SSE / streaming responses

    # Open file cache — avoid repeated stat() calls for static files
    open_file_cache max=1000 inactive=20s;
    open_file_cache_valid 30s;
    open_file_cache_min_uses 2;
    open_file_cache_errors on;
}
```

---

## 12. Health Check Endpoint — Traffic Routing for Unhealthy Instances

Your Spring Boot app needs a health endpoint that NGINX (and your orchestrator) can poll.

### Spring Boot Actuator Health Endpoint

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
      base-path: /actuator
  endpoint:
    health:
      show-details: when_authorized  # Don't expose details publicly
      probes:
        enabled: true  # Enables /actuator/health/liveness and /actuator/health/readiness
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
```

### Custom Health Indicator

```kotlin
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class DatabaseHealthIndicator(
    private val dataSource: DataSource
) : HealthIndicator {

    override fun health(): Health {
        return try {
            dataSource.connection.use { conn ->
                val stmt = conn.prepareStatement("SELECT 1")
                stmt.execute()
            }
            Health.up()
                .withDetail("database", "PostgreSQL")
                .withDetail("status", "reachable")
                .build()
        } catch (ex: Exception) {
            Health.down()
                .withDetail("error", ex.message)
                .build()
        }
    }
}
```

### NGINX Active Health Check (NGINX Plus / Community Module)

For Open Source NGINX, use a simple passive health check with a separate monitoring endpoint:

```nginx
upstream springboot_backend {
    server 10.0.0.1:8080 max_fails=3 fail_timeout=30s;
    server 10.0.0.2:8080 max_fails=3 fail_timeout=30s;
    server 10.0.0.3:8080 max_fails=3 fail_timeout=30s;
    keepalive 32;
}

server {
    # Health check endpoint — restrict access to monitoring systems only
    location /actuator/health {
        proxy_pass http://springboot_backend;
        access_log off;  # Don't pollute access logs with health checks
        allow 10.0.0.0/8;    # Internal monitoring
        allow 127.0.0.1;
        deny all;
    }
}
```

> [!NOTE]
> In Kubernetes, you use liveness and readiness probes at the pod level, not NGINX health checks. NGINX health checks matter when you're managing your own VMs/EC2 instances behind an NGINX load balancer without Kubernetes. With Kubernetes, NGINX Ingress Controller + K8s readiness probes handle traffic routing automatically — unhealthy pods are removed from service endpoints.

---

## 13. Complete Production nginx.conf

This is a real-world NGINX configuration for a Spring Boot REST API with 3 upstream instances.

```nginx
# /etc/nginx/nginx.conf

user nginx;
worker_processes auto;  # One worker per CPU core
worker_rlimit_nofile 65535;  # Max open file descriptors per worker

error_log /var/log/nginx/error.log warn;
pid       /var/run/nginx.pid;

events {
    worker_connections 4096;   # Max connections per worker
    use epoll;                  # Linux epoll event model (most efficient)
    multi_accept on;            # Accept multiple connections per event loop iteration
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    # =========================================================
    # Logging
    # =========================================================
    log_format main '$remote_addr - $remote_user [$time_local] '
                    '"$request" $status $body_bytes_sent '
                    '"$http_referer" "$http_user_agent" '
                    'rt=$request_time uct=$upstream_connect_time '
                    'uht=$upstream_header_time urt=$upstream_response_time';

    access_log /var/log/nginx/access.log main;

    # =========================================================
    # Performance
    # =========================================================
    sendfile           on;
    tcp_nopush         on;   # Send headers and body in single TCP packet
    tcp_nodelay        on;   # Disable Nagle algorithm for real-time
    keepalive_timeout  65s;
    keepalive_requests 1000;
    server_tokens      off;  # Hide NGINX version from responses

    # =========================================================
    # Gzip
    # =========================================================
    gzip              on;
    gzip_vary         on;
    gzip_proxied      any;
    gzip_comp_level   6;
    gzip_min_length   256;
    gzip_types        text/plain text/css application/json
                      application/javascript text/xml
                      application/xml image/svg+xml;

    # =========================================================
    # Rate Limiting Zones
    # =========================================================
    limit_req_zone $binary_remote_addr zone=api_zone:10m    rate=30r/s;
    limit_req_zone $binary_remote_addr zone=login_zone:10m  rate=5r/m;
    limit_req_zone $binary_remote_addr zone=otp_zone:10m    rate=3r/m;
    limit_req_zone $binary_remote_addr zone=payment_zone:10m rate=10r/m;
    limit_conn_zone $binary_remote_addr zone=conn_zone:10m;

    # =========================================================
    # Bot / Bad Agent Blocking
    # =========================================================
    map $http_user_agent $bad_agent {
        default         0;
        ""              1;
        "~*sqlmap"      1;
        "~*nikto"       1;
        "~*masscan"     1;
        "~*zgrab"       1;
    }

    # =========================================================
    # Upstream (Spring Boot instances)
    # =========================================================
    upstream springboot_backend {
        least_conn;
        server 10.0.1.10:8080 max_fails=3 fail_timeout=30s;
        server 10.0.1.11:8080 max_fails=3 fail_timeout=30s;
        server 10.0.1.12:8080 max_fails=3 fail_timeout=30s;
        keepalive 64;
    }

    # =========================================================
    # Default server — reject direct IP access
    # =========================================================
    server {
        listen 80  default_server;
        listen 443 ssl default_server;
        ssl_certificate     /etc/nginx/ssl/dummy.crt;
        ssl_certificate_key /etc/nginx/ssl/dummy.key;
        return 444;
    }

    # =========================================================
    # HTTP → HTTPS redirect
    # =========================================================
    server {
        listen 80;
        server_name api.myapp.com;
        return 301 https://$host$request_uri;
    }

    # =========================================================
    # Main HTTPS server
    # =========================================================
    server {
        listen 443 ssl http2;
        server_name api.myapp.com;

        # --- SSL ---
        ssl_certificate     /etc/letsencrypt/live/api.myapp.com/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/api.myapp.com/privkey.pem;
        ssl_protocols       TLSv1.2 TLSv1.3;
        ssl_ciphers         ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
        ssl_prefer_server_ciphers off;
        ssl_session_cache   shared:SSL:10m;
        ssl_session_timeout 1d;
        ssl_session_tickets off;
        ssl_stapling        on;
        ssl_stapling_verify on;
        resolver 8.8.8.8 valid=300s;

        # --- Security Headers ---
        add_header Strict-Transport-Security "max-age=63072000; includeSubDomains" always;
        add_header X-Content-Type-Options    "nosniff"                             always;
        add_header X-Frame-Options           "DENY"                                always;
        add_header X-XSS-Protection          "1; mode=block"                       always;
        add_header Referrer-Policy           "strict-origin-when-cross-origin"     always;
        add_header Permissions-Policy        "geolocation=(), microphone=()"       always;

        # --- Request size limits ---
        client_max_body_size 10m;
        client_body_timeout  12s;
        client_header_timeout 12s;

        # --- Timeouts ---
        proxy_connect_timeout 10s;
        proxy_send_timeout    60s;
        proxy_read_timeout    60s;
        send_timeout          10s;

        # --- Block bad agents ---
        if ($bad_agent) { return 403; }

        # --- Proxy common headers ---
        proxy_http_version 1.1;
        proxy_set_header Connection         "";
        proxy_set_header Host               $host;
        proxy_set_header X-Real-IP          $remote_addr;
        proxy_set_header X-Forwarded-For    $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto  $scheme;

        # =====================================================
        # Routes
        # =====================================================

        # Health check — internal only
        location = /actuator/health {
            proxy_pass http://springboot_backend;
            access_log off;
            allow 10.0.0.0/8;
            allow 127.0.0.1;
            deny all;
        }

        # Auth endpoints — strict rate limiting
        location /api/v1/auth/login {
            limit_req zone=login_zone burst=3 nodelay;
            limit_req_status 429;
            limit_conn conn_zone 5;
            proxy_pass http://springboot_backend;
        }

        location /api/v1/auth/otp {
            limit_req zone=otp_zone burst=1 nodelay;
            limit_req_status 429;
            proxy_pass http://springboot_backend;
        }

        # Payment endpoints
        location /api/v1/payments/ {
            limit_req zone=payment_zone burst=5 nodelay;
            limit_req_status 429;
            proxy_read_timeout 90s;  # Payment processing can be slow
            proxy_pass http://springboot_backend;
        }

        # File upload
        location /api/v1/upload/ {
            client_max_body_size 50m;
            proxy_read_timeout 120s;
            proxy_pass http://springboot_backend;
        }

        # General API
        location /api/ {
            limit_req zone=api_zone burst=50 nodelay;
            limit_conn conn_zone 20;
            proxy_pass http://springboot_backend;
        }

        # Static assets (if served from NGINX directly)
        location /static/ {
            root /var/www/myapp;
            expires 1y;
            add_header Cache-Control "public, immutable";
            access_log off;
        }

        # Block access to sensitive paths
        location ~ /\. {
            deny all;
        }

        location ~ /actuator/ {
            deny all;  # Block all actuator except /health (handled above)
        }
    }
}
```

---

## 14. NGINX Worker Process Tuning

```nginx
worker_processes auto;    # Set to number of CPU cores
worker_rlimit_nofile 65535;

events {
    worker_connections 4096;
}
```

**Max concurrent connections = `worker_processes × worker_connections`**

On a 4-core server: `4 × 4096 = 16,384` simultaneous connections.

Each connection uses ~1-2KB of memory. 16,384 connections = ~16-32MB RAM. This is negligible.

```bash
# Check current CPU cores
nproc

# Check current file descriptor limit
ulimit -n

# Increase system file descriptor limit
echo "nginx soft nofile 65535" >> /etc/security/limits.conf
echo "nginx hard nofile 65535" >> /etc/security/limits.conf
```

---

## 15. Common Production Pitfalls

### Pitfall 1: NGINX Caching Upstream Errors

```nginx
# By default, NGINX will retry failed requests on the next upstream server
# This can cause duplicate POST requests (dangerous for payments, orders)
proxy_next_upstream error timeout;
# NOT: proxy_next_upstream error timeout invalid_header http_500 http_502;
# Retrying on 500 will double-submit POST requests!
```

### Pitfall 2: Missing `$host` in `proxy_set_header`

Without `proxy_set_header Host $host`, Spring Boot sees every request as coming to `localhost:8080` — this breaks any host-based routing, virtual hosting, or URL generation (e.g., email confirmation links with wrong domain).

### Pitfall 3: `proxy_buffering off` for SSE

Server-Sent Events (SSE) and streaming responses MUST have buffering disabled, or the client never receives partial data:

```nginx
location /api/v1/events {
    proxy_pass http://springboot_backend;
    proxy_buffering off;
    proxy_cache off;
    proxy_set_header Connection '';
    proxy_http_version 1.1;
    chunked_transfer_encoding on;
}
```

### Pitfall 4: Reload vs Restart

```bash
# SAFE: Zero-downtime reload (re-reads config, gracefully drains connections)
sudo nginx -s reload
# or
sudo systemctl reload nginx

# DANGEROUS: Full restart (drops all active connections)
sudo systemctl restart nginx
```

Always use `nginx -t` to test config before reload:

```bash
sudo nginx -t && sudo nginx -s reload
```

---

## Summary

| Layer | Tool | What it handles |
|---|---|---|
| Layer 3/4 DDoS | Cloudflare Magic Transit / AWS Shield | Volumetric attacks, SYN floods |
| Edge WAF | Cloudflare WAF | OWASP Top 10, bot traffic, geo-blocking |
| Reverse Proxy | NGINX | TLS termination, load balancing, rate limiting |
| Application | Spring Boot | Business logic |
| App Security | Spring Security | AuthN/AuthZ, CSRF |

The architecture is defense-in-depth. Each layer handles what it's best at. Never rely on a single layer for security.

---

*References: NGINX documentation, Let's Encrypt, Cloudflare Developer Docs, Pro Spring Boot 3 with Kotlin (Späth & Gutierrez, 2025)*
