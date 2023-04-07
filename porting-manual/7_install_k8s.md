![header](https://capsule-render.vercel.app/api?type=waving&color=auto&height=300&section=header&text=Install K8s&fontSize=70)

## Ansible Playbook을 이용하여 K8s-master, Build 서버에 K8s 설치

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

- 완료 시 k8s-master, build-1~3 서버에 K8s 및 구성 요소가 설치됩니다. 정상적으로 설치되었다면 [다음 단계로 이동](https://lab.ssafy.com/s08-s-project/S08P21S003/-/blob/develop/porting-manual/8_connect_build_to_master.md)해주세요.
