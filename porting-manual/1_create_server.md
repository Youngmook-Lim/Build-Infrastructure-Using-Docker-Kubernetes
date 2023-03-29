DaeGuOps 프로젝트를 진행하기 위해서는 다음과 같은 준비물들이 필요합니다.

- AWS Account
- AWS, Docker, Ansible, Kubernetes 및 Jenkins에 대한 기본 지식

< 기술스택을 적고 해당 스택으로 인한 이유로 이 정도 스펙의 서버가 필요하다는 걸 논리적으로 설명해주면 좋을 것 같다는 GOAT성태 님의 의견 👍👍>

- 서버(권장 사양)

  - 인스턴스 유형 : m5.large
  - 운영 체제 : Ubuntu Server 20.04 LTS
  - 스토리지 : 루트 볼륨용 EBS 스토리지 최소 50GB

- 명칭

  - main server
    - jenkins-server
    - ansible-server
  - k8s-master server
  - k8s-workers server
    - worker-1 server
    - worker-2 server
    - worker-3 server

- AWS에 EC2 생성하는 것도 넣어놓아야 할 듯

<아직 정리 안한 상태>

## EC2 인스턴스 생성(main, k8s master, k8s workers)

< 추후 사진 업로드 >

1. AWS 계정에 로그인하고 EC2 대시보드로 이동합니다.
2. "Launch Instance"를 클릭하고 "Ubuntu 20.04 LTS" AMI를 선택합니다.
3. k8s 마스터 및 k8s 작업자 서버의 인스턴스 유형으로 "m5.large"를 선택합니다.
4. 인스턴스 스토리지를 50GB로 설정합니다.
5. 필요한 포트(22, 80, 443, 8080 등)를 허용하도록 보안 그룹을 구성합니다.
6. 인스턴스를 시작하고 해당 IP 주소 또는 도메인 이름을 할당합니다.
