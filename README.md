# MyBatis Easy Sync Starter

MyBatis에서 반복되는 CRUD 작성과 Mapper/XML 불일치 확인을 돕는 Java 17 기반 라이브러리입니다. 직접 작성한 SQL을 유지하면서 반복 작업만 자동화하는 것이 목표입니다.

[English](README.en.md) · 한국어

## 모듈

| 모듈 | 역할 |
| --- | --- |
| `mybatis-easy-core` | 기본 CRUD SQL 병합, 필드·컬럼 매핑, 개발용 엔티티 생성 |
| `mybatis-easy-processor` | 컴파일 시 Mapper 메서드와 XML statement ID 비교, 선택적 stub 생성 |

런타임 SQL 병합과 컴파일 시점 검증은 별도 기능입니다. Processor가 생성하는 stub은 완성된 업무 SQL이 아닙니다.

## 설치 — 로컬 Maven 저장소 기준

JDK 17과 저장소에 포함된 Gradle Wrapper를 준비합니다. Maven Central의 기존 배포본은 core와 processor 모두 `1.0.3`입니다. 현재 소스의 Lombok 선택 옵션과 SQL 경계값·오류 전파 개선은 아직 새 버전으로 배포하지 않았습니다. 이 동작을 확인하려면 아래 절차로 현재 소스를 로컬 빌드해 사용하세요. 소스의 버전 값은 `1.0.3`으로 유지되어 있으므로 `mavenLocal()`이 Central보다 먼저 적용됩니다.

라이브러리 저장소 루트:

```bash
./gradlew :mybatis-easy-core:publishToMavenLocal :mybatis-easy-processor:publishToMavenLocal
```

Windows에서는 `./gradlew` 대신 `.\gradlew.bat`를 사용합니다.

소비자 Spring Boot 프로젝트의 `build.gradle`:

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'io.github.mycrocosmo:mybatis-easy-core:1.0.3'
    // Mapper/XML 컴파일 시점 검증이 필요한 경우에만 추가
    annotationProcessor 'io.github.mycrocosmo:mybatis-easy-processor:1.0.3'
    runtimeOnly 'org.postgresql:postgresql'
}
```

Maven 좌표의 groupId는 `io.github.mycrocosmo`이고, Java 패키지는 `com.thenoah.dev...` 그대로입니다(코드의 import는 바뀌지 않습니다).

소비자 프로젝트는 Spring Boot 3.x와 Java 17 환경을 기준으로 구성합니다. 사용하는 DB의 JDBC 드라이버와 DataSource 설정도 필요합니다.

## AutoSQL 활성화에 필요한 조건

**`BaseMapper` 상속만으로 동작하지 않습니다.** 현재 자동 구성은 `mybatis-easy.autosql.enabled=true`일 때 생성되며, 설정이 없으면 활성화되지 않습니다. 또한 읽을 수 있는 Mapper XML 리소스가 없으면 병합을 종료합니다.

소비자 프로젝트의 `application.yml`:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

mybatis:
  mapper-locations: classpath*:mapper/**/*.xml

mybatis-easy:
  autosql:
    enabled: true
  generator:
    enabled: false
    allow-write: false
```

`DB_URL`에는 개발 DB의 JDBC URL을 설정합니다. `generator`를 끈 상태에서는 Java 소스나 DB 테이블을 자동으로 만들어 주지 않으므로 엔티티와 테이블은 직접 준비해야 합니다.

### 엔티티와 Mapper 예시

```java
package com.example.demo;

import com.thenoah.dev.mybatis_easy_starter.core.annotation.Column;
import com.thenoah.dev.mybatis_easy_starter.core.annotation.Id;
import com.thenoah.dev.mybatis_easy_starter.core.annotation.Table;

@Table(name = "users")
public class User {
    @Id
    @Column(name = "user_id")
    private Long id;
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
```

```java
package com.example.demo;

import com.thenoah.dev.mybatis_easy_starter.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User, Long> {
}
```

### 반드시 준비할 XML 리소스

