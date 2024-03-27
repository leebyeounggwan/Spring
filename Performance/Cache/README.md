
# 📖 실습 환경
* spring boot 2.7.4
* java 11
* ehcache 2.x
* mysql 8.0.33
* Maven
* intellij

---

### Cahcing 적용 전후 성능 비교 _ Ngrinder
- *TTL : 20 sec
- `GET /api/v1/boards/1`
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