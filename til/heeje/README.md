## Jenkins Master 설치

### 1. Main EC2 접속

- 여유있다면 Termius로 접속하는 방법까지?

### 2. Docker 설치

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

### 3. Jenkins Master 설치 및 설정

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

## Ansible Server 설치

### 1. Docker Container 생성

```jsx
docker run --privileged -itd --name ansible-server -p 20022:22 -p 8081:8080 -e container=docker -v /sys/fs/cgroup:/sys/fs/cgroup ubuntu:20.04 /bin/bash
```

### 2. Ansible-Server 접속

```jsx
docker exec -it ansible-server bash
```

### 3. Ansible 설치

```
apt update
apt install software-properties-common
```

- 도시 설정

![Untitled.png](README_assets/7d147e9ed999df39a9d22f20750e318a8b348272.png)

![Untitled 1.png](README_assets/3d4a6ad052310f9f79491ecc53fac095f2c25244.png)

```jsx
add-apt-repository --yes --update ppa:ansible/ansible
apt install ansible
```

![Untitled 2.png](README_assets/7c7c3f888ea59db2d322d32e8c79589d6dc8db4a.png)

## Host 파일 생성

### 1. vim 설치

```jsx
apt-get install vim
```

![Untitled 2.png](README_assets/7c7c3f888ea59db2d322d32e8c79589d6dc8db4a.png)

### 2. Inventory 작성

1. `sudo vi /etc/ansible/hosts`
2. 다음과 같이 수정

```
[k8s-workers]
(빌드 서버1 IP)
(빌드 서버2 IP)
(빌드 서버3 IP)

[k8s-master]
(k8s 서버 IP)
```

## SSH 연결

### 1. K8s Master, Build Server에 Password 생성(자동화 생각해보기)

- K8s Master Server에 접속
- /etc/ssh/sshd_config 파일 수정(PasswordAuthentication yes)

```jsx
sudo vi /etc/ssh/sshd_config
```

![Untitled.png](README_assets/96be1d47ecaab63a8aadec4fe2b0139d78d910d6.png)

- root 계정 접속

```jsx
sudo su
```

- `ubuntu` 사용자의 비밀번호 변경

```jsx
passwd ubuntu
```

![Untitled 1.png](README_assets/073a0b9321e4d1f822012a145e6714bac7422182.png)

- `ubuntu` 사용자로 돌아와 ssh 재시작

```
# return to user "ubuntu"
exit

# restart ssh
sudo systemctl restart ssh
```

- 모든 빌드 서버에 대하여 위 과정 실행

### 2. SSH key 생성 (자동화 생각해보기)

- ansible-server에서 `ssh-keygen` 입력
- Enter 3번 입력

![Untitled 2.png](README_assets/29712fb3f9d1ca07c1dc318ff487f1f89b64e4e6.png)

### 3. K8s Master, Build Server에 SSH key 복사

- ansible-server에서 다음과 같이 입력(SSH key를 해당 서버에 복사)

```
# copy the SSH key to the server
ssh-copy-id -i ~/.ssh/id_rsa.pub ubuntu@[k8s master 또는 빌드 서버 IP]
```

- yes 입력 후 해당 빌드 서버에 저장해둔 비밀번호 입력

![Untitled 3.png](README_assets/ef454e8b2bb498cd668a8332c9d17eff59d44692.png)

![Untitled 4.png](README_assets/fce2b712f58c9385b76672fd2f99169445642971.png)

- 나머지 서버에도 위 과정 반복

## Docker 설치

### 1. Playbook, Shell script를 저장할 폴더 생성

```
mkdir /home/ansible-playbook
mkdir /home/init-scripts
```

### 2. Docker 설치를 위한 Shell script 파일 생성

```
vi /home/init-scripts/install-docker.sh
```

- 아래 내용 입력 및 저장

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

### 3. Docker 설치를 위한 Ansible-Playbook 파일 생성

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

### 4. Docker 설치를 위한 Ansible Playbook 실행

```
ansible-playbook /home/ansible-playbooks/playbook-install-docker.yml# resetting the permissions
```

## Kubernetes 설치

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

# add kubetnetes storage
echo "deb [signed-by=/usr/share/keyrings/kubernetes-archive-keyring.gpg] https://apt.kubernetes.io/ kubernetes-xenial main" | sudo tee /etc/apt/sources.list.d/kubernetes.list

# install kubelet, kubeadm, kubectl
sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl

