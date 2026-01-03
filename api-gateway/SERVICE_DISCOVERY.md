# Service Discovery Options for API Gateway

## Overview

The API Gateway supports **3 service discovery approaches** instead of hardcoding URIs:

1. **SmallRye Stork + Consul** (Recommended - Currently Configured)
2. **SmallRye Stork + Kubernetes DNS** (For K8s deployments)
3. **Static List** (For testing/simple deployments)

---

## ✅ Option 1: SmallRye Stork + Consul (ACTIVE)

**Best for:** Production, multi-instance deployments, cloud environments

### Architecture

```
API Gateway
    ↓
SmallRye Stork (service discovery + load balancing)
    ↓
Consul Agent (service registry)
    ↓
Backend Services (register themselves with Consul)
```

### Features

✅ **Dynamic Discovery** - Services register/deregister automatically
✅ **Health Checks** - Only routes to healthy instances
✅ **Load Balancing** - Round-robin, random, least-requests
✅ **Multi-Instance** - Automatically discovers all instances
✅ **Fault Tolerance** - Removes unhealthy instances
✅ **Zero Downtime Deployments** - New instances auto-discovered

### Configuration

Already configured in `application.properties`:

```properties
# Consul connection
quarkus.consul-config.agent.host-port=localhost:8500

# Stork service discovery per service
quarkus.stork.auth-service.service-discovery.type=consul
quarkus.stork.auth-service.service-discovery.consul-host=localhost
quarkus.stork.auth-service.service-discovery.consul-port=8500
quarkus.stork.auth-service.load-balancer.type=round-robin
```

### Load Balancer Types

| Type | Description | Use Case |
|------|-------------|----------|
| `round-robin` | Distributes evenly across instances | Default, works for most cases |
| `random` | Random selection | Stateless services |
| `least-requests` | Routes to least busy instance | CPU-intensive services |
| `power-of-two-choices` | Picks best of 2 random | Good balance of efficiency |

### Running with Consul

**1. Start Consul**
```bash
cd docker
docker-compose up -d consul
```

Consul UI: http://localhost:8500

**2. Register a Service**

Each backend service must register with Consul on startup:

```java
// In each backend service (auth-service, patient-service, etc.)
@ApplicationScoped
public class ConsulRegistration {

    @ConfigProperty(name = "quarkus.application.name")
    String serviceName;

    @ConfigProperty(name = "quarkus.http.port")
    int port;

    void onStart(@Observes StartupEvent event) {
        // Register with Consul
        // Quarkus does this automatically with quarkus-consul-config
    }
}
```

**3. Automatic Registration (Recommended)**

Add to each backend service's `pom.xml`:
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-consul-config</artifactId>
</dependency>
```

Add to each backend service's `application.properties`:
```properties
# Enable Consul registration
quarkus.consul-config.agent.host-port=localhost:8500
quarkus.consul-config.enabled=true

# Service registration
quarkus.application.name=patient-service
quarkus.http.port=8082

# Health check
quarkus.consul-config.agent.health-check-interval=10s
```

**4. Verify Registration**

```bash
# List all registered services
curl http://localhost:8500/v1/catalog/services

# Get instances of a specific service
curl http://localhost:8500/v1/catalog/service/patient-service
```

### Testing Service Discovery

```bash
# Start Consul
docker-compose up -d consul

# Start backend service (it auto-registers)
cd patient-service
./mvnw quarkus:dev

# Verify in Consul UI
open http://localhost:8500/ui/dc1/services

# Start API Gateway (uses Stork to discover services)
cd api-gateway
./mvnw quarkus:dev

# Test request (gateway auto-discovers patient-service)
curl http://localhost:8080/api/patients
```

---

## Option 2: SmallRye Stork + Kubernetes DNS

**Best for:** Kubernetes deployments

### How It Works

In Kubernetes, services are accessible via DNS:
- `http://patient-service:8080`
- `http://doctor-service:8080`

Stork can use Kubernetes service discovery to find pod IPs.

### Configuration

```properties
# Kubernetes service discovery
quarkus.stork.patient-service.service-discovery.type=kubernetes
quarkus.stork.patient-service.service-discovery.k8s-namespace=hospital
quarkus.stork.patient-service.load-balancer.type=round-robin
```

