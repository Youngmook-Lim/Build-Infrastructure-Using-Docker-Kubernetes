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

---

# Jenkins-Plugin

이번 연계 프로젝트에서 Jenkins Plugin을 직접 개발해 사용해야 하는 부분이 생겼다.

자세하게는 언급할 수 없지만 간단하게 이야기하면 앞으로 개발할 플러그인의 기능은 화면단에서 원하는 input 값을 받아 그 input 값으로 Jenkins Pipeline을 구축해 Job으로 등록한다.

input 값의 형태는 String 값을 받고, jenkins plugin documentation을 최대한 참고하여 제작할 계획이다.

[https://www.jenkins.io/doc/developer/tutorial/](https://www.jenkins.io/doc/developer/tutorial/ "Jenkins Plugin Docuimentation")

[Plugin Tutorial www.jenkins.io](https://www.jenkins.io/doc/developer/tutorial/)

## 0\. 들어가면서...

- 시작하기에 앞서 앞으로의 진행과정은 Jenkins Plugin 공식문서에 기반하여 어떻게 진행했는지 흐름을 작성해나갈 계획이다.
- 위의 Document를 보며 진행하면 도움이 될 것 같다라고라고라파덕.

## 1\. Java 및 Maven 설치

- Java Version은 Jenkins 에서의 권장버전인 11버전을 설치했다.
- Maven Version 또한 권장 버전인 3버전을 사용했다.
  - (Jenkins에서는 Maven 버전의 경우 3.8.3 보다 최신버전을 사용하는 것을 이상적이라고 한다.)

각각의 설치 이후 버전을 확인해준다.

```
$ java --version
$ mvn --version
```

## 2\. IDE 준비

- Jenkins에서 추천하는 IDE로는 크게 `NetBeans` `IntelliJ IDEA` `Eclipse` 가 있다.
- 인텔리제이를 선택했다.(Ultimate 버전을 사용한다.)

## 3\. Skeleton 코드를 포함한 프로젝트 생성

- 본인은 혼자 진행하는 과정에서 여기서 애를 좀 먹었다.
- 이전 튜토리얼에서는 IDE 를 설치 후 간단하게 프로젝트를 생성하는 예제가 있었는데 해당 예제 프로젝트 경로에서 아래의 명령어를 치면 pom.xml 파일의 중복으로 에러가 발생한다.
- 정리하자면 그냥 새로 생성할 디렉토리를 잡아 그곳에서 아래의 명령어를 통해 Jenkins Plugin Skeleton 프로젝트를 생성하자.

```
mvn -U archetype:generate -Dfilter="io.jenkins.archetypes:"
```

- 위의 명령어를 입력하면 archetype을 설정하라는 내용이 나타나는데 본인은 4번 타입(Skeleton of a Jenkins plugin with a POM and an example build step.) 을 선택했고 버전의 경우 가장 최신버전으로 나타나는 17버전을 사용했다.
- 이후 프로젝트가 생성되는데 아래의 명령어를 따라 mvn verify를 실행한다.
  - 혹시 mvn verify 가 궁금하다면 아래의 더보기를 클릭해보도록 하자.

더보기

mvn verify 명령어를 실행하면 다음과 같은 일들이 발생한다.

1.  Maven은 프로젝트의 소스 코드를 가져와 컴파일한다.
2.  컴파일된 코드에 대해 단위 테스트를 수행한다.
3.  단위 테스트를 통과한 코드에 대해 패키징을 수행한다.
4.  패키징된 코드에 대해 통합 테스트를 수행한다.
5.  통합 테스트를 통과한 코드에 대해 검증을 수행한다. 이 과정에서는 코드의 정적 분석, 문서 생성, 리소스 복사 등의 작업이 수행된다.
6.  검증을 마친 후, 빌드 결과물이 로컬 레파지토리에 설치된다.

즉, mvn verify 명령어를 실행하면, 프로젝트의 소스 코드가 올바르게 빌드되고 검증되어 완전하고 신뢰성 높은 빌드 결과물이 생성된다.

\- 아래의 명령어를 순차적으로 입력한다.

```
$ mv demo demo-plugin
$ cd demo-plugin
$ mvn verify
```

## 4\. 빌드 및 실행

- 3번 과정을 통해 `mvn verify` 까지 성공했다면 이후 아래 명령어를 통해 프로젝트를 실행한다.

```
$ mvn hpi:run
```

- 위의 명령어에 있어서 옵션을 통해 노출할 포트를 변경할 수 있다. Ex) `mvn hpi:run -Dport=5000`
- 해당 과정이 정상적으로 이루어졌다면 젠킨스 플러그인을 통해 개발할 준비가 완료된 것이다.

## 5. ## 젠킨스 플러그인 코드 (주요 부분)

- 젠킨스 플러그인에서 구현해야할 사항은 크게 나눠보면 UI와 내부 기능이 있다.
- 기존 프론트엔드 프로젝트와 비교했을때 UI ⇒ HTML,CSS, Function ⇒ Javascript로 나눌 수 있다고하면 Jenkins 플러그인에서의 UI는 Jelly 파일을 통해 작성하고, 기능은 Java 클래스에 정의할 수 있다.

<br />

### UI : Jelly File

- 먼저 UI 부분에서 Jenkins Plugin의 경우 Jelly 파일을 이용해 UI를 작성한다.
- 이때 UI에서 발생하는 이벤트로직 처리는 Javascript를 이용해서 구현했다.
- XML 파일의 특성상 `<` 혹은 `>` 문자열의 경우 태그로 인식하기 때문에 javascript 코드에서 기존 사용하던 >, < 대소비교는 사용할 수 없었다.
  - 특이하게도 화살표함수는 사용할 수 있었으나 최대한 한정된 Javascript 문법으로 UI 이벤트 로직을 구현할 수 있는 선에서 필요한 기능들을 구현하였다.

<br />

```html
<?jelly escape-by-default='true'?>
<j:jelly
  xmlns:j="jelly:core"
  xmlns:st="jelly:stapler"
  xmlns:d="jelly:define"
  xmlns:l="/lib/layout"
  xmlns:t="/lib/hudson"
  xmlns:f="/lib/form"
>
  <f:entry title="${%Pipeline Job Name}" field="name" class="nameField">
    <input
      type="text"
      name="name"
      class="jenkins-input"
      onchange="handleChange(event,0)"
    />
    <div class="nameErr errTxt"></div>
  </f:entry>
  <f:entry title="${%GitURL}" field="gitUrl">
    <input
      type="text"
      name="gitUrl"
      class="jenkins-input"
      onchange="handleChange(event,1)"
    />
    <div class="urlErr errTxt"></div>
  </f:entry>
  <f:entry title="${%Commit Hash}" field="commitHash">
    <input
      type="text"
      name="commitHash"
      class="jenkins-input"
      onchange="handleChangeCommitHash(event)"
    />
  </f:entry>
  <f:entry title="${%Branch}" field="branch">
    <input
      type="text"
      name="branch"
      class="jenkins-input"
      onchange="handleChange(event,2)"
    />
    <div class="branchErr errTxt"></div>
  </f:entry>
  <f:entry title="${%Build Path}" field="buildPath">
    <input
      type="text"
      name="buildPath"
      class="jenkins-input"
      onchange="handleChange(event,3)"
    />
    <div class="buildPathErr errTxt"></div>
  </f:entry>
  <f:entry title="${%Language}" field="language">
    <select
      class="jenkins-input"
      name="language"
      onchange="handleChange(event,4)"
    >
      <option value="">Select Language</option>
      <option value="java">Java</option>
      <option value="c">C</option>
      <option value="c++">C++</option>
      <option value="csharp">C#</option>
    </select>
    <div class="languageErr errTxt"></div>
  </f:entry>
  <f:entry title="${%Build Environment}" field="buildEnv">
    <select
      class="jenkins-input buildEnvSelect"
      name="buildEnv"
      onchange="handleChange(event,5)"
    >
      <option value="">Select Build Environment</option>
    </select>
    <div class="buildEnvErr errTxt"></div>
  </f:entry>

  <script>
    document
      .getElementsByTagName("form")[1]
      .addEventListener("submit", (event) => {
        // submit event 등록
        const hasEmptyValue = variables.some((v) => v === null || v === "");
        if (hasEmptyValue) {
          event.preventDefault();
          alert("입력되지 않은 값이 존재합니다. 다시 한번 확인해주세요.");
          return;
        }
      });

    const buildEnvObject = {
      java: [{ Maven: "maven" }, { Gradle: "gradle" }],
      c: [{ "Temp C Build Env": "cbuildenv" }],
      "c++": [{ "Temp C++ Build Env": "c++buildenv" }],
      csharp: [{ MSBuild: "msbuild" }],
    };

    let commitHash = null;
    const variables = [null, null, null, null, null, null];
    const types = [
      "nameErr",
      "urlErr",
      "branchErr",
      "buildPathErr",
      "languageErr",
      "buildEnvErr",
    ];
    const errTexts = {
      nameErr: "Please set a job name",
      urlErr: "Please set a git URL",
      branchErr: "Please set a branch",
      buildPathErr: "Please set a build path",
      languageErr: "Please select a language",
      buildEnvErr: "Please select a build environment",
    };

    variables.map((v, i) => {
      const node = document.getElementsByClassName(types[i])[0];
      if (v === null || v === "") {
        if (node) {
          node.innerText = errTexts[types[i]];
        }
      }
    });

    function handleChange(event, type) {
      variables[type] = event.target.value;
      if (variables[type] === null || variables[type] === "") {
        document.getElementsByClassName(types[type])[0].innerText =
          errTexts[types[type]];
      } else {
        document.getElementsByClassName(types[type])[0].innerText = "";
        if (type === 4) setBuildEnvOptions(event.target.value);
      }
    }

    function setBuildEnvOptions(selectedLang) {
      const buildList = buildEnvObject[selectedLang];
      const select = document.getElementsByClassName("buildEnvSelect")[0];
      const defaultOp = document.createElement("option");

      select.innerHTML = "";
      defaultOp.innerText = "Select Build Environment";
      defaultOp.value = "";
      select.appendChild(defaultOp);

      buildList.map((meta) => {
        const option = document.createElement("option");
        const key = Object.keys(meta)[0];
        const value = meta[key];

        option.innerText = key;
        option.value = value;

        select.appendChild(option);
      });
    }

    function handleChangeCommitHash(event) {
      commitHash = event.target.value;
    }
  </script>

  <style>
    .errTxt {
      color: red;
      font-weight: bold;
    }
  </style>
</j:jelly>
```

- Form 태그에서는 사용자로부터 다음과 같은 값들을 입력받는다.

  - Job Name
  - Git URL
  - Commit Hash
  - Branch
  - BuildPath
  - Language
  - BuildEnv

- 그 외의 Javascript 코드는 값이 비었을때와 각각의 값에 대한 Validation 역할을 수행한다.

### Message.properties

- Message 클래스내 .properties 파일에서는 표시할 메세지들을 미리 정의할 수 있다.

<span align="center">

![](./assets/msgPorperties.png)

</span>

### help.html

- 각각의 파라미터에 대한 부가 설명에 대한 help-\*.html 파일들은 resource 하위에 config.jelly와 같은 레이어에 위치한다.

<span align="center">

![](./assets/help.png)

</span>

### Java 기능부

- 다음 부터는 java의 기능 부분 역할이다.

```java
@Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {

        Jenkins jenkinsInstance = Jenkins.get();
        if(name.equals("") || gitUrl.equals("") || language.equals("") || buildEnv.equals("") || branch.equals("") || buildPath.equals("")) {
            listener.getLogger().println("The build failed. A required input value is empty.");
            WorkflowJob job = jenkinsInstance.createProject(WorkflowJob.class, name);
            job.makeDisabled(true);
            return;
        }

        String jobName = run.getParent().getDisplayName();

        // Gets the logged in username.
        String currentUsername=jobName.split("-")[0];
        Folder userFolder;
        try {
            userFolder = getUserFolder(currentUsername);
        } catch (IOException e) {
            e.printStackTrace(listener.error("사용자 폴더를 가져오거나 생성하는데 실패했습니다."));
            return;
        }

        // check job name duplication
        TopLevelItem jobItem = userFolder.getItem(currentUsername+"-"+name);
        if (jobItem != null) {
            listener.getLogger().println("Job with this name already exists: " + name);
//            WorkflowJob job = jenkinsInstance.createProject(WorkflowJob.class, name);
//            job.makeDisabled(true);
            // ???
            return;
        }

        // Create a new Pipeline Job
        try {
            TopLevelItem item = userFolder.createProject(WorkflowJob.class, currentUsername+"-"+language+"-"+name);
            if (item instanceof WorkflowJob) {
                WorkflowJob job = (WorkflowJob) item;
                job.setDefinition(new CpsFlowDefinition(generateScript(), true));
                job.save();
                job.scheduleBuild2(0).waitForStart();
            } else {
                listener.getLogger().println("Creating a new pipeline job failed.");
            }
        } catch (Exception e) {
            e.printStackTrace(listener.error("Creating a new pipeline job failed."));
        }
    }
```

- perform 메서드에서는 사용자로부터 각각의 Parameter를 받아 값이 제대로 전달 되었는지 검증한다.
- 이후 해당 유저에 대한 Job 폴더를 가져오거나 생성하고, 이후 해당 폴더 내에 새로운 pipeline job을 등록한다.
- 우리의 서비스의 경우 freestyle job으로 등록된 Pipeline Generator를 통해서 해당 Pipeline Job을 등록하는 방식으로 작동하기 때문에 Pipeline Job을 생성하고 빌드를 수행하는 코드도 포함된다.

- 또한 pipeline 스크립트의 경우 getRestScript 메서드를 통해 가져오며 return 값으로 사용자 파라미터 값이 적용된 script를 반환한다.

<span align="center">

![](./assets/getScript.png)

</span>

### 권한 설정 관련 Groovy File

```groovy
import jenkins.model.Jenkins
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy
import com.michelin.cio.hudson.plugins.rolestrategy.Role
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType
import hudson.security.Permission
import hudson.model.User
import hudson.model.FreeStyleProject
import com.cloudbees.hudson.plugins.folder.Folder

Jenkins jenkins = Jenkins.get()
authorizationStrategy=jenkins.getAuthorizationStrategy()
rbas=(RoleBasedAuthorizationStrategy) authorizationStrategy

def users = User.getAll()

ItemRoleMap = rbas.getRoleMaps()[RoleType.Project]
Set<Permission> userPermissions = Permission.getAll().toSet()

def roleLength = ItemRoleMap.getRoles().size()
def userLength = users.size()

if(roleLength != userLength){
    println "A new user has signed up. Renew authentication."
    users.each { user ->
        def itemRolePattern = "${user.getId()}-.*"
        def userName = "${user.getId()}"
        def itemRole = new Role(userName, itemRolePattern, userPermissions)
        ItemRoleMap.addRole(itemRole)
        ItemRoleMap.assignRole(itemRole,user.getId())
        def jobName= userName+"-PipelineGenerator"
        def folderName= userName+"-folder"
        // Check if freestyle job exists with user's ID as name
        if (!jenkins.getItemByFullName(folderName, Folder.class)) {
            // Create new folder  & job
            def folder = jenkins.createProject(Folder.class, folderName)
            folder.save()
            def job = folder.createProject(FreeStyleProject.class, jobName)
            job.setDescription("Pipeline Generator입니다. Pipeline Generator build step을 수정 후 빌드하세요.")
            job.save()
            println userName +" has signed up"
        }
    }
    jenkins.setAuthorizationStrategy(rbas)
}else{
    println "There are no new users."
}
```

- 권한 설정의 경우 해당 groovy 스크립트를 admin에서 실행하는 job을 생성하여 주기적으로 실행한다.
- 이때 1분 주기로 해당 스크립트가 동작하며 새롭게 추가된 유저에 대한 권한을 설정하고 폴더와 Pipeline Generator를 추가해준다.

---

## Grafana & Prometheus

- 우리가 구축한 Build Infra에는 총 5개의 EC2 인스턴스를 사용한다. 그중에서도 우리가 지켜봐야할 서버자원은 총 4개로 Build1,2,3 서버와 Kubernetes Master서버(이한 k8s master)의 상태를 모니터링 해야한다.

- 전체 적인 구상은 이렇다.
  - 각각의 서버 4대(build 1,2,3, k8s-master)에 Node-Exporter(서버의 자원 상태를 Prometheus에게 전송해 줌)를 설치하고 prometheus에서 node-exporter들로부터 시계열 데이터를 수집한다.
  - 이후 grafana에서 datasource로 prometheus를 등록하고 해당 prometheus에서 수집된 데이터를 사용자에게 시각화하여 모니터링 할 수 있도록 한다.
- 초기 yaml파일을 활용한 직접 설정을 계획하고 구축했으나, 각각의 툴들의 연동을 하는 과정에서 이상이 있어 결국 Helm(Kubernetes Package Manager)을 사용하여 Prometheus 관련 툴들을 설치하였다.

- 이때 필요사항으로는
  - Kubernetes 1.16이상
  - Helm 3이상
  이다.
- 다음 과정들은 k8s-master서버에서 수행되었다.

1. **Prometheus 공식 Github에 있는 레포지토리를 helm을 이용하여 추가**

```shell
$ helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
$ helm repo update
```

2. **Prometheus 환경을 구성할 수 있는 kube-prometheus-stack을 pull**

```bash
$ helm pull prometheus-community/kube-prometheus-stack
```

3. **pull 받은 압축파일 압축 해제**

```bash
$ tar xvfz kube-prometheus-stack-42.3.0.tgz
```

4. \***\*values.yaml 설정\*\***

- 우리가 구축한 Build Infra의 경우 values.yaml에서 설정할 것들이 몇가지 존재했다. 우선 Grafana의 adminPassword를 수정하였다.
- 또한 가장 상위에 존재하는 values.yaml의 경우 prometheus Service의 type 이 ClusterIP로 되어있었기 떄문에 외부에서 접근이 불가능했다. 따라서 해당 부분을 NodePort 타입으로 변경하였다.

<span align="center">

![](./assets/value1.png)

</span>

- 같은 이유로 grafana 폴더 내 values.yaml에서도 grafana Service의 타입을 NodePort로 정의하여 외부에서도 접근할 수 있게끔 수정하였다.

<span align="center">

![](./assets/value2.png)

</span>

5. \***\*kube-prometheus-stack 설치\*\***

```bash
$ helm install prometheus . -n monitoring -f values.yaml
```

- 위의 명령어를 수행하면 설정한 values.yaml파일을 기반으로 프로메테우스 관련 툴들이 클러스터 전반에 걸쳐 실행된다.
- 이후 각각의 띄워진 주소는 아래와 같다.
  - Grafana : [http://52.78.18.204:31000/](http://52.78.18.204:31000/)
  - Prometheus : [http://15.165.217.62:30090/](http://15.165.217.62:30090/)

---

## NginX

- DaeGuOps 빌드인프라는 두개의 진입점이 존재한다.
  - 사용자가 접속해 빌드인프라를 활용할 Jenkins Page
  - 관리자가 접속해 빌드인프라의 서버 상태를 모니터링할 grafana 페이지
- 두개의 서버를 접속함에 있어 WebServer 설정을 해야했고 각각은 / 와 /monitoring 을 통해 프록시 한다.

  - Main 페이지(Jenkins) : [https://j8s003.p.ssafy.io](https://j8s003.p.ssafy.io)
  - Monitoring 페이지 (grafana) : [https://j8s003.p.ssafy.io/monitoring](https://j8s003.p.ssafy.io/monitoring)

- NginX Config File

```shell
upstream jenkins {
    server 127.0.0.1:8080;
}

upstream monitoring {
    server 52.78.18.204:31000;
}

server {
        listen 80;
        server_name j8s003.p.ssafy.io;

        location / {
                return 308 https://$host$request_uri;
        }
}

server {
        listen 443 ssl;
        server_name j8s003.p.ssafy.io;

        ssl_certificate /etc/letsencrypt/live/j8s003.p.ssafy.io/fullchain.pem; # managed by Certbot
        ssl_certificate_key /etc/letsencrypt/live/j8s003.p.ssafy.io/privkey.pem; # managed by Certbot
        include /etc/letsencrypt/options-ssl-nginx.conf;
        ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

        location /monitoring/ {
                rewrite ^/monitoring(/.*)$ $1 break;
                proxy_pass http://monitoring;
                proxy_set_header Host $host;
                proxy_set_header X-Real-IP $remote_addr;
                proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                proxy_redirect off;
        }

        location / {
                proxy_pass http://jenkins;
                proxy_set_header Host $host;
                proxy_set_header X-Real-IP $remote_addr;
                proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        }
}
```

- Grafana Root URL 설정

<span align="center">

![](./assets/grafana%20value.png)

</span>

- 위 파일과 grafana환경에서의 value.yaml 내의 root_url 설정을 통해 리버스 프록시를 해주었고, 그 결과는 아래와 같다.

<span align="center">

![](./assets/jenkinsURL.png)

</span>

<span align="center">

![](./assets/grafanaURL.png)

</span>

---
