![header](https://capsule-render.vercel.app/api?type=waving&color=auto&height=300&section=header&text=Install Ansible&fontSize=70)

## Ansible Server 설치 및 실행

1. Docker Container를 생성

```jsx
docker run --privileged -itd --name ansible-server -p 20022:22 -p 8081:8080 -e container=docker -v /sys/fs/cgroup:/sys/fs/cgroup ubuntu:20.04 /bin/bash
```

- `--privileged` : 호스트 시스템의 장치에 대한 전체 액세스 권한을 컨테이너에 부여합니다.
- `-itd` : "interactive", "tty" 및 "detach" 모드를 나타내며 대화형 터미널 세션을 사용하여 백그라운드에서 컨테이너를 실행하는 데 사용됩니다.
- `-e` : 환경 변수를 설정합니다. 위 명령어에서는 container라는 환경 변수의 값을 docker로 설정합니다.

2. Ansible-Server 접속

```jsx
docker exec -it ansible-server bash
```

3. Ansible 설치

1. 소프트웨어 Repository 관리를 위한 유틸리티를 제공하는 패키지를 설치합니다.

```
apt update
apt install software-properties-common
```

- 새 소프트웨어 패키지를 설치하거나 기존 패키지를 업데이트하기 전에 일반적으로 사용됩니다.

- 도시 설정(Asia: 6 입력 후 Seoul: 69 입력)

![image](https://user-images.githubusercontent.com/89143804/229355156-990b301b-7624-40a5-9923-57f779105e83.png)

![image](https://user-images.githubusercontent.com/89143804/229355171-004a7434-85fd-4283-bc03-da53634f8a78.png)

- ansible을 설치합니다.

```jsx
add-apt-repository --yes --update ppa:ansible/ansible
apt install -y ansible
```

여기까지 진행하셨다면 [다음 단계로 이동(업뎃 필요)]()해주세요.
