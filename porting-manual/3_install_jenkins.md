### Jenkins Master 설치 및 설정

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

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/efe63d83-606a-4f92-be39-2da5a9bd08c5/Untitled.png)

3. Docker 로그에 접근하여 초기 비밀번호를 복사합니다.

```jsx
docker logs jenkins-server
```

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/9adce8ad-5d13-4a9f-8fdd-0663e85e9f87/Untitled.png)

4. `[메인 EC2 도메인 or IP]:8080/` 에 접속하여 초기 비밀번호를 붙여넣습니다.

   ![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/b235ed26-e113-4d21-9f54-095ac519768f/Untitled.png)

5. install 버튼 클릭
   - 나중에 캡쳐 사진 가져오기
6. 계정 생성

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/be1afcc1-994e-4ada-9d07-1460eeb3be06/Untitled.png)

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/514009ad-4192-4314-93c7-1d38dc3023aa/Untitled.png)

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/186dda05-49e0-48a3-86e5-b1b3de9706c8/Untitled.png)

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/8cbc8df8-cb8e-4bcb-aaa4-1be9d65edb93/Untitled.png)

- 위와 같은 Dashboard가 나올 시 설정이 완료된 것입니다.