경로: `src/main/resources/mapper/UserMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.demo.UserMapper">
</mapper>
```

XML 경로는 `mybatis.mapper-locations` 설정을 따르며, 설정이 없으면 `classpath*:mapper/**/*.xml`을 사용합니다.

`namespace`는 실제 Mapper의 전체 클래스명과 일치해야 합니다. 자동 구성은 XML 리소스를 찾아 `BaseMapper`와 연결한 뒤 필요한 SQL을 가상 리소스에 병합합니다. 커스텀 statement를 XML에 작성하면 동일 ID의 자동 생성 SQL보다 우선합니다.

## 주요 설정

| 프로퍼티 | 기본값 | 설명 |
| --- | --- | --- |
| `mybatis-easy.autosql.enabled` | `false` | AutoSQL 병합 활성화 |
| `mybatis-easy.autosql.refresh-mode` | `NONE` | `UPDATE_MARKER_BLOCK`이면 XML의 마커 블록 내부를 최신 엔티티 구조로 갱신 |
| `mybatis-easy.autosql.quote-identifiers` | `false` | 방언에 맞춰 테이블·컬럼 식별자에 quote 적용 |
| `mybatis-easy.autosql.generated-key.key-column` | `id` | 생성키 컬럼명. PK 컬럼이 `id`가 아니면(예: `member_id`) 지정 |
| `mybatis-easy.autosql.generated-key.strategy` | `AUTO` | `JDBC`(useGeneratedKeys) / `NONE`. AUTO는 Oracle에서 `NONE` |
| `mybatis-easy.pagination.enabled` | `false` | `true`일 때만 `findPage`/`countAll` SQL 생성 |
| `mybatis-easy.pagination.dialect` | `AUTO` | DB 제품명으로 추론. 연결 불가 시 명시 권장 |
| `mybatis-easy.pagination.find-all.policy` | `NONE` | `CAP`(상한 적용) / `DISABLE`(findAll 생성 안 함) |

`findById`와 `deleteById`는 `@Param("id")`로 바인딩되므로 엔티티의 PK 필드명이 `id`가 아니어도 동작합니다.

## 제공 API

```java
int insert(Object entity);
Optional<T> findById(ID id);
List<T> findAll();
List<T> findPage(long offset, int limit);
long countAll();
int update(Object entity);
int deleteById(ID id);
```

이 블록은 인터페이스 시그니처 요약입니다. 결과가 커질 수 있는 테이블에서는 무제한 `findAll()` 대신 페이지 조회를 사용하세요. DB별 SQL 호환성과 사용자 XML 우선 정책은 소비자 프로젝트에서 검증해야 합니다.

`findPage`는 음수 offset을 0으로, 1보다 작은 limit을 1로 보정하며 `mybatis-easy.pagination.max-page-size`를 상한으로 사용합니다. SQL 생성 중 메타데이터 분석이 실패하면 엔티티 이름과 원인 예외를 포함한 `IllegalStateException`이 자동 구성 호출부까지 전달됩니다.

## 선택 사항: Mapper/XML 컴파일 검증

Processor는 missing statement, orphan statement, XML ID와 충돌하는 메서드 오버로딩을 검사합니다.

- `mes.xmlDir` 아래 하위 디렉터리의 XML도 함께 검사합니다.
- `BaseMapper`에서 상속한 메서드는 런타임에 AutoSQL이 제공하므로 XML에 없어도 missing이 아니며, XML에 직접 정의해도 orphan으로 보지 않습니다.
- Mapper가 직접 선언하거나 다른 인터페이스에서 상속한 메서드는 XML statement가 있어야 합니다. `@Select` 등 애너테이션 SQL 메서드는 제외됩니다.

```groovy
tasks.withType(JavaCompile).configureEach {
    options.compilerArgs += [
        '-Ames.xmlDir=src/main/resources/mapper',
        '-Ames.failOnMissing=true',
        '-Ames.failOnOrphan=false',
        '-Ames.generateMissing=false',
        '-Ames.debug=false'
    ]
}
```

