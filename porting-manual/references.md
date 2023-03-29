빌드 인프라 포팅 매뉴얼

이 설명서는 Docker, Ansible, Kubernetes 및 Jenkins를 사용하여 빌드 인프라를 포팅하는 단계별 가이드를 제공합니다.

전제 조건:

AWS 계정
AWS, Docker, Ansible, Kubernetes 및 Jenkins에 대한 기본 지식
1단계: EC2 인스턴스 생성(메인, k8s 마스터, k8s 작업자)

AWS 계정에 로그인하고 EC2 대시보드로 이동합니다.
"Launch Instance"를 클릭하고 "Ubuntu 20.04 LTS" AMI를 선택합니다.
k8s 마스터 및 k8s 작업자 서버의 인스턴스 유형으로 "m5.large"를 선택합니다.
인스턴스 스토리지를 50GB로 설정합니다.
필요한 포트(22, 80, 443, 8080 등)를 허용하도록 보안 그룹을 구성합니다.
인스턴스를 시작하고 해당 IP 주소 또는 도메인 이름을 할당합니다.
2단계: 메인 서버에 Docker 설치

기본 서버에 SSH로 연결하고 다음 명령을 실행하여 Docker를 설치합니다.

```
sudo apt update
sudo apt install apt-transport-https ca-certificates curl software-properties-common
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install docker-ce docker-ce-cli containerd.io
sudo systemctl enable docker
sudo systemctl start docker
```

sudo 없이 docker 명령을 실행하려면 현재 사용자를 docker 그룹에 추가합니다.

```
sudo usermod -aG docker $USER
```

3단계: Docker에서 Jenkins 서버 및 Ansible 서버 컨테이너 설치 및 실행

다음 콘텐츠로 docker-compose.yml 파일을 만듭니다.

```
version: "3.8"

services:
  ansible-server:
    image: ansible/ansible-runner:latest
    container_name: ansible-server
    volumes:
      - /etc/ansible:/etc/ansible
      - /var/run/docker.sock:/var/run/docker.sock

  jenkins-server:
    image: jenkins/jenkins:lts
    container_name: jenkins-server
    ports:
      - "8080:8080"
      - "50000:50000"
    volumes:
      - /var/jenkins_home:/var/jenkins_home
```

docker-compose up -d를 실행하여 Jenkins 및 Ansible 서버 컨테이너를 시작합니다.

4단계: Jenkins 구성

http://j8s003.p.ssafy.io:8080을 통해 Jenkins에 액세스합니다.
컨테이너에서 Jenkins 잠금 해제 키를 검색합니다.

```
docker exec jenkins-server cat /var/jenkins_home/secrets/initialAdminPassword
```

잠금 해제 키를 입력하고 설치 마법사에 따라 Jenkins를 설정합니다.

5단계: Ansible 서버에 Ansible 설치 및 구성

주 서버에 SSH로 연결하고 다음 명령을 실행하여 호스트 시스템에 Ansible을 설치합니다.

```
sudo apt update
sudo apt install ansible
```

k8s 마스터 및 k8s 작업자 서버 정보를 사용하여 인벤토리 파일 /etc/ansible/hosts를 만들고 구성합니다.

```
[k8s-master]
43.200.19.67

[k8s-workers]
3.39.143.183
15.165.217.62
52.78.18.204

6단계: Ansible 서버와 k8s 작업자 서버 간에 SSH 연결 만들기

1. main 서버에서 SSH 키 쌍을 생성합니다.

```

ssh-keygen

```

2. 공개 키를 k8s 마스터 및 작업자 서버에 복사합니다.

```

ssh-copy-id ubuntu@43.200.19.67
ssh-copy-id ubuntu@3.39.143.183
ssh-copy-id ubuntu@15.165.217.62
ssh-copy-id ubuntu@52.78.18.204

```

7단계: Ansible 플레이북을 사용하여 k8s 마스터 서버 및 k8s 작업자 서버에 Docker 및 Kubernetes 설치


다음 콘텐츠로 install_k8s.yml이라는 플레이북을 만듭니다.

```

- name: Install Docker and Kubernetes
  hosts: all
  become: yes
  tasks:
  - name: Install Docker
    apt:
    name: "{{ item }}"
    state: present
    update_cache: yes
    with_items:

    - apt-transport-https
    - ca-certificates
    - curl
    - software-properties-common
    - docker-ce
    - docker-ce-cli
    - containerd.io

  - name: Add Kubernetes repo
    apt_repository:
    repo: "deb https://apt.kubernetes.io/ kubernetes-xenial main"
    state: present
    filename: kubernetes.list
    key_url: https://packages.cloud.google.com/apt/doc/apt-key.gpg

  - name: Install Kubernetes
    apt:
    name: "{{ item }}"
    state: present
    update_cache: yes
    with_items:
    - kubelet
    - kubeadm
    - kubectl

```
다음 명령을 사용하여 플레이북을 실행합니다.

```

ansible-playbook install_k8s.yml

```

8단계: k8s 마스터 초기화


k8s 마스터 서버에 SSH로 연결하고 다음 명령을 실행하여 Kubernetes를 초기화합니다.

```

sudo kubeadm init --pod-network-cidr=10.244.0.0/16

```

제공된 지침에 따라 로컬 kubeconfig 파일을 설정합니다.
CNI 플러그인으로 Flannel을 설치합니다.

```

kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml

```

9단계: k8s 마스터와 k8s 작업자 연결

1. k8s 마스터 서버에서 가입 명령을 검색합니다.

```

kubeadm token create --print-join-command

```

SSH를 통해 각 k8s 작업자 서버에 연결하고 조인 명령을 실행합니다.

10단계: Jenkins 에이전트 배포


- Jenkins 서버에서 새 에이전트를 생성하고 에이전트 암호를 검색합니다.
- SSH를 통해 k8s 마스터 서버에 연결하고 에이전트 이름과 시크릿이 포함된 Jenkins 에이전트 배포 yaml 파일을 생성합니다.

1. kubectl을 사용하여 Jenkins 에이전트를 배포합니다.

```

kubectl create -f jenkins-agent.yaml

```

- NodePort 또는 LoadBalancer를 사용하여 Jenkins 에이전트 서비스를 노출합니다.

11단계: Jenkins 마스터와 Jenkins 에이전트 연결

1. Kubernetes 클러스터에서 Jenkins 에이전트가 실행 중인지 확인합니다.
2. Jenkins 서버에서 에이전트가 성공적으로 연결되었는지 확인합니다.
```
