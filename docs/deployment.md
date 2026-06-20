# Deployment

## Local Docker Compose

```bash
docker compose -f deploy/docker-compose.yml up --build -d
```

Services:

- Backend: `http://localhost:8080/rule-engine`
- Swagger: `http://localhost:8080/rule-engine/swagger-ui.html`
- Frontend: `http://localhost:5173`
- Nacos: `http://localhost:8848/nacos`

## Kubernetes

Build images:

```bash
docker build -t rule-engine-service:0.1.0 -f backend/rule-engine-service/rule-engine-start/Dockerfile .
docker build -t rule-engine-ui:0.1.0 -f frontend/rule-engine-ui/Dockerfile .
```

Create secret:

```bash
kubectl create secret generic rule-engine-secret \
  --from-literal=mysql-user=rule \
  --from-literal=mysql-password=rule123456
```

Apply:

```bash
kubectl apply -f deploy/k8s/rule-engine-service.yaml
kubectl apply -f deploy/k8s/rule-engine-ui.yaml
```

## Performance Targets

- Single rule execution target: `< 10ms` for warmed simple rules
- 1000-rule concurrent scenario: `P95 <= 200ms`
- Recommended production tuning:
  - Precompile and cache `KieBase` by `ruleCode:version`
  - Use Redis only for metadata/cache invalidation, not hot-path persistence
  - Persist execution logs asynchronously through MQ for high-throughput paths

## Quality Gate

The backend is wired with JaCoCo and domain tests for lifecycle branches and injection guard policy. Expand application/infrastructure tests before production rollout to enforce the 90% target across non-DTO business packages.
