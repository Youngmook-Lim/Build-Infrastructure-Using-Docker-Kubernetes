## 1. Prerequisites

DaeGuOps 프로젝트를 진행하기 위해서는 다음과 같은 전제 조건이 필요합니다.

1. AWS 계정을 보유하고 있어야 하며 진행 시 요금이 발생할 수 있음을 인지하고 있어야 합니다.
2. AWS, Docker, Ansible, Kubernetes, jenkins에 대한 기본 지식이 있어야 합니다.

AWS 계정이 없다면 [Multipass](https://multipass.run/)를 통해 대체할 수 있지만, 본 문서에서 따로 다루고 있지는 않습니다.

## 2. 서버 생성

### InfraStructure Overview

인프라는 다음 사양의 AWS EC2 인스턴스 5개로 구성됩니다. (k8s는 kubernetes의 줄임말입니다.)

- **메인 서버 인스턴스**

  - 명칭: daeguops-main (이하 main)
  - 인스턴스 유형: m5.large
  - 운영 체제: 우분투 20.04
  - 개수: 1
  - 서비스: jenkins-server 및 ansible-server를 실행하는 Docker 컨테이너
  - 소프트웨어 요구 사항: Docker, Jenkins(Jenkins 서버) 및 Ansible(Ansible 서버)
  - 저장 공간: 30GB(OS, Docker, Jenkins, Ansible 및 추가 컨테이너용)

- **K8s 마스터 서버 인스턴스**

  - 명칭: daeguops-k8s-master (이하 k8s-master)
  - 인스턴스 유형: m5.large
  - 운영 체제: 우분투 20.04
  - 개수: 1
  - 서비스: 작업자 노드를 관리하는 Kubernetes 마스터
  - 소프트웨어 요구 사항: Docker 및 Kubernetes
  - 스토리지: 30GB(OS, Docker, Kubernetes 및 추가 관리 구성 요소용)

- **K8s 작업자 서버 인스턴스**

  - 명칭: daeguops-build-1~3 (이하 build-1~3)
  - 인스턴스 유형: m5.large
  - 운영 체제: 우분투 20.04
  - 개수: 3
  - 서비스: Jenkins Agent 및 Kubernetes 작업자 노드
  - 소프트웨어 요구 사항: Docker 및 Kubernetes
  - 스토리지: 인스턴스당 50GB(OS, Docker, Jenkins Agent, Kubernetes 및 애플리케이션 컨테이너용)
    <아직 정리 안한 상태>

### EC2 인스턴스 생성(main, k8s-master, build-1~3)

1. AWS 계정에 로그인하고 EC2 대시보드에서 "인스턴스 시작"을 클릭합니다.

![image](https://user-images.githubusercontent.com/89143804/229323018-9b014ece-99a7-46ca-a719-643953da5d4c.png)

2. 이름은 각 서버의 역할에 알맞게 지정합니다.(daeguops-main 등)
3. AMI는 Ubuntu Server 20.04 LTS (HVM), SSD Volume Type으로 설정합니다.
4. 인스턴스 유형으로 "m5.large"를 선택합니다.
5. 인스턴스 스토리지를 서버에 따라 30GB 또는 50GB로 설정합니다.

![image](https://user-images.githubusercontent.com/89143804/229323250-3cf48890-480f-4d3b-a88d-fbb04f1a138c.png)

![image](https://user-images.githubusercontent.com/89143804/229323282-21ae8069-e1c0-49e5-b9e8-22f385394e09.png)

## 2. Main Server에 Docker 설치

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

## 3. Jenkin-server 설치 및 실행

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

## 4. Ansible Server 설치 및 실행

## 1. Docker Container를 생성

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

## 5. Host 파일 생성 및 SSH 연결

### Host 파일 생성

1. vim 설치

- Linux 용으로 널리 사용되는 텍스트 편집기인 `vim`을 설치합니다.

```jsx
apt-get install -y vim
```

2. Inventory 작성

- Hosts 목록 파일 접속에 접속합니다.

```
sudo vi /etc/ansible/hosts
```

- 다음과 같이 내용을 삽입합니다.

```
[all:vars]
ansible_user=(사용자)

[k8s-workers]
(빌드 서버1 IP)
(빌드 서버2 IP)
(빌드 서버3 IP)

[k8s-master]
(k8s 서버 IP)
```

### SSH 연결

1. K8s Master, Build Server에 Password 생성

- K8s Master Server에 접속합니다.
- /etc/ssh/sshd_config 파일 수정(PasswordAuthentication yes)

```jsx
sudo vi /etc/ssh/sshd_config
```

![image](https://user-images.githubusercontent.com/89143804/229355578-70b1a42e-dc58-4a67-a36b-56abb2195479.png)

- root 계정에 접속합니다.

```jsx
sudo su
```

- 사용자의 비밀번호를 변경합니다.(사용자 이름을 ubuntu라 가정합니다.)

```jsx
passwd ubuntu
```

![image](https://user-images.githubusercontent.com/89143804/229355654-7e91d2cd-f584-4a25-895c-06287bb00c31.png)

- `ubuntu` 사용자로 돌아와 ssh를 재시작합니다.

```
# return to user "ubuntu"
exit

# restart ssh
sudo systemctl restart ssh
```

- **모든 빌드 서버에 대하여 위 과정을 실행합니다.**

2. SSH key 생성

- ansible-server에서 SSH key을 생성합니다.

```
ssh-keygen -t rsa -N '' -f ~/.ssh/id_rsa <<< y
```

3. K8s Master, Build Server에 SSH key 복사

- ansible-server에서 SSH key를 해당 서버에 복사하기 위해 다음과 같이 입력합니다.

```
# copy the SSH key to the server
ssh-copy-id -i ~/.ssh/id_rsa.pub ubuntu@[k8s master 또는 빌드 서버 IP]
```

- yes 입력 후 해당 빌드 서버에 저장해둔 비밀번호를 입력합니다.

![image](https://user-images.githubusercontent.com/89143804/229356320-4929fa43-4fd3-45a7-b5e7-cdacd6d2a608.png)

![image](https://user-images.githubusercontent.com/89143804/229356337-937ad8e9-a4b5-4f97-ab99-1a8e8099969c.png)

## 6. Ansible Playbook을 이용하여 나머지 서버에 Docker 설치

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

- 완료 시 k8s-master, build-1~3 서버에 Docker가 설치됩니다.

## 7. Ansible Playbook을 이용하여 K8s-master, Build 서버에 K8s 설치

### 1. Kubernetes 설치를 위한 Shell script 파일 생성

```
vi /home/init-scripts/install-k8s.sh
```

- 아래 내용 입력 및 저장

```
# swap disable setting
sudo swapoff -a && sudo sed -i '/swap/s/^/#/' /etc/fstab

# iptable setting
cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
br_netfilter
EOF

cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
EOF
sudo sysctl --system

# apt-get update, add required package
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl

# download google cloud public key
sudo curl -fsSLo /usr/share/keyrings/kubernetes-archive-keyring.gpg https://packages.cloud.google.com/apt/doc/apt-key.gpg

# add kubernetes storage
echo "deb [signed-by=/usr/share/keyrings/kubernetes-archive-keyring.gpg] https://apt.kubernetes.io/ kubernetes-xenial main" | sudo tee /etc/apt/sources.list.d/kubernetes.list

# install kubelet, kubeadm, kubectl
sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl

# register k8s, restart
sudo systemctl daemon-reload
sudo systemctl restart kubelet
```

1. 스왑 비활성화: 스왑을 비활성화하고 `/etc/fstab`의 스왑 항목을 주석 처리합니다. 시스템이 메모리 페이지 스와핑을 시작할 때 Kubernetes 성능이 저하될 수 있으므로 스왑이 비활성화됩니다.
2. iptable 설정: 부팅 시 `br_netfilter` 커널 모듈이 로드되도록 하는 구성 파일을 만듭니다. 이 모듈은 브리징 및 iptables와 같은 Kubernetes 네트워킹 기능에 필요합니다.
3. 트래픽 전달 활성화: Kubernetes 클러스터의 컨테이너 간에 트래픽 전달을 활성화하는 sysctl 구성 파일을 생성합니다.
4. 패키지 설치: Kubernetes 리포지토리와 같은 외부 리포지토리에서 패키지를 안전하게 가져오는 데 필요한 패키지를 설치합니다.
5. Google Cloud 공개 키 다운로드: Kubernetes 패키지의 신뢰성을 확인하는 데 사용되는 Google Cloud 공개 키를 다운로드하고 저장합니다.
6. K8s Repository 추가: Kubernetes 리포지토리를 시스템의 패키지 소스 목록에 추가하여 공식 리포지토리에서 Kubernetes 구성 요소를 설치할 수 있도록 합니다.
7. K8s 구성 요소 설치: 필요한 Kubernetes 구성 요소인 kubelet(노드 에이전트), kubeadm(클러스터 관리용) 및 kubectl(클러스터와 상호 작용하기 위한 명령줄 도구)을 설치합니다.
8. 업데이트 방지: 설치된 Kubernetes 구성 요소를 "보류"로 표시하여 시스템 업데이트 중에 실수로 업그레이드되는 것을 방지합니다. 클러스터 내에서 호환성과 안정성을 유지하는 데 도움이 됩니다.
9. 변경사항 적용 및 재시작: 시스템 관리자 구성을 다시 로드하여 구성 파일에 대한 변경 사항이 고려되도록 합니다. 이후 kubelet 서비스를 다시 시작하여 새 구성 설정을 적용하고 서비스가 올바른 설정으로 실행되는지 확인합니다.

### 2. Kubernetes 설치를 위한 Ansible-Playbook 파일 생성

```
vi /home/ansible-playbooks/playbook-install-k8s.yml
```

- 아래 내용 입력 및 저장

```
- name: Install K8s
  hosts: all
  remote_user: ubuntu
  tasks:
    - name: Copy K8s install shell script
      copy:
        src=/home/init-scripts/install-k8s.sh
        dest=/home/ubuntu/scripts/
        mode=0777

    - name: Execute script
      command: sh /home/ubuntu/scripts/install-k8s.sh
      async: 3600
      poll: 5
```

### 3. Kubernetes 설치를 위한 Ansible Playbook 실행

```
ansible-playbook /home/ansible-playbooks/playbook-install-k8s.yml
```

![image](https://user-images.githubusercontent.com/89143804/229373748-dd44eda3-cf3a-4583-bd9e-a63948b04155.png)

## 8. Kubernetes 초기화 및 Worker Node 연결을 위한 Ansible-Playbook 파일 생성

```
vi /home/ansible-playbooks/playbook-init-k8s.yml
```

- 아래 내용 입력 및 저장

```
---
- name: Kubernetes setup
  hosts: all
  remote_user: ubuntu
  become: yes
  tasks:
    - name: Remove containerd config file
      ansible.builtin.file:
        path: /etc/containerd/config.toml
        state: absent

    - name: Restart containerd service
      ansible.builtin.systemd:
        name: containerd
        state: restarted

    - name: Initialize Kubernetes control-plane node
      ansible.builtin.command:
        cmd: kubeadm init --pod-network-cidr=10.244.0.0/16
      register: kubeadm_init_output

    - name: Save kubeadm join command
      set_fact:
        kubeadm_join: "{{ kubeadm_init_output.stdout_lines[-1] }}"
      when: "'kubeadm join' in kubeadm_init_output.stdout"

    - name: Set up kubeconfig for all users
      become: no
      block:
        - name: Create .kube directory
          ansible.builtin.file:
            path: "{{ ansible_env.HOME }}/.kube"
            state: directory
            mode: '0755'

        - name: Copy admin.conf to .kube/config
          ansible.builtin.copy:
            src: /etc/kubernetes/admin.conf
            dest: "{{ ansible_env.HOME }}/.kube/config"
            remote_src: yes

        - name: Set owner for .kube/config
          ansible.builtin.file:
            path: "{{ ansible_env.HOME }}/.kube/config"
            owner: "{{ ansible_env.USER }}"
            group: "{{ ansible_env.USER }}"
            mode: '0644'

    - name: Install Flannel pod network addon
      ansible.builtin.k8s:
        src: https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml
        state: present

- name: Join Kubernetes worker nodes to the cluster
  hosts: workers
  remote_user: ubuntu
  become: yes
  tasks:
    - name: Create kubelet service directory
      ansible.builtin.file:
        path: /etc/systemd/system/kubelet.service.d/
        state: directory
        mode: '0755'

    - name: Add containerd configuration to kubelet
      ansible.builtin.copy:
        content: |
          [Service]
          Environment="KUBELET_EXTRA_ARGS=--container-runtime=remote --container-runtime-endpoint=unix:///run/containerd/containerd.sock --runtime-cgroups=/system.slice/containerd.service --kubelet-cgroups=/system.slice/containerd.service"
        dest: /etc/systemd/system/kubelet.service.d/10-containerd.conf

    - name: Reload systemd daemon
      ansible.builtin.systemd:
        daemon_reload: yes

    - name: Restart kubelet service
      ansible.builtin.systemd:
        name: kubelet
        state: restarted

    - name: Make workspace directory
      ansible.builtin.command:
        cmd: mkdir -p /home/ubuntu/tmp/workspace

    - name: Run kubeadm join command
      ansible.builtin.shell: "{{ hostvars['master'].kubeadm_join }}"
      args:
        executable: /bin/bash
```

1. 기본 구성이 사용되도록 containerd 구성 파일을 제거합니다.
2. 새로운 구성을 적용하기 위해 containerd 서비스를 다시 시작합니다.
3. 지정된 포드 네트워크 CIDR과 함께 kubeadm을 사용하여 Kubernetes 컨트롤 플레인 노드를 초기화합니다.
4. 작업자 노드를 클러스터에 조인할 때 나중에 사용할 수 있도록 `kubeadm init` 명령에 의해 출력된 kubeadm join 명령을 저장합니다.
5. 모든 사용자가 Kubernetes 클러스터와 상호 작용할 수 있도록 kubeconfig를 구성합니다.
6. 사용자의 홈 디렉토리에 `.kube` 디렉토리를 생성합니다.
7. Kubernetes 관리자 구성 파일을 `.kube` 디렉터리에 복사합니다.
8. kubeconfig 파일에 대한 적절한 소유권을 설정합니다.
9. Kubernetes 클러스터에 네트워킹 기능을 제공하는 Flannel 포드 네트워크 애드온을 설치합니다.
10. 작업자 노드를 Kubernetes 클러스터에 결합합니다.
11. containerd 구성을 저장하기 위한 kubelet 서비스 디렉토리를 생성합니다.
12. 컨테이너 런타임 및 엔드포인트를 지정하여 kubelet 서비스에 containerd 구성을 추가합니다.
13. 새로운 kubelet 구성을 적용하기 위해 systemd 데몬을 다시 로드합니다.
14. kubelet 서비스를 다시 시작하여 새 구성 설정을 적용합니다.
15. 사용자의 홈 디렉토리에 작업 공간 디렉토리를 생성합니다.
16. 이전에 저장된 kubeadm join 명령을 실행하여 작업자 노드를 Kubernetes 클러스터에 조인합니다.

### Kubernetes 초기화를 위한 Ansible Playbook 실행

```
ansible-playbook /home/ansible-playbooks/playbook-init-k8s.yml
```

![image](https://user-images.githubusercontent.com/89143804/229374115-af092cd2-64dc-4a9c-854b-64ab38b06d47.png)

## 9. Jenkins Agent 배포 및 서비스

### 1. Jenkins Agent Deployment & Service를 위한 playbook 생성

- `playbook-create-jenkins-agent.yml` 생성

```
---
- name: Create jenkins agent daemonset
  hosts: k8s-master
  remote_user: ubuntu
  tasks:
    - name: Create jenkins-agent namespace
      ansible.builtin.command:
        cmd: kubectl create namespace jenkins-agent
      register: namespace_result
      changed_when: "'created' in namespace_result.stdout"
      failed_when: "namespace_result.rc != 0 and 'AlreadyExists' not in namespace_result.stderr"

    - name: Create Jenkins agent daemonset manifest
      ansible.builtin.copy:
        content: |
          apiVersion: apps/v1
          kind: DaemonSet
          metadata:
            name: jenkins-agent
            namespace: jenkins-agent
          spec:
            selector:
              matchLabels:
                name: jenkins-agent
            template:
              metadata:
                labels:
                  name: jenkins-agent
              spec:
                containers:
                  - name: jenkins-agent
                    image: youngmookk/daeguops-jenkins-agent
                    env:
                      - name: JENKINS_URL
                        value: "http://j8s003.p.ssafy.io:8080"
                      - name: JENKINS_AGENT_SSH_PUBKEY
                        value: "ssh-rsa {{ SSH_PUBLIC_KEY }}"
                    securityContext:
                      runAsUser: 0
                    ports:
                      - containerPort: 22
                        name: jnlp
                        protocol: TCP
                    tty: true
          ---
          apiVersion: v1
          kind: Service
          metadata:
            name: jenkins-agent-service
            namespace: jenkins-agent
          spec:
            selector:
              name: jenkins-agent
            ports:
              - protocol: TCP
                port: 4444
                targetPort: 22
                nodePort: 30080
            type: NodePort
        dest: /tmp/jenkins-agent-daemonset.yml

    - name: Deploy jenkins agent daemonset
      ansible.builtin.command:
        cmd: kubectl apply -f /tmp/jenkins-agent-daemonset.yml
      register: deploy_result
      changed_when: "'created' in deploy_result.stdout or 'configured' in deploy_result.stdout"

    - name: Remove temporary manifest file
      ansible.builtin.file:
        path: /tmp/jenkins-agent-daemonset.yml
        state: absent
```

1. Jenkins 에이전트 리소스를 배포하기 위해 "jenkins-agent"라는 새 네임스페이스를 만듭니다. 작업은 네임스페이스가 생성되었는지 또는 이미 존재하는지 확인하기 위해 결과를 등록합니다.
2. Jenkins 에이전트 DaemonSet 및 해당 서비스 구성으로 YAML 파일을 생성합니다. DaemonSet은 사용자 지정 Jenkins 에이전트 이미지를 사용하고 Jenkins URL 및 SSH 공개 키에 대한 환경 변수를 포함합니다. 이 서비스는 포트 30080에서 NodePort 유형 서비스를 사용하여 포트 4444에서 Jenkins 에이전트를 노출합니다.
   1. `SSH_PUBLIC_KEY` 에 `id_rsa.pub` 파일 내에 있는 key값을 입력합니다.
3. 이전에 생성한 YAML 파일을 적용하여 Kubernetes 클러스터에 Jenkins 에이전트 DaemonSet 및 서비스를 생성합니다. 태스크는 리소스가 생성 또는 업데이트되었는지 확인하기 위해 결과를 등록합니다.
4. Jenkins 에이전트 DaemonSet 및 서비스를 배포한 후 더 이상 필요하지 않으므로 이전에 생성한 임시 YAML 파일을 제거합니다.

- `ansible-playbook /home/ansible-playbooks/playbook-create-jenkins-agent.yml` 실행

## 10. jenkins-cli를 활용한 jenkins-agent 추가

### 1. jenkins cli를 사용하기위해 다운로드합니다.

```sh
curl -l <젠킨스URL>/jnlpJars/jenkins-cli.jar --output jenkins-cli.jar
```

### 2. 젠킨스에 로그인할 관리자 계정의 정보를 jenkins-cli-credential에 저장합니다.

```sh
touch jenkins-cli-credential
echo 'JENKINS_URL=<젠킨스주소>' >> jenkins-cli-credential
echo 'JENKINS_USER=<아이디>' >> jenkins-cli-credential
echo 'JENKINS_PASSWORD=<비밀번호>' >> jenkins-cli-credential
```

### 3. 스크립트 본체를 작성합니다.

```add-agent.sh
#!/bin/sh

# load credential
. ./jenkins-cli-credential

# Define variables
NODE_NAME=$3
NODE_DESCRIPTION=$3
NODE_REMOTE_ROOT=/home/jenkins
NODE_LABELS=$4
REMOTE_FS=/home/jenkins

# Define SSH agent variables
SSH_ADDRESS=$1
SSH_PORT=$2
CREDENTIAL_ID=$5

# create node using Jenkins CLI
java -jar jenkins-cli.jar -s $JENKINS_URL -auth "$JENKINS_USER:$JENKINS_PASSWORD" create-node $NODE_NAME << EOF
<slave>
  <name>$NODE_NAME</name>
  <description>$NODE_NAME</description>
  <remoteFS>$REMOTE_FS</remoteFS>
  <numExecutors>1</numExecutors>
  <mode>NORMAL</mode>
  <retentionStrategy class="hudson.slaves.RetentionStrategy\$Always"/>
  <launcher class="hudson.plugins.sshslaves.SSHLauncher" plugin="ssh-slaves@2.877.v365f5eb_a_b_eec">
    <host>$SSH_ADDRESS</host>
    <port>$SSH_PORT</port>
    <credentialsId>$CREDENTIAL_ID</credentialsId>
    <launchTimeoutSeconds>60</launchTimeoutSeconds>
    <maxNumRetries>10</maxNumRetries>
    <retryWaitTime>15</retryWaitTime>
    <sshHostKeyVerificationStrategy class="hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy"/>
    <tcpNoDelay>true</tcpNoDelay>
  </launcher>
  <label>$NODE_LABELS</label>
  <nodeProperties />
</slave>
EOF
```

### 4. 스크립트를 실행합니다.

```sh
sh add-agent.sh <agent 주소> <agent ssh 포트> <agent 이름> <agent 레이블> <credential ID>
```
