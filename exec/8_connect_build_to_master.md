![header](https://capsule-render.vercel.app/api?type=waving&color=auto&height=300&section=header&text=Connect Build to Master&fontSize=70)

## Kubernetes 초기화 및 Worker Node 연결을 위한 Ansible-Playbook 파일 생성

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

- 완료 시 k8s-master에 build-1~3 서버들이 worker node로 등록됩니다.정상적으로 등록되었다면 [다음 단계로 이동](https://lab.ssafy.com/s08-s-project/S08P21S003/-/blob/develop/porting-manual/9_deployment_and_service_jenkins_agent.md)해주세요.
