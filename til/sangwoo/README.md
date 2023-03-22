# S003_대구옵스팀 한상우입니다.

---

## 프로젝트에 임하는 자세
<aside>
💡 요행을 바라지 않는다.
</aside>

<aside>
💡 내가 할 수 있는 일에 최선을 다한다.
</aside>


---

## 일자별 작업 내용
### 2023-03-13 작업 내용
- [x]  [프로젝트] multipass 설치
- [x]  [프로젝트] jenkins Master용 VM 생성
- [x]  [프로젝트] jenkins Master 설치
- [x]  [프로젝트] kubernetes master 서버용 VM 생성

### 2023-03-15 작업 내용
- [x]  [프로젝트] 3개 VM 서버에 ansible-playbook 접근 권한 설정
- [ ]  [프로젝트] ansible-playbook을 활용해서 3개의 jenkins agent 서버에 jenkins 설치
    - [x]  [프로젝트] jenkins-master에 agent용 node 등록
- [x]  [프로젝트] jenkins master에서 jenkins agent로 build 테스트

### 2023-03-16 작업 내용
- [x]  jenkins 한 개의 작업을 동시에 여러번 빌드하도록 허용하기
- [x]  jenkins 대기큐 동작 확인

### 2023-03-17 ~ 2023-03-20 작업 내용
- [ ]  PriorityQueue Sorter로 대기큐 정렬 구현 (삽질했으나 실패 ㅠㅠ)

### 2023-03-21 작업 내용
- [x] 커밋 해시값 - 이전에 이미 빌드된 커밋인지 확인
    - [x]  Jenkins에서 이전 커밋 조회 방법 찾기
    - [x]  Jenkins agent에 빌드 요청을 보내기전 master에서 현재 커밋과 비교하는 기능 구현
    - [x]  (이전에 빌드한 커밋으로 판단될 경우) 빌드 중단하도록 만들기

### 2023-03-22 작업 내용
- [x]  이전에 빌드된 커밋인지 확인하는 기능을 script로 만들기
    - [x]  Jenkins Freestyle Job에서 동작하도록 만들기
    - [x]  Jenkins Pipeline Script로 변환 (Jenkinsfile)

