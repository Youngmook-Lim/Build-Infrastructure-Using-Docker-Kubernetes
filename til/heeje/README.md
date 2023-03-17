# Jenkins Master 설치

## 1. Main EC2 접속

- 여유있다면 Termius로 접속하는 방법까지?

## 2. Docker 설치

1. package 업데이트

```
sudo apt-get update
```

1. Docker 설치

```
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
```

1. docker 그룹에 현재 사용자 추가

```
sudo usermod -aG docker [username]
```

1. **서버 재접속**
- 재접속 안하고 docker 명령어를 사용했을 때 생기는 오류 캡쳐해서 넣기

## 3. Jenkins Master 설치 및 설정

1. Docker Hub에서 jenkins 이미지 pull

```
$ docker pull jenkins/jenkins:lts-jdk11
```

1. 위 이미지로 Container 구동

```
$ docker run -d -v jenkins_home:/var/jenkins_home -p 8080:8080 -p 50000:50000 --restart=on-failure --name jenkins-server jenkins/jenkins:lts-jdk11
```

- docker ps로 Container 구동 확인
1. 로그 접근 후 초기 비밀번호 복사
   
   ```jsx
   docker logs jenkins-server
   ```

 

![c5792252539aee58d325e71c9fa9d999e3266fdf.png](README_assets/ac9fba9f151902ae718e4d0c69be706049049b0a.png)

2. `[메인 EC2 도메인 or IP]:8080/` 에 접속하여 초기 비밀번호 붙여넣기

   

![fb1ca6ab0b0dc62d6dcdaa17c9a25513899b9149.png](README_assets/19dcf933d97135ed83572fb0dd29676769ced808.png)

3. install 버튼 클릭
   
   - 나중에 캡쳐 사진 가져오기

4. 계정 생성

![838a036132088283ad44db80deab72dda6ba9792.png](README_assets/a6b3a9693a6c0a265c47830cd85a6259ee970f3b.png)

![6e18cad6bd7d4b61d6463697ee55a747b161c3be.png](README_assets/0014a1ebc2aa0b95215b43405e9a6e2723460ed3.png)

![5c09fcad7150a33be6a048e4a2e724e27f6a953a.png](README_assets/db124486904591ff6520c2779b45e11b23067762.png)

![de5861ca90dd2d0c4ab7d14e5ba77084cd8a0894.png](README_assets/5333b4ee03834c86187cac1d7db1b1d0a52f7bcb.png)