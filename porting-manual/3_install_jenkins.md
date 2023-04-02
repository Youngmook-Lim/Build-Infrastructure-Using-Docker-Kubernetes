![header](https://capsule-render.vercel.app/api?type=waving&color=auto&height=300&section=header&text=Install Jenkins&fontSize=70)

## Jenkins Master 설치 및 설정

1. Docker Hub에서 jenkins 이미지를 가져옵니다.

```
docker pull jenkins/jenkins:lts-jdk11
```

- `permission denied while trying to connect to the Docker daemon socket at unix:///var/run/docker.sock: ...` 발생 시 docker 그룹에 현 사용자를 추가한 것이 반영되지 않은 것으로, 서버에서 로그아웃한 후 다시 로그인하면 해결된다.

2. 위 이미지로 Docker Container를 구동합니다.

```
docker run --name jenkins-server -d -p 8080:8080 -p 50000:50000 -v /home/jenkins:/var/jenkins_home -u root jenkins/jenkins:lts
```

- `-d` : 분리 모드를 의미하며 터미널이 차단되지 않도록 백그라운드에서 컨테이너를 실행하는 데 사용됩니다.
- `-p` : Docker 컨테이너의 포트를 호스트 시스템의 포트에 매핑합니다.
- `-v` : 호스트 머신의 디렉토리와 컨테이너 내부의 디렉토리 사이에 바인드 마운트 볼륨을 생성합니다.
- `-u` : Docker 컨테이너 내부의 사용자를 변경합니다.

- docker ps로 Container 구동 확인

![image](https://user-images.githubusercontent.com/89143804/229354763-0df113e1-a187-41ec-aa6d-003848fb0e20.png)

3. Docker 로그에 접근하여 초기 비밀번호를 복사합니다.

```jsx
docker logs jenkins-server
```

![image](https://user-images.githubusercontent.com/89143804/229354792-7fe8caee-58fa-4eeb-807a-dccb9d10384e.png)

4. `[메인 EC2 도메인 or IP]:8080/` 에 접속하여 초기 비밀번호를 붙여넣습니다.

![image](https://user-images.githubusercontent.com/89143804/229354824-d9a764b7-9407-4039-9b75-fbbd29bdb4fa.png)

5. 설치를 진행합니다.
6. 계정 생성

- 입력 칸을 모두 채운 뒤 "Save and Continue"를 클릭합니다.
  ![image](https://user-images.githubusercontent.com/89143804/229354856-0c89720c-a121-48fe-ab4b-c7a6577e5bd1.png)

- 입력되어 있는 Jenkins URL이 맞는지 확인 후 "Save and Finish"를 클릭합니다.
  ![image](https://user-images.githubusercontent.com/89143804/229354878-cfcb60b3-841f-43fe-b90d-98e9b63080ed.png)

- "Start using Jenkins"를 클릭합니다.
  ![image](https://user-images.githubusercontent.com/89143804/229354897-a5984ef3-786a-4917-ae58-4132d43c882e.png)

- 아래와 같은 Dashboard가 나올 시 설정이 완료된 것입니다.
  ![image](https://user-images.githubusercontent.com/89143804/229354914-183f520d-4cc0-4fee-97a0-03f061d7e618.png)

여기까지 진행하셨다면 [다음 단계로 이동(업뎃 필요)]()해주세요.