### Kubernetes Deployment

```yaml
# patient-service deployment
apiVersion: v1
kind: Service
metadata:
  name: patient-service
spec:
  selector:
    app: patient-service
  ports:
    - port: 8080
      targetPort: 8080
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: patient-service
spec:
  replicas: 3  # Stork auto-discovers all 3 pods
  selector:
    matchLabels:
      app: patient-service
  template:
    metadata:
      labels:
        app: patient-service
    spec:
      containers:
      - name: patient-service
        image: patient-service:1.0.0
        ports:
        - containerPort: 8080
```

Stork will automatically discover all 3 pods and load balance requests.

---

## Option 3: Static List (For Testing)

**Best for:** Local development, testing, simple deployments

### Configuration

```properties
# Static list of service instances
quarkus.stork.patient-service.service-discovery.type=static
quarkus.stork.patient-service.service-discovery.address-list=localhost:8082,localhost:8092
quarkus.stork.patient-service.load-balancer.type=round-robin
```

This is useful when you have multiple instances running locally for testing:
- Instance 1: `localhost:8082`
- Instance 2: `localhost:8092`

Stork will load balance between them.

---

## Comparison Table

| Feature | Consul | Kubernetes | Static List | Hardcoded URLs |
|---------|--------|------------|-------------|----------------|
| **Dynamic Discovery** | ✅ Yes | ✅ Yes | ❌ No | ❌ No |
| **Health Checks** | ✅ Yes | ✅ Yes | ❌ No | ❌ No |
| **Load Balancing** | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No |
| **Multi-Instance** | ✅ Yes | ✅ Yes | ✅ Manual | ❌ No |
| **Configuration Changes** | ✅ None | ✅ None | ⚠️ Manual | ❌ Manual |
| **Fault Tolerance** | ✅ Auto | ✅ Auto | ❌ No | ❌ No |
| **Infrastructure Required** | Consul | Kubernetes | None | None |
| **Best For** | Production | K8s | Testing | ❌ Never |

---

## Migration from Hardcoded URLs

### Before (ServiceRouter.java)

```java
@ConfigProperty(name = "services.patient-service.url")
String patientServiceUrl; // http://localhost:8082

public void route(RoutingContext context) {
    String targetUrl = patientServiceUrl + path;
    webClient.requestAbs(method, targetUrl).send();
}
```

**Problems:**
- ❌ Hardcoded in config
- ❌ No load balancing
- ❌ No health checks
- ❌ Manual updates when instances change

### After (StorkServiceRouter.java)

```java
@Inject
Stork stork;

public void route(RoutingContext context) {
    stork.getService("patient-service")
        .selectInstanceAndRecordStart(false)
        .thenAccept(instance -> {
            String targetUrl = String.format("http://%s:%d%s",
                instance.getHost(), instance.getPort(), path);
            webClient.requestAbs(method, targetUrl).send();
        });
}
```

**Benefits:**
- ✅ Dynamic discovery from Consul
- ✅ Automatic load balancing
- ✅ Health checks integrated
- ✅ Zero-config when instances change

---

## Service Registration Example

Each backend service should register with Consul on startup.

### Auto-Registration (Recommended)

**pom.xml:**
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-consul-config</artifactId>
</dependency>
```

**application.properties:**
```properties
# Service info
quarkus.application.name=patient-service
quarkus.http.host=0.0.0.0
quarkus.http.port=8082

# Consul registration
quarkus.consul-config.agent.host-port=localhost:8500
quarkus.consul-config.enabled=true

# Health check configuration
quarkus.consul-config.agent.health-check-interval=10s
quarkus.consul-config.agent.health-check-timeout=5s
```

Quarkus will automatically:
1. Register service with Consul on startup
2. Deregister on shutdown
3. Report health status from `/q/health` endpoint

### Manual Registration (Alternative)

```java
@ApplicationScoped
public class ConsulServiceRegistration {

    @ConfigProperty(name = "quarkus.application.name")
    String serviceName;

    @ConfigProperty(name = "quarkus.http.port")
    int port;

