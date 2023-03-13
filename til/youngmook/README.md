# README : 임영묵

## 1. Ansible 설치 및 초기설정

1. Ansible 설치

```bash
sudo apt update
sudo apt install software-properties-common
sudo add-apt-repository --yes --update ppa:ansible/ansible
sudo apt install ansible
```

2. 프로젝트 directory 생성 + inventory.yml

```bash
mkdir daeguops-project
cd daeguops-project
vi inventory.yml

###
workernodes:
  hosts:
    node1:
      ansible_host: 192.168.242.236
    node2:
      ansible_host: 192.168.248.92
    node3:
      ansible_host: 192.168.240.236

masternode:
  hosts:
    kmaster:
      ansible_host: 192.168.250.196
###
```

3. ssh 세팅

```bash
ssh-keygen -> 엔터...
cat .ssh/id_rsa.pub

# 내용 복사해서 각 노드의 .ssh/authorized_keys에 복사 (ssh-copy-id가 안먹혀서 직접 함)

ssh ubuntu@노드IP
```

\*\*\* ssh-copy-id로 한번에 대상 노드에 public key 복사하는 방법 :

[ssh-copy-id 시 Permission Denied 에러](https://www.notion.so/ssh-copy-id-Permission-Denied-6ce71812b7124bb4b8d2197eda06d455)

4. Ping 테스트

```bash
ansible all -m ping -i inventory.yml
```

5. Jenkins 컨테이너

```bash
docker run --name jenkins-docker -d -p 8080:8080 -p 50000:50000 -v /home/jenkins:/var/jenkins_home -u root jenkins/jenkins:lts

# docker logs jenkins-docker -> password를 localhost:8080에 복붙 + 추천 플러그인 설치
```

## 2. 노드들 초기세팅

1. sh 파일 저장할 디렉토리 생성 + sh 파일 생성

```bash
mkdir init-scripts
cd init-scripts

touch install-docker.sh
touch install-k8s.sh
touch init-k8s-nodes.sh
touch init-k8s-master.sh
```

- install-docker.sh

```bash
sudo apt-get remove docker docker-engine docker.io containerd runc

sudo apt-get update
sudo apt-get install \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

sudo mkdir -m 0755 -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

- install-k8s.sh
- [https://confluence.curvc.com/pages/releaseview.action?pageId=98048155](https://confluence.curvc.com/pages/releaseview.action?pageId=98048155)

```bash
# swap 비활성화
sudo swapoff -a && sudo sed -i '/swap/s/^/#/' /etc/fstab

# iptable 설정
cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
br_netfilter
EOF

cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
EOF
sudo sysctl --system

sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl

sudo curl -fsSLo /etc/apt/keyrings/kubernetes-archive-keyring.gpg https://packages.cloud.google.com/apt/doc/apt-key.gpg

echo "deb [signed-by=/etc/apt/keyrings/kubernetes-archive-keyring.gpg] https://apt.kubernetes.io/ kubernetes-xenial main" | sudo tee /etc/apt/sources.list.d/kubernetes.list

sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl
```

2. Ansible Playbook 생성

```bash
mkdir ansible-playbooks
cd ansible-playbooks

touch playbook-docker.yml
touch playbook-k8s.yml
touch playbook-init-k8s-nodes.yml
touch playbook-init-k8s-master.yml
```

[Ansible Playbook으로 스크립트 파일 실행 시 무한 대기 걸리는 에러](https://www.notion.so/Ansible-Playbook-a3c727bbfd8144e083716b7bd4704c3c)

- playbook-docker.yml

```yaml
- name: Install Docker
  hosts: all
  tasks:
    - name: Copy Docker install shell script
      copy: src=/home/ubuntu/daeguops-project/init-scripts/install-docker.sh
        dest=/home/ubuntu/scripts/
        mode=0777

    - name: Execute script
      command: sh /home/ubuntu/scripts/install-docker.sh
      async: 3600
      poll: 5

    - name: Reset SSH Connection
      meta: reset_connection
```

## 3. 젠킨스 에이전트 세팅

1. 젠킨스 SSH Credential Setting

   1. Jenkins dashboard → Manage Jenkins → Manage Credentials → Add Credentials
   2. “SSH Username with private key”로 젠킨스 에이전트를 설치할 노드로 연결할때 쓸 수 있는 private key를 등록

2. 젠킨스 에이전트 컨테이너 생성

```bash
docker run -d --rm --name=agent1 -p 4444:22 \
-e "JENKINS_AGENT_SSH_PUBKEY=[your-public-key]" \
jenkins/ssh-agent:alpine
```
