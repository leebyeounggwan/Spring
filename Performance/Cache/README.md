
# 📖 실습 환경
* spring boot 2.7.4
* java 11
* ehcache 2.x
* mysql 8.0.33
* Maven
* intellij

---

# Cahcing

### 1. Cahcing 적용 전후 성능 비교
- TTL : 20 sec
- `GET /api/v1/boards/1`
=======
### Cahcing 적용 전후 성능 비교 _ Ngrinder
- *TTL : 20 sec
- `GET /api/notices`

- Vuser : 1
- Duration : 60 sec

<br>

#### *캐싱 적용 전*
![before_cached.png](src%2Fmain%2Fresources%2Fimages%2Fbefore_cached.png)

#### *캐싱 적용 후*
![after_cached.png](src%2Fmain%2Fresources%2Fimages%2Fafter_cached.png)

<br>

### 수치변화
- *캐싱 적용 전*
  - 평균 TPS : 12 
  - Peek TPS : 14
  - Mean Test Time : 849 ms
  - Executed Tests : 649

  
- *캐싱 적용 후*
  - 평균 TPS : 251
  - Peek TPS : 284
  - Mean Test Time : 40 ms
  - Executed Tests : 13,652


> 캐싱 적용 후 TPS 및 응답시간, 실행횟수가 증가했으며, TTL(20초) 마다 캐시가 초기화 되는 것을 그래프에서 확인할 수 있다.

<br>

---

### 2. 공지사항 페이지를 조회한다고 가정(1page ~ 10page 까지 랜덤 호출)
<br>

#### Ngrinder groovy script code

```Groovy
@Test
public void test() {
  // 랜덤한 페이지 값 생성 (1 이상, 10 이하)
  def randomPage = new Random().nextInt(10) + 1

  // API 호출 URL 조합
  def apiUrl = "http://127.0.0.1:8080/api/notices/${randomPage}"

  // API 호출 및 응답 획득
  HTTPResponse response = request.GET(apiUrl, params)

  if (response.statusCode == 301 || response.statusCode == 302) {
    grinder.logger.warn("Warning. The response may not be correct. The response code was {}.", response.statusCode)
  } else {
    assertThat(response.statusCode, is(200))
  }
}


```


- `GET /api/notices/{pageNuumber}`
- Vuser : 1
- Duration : 60 sec


#### *캐싱 적용 전*
![page_before.png](src%2Fmain%2Fresources%2Fimages%2Fpage_before.png)

#### *캐싱 적용 후*
![page_after.png](src%2Fmain%2Fresources%2Fimages%2Fpage_after.png)

### 수치변화
- *캐싱 적용 전*
  - 평균 TPS : 2051
  - Peek TPS : 2512
  - Mean Test Time : 4 ms
  - Executed Tests : 111,670

  
- *캐싱 적용 후* (1~5페이지만 캐싱 적용)
  - 평균 TPS : 2848
  - Peek TPS : 3493
  - Mean Test Time : 3 ms
  - Executed Tests : 154,822

=======
<br>

캐싱처리 -> local cache / global cache
local cache -> 개별 서버 내부에서만 사용 -> 메모리 내에 데이터를 저장하기 때문에 성능 향상
global cache -> 주로 redis, memcached 등을 사용 -> 여러 서버 간에 데이터를 공유하기 위해 사용

-> 실습한 공지사항 처럼 자주 변하지 않고 다른 서비스와 공유가 필요 없는 데이터의 경우,
local cache가 좋은 선택이라고 생각하지만 각각의 장단점을 알고 상황에 따라 적절히 사용하는 것이 중요함.

redis와 같은 캐시서버를 구축을 필요로 하지 않고, 의존성 추가만으로 쉽게 적용가능하기 때문에 별도의 리소스가 필요없다는 장점도 있다.

캐시 용량제한 도달 시 어떤 데이터를 삭제할지? -> LRU, LFU, FIFO

LRU : 가장 오랫동안 참조되지 않은 데이터를 삭제<br>
LFU : 참조 횟수가 가장 적은 데이터를 삭제<br>
FIFO : 가장 먼저 삽입된 데이터를 삭제

공지사항이더라도 상황에 따라 어떤 페이지 교체 알고리즘을 사용할지 달라질 것 같음.


### 데이터 갱신 시 처리방법
1. @CacheEvict를 통해 특정 상황에서 캐시를 갱신
2. TTL을 설정하여 일정 시간마다 캐시를 갱신


### 캐시를 언제 사용할 것인지
캐시 적용 시 캐시에 필요한 데이터가 있는지 확인 후 없으면 DB에서 조회하기 때문에,
히트율이 떨어지는 데이터인 경우 캐시 데이터 조회 + DB 조회 -> 이중 조회가 발생하여 오히려 성능이 저하 될 수도 있음

1. 자주 변경되지 않는 데이터
2. 조회가 많은 데이터
3. 실시간 데이터가 중요하지 않은 경우
등 목적, 상황, 요인을 고려하는 거이 중요



# Index
- Where절의 특정컬럼을 인덱스를 활용하여 빠른 검색 가능
- Join절에서 인덱스가 있는 경우 인덱스를 이용하여 조인을 수행하기 때문에 성능 향상
- Order By절에서 인덱스를 통해 빠른 정렬 가능
- 범위 검색 시 인덱스를 사용하면 빠르게 검색 가능

<br>

인덱스 생성 기준
1. 조회가 많은 컬럼
2. 범위 검색이나 정렬이 필요한 컬럼
3. 조인이나 서브쿼리에 사용되는 컬럼
4. 유니크한 값이나 외래키로 사용되는 컬럼
5. 자주 변경되지 않는 컬럼

※ 상황에 따라 Full Scan이 더 빠를 수도 있고 옵티마이저에 따라 실행계획이 달라지므로,
 실행계획을 확인하고 필요시 Hint를 사용하여 인덱스를 적용
