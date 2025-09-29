# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

Spring Boot, Kafka, Redis, MySQL을 사용한 Kotlin 마이크로서비스 샘플 프로젝트입니다. 이벤트 기반 통신을 사용하는 기본적인 MSA(Microservices Architecture) 패턴을 보여줍니다.

### 아키텍처

- **멀티 모듈 Gradle 프로젝트**: 루트 `build.gradle`에서 공유 의존성과 플러그인 설정
- **마이크로서비스**: 현재 `post-server`, `comment-server`, `user-server` 모듈 포함
- **공통 컴포넌트**: `common-component` 모듈로 공유 DTO, 예외 처리, API 응답 형식 표준화
- **이벤트 기반 통신**: 서비스 간 메시징을 위한 Kafka 사용
- **서비스별 데이터베이스** 패턴: 각 서비스마다 별도의 MySQL 인스턴스
- **캐싱 레이어**: 성능 최적화를 위한 Redis 사용
- **패키지 구조**: 각 서비스는 `msa.{service-name}` 패턴을 따르며 표준 Spring Boot 계층 구조 (controller, service, repository, dto, model, config)

### 인프라 의존성

서비스들은 다음 외부 의존성이 필요합니다 (`compose.yaml`에서 설정):
- **MySQL 데이터베이스**:
  - post-server: `mysql-post` (포트 3307)
  - comment-server: `mysql-comment` (포트 3308)
  - user-server: `mysql-user` (포트 3309)
- **Redis**: 캐싱용 포트 6379
- **Kafka**: KRaft 모드 포트 29092 (Zookeeper 불필요)

## 개발 명령어

### 빌드 및 테스트
```bash
# 모든 모듈 빌드
./gradlew build

# 특정 모듈 빌드
./gradlew common-component:build
./gradlew post-server:build
./gradlew comment-server:build
./gradlew user-server:build

# 테스트 실행 (모든 모듈)
./gradlew test

# 특정 모듈 테스트
./gradlew common-component:test
./gradlew post-server:test
./gradlew comment-server:test
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

# comment-server 실행 (포트 8082)
./gradlew comment-server:bootRun

# user-server 실행 (포트 8083)
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
# comment-server/build/libs/comment-server-0.0.1-SNAPSHOT.jar
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
- **테스팅**: JUnit 5, Spring Boot Test (Testcontainers 의존성 제거됨)
- **보안**: Spring Security with JWT, BCrypt 패스워드 암호화
- **이메일**: Spring Mail with SMTP (Gmail 지원)

## 주요 설정 참고사항

- 모든 서비스는 루트 `build.gradle`에서 설정된 공유 의존성 사용
- JPA 엔티티는 Kotlin 호환성을 위해 `allOpen` 플러그인 사용
- 서비스들은 독립적인 배포를 위해 별도의 메인 클래스로 설정
- Redis 설정은 커스텀 `@ConfigurationProperties`로 처리
- 각 서비스는 데이터 격리를 위해 전용 데이터베이스 스키마 사용

## 핵심 아키텍처 패턴

### 이벤트 기반 통신
- **이벤트 발행**: PostService와 CommentService에서 생성/삭제 시 Kafka 이벤트 발행
- **토픽 구조**: `post.created`, `post.deleted`, `comment.created`, `comment.updated`, `comment.deleted` 토픽 사용
- **비동기 처리**: EventPublisher를 통한 논블로킹 이벤트 발행
- **서비스 간 통신**: comment-server가 post 관련 이벤트를 구독하여 데이터 동기화
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

### 공통 컴포넌트 아키텍처 (common-component)
- **표준 API 응답**: `ApiResponse<T>` 클래스로 success/error 응답 형식 통일
- **중앙집중식 예외 처리**: `GlobalExceptionHandler`로 모든 서비스의 예외 처리 표준화
- **커스텀 예외 체계**: `CustomException`과 `ErrorCode` enum으로 구조화된 오류 관리
- **페이지네이션 표준화**: `PageResponse<T>` 클래스로 일관된 페이징 응답
- **라이브러리 모듈 설정**: `bootJar` 비활성화, 일반 `jar` 파일로 빌드하여 다른 모듈에서 의존성으로 사용

## 테스트 환경 설정

### Testcontainers 의존성 제거

프로젝트에서 Testcontainers 의존성이 제거되었습니다:

**제거 사유**:
- 테스트 클래스별로 컨테이너가 새로 기동되어 시간 소요가 큰 문제
- 테스트 컨테이너 의존성이 없는 테스트까지 property 관련 의존성을 가지는 문제
- 컨테이너 구성이 주는 안정성 대비 복잡성이 큰 문제
- 컨테이너를 통한 테스트 시 오작동 발생 사례

**현재 테스트 환경**:
- 로컬 인프라 서비스(`docker-compose up -d`)에 의존하는 통합 테스트
- 단위 테스트는 모킹을 통한 독립적 실행

## 서비스 포트 및 엔드포인트

### 인프라 서비스
- **MySQL (post-server)**: localhost:3307 (database: post_db)
- **MySQL (comment-server)**: localhost:3308 (database: comment_db)
- **MySQL (user-server)**: localhost:3309 (database: user_db)
- **Redis**: localhost:6379
- **Kafka**: localhost:9092

### 애플리케이션 서비스
- **post-server**: localhost:8081
  - GET /posts - 게시물 페이지네이션 조회
  - GET /posts/{id} - 개별 게시물 조회
  - POST /posts - 게시물 생성
  - DELETE /posts/{id} - 게시물 삭제

- **comment-server**: localhost:8082
  - GET /comments?postId={id} - 특정 게시물의 댓글 조회
  - POST /comments - 댓글 생성
  - PUT /comments/{id} - 댓글 수정
  - DELETE /comments/{id} - 댓글 삭제

- **user-server**: localhost:8083
  - POST /api/auth/signup - 회원가입 (이메일 인증 필요)
  - POST /api/auth/login - 로그인 (JWT 토큰 발급)
  - POST /api/auth/send-verification-code - 회원가입용 인증코드 발송
  - POST /api/auth/send-password-reset-code - 비밀번호 재설정용 인증코드 발송
  - POST /api/auth/verify-code - 인증코드 검증
  - POST /api/auth/reset-password - 비밀번호 재설정
  - POST /api/auth/find-email - 이메일 찾기 (마스킹된 이메일 반환)