# Redis 캐시 환경 설정

이 디렉토리는 결제 시스템의 성능 개선을 위한 Redis 캐시 환경을 제공합니다.

## 구성 요소

- **Redis**: 메모리 기반 캐시 서버 (포트: 6379)

## 사용 방법

### 1. Redis 인스턴스 시작

```bash
cd infra/cache
docker-compose up -d
```

### 2. Spring Boot 애플리케이션 실행

IntelliJ IDEA에서 Spring Boot 애플리케이션을 실행합니다.
- 포트: 8080
- Redis 연결: localhost:6379

### 3. 캐시 확인

Redis CLI를 통해 캐시 상태를 확인할 수 있습니다:

```bash
docker exec -it payment-redis redis-cli
KEYS payment:*
```

### 4. 중지

```bash
docker-compose down
```

## 캐시 설정

### 캐시 전략

- **Cache-Aside 패턴** 적용
- **결제 조회 API**: 캐시에 데이터가 없으면 DB에서 조회 후 캐시에 저장
- **결제 생성 API**: 새 결제 생성 시 관련 캐시 무효화

### 캐시 네임스페이스

- `paymentQueries`: 결제 조회 결과 (TTL: 10분)
- `paymentSummaries`: 결제 통계 데이터 (TTL: 15분)

### 캐시 키 패턴

- **결제 조회**: `partnerId:status:from:to:limit:cursorAt:cursorId`
- **결제 통계**: `partnerId:status:from:to`

## 설정 파일

### Redis 설정
- `redis.conf`: Redis 서버 설정
- `docker-compose.yml`: Redis 컨테이너 설정

### Spring Boot 설정
- `application.yml`: Redis 연결 및 캐시 설정
- `CacheConfig.kt`: Spring Cache 설정 클래스

## 모니터링

Redis 메모리 사용량과 캐시 히트율을 모니터링할 수 있습니다:

```bash
# Redis 정보 확인
docker exec -it payment-redis redis-cli INFO memory

# 캐시 키 개수 확인  
docker exec -it payment-redis redis-cli DBSIZE
```

## 문제 해결

### Redis 연결 실패 시
1. Redis 컨테이너가 실행 중인지 확인
2. 네트워크 설정 확인 (포트 6379)
3. Spring Boot 애플리케이션 로그 확인

### 캐시 성능 이슈 시
1. Redis 메모리 사용량 확인
2. TTL 설정 검토
3. 캐시 키 패턴 최적화 검토



