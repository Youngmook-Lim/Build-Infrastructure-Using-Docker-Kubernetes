![header](https://capsule-render.vercel.app/api?type=waving&color=auto&height=300&section=header&text=Deployment and Service Jenkins Agent&fontSize=45)

## Jenkins Agent 배포 및 서비스

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

- 완료 시 build-1~3 서버에서 Jenkins Agent가 배포 및 서비스됩니다.정상적으로 완료되었다면 [다음 단계로 이동](https://lab.ssafy.com/s08-s-project/S08P21S003/-/blob/develop/porting-manual/10_connect_jenkins_master_and_agents.md)해주세요.