런타임 AutoSQL 생성과 Processor의 XML 검사는 실행 시점이 다릅니다. Processor 설정을 추가할 때는 자신의 Mapper 구성에서 missing 판정 범위를 확인하세요. 자동 stub 생성은 관리 영역을 사용하지만 SQL 완성을 대신하지 않습니다.

## 개발용 엔티티 생성기

JDBC 메타데이터를 이용한 소스 생성기는 로컬 파일을 수정합니다. 현재 자동 구성은 `generator.enabled`, `generator.allow-write`, 개발 환경 판정을 함께 확인합니다. 운영에서는 비활성화하고, 로컬에서 켜더라도 생성 전후 Git diff를 검토하세요.

새 엔티티는 기본적으로 Lombok 없이 컴파일되는 필드와 public getter/setter를 생성합니다. 컬럼을 추가해 동기화할 때도 접근자를 함께 만듭니다. 이 기본 모드는 `equals`, `hashCode`, `toString`을 생성하지 않습니다.

기존 Lombok `@Data`의 값 비교·문자열 표현 등에 의존한다면 다음 옵션으로 기존 생성 방식을 선택할 수 있습니다.

```yaml
mybatis-easy:
  generator:
    use-lombok: true # 기본 false. 파일 쓰기 허용 조건은 별도로 충족해야 합니다.
```

이 모드는 `import lombok.Data`와 `@Data`를 생성하므로 소비자 프로젝트에 Lombok 의존성과 annotation processor 설정이 필요합니다. 생성기를 직접 호출할 때는 `new EntityGenerator(true)`를 사용합니다.

이미 존재하는 `@Data` 또는 `@lombok.Data` 클래스는 옵션과 관계없이 해당 스타일을 유지하며, 새 컬럼에 중복 접근자를 만들지 않습니다. 기존 일반 Java 클래스에는 접근자를 추가합니다. 기존 Lombok 클래스를 일반 Java로 자동 변환하지 않으므로 의존성을 제거하려면 사용자 수정 사항을 보존한 뒤 별도로 전환해야 합니다.

## DB 지원 범위

| DB | 상태 |
| --- | --- |
| H2 | 통합 테스트(실제 CRUD와 페이지 경계값 조회) 통과 |
| PostgreSQL, MySQL, MariaDB, SQL Server, Oracle, SQLite | 방언별 SQL 생성 문자열, MyBatis XML 파싱 및 페이지 인자 바인딩을 확인. **실제 DB 실행은 검증하지 않았으므로** 도입 전에 소비자 프로젝트에서 확인하세요. |

## 검증 범위

```bash
./gradlew clean build
```

테스트 실행 결과와 실제 소비자 프로젝트의 컴파일·DB CRUD 결과는 구분해야 합니다. 이 README는 설치·활성화에 필요한 조건을 설명하며, 모든 DB·Spring Boot 조합의 호환성을 보장하지 않습니다. JPA의 영속성 컨텍스트나 엔티티 생명주기를 제공하는 ORM을 목표로 하지 않습니다.

회귀 테스트는 core JAR와 런타임 의존성만으로 소비자 코드를 컴파일하고, 생성·동기화한 엔티티의 접근자를 사용하는 코드도 Lombok 없이 컴파일합니다. SQL 테스트는 기존 사용자 statement 우선 정책을 유지하면서 페이지 인자 보정과 생성 오류 전파를 검증합니다.

## 저장소 운영

공개 소스 저장소는 [mybatis-easy-sync-starter-public](https://github.com/MycroCosmo/mybatis-easy-sync-starter-public)입니다. Maven 배포와 Secrets는 별도로 관리하는 비공개 원본 저장소에서만 관리합니다.

공개본에는 배포 워크플로를 추가하지 않으며 GitHub Actions 비활성화 설정을 유지합니다. Maven 좌표는 그대로 사용합니다. 소스 변경과 새 버전 배포는 별도 절차이며, 운영 규칙은 [RELEASING.md](RELEASING.md)를 참고하세요.
