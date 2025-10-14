# 백엔드 결제 도메인 서버 프로젝트 분석 보고서

## 📋 프로젝트 개요

이 프로젝트는 **나노바나나 페이먼츠**의 결제 도메인 서버를 구현한 백엔드 과제 프로젝트입니다.

### 주요 특징
- **헥사고널 아키텍처** 기반 설계
- **멀티모듈 구조** (Gradle 기반)
- **Kotlin + Spring Boot** 기술 스택
- **JDK 21** 사용
- **H2 인메모리 DB** 기본 설정

### 과제 목표
1. 새로운 결제 제휴사 연동
2. 결제 내역 조회 API (통계 포함, 커서 기반 페이지네이션)
3. 제휴사별 수수료 정책 적용

---

## 🏗️ 아키텍처 구조

### 헥사고널 아키텍처 레이어
```
┌─────────────────────────────────────┐
│           Bootstrap Layer           │ ← API Gateway (실행 진입점)
├─────────────────────────────────────┤
│          Application Layer          │ ← 유스케이스 & 포트
├─────────────────────────────────────┤
│            Domain Layer             │ ← 순수 도메인 모델
├─────────────────────────────────────┤
│        Infrastructure Layer         │ ← JPA, Repository
├─────────────────────────────────────┤
│          External Layer             │ ← PG 클라이언트
└─────────────────────────────────────┘
```

---

## 📁 모듈별 상세 분석

### 1. **modules/domain** (도메인 계층)
**역할**: 순수한 비즈니스 로직과 도메인 모델을 담당

**주요 구성요소**:
- `Partner.kt` - 제휴사 도메인 모델
- `FeePolicy.kt` - 수수료 정책 도메인 모델  
- `Payment.kt` - 결제 도메인 모델
- `FeeCalculator.kt` - 수수료 계산 로직

**특징**:
- 프레임워크 의존성 없음 (순수 Kotlin)
- 의존성: 없음 (최상위 계층)

### 2. **modules/application** (애플리케이션 계층)
**역할**: 유스케이스 구현과 포트 정의

**주요 구성요소**:
```
application/
├── partner/
│   └── port/out/          # 제휴사 관련 포트
├── payment/
│   ├── port/in/           # 결제 입력 포트 (UseCase)
│   ├── port/out/          # 결제 출력 포트 (Repository)
│   └── service/           # 결제 서비스 구현체
└── pg/
    └── port/out/          # PG 클라이언트 포트
```

**핵심 서비스**:
- `PaymentService.kt` - 결제 생성 유스케이스 (현재 하드코드된 수수료)
- `QueryPaymentsService.kt` - 결제 조회 유스케이스

**의존성**: `domain` 모듈만 의존

### 3. **modules/infrastructure/persistence** (영속성 계층)
**역할**: 데이터베이스 접근 및 JPA 구현

**주요 구성요소**:
```
persistence/
├── config/
│   └── JpaConfig.kt       # JPA 설정
├── partner/
│   ├── entity/            # 제휴사 엔티티
│   ├── repository/        # JPA 리포지토리
│   └── adapter/           # 포트 구현체
└── payment/
    ├── entity/            # 결제 엔티티
    ├── repository/        # JPA 리포지토리
    └── adapter/           # 포트 구현체
```

**기술 스택**:
- Spring Data JPA
- H2 Database (기본)
- MariaDB (선택적)

**의존성**: `domain`, `application` 모듈 의존

### 4. **modules/external/pg-client** (외부 연동 계층)
**역할**: PG 사업자와의 외부 연동

**주요 구성요소**:
- PG 클라이언트 어댑터
- Mock PG 구현체
- TestPay 연동 예시

**의존성**: `application`, `domain` 모듈 의존

### 5. **modules/bootstrap/api-payment-gateway** (실행 계층)
**역할**: 애플리케이션 실행 진입점 및 API 컨트롤러

**주요 구성요소**:
- `PgApiApplication.kt` - Spring Boot 메인 클래스
- `PaymentController.kt` - REST API 컨트롤러
- `DataInitializer.kt` - 초기 데이터 설정

**API 엔드포인트**:
- `POST /api/v1/payments` - 결제 생성
- `GET /api/v1/payments` - 결제 조회 (통계 포함)

**의존성**: 모든 하위 모듈 의존 (실행을 위해)

### 6. **modules/common** (공통 모듈)
**역할**: 공통 유틸리티 및 설정

---

## 🗄️ 데이터베이스 스키마

### 주요 테이블
1. **partner** - 제휴사 마스터
   - `id`, `code`, `name`, `active`

2. **partner_fee_policy** - 제휴사별 수수료 정책
   - `partner_id`, `effective_from`, `percentage`, `fixed_fee`

3. **payment** - 결제 내역
   - `partner_id`, `amount`, `applied_fee_rate`, `fee_amount`, `net_amount`
   - `card_bin`, `card_last4`, `approval_code`, `approved_at`, `status`

### 인덱스 전략
- `payment(created_at desc, id desc)` - 커서 페이지네이션용
- `payment(partner_id, created_at desc)` - 제휴사별 조회용

---

## 🔧 기술 스택 상세

### 빌드 도구
- **Gradle** (Kotlin DSL)
- **Kotlin 1.9.25**
- **Spring Boot 3.4.4**

### 의존성 관리
- `gradle/libs.versions.toml` - 버전 중앙 관리
- MockK, JUnit5 테스트 프레임워크
- Ktlint 코드 스타일 검사

### 데이터베이스
- **H2** (인메모리, 기본)
- **MariaDB** (선택적)

---

## ⚠️ 현재 상태 및 개선 필요사항

### 1. 하드코드된 수수료 계산
- `PaymentService`에서 3% + 100원 고정 수수료 사용
- **개선 필요**: 제휴사별 정책 기반 계산으로 변경

### 2. 미완성 기능들
- 결제 내역 조회 API 구현 필요
- 커서 기반 페이지네이션 구현 필요
- 제휴사별 수수료 정책 적용 로직 필요

### 3. 테스트 커버리지
- 현재 테스트 파일은 기본 구조만 존재
- 단위/통합 테스트 구현 필요

---

## 🚀 실행 방법

```bash
# 빌드 및 테스트
./gradlew build

# API 서버 실행
./gradlew :modules:bootstrap:api-payment-gateway:bootRun

# 코드 스타일 검사
./gradlew ktlintCheck
```

**기본 포트**: 8080

---

## 📊 모듈 의존성 다이어그램

```
bootstrap/api-payment-gateway
    ├── application
    ├── domain
    ├── infrastructure/persistence
    └── external/pg-client

application
    └── domain

infrastructure/persistence
    ├── domain
    └── application

external/pg-client
    ├── domain
    └── application

domain
    └── (의존성 없음)
```

---

## 💡 아키텍처 장점

1. **의존성 역전**: 상위 계층이 하위 계층에 의존하지 않음
2. **모듈 분리**: 각 모듈이 명확한 책임을 가짐
3. **테스트 용이성**: 포트-어댑터 패턴으로 Mock 구현 가능
4. **확장성**: 새로운 PG 연동이나 정책 추가 시 기존 코드 변경 최소화

---

*분석 일시: 2025년 1월 27일*
