# 모니터링 환경 설정

이 디렉토리는 Spring Boot 애플리케이션을 모니터링하기 위한 Prometheus와 Grafana 환경을 제공합니다.

## 구성 요소

- **Prometheus**: 메트릭 수집 및 저장
- **Grafana**: 메트릭 시각화 및 대시보드

## 사용 방법

### 1. 모니터링 환경 시작

```bash
cd infra/monitoring
docker-compose up -d
```

### 2. Spring Boot 애플리케이션 실행

IntelliJ IDEA에서 Spring Boot 애플리케이션을 실행합니다.
- 포트: 8080
- Actuator 엔드포인트: http://localhost:8080/actuator/prometheus

### 3. 모니터링 도구 접속

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000
  - 사용자명: admin
  - 비밀번호: admin

### 4. 모니터링 중지

```bash
docker-compose down
```

## 설정 파일

### Prometheus 설정
- `prometheus/prometheus.yml`: Prometheus 메인 설정
- `prometheus/rules/spring-boot-alerts.yml`: 알림 규칙

### Grafana 설정
- `grafana/provisioning/datasources/prometheus.yml`: Prometheus 데이터소스 설정
- `grafana/provisioning/dashboards/dashboards.yml`: 대시보드 프로비저닝 설정
- `grafana/dashboards/spring-boot-dashboard.json`: Spring Boot 모니터링 대시보드

## 모니터링 메트릭

### HTTP 메트릭
- 요청 수 (Request Rate)
- 응답 시간 (Response Time)
- 에러율 (Error Rate)

### JVM 메트릭
- 메모리 사용량
- GC 통계
- 스레드 정보

### 커스텀 메트릭
- 비즈니스 로직 메트릭
- 데이터베이스 연결 풀 상태

## 알림 규칙

다음과 같은 상황에서 알림이 발생합니다:
- 높은 에러율 (5분간 5xx 에러율 > 10%)
- 높은 응답 시간 (95th percentile > 1초)
- 애플리케이션 다운
- 높은 메모리 사용량 (80% 이상)

## 문제 해결

### Spring Boot 애플리케이션이 Prometheus에서 보이지 않는 경우
1. 애플리케이션이 실행 중인지 확인
2. http://localhost:8080/actuator/prometheus 접속 가능한지 확인
3. Docker 네트워크 설정 확인 (host.docker.internal 사용)

### Grafana에서 데이터가 보이지 않는 경우
1. Prometheus에서 타겟이 UP 상태인지 확인
2. 데이터소스 설정 확인
3. 시간 범위 설정 확인

