# 과제 테스트 Readme.md

* 테스트 응시자 : 류성열
* 테스트 응시 기간 : 2025.10.14~2025.10.21

# 구현 과정
* 구현 과정 문서화 : [Notion](https://www.notion.so/28c85b8cc92d80f6b25ee54e9bb240dd?pvs=21)
* 칸반 보드를 활용한 일정 관리 : [칸반 보드](https://github.com/users/passionryu/projects/2/views/1)
* 칸반보드의 백로그 기반 Issue 생성 : [이슈 리스트](https://github.com/passionryu/backend-test-v1/issues?q=is%3Aissue%20state%3Aclosed)
* Issue 기반 브랜치 생성 : [브랜치 리스트](https://github.com/passionryu/backend-test-v1/branches/all)
* 작은 단위 Git Commit : [Commit 리스트](https://github.com/passionryu/backend-test-v1/commits/main/?before=4e1ec1e23532f97e142285ab1c97c9b0ef62aacf+70)
* 고정 PR 템플릿을 활용한 PR 메시지 작성 : [PR 메시지 작성](https://github.com/passionryu/backend-test-v1/pull/6)
* Code Rabbit AI의 코드 리뷰 : [AI 코드 리뷰](https://github.com/passionryu/backend-test-v1/pull/4)
* Main 브랜치 병합 후 Issue 자동 Close : [Close된 Issue 리스트](https://github.com/passionryu/backend-test-v1/issues?q=is%3Aissue%20state%3Aclosed)
  
  ---

# 추가 과제 구현 
* Data Base 마이그레이션 : H2 ->(LiquiBase) -> PostgreSQL
* 오픈API 문서화 : [Swagger 페이지](http://localhost:8080/swagger-ui/index.html#/)
* Prometheus/Grafana 기반 모니터링 시스템 : [JVM 공유 대시보드]( http://localhost:3000/d/24437602-aaf4-47be-b334-4462a123a33d/jvm-micrometer?orgId=1&from=now-5m&to=now&timezone=browser&var-application=&var-instance=host.docker.internal:8080&var-jvm_memory_pool_heap=$__all&var-jvm_memory_pool_nonheap=$__all&var-jvm_buffer_pool=$__all&refresh=5s)
* Redis 기반 캐싱 시스템 적용 : TTL커스터마이징, Cache Eviction 전략 적용 등을 통한 조회 성능 개선 및 DataBase 부하 감소

# 기타

<details>
<summary>구현 범위</summary>

1. **프로젝트 요구사항 분석**  
2. **도메인 공부 및 Notion 정리**  
   - 결제 시스템 용어 정리  
   - 결제 시스템에서 중시하는 아키텍처 등 문서화 정리  
3. **결제 생성 API 구현**  
4. **결제 조회 API 구현**  
5. **결제 생성 API에 대한 테스트 코드 작성**  
6. **결제 조회 API에 대한 테스트 코드 작성**  
7. **Prometheus, Grafana를 활용한 모니터링 스택 적용**  
8. **PostgreSQL을 활용한 외부 DB 선언**  
9. **LiquiBase Tool을 활용한 DB 마이그레이션**  
10. **Redis 기반의 커스터마이징 캐싱 시스템 적용**  
11. **최종 문서화**

</details>

<details>
<summary>프로젝트 진행 방법론</summary>

- **코드 컨벤션:** Kotlin Naming Convention  
- **커밋 컨벤션:** Gitmoji를 활용한 한국어 기반 커밋  
- **브랜치 컨벤션:** `<타입>/<번호>-<간결한설명>`  
- **문서화 방법:** Notion 활용  
- **PR 템플릿 활용:** `.github` 디렉토리 아래 PR 자동 템플릿 적용 스크립트 추가  
- **코드 리뷰:** Code Rabbit AI를 활용한 코드 리뷰  

</details>

## 만약, 시간이 더 있었다면 ...
안정성이 중요한 결제시스템의 DB에 CQRS 패턴과 Primary-Replica 구조를 적용하여 대규모 트래픽을 제어할 것이고,     
Patroni&etcd 기반의 Fail-Over 시스템을 통해 HA를 보장하였을 것입니다.   

이에 대한 연구와 기록은 다음 페이지에 평소에 하고 있었습니다.
### 1. 대규모 트래픽 연구 프로젝트 문서화 링크 : 
https://knotty-toast-80a.notion.site/26b1979809dd800681eff595e8dbe3bd?source=copy_link     
### 2. 대규모 트래픽 연구 프로젝트 Github 링크 : 
https://github.com/Research-Project-rsy/SessionServer

