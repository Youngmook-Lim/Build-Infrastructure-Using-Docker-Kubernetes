![header](https://capsule-render.vercel.app/api?type=waving&color=auto&height=300&section=header&text=Install Docker to others&fontSize=70)

## Ansible Playbook을 이용하여 나머지 서버에 Docker 설치

1. Playbook, Shell script를 저장할 폴더 생성

```
mkdir /home/ansible-playbooks
mkdir /home/init-scripts
```

2. Docker 설치를 위한 Shell script 파일 생성

```
vi /home/init-scripts/install-docker.sh
```

```
# remove docker if exists
sudo apt-get remove docker docker-engine docker.io containerd runc

# update apt packages
sudo apt-get update
sudo apt-get -y install \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

# add docker's GPG key
sudo rm -r /etc/apt/keyrings
sudo mkdir -m 0755 -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \
     $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# install docker engine
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# add docker group
sudo usermod -aG docker $USER

# resetting the permissions
sudo chown -R $USER:$USER /var/run/docker.sock
sudo chmod -R 660 /var/run/docker.sock
```

1. 기존 Docker 제거: 시스템에서 기존 Docker 패키지를 제거합니다.
2. apt 패키지 업데이트: 다음으로 스크립트는 `sudo apt-get update`를 사용하여 apt 패키지 목록을 업데이트합니다. 그런 다음 `sudo apt-get -y install`을 사용하여 `ca-certificates`, `curl`, `gnupg` 및 `lsb-release`를 포함하는 필수 패키지를 설치합니다.
3. Docker의 GPG 키 추가: Docker 패키지의 신뢰성을 보장하기 위해 스크립트는 `/etc/apt/keyrings`에서 기존 키링을 제거하고 이를 위한 새 디렉토리를 생성합니다. 그런 다음 `curl`을 사용하여 Docker의 GPG 키를 다운로드하고 새로 생성된 키링에 추가합니다.
4. Docker 리포지토리 추가: 스크립트는 Docker 리포지토리를 시스템의 패키지 소스 목록에 추가합니다. 적절한 리포지토리 URL 및 서명 키를 사용하여 `/etc/apt/sources.list.d/docker.list`에 새 항목을 생성하여 이를 수행합니다.
5. Docker 구성 요소 설치: 리포지토리가 준비되면 스크립트가 `sudo apt-get update`를 사용하여 패키지 목록을 다시 업데이트합니다. 그런 다음 `docker-ce`(Docker 엔진), `docker-ce-cli`(Docker CLI), `containerd.io`(컨테이너 런타임), `docker-buildx-plugin`(Buildx 플러그인용 다중 플랫폼 빌드) 및 `docker-compose-plugin`(다중 컨테이너 애플리케이션 관리를 위한 Compose 플러그인).
6. Docker 그룹에 사용자 추가: 스크립트는 `sudo usermod -aG docker $USER`를 사용하여 `docker` 그룹에 현재 사용자를 추가합니다. 이를 통해 사용자는 `sudo`를 사용할 필요 없이 Docker 명령을 실행할 수 있습니다.
7. Docker 소켓의 권한 재설정: 마지막으로 스크립트는 `/var/run/docker.sock` 파일의 소유자와 권한을 변경하여 현재 사용자가 액세스할 수 있도록 합니다. 이것은 `sudo chown` 및 `sudo chmod` 명령을 사용하여 수행됩니다.

8. Docker 설치를 위한 Ansible Playbook 파일 생성

```
vi /home/ansible-playbooks/playbook-install-docker.yml
```

- 아래 내용 입력 및 저장

```
- name: Install Docker
  hosts: all
  remote_user: ubuntu
  tasks:
    - name: Copy Docker install shell script
      copy:
        src=/home/init-scripts/install-docker.sh
        dest=/home/ubuntu/scripts/
        mode=0777

    - name: Execute script
      command: sh /home/ubuntu/scripts/install-docker.sh
      async: 3600
      poll: 5
```

4. Docker 설치를 위한 Ansible Playbook 실행

```
ansible-playbook /home/ansible-playbooks/playbook-install-docker.yml
```

![image](https://user-images.githubusercontent.com/89143804/229357387-2a03ea69-57fb-4a6e-b639-4b859949a01a.png)

- 완료 시 k8s-master, build-1~3 서버에 Docker가 설치됩니다. 정상적으로 설치되었다면 [다음 단계로 이동(업뎃 필요)]()해주세요.
