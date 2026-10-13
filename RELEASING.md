# 공개 소스와 Maven 릴리스

이 저장소는 공개 소스 복사본입니다. Maven 배포는 기존 비공개 원본 저장소에서만 수행합니다.
공개본의 GitHub Actions는 비활성화되어 있으며, 현재 브랜치에는 배포 워크플로가 없습니다.

Maven 좌표는 변경하지 않습니다.

- `io.github.mycrocosmo:mybatis-easy-core:1.0.3`
- `io.github.mycrocosmo:mybatis-easy-processor:1.0.3`

기존 릴리스 태그는 해당 버전의 소스를 확인하기 위해 보존했습니다.
다음 버전을 배포할 때는 원본의 변경 사항과 릴리스 태그를 공개본에도 반영합니다.
공개본에서는 배포 워크플로를 다시 추가하거나 Maven 배포용 Secrets를 등록하지 않습니다.

로컬 빌드와 Maven 로컬 설치는 기존과 동일합니다.

```bash
./gradlew build
./gradlew publishToMavenLocal
```
