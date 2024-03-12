### Scouter 설치
- Scouter/Server/startup.bat 실행 시 아래 에러메시지
java.lang.NoSuchMethodException: sun.misc.Unsafe.defineClass(java.lang.String,[B,int,int,java.lang.ClassLoader,java.security.ProtectionDomain)

: jdk17 버전을 사용하고 있었는데 https://bugs.openjdk.org/browse/JDK-8267178 17버전에서 해당 메서드가 삭제 되어 발생한 문제로 보임
-> jdk11로 다운그레이드 후 정상동작

