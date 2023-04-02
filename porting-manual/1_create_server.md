![header](https://capsule-render.vercel.app/api?type=waving&color=auto&height=300&section=header&text=Create Server&fontSize=70)

## InfraStructure Overview

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

## EC2 인스턴스 생성(main, k8s-master, build-1~3)

1. AWS 계정에 로그인하고 EC2 대시보드에서 "인스턴스 시작"을 클릭합니다.

![image](https://user-images.githubusercontent.com/89143804/229323018-9b014ece-99a7-46ca-a719-643953da5d4c.png)

2. 이름은 각 서버의 역할에 알맞게 지정합니다.(daeguops-main 등)
3. AMI는 Ubuntu Server 20.04 LTS (HVM), SSD Volume Type으로 설정합니다.
4. 인스턴스 유형으로 "m5.large"를 선택합니다.
5. 인스턴스 스토리지를 서버에 따라 30GB 또는 50GB로 설정합니다.

![image](https://user-images.githubusercontent.com/89143804/229323250-3cf48890-480f-4d3b-a88d-fbb04f1a138c.png)

![image](https://user-images.githubusercontent.com/89143804/229323282-21ae8069-e1c0-49e5-b9e8-22f385394e09.png)

6. 생성 버튼을 클릭합니다. 좌측 메뉴 바에 "인스턴스 > 인스턴스"를 클릭하여 인스턴스가 정상적으로 생성되는 것을 확인 후 [다음 단계로 이동(업뎃 필요)]()해주세요.