# register k8s, restart
sudo systemctl daemon-reload
sudo systemctl restart kubelet
```

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

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/9cea00a7-7353-44b3-b52b-28639285f1e5/Untitled.png)

## Kubernetes Master 초기화

### 1. Kubernetes 초기화를 위한 Shell script 파일 생성

```
vi /home/init-scripts/init-k8s-master.sh
```

- 아래 내용 입력 및 저장

```
sudo rm /etc/containerd/config.toml
sudo systemctl restart containerd

# initialize control-plane node
sudo kubeadm init --pod-network-cidr=10.244.0.0/16

# setting for using kube command all of users
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

# install Pod network addon
kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml
```

- `kubeadm join` 으로 시작하는 명령어가 출력되면 저장해둔다. (Worker Node를 Kubernetes Master와 연결할 때 쓰임)

### 2. Kubernetes 초기화를 위한 Ansible-Playbook 파일 생성

```
vi /home/ansible-playbooks/playbook-init-k8s-master.yml
```

- 아래 내용 입력 및 저장

```
- name: Setting K8s master
  hosts: k8s-master
  remote_user: ubuntu
  tasks:
    - name: Copy K8s initial setting shell script
      copy:
        src=/home/init-scripts/init-k8s-master.sh
        dest=/home/ubuntu/scripts/
        mode=0777

    - name: Execute script
      command: sh /home/ubuntu/scripts/init-k8s-master.sh
      async: 3600
      poll: 5
```

### 3. Kubernetes 초기화를 위한 Ansible Playbook 실행

```
ansible-playbook /home/ansible-playbooks/playbook-init-k8s-master.yml
```

## Worker Node 초기화

### 1. Worker Node 초기화를 위한 Shell script 파일 생성

```
vi /home/init-scripts/init-k8s-worker.sh
```

- 아래 내용 입력 및 저장

```
sudo rm /etc/containerd/config.toml
sudo systemctl restart containerd

# initialize control-plane node
sudo kubeadm init

# setting for using kube command all of users
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

# install Pod network addon
kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml
```

### 2. Worker Node 초기화를 위한 Ansible-Playbook 파일 생성

```
vi /home/ansible-playbooks/playbook-init-k8s-worker.yml
```

- 아래 내용 입력 및 저장

```
- name: Setting K8s master
  hosts: k8s
  remote_user: ubuntu
  tasks:
    - name: Copy K8s initial setting shell script
      copy:
        src=/home/init-scripts/init-k8s-master.sh
        dest=/home/ubuntu/scripts/
        mode=0777

    - name: Execute script
      command: sh /home/ubuntu/scripts/init-k8s-master.sh
      async: 3600
      poll: 5
```

### 3. Worker Node 초기화를 위한 Ansible Playbook 실행

```
ansible-playbook /home/ansible-playbooks/playbook-init-k8s-worker.yml
```

## Worker Node를 Kubernetes Master에 연결

### 1. Worker Node 구성

- Kubernetes Master 초기화에서 저장했던 `kubeadm join` 사용

```
kubeadm join <master-ip>:<master-port> --token <token> --discovery-token-ca-cert-hash <hash>
```

### 에러 발생

![Untitled (1).png](README_assets/93a2e1230695f3f7ce3a751d9cd2870e7c793dd2.png)

- Docker 삭제 후 재설치

```
# if you have docker containers
sudo docker stop $(sudo docker ps -a -q)
sudo docker rm $(sudo docker ps -a -q)

# uninstall the Docker package
sudo apt-get purge docker-ce docker-ce-cli containerd.io

# Remove any Docker directories that might still be present
sudo rm -rf /var/lib/docker

# Remove any Docker related configuration files
sudo rm -rf /etc/docker

# Restart the system
sudo reboot

# Update the system
sudo apt-get update && sudo apt-get upgrade -y

# Install Docker
sudo apt-get install -y docker.io

# Enable and start Docker
sudo systemctl enable docker
sudo systemctl start docker

# Add Kubernetes repository
sudo apt-get update && sudo apt-get install -y apt-transport-https curl
curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key add -
cat <<EOF | sudo tee /etc/apt/sources.list.d/kubernetes.list
deb https://apt.kubernetes.io/ kubernetes-xenial main
EOF
sudo apt-get update

# Install Kubernetes components
sudo apt-get install -y kubelet kubeadm kubectl

# Disable swap
sudo swapoff -a
```

- 이후 다시 `kubeadm join...` 실행
