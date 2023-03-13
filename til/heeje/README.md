## 1. Docker 설치

1. apt-get 업데이트

```
$ sudo apt-get update
```

2. Docker 설치

```
$ curl -fsSL https://get.docker.com -o get-docker.sh
$ sudo sh get-docker.sh
```

3. docker 그룹에 현재 사용자 추가

```
$ sudo usermod -aG docker [username]
```

1. 서버 재접속 후 docker 명령어 정상 동작 확인

## 2. Jenkins 설치 및 설정

1. Docker Hub에서 jenkins 이미지 pull

```
$ docker pull jenkins/jenkins:lts-jdk11
```

1. 위 이미지로 Container 구동

```
$ docker run -d -v jenkins_home:/var/jenkins_home -p 8080:8080 -p 50000:50000 --restart=on-failure --name jenkins-server jenkins/jenkins:lts-jdk11
```

- docker ps로 Container 구동 확인

1. 초기 비밀번호 입력
   - docker logs jenkins-server를 통해 초기 비밀번호 복사
