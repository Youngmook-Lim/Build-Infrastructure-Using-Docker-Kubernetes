![header](https://capsule-render.vercel.app/api?type=waving&color=auto&height=300&section=header&text=Install Docker to main&fontSize=70)

## Main Server에 Docker 설치

1. package를 업데이트합니다.

```
sudo apt-get update
```

2. Docker 웹사이트에서 Docker 설치 스크립트를 다운로드하여 실행합니다.

```
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
```

- `-fsSL` : Docker 설치 스크립트를 자동으로 다운로드하는 데 사용되며 HTTP 리디렉션을 따르는 동안 발생한 모든 오류를 표시하지만 진행 상황이나 자세한 정보 출력은 표시하지 않습니다.
- `-o` : 다운로드한 콘텐츠의 출력 파일 이름을 지정하는 데 사용됩니다.

3. docker 그룹에 현재 사용자를 추가합니다.

```
sudo usermod -aG docker [username]
```

- `-aG` : 사용자가 속해 있을 수 있는 다른 그룹에서 사용자를 제거하지 않고 그룹에 추가하는 데 사용됩니다

4. 시스템이 부팅될 때 Docker 서비스가 자동으로 시작되도록 하고, Docker 서비스를 즉시 시작합니다.

```
sudo systemctl enable docker
sudo systemctl start docker
```

5. Main Server에서 로그아웃 후 다시 로그인합니다.

여기까지 진행되셨다면 [다음 단계로 이동](https://lab.ssafy.com/s08-s-project/S08P21S003/-/blob/develop/porting-manual/3_install_jenkins.md)해주세요.
