# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

Spring Boot, Kafka, Redis, MySQL을 사용한 Kotlin 마이크로서비스 샘플 프로젝트입니다. 이벤트 기반 통신을 사용하는 기본적인 MSA(Microservices Architecture) 패턴을 보여줍니다.

### 아키텍처

- **멀티 모듈 Gradle 프로젝트**: 루트 `build.gradle`에서 공유 의존성과 플러그인 설정
- **마이크로서비스**: 현재 `post-server`와 `user-server` 모듈 포함 (참고: `comment-server`는 settings에 참조되어 있지만 디렉터리가 존재하지 않음)
- **이벤트 기반 통신**: 서비스 간 메시징을 위한 Kafka 사용
- **서비스별 데이터베이스** 패턴: 각 서비스마다 별도의 MySQL 인스턴스
- **캐싱 레이어**: 성능 최적화를 위한 Redis 사용
- **패키지 구조**: 각 서비스는 `msa.{service-name}` 패턴을 따르며 표준 Spring Boot 계층 구조 (controller, service, repository, dto, model, config)

### 인프라 의존성

서비스들은 다음 외부 의존성이 필요합니다 (`compose.yaml`에서 설정):
- **MySQL 데이터베이스**:
  - post-server: `mysql-post` (포트 3307)
  - user-server: `mysql-user` (포트 3308)
- **Redis**: 캐싱용 포트 6379
- **Kafka**: KRaft 모드 포트 9092 (Zookeeper 불필요)

## 개발 명령어

### 빌드 및 테스트
```bash
# 모든 모듈 빌드
./gradlew build

# 특정 모듈 빌드
./gradlew post-server:build
./gradlew user-server:build

# 테스트 실행 (모든 모듈)
./gradlew test

# 특정 모듈 테스트
./gradlew post-server:test
./gradlew user-server:test

# 단일 테스트 클래스 실행
./gradlew post-server:test --tests "*PostServiceTest"

# 단일 테스트 메서드 실행
./gradlew post-server:test --tests "*PostServiceTest.shouldReturnPageFromRedisWhenSufficientDataExists"

# 빌드 아티팩트 정리
./gradlew clean
```

### 서비스 실행
```bash
# 인프라 서비스 시작 (MySQL, Redis, Kafka)
docker-compose up -d

# 인프라 서비스 상태 확인
docker-compose ps

# post-server 실행 (포트 8081)
./gradlew post-server:bootRun

# user-server 실행 (포트 미정의)
./gradlew user-server:bootRun

# 테스트 프로파일로 실행
./gradlew post-server:bootTestRun

# 인프라 서비스 종료
docker-compose down
```

### 패키징
```bash
# 실행 가능한 JAR 생성
./gradlew bootJar

# JAR 파일 생성 위치:
# post-server/build/libs/post-server-0.0.1-SNAPSHOT.jar
# user-server/build/libs/user-server-0.0.1-SNAPSHOT.jar
```

## 기술 스택

- **언어**: Kotlin 1.9.25
- **프레임워크**: Spring Boot 3.5.5
- **Java 버전**: 21
- **데이터베이스**: MySQL 8.4 with Spring Data JPA
- **메시징**: Apache Kafka with Spring Kafka
- **캐싱**: Redis with Spring Data Redis
- **빌드 도구**: Gradle with Kotlin DSL
- **테스팅**: JUnit 5, Testcontainers, Spring Boot Test

## 주요 설정 참고사항

- 모든 서비스는 루트 `build.gradle`에서 설정된 공유 의존성 사용
- JPA 엔티티는 Kotlin 호환성을 위해 `allOpen` 플러그인 사용
- 서비스들은 독립적인 배포를 위해 별도의 메인 클래스로 설정
- Redis 설정은 커스텀 `@ConfigurationProperties`로 처리
- 각 서비스는 데이터 격리를 위해 전용 데이터베이스 스키마 사용

## 핵심 아키텍처 패턴

### 이벤트 기반 통신
- **이벤트 발행**: PostService에서 생성/삭제 시 Kafka 이벤트 발행
- **토픽 구조**: `post.created`, `post.deleted` 토픽 사용
- **비동기 처리**: PostEventPublisher를 통한 논블로킹 이벤트 발행
- **장애 복구**: 이벤트 발행 실패 시 로깅 후 서비스 로직은 계속 진행

### 캐싱 전략 (Redis)
- **Write-Through**: 게시물 생성 시 DB 저장 후 즉시 Redis 캐싱
- **Cache-Aside**: 개별 게시물 조회 시 캐시 미스 발생 시 DB 조회 후 캐싱
- **Query then Validate**: 페이지네이션에서 Redis 먼저 조회 후 데이터 충분성 검증
- **TTL 관리**: 최근 게시물은 12시간 TTL로 관리

### 서비스 레이어 설계
- **트랜잭션 분리**: `@Transactional(readOnly = true)` 기본, 쓰기 작업만 별도 트랜잭션
- **의존성 주입**: 생성자 기반 DI로 테스트 용이성 확보
- **계층 분리**: Controller → Service → Repository 계층 구조
- **DTO 변환**: 도메인 객체와 API 응답 객체 분리

## 테스트 환경 최적화

### Testcontainers 컨테이너 재사용 설정

로컬 개발 환경에서 테스트 실행 속도를 향상시키기 위해 Testcontainers 컨테이너 재사용 기능을 활용할 수 있습니다.

#### 환경 설정 방법

1. **홈 디렉터리에 설정 파일 생성**:
   ```bash
   # Windows
   echo testcontainers.reuse.enable=true > %USERPROFILE%\.testcontainers.properties

   # macOS/Linux
   echo "testcontainers.reuse.enable=true" > ~/.testcontainers.properties
   ```

2. **환경 변수 설정 (선택사항)**:
   ```bash
   export TESTCONTAINERS_REUSE_ENABLE=true
   ```

#### 주의사항

- **로컬 개발 전용**: 컨테이너 재사용은 CI 환경에서는 권장되지 않습니다
- **수동 정리 필요**: 재사용된 컨테이너는 테스트 종료 후 자동으로 종료되지 않으므로 필요시 수동으로 정리해야 합니다
- **설정 동일성**: 컨테이너 재사용을 위해서는 컨테이너 설정이 완전히 동일해야 합니다

#### 수동 컨테이너 정리

```bash
# 모든 Testcontainers 컨테이너 정리
docker ps -a --filter "label=org.testcontainers" --format "table {{.ID}}\t{{.Image}}\t{{.Status}}"
docker rm -f $(docker ps -aq --filter "label=org.testcontainers")
```

## 서비스 포트 및 엔드포인트

### 인프라 서비스
- **MySQL (post-server)**: localhost:3307
- **MySQL (comment-server)**: localhost:3308
- **Redis**: localhost:6379
- **Kafka**: localhost:9092

### 애플리케이션 서비스
- **post-server**: localhost:8081
  - GET /posts - 게시물 페이지네이션 조회
  - GET /posts/{id} - 개별 게시물 조회
  - POST /posts - 게시물 생성
  - DELETE /posts/{id} - 게시물 삭제