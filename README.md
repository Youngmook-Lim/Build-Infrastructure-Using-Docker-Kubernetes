# 🛠️ Docker & Kubernetes를 활용한 Build Infra 구축 🛠️

## InfraStructure Overview

인프라는 다음 사양의 AWS EC2 인스턴스 5개로 구성됩니다.

- 메인 서버 인스턴스

  - 인스턴스 유형: m5.large
  - 운영 체제: 우분투 20.04
  - 개수: 1
  - 서비스: Jenkins-server 및 Ansible-server를 실행하는 Docker 컨테이너
  - 소프트웨어 요구 사항: Docker, Jenkins(Jenkins 서버) 및 Ansible(Ansible 서버)
  - 저장 공간: 30GB(OS, Docker, Jenkins, Ansible 및 추가 컨테이너용)

- **K8s 마스터 서버 인스턴스**

  - 인스턴스 유형: m5.large
  - 운영 체제: 우분투 20.04
  - 개수: 1
  - 서비스: 작업자 노드를 관리하는 Kubernetes 마스터
  - 소프트웨어 요구 사항: Docker 및 Kubernetes
  - 스토리지: 30GB(OS, Docker, Kubernetes 및 추가 관리 구성 요소용)

- **K8s 작업자 서버 인스턴스**
  - 인스턴스 유형: m5.large
  - 운영 체제: 우분투 20.04
  - 개수: 3
  - 서비스: Jenkins Agent 및 Kubernetes 작업자 노드
  - 소프트웨어 요구 사항: Docker 및 Kubernetes
  - 스토리지: 인스턴스당 50GB(OS, Docker, Jenkins Agent, Kubernetes 및 애플리케이션 컨테이너용)