    void onStart(@Observes StartupEvent event, Vertx vertx) {
        WebClient client = WebClient.create(vertx);

        JsonObject registration = new JsonObject()
            .put("ID", serviceName + "-" + UUID.randomUUID())
            .put("Name", serviceName)
            .put("Address", "localhost")
            .put("Port", port)
            .put("Check", new JsonObject()
                .put("HTTP", "http://localhost:" + port + "/q/health")
                .put("Interval", "10s")
                .put("Timeout", "5s")
            );

        client.put(8500, "localhost", "/v1/agent/service/register")
            .sendJsonObject(registration)
            .subscribe().with(
                response -> Log.infof("Registered with Consul: %s", serviceName),
                error -> Log.errorf("Failed to register with Consul: %s", error)
            );
    }
}
```

---

## Monitoring Service Discovery

### Consul UI

Access Consul UI at http://localhost:8500/ui

You'll see:
- **Services**: All registered services
- **Nodes**: Service instances
- **Health Checks**: Pass/fail status
- **Key/Value**: Configuration (optional)

### Stork Metrics

Stork exposes metrics at `/q/metrics`:

```
stork_service_selection_duration_seconds{service="patient-service"}
stork_service_selection_failures_total{service="patient-service"}
stork_instance_health_checks_total{service="patient-service",instance="localhost:8082"}
```

### Gateway Logs

```bash
# View Stork service resolution
./mvnw quarkus:dev

# Logs show:
# Stork resolved patient-service to http://10.0.0.5:8082
# Routed GET /api/patients → 200 via http://10.0.0.5:8082/api/patients
```

---

## Troubleshooting

### Service Not Discovered

**Problem:** Stork can't find service instances

**Solution:**
1. Check Consul is running: `curl http://localhost:8500/v1/catalog/services`
2. Verify service is registered: `curl http://localhost:8500/v1/catalog/service/patient-service`
3. Check backend service logs for registration errors
4. Ensure Consul agent address is correct in both gateway and backend

### Health Check Failing

**Problem:** Service registered but marked unhealthy

**Solution:**
1. Check `/q/health` endpoint: `curl http://localhost:8082/q/health`
2. Verify service is actually running
3. Check health check interval/timeout in Consul config

### Load Balancing Not Working

**Problem:** All requests go to same instance

**Solution:**
1. Verify multiple instances registered: `curl http://localhost:8500/v1/catalog/service/patient-service`
2. Check load balancer type: `quarkus.stork.patient-service.load-balancer.type=round-robin`
3. Enable debug logs: `quarkus.log.category."io.smallrye.stork".level=DEBUG`

---

## Production Deployment

### High Availability Setup

**Consul Cluster (3 nodes minimum):**
```bash
docker-compose -f docker-compose-ha.yml up -d
```

**Multiple Service Instances:**
```bash
# Patient service - instance 1
QUARKUS_HTTP_PORT=8082 ./mvnw quarkus:dev

# Patient service - instance 2
QUARKUS_HTTP_PORT=8092 ./mvnw quarkus:dev

# Patient service - instance 3
QUARKUS_HTTP_PORT=8102 ./mvnw quarkus:dev
```

All 3 instances auto-register with Consul, and Stork load balances across them.

### Kubernetes Deployment

Use Kubernetes service discovery instead of Consul:

```properties
quarkus.stork.patient-service.service-discovery.type=kubernetes
quarkus.stork.patient-service.service-discovery.k8s-namespace=hospital
```

No Consul needed - Kubernetes DNS handles discovery.

---

## Summary

**Currently Configured:** SmallRye Stork + Consul

**Advantages:**
- ✅ No hardcoded URLs
- ✅ Dynamic service discovery
- ✅ Health checks integrated
- ✅ Load balancing built-in
- ✅ Multi-instance support
- ✅ Zero-downtime deployments

**To Use:**
1. Start Consul: `docker-compose up -d consul`
2. Backend services auto-register with Consul
3. Gateway uses Stork to discover services
4. Requests automatically load balanced

**Next Steps:**
- Deploy backend services with Consul registration
- Monitor service health in Consul UI
- Scale services up/down (Stork auto-discovers changes)
