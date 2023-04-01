![header](https://capsule-render.vercel.app/api?type=waving&color=auto&height=300&section=header&text=DaeGuOps&fontSize=90)

# 🛠️ Docker & Kubernetes를 활용한 Build Infra 구축 🛠️

##### 🏆 삼성 청년 SW 아카데미 2학기 삼성전자 VD사업부 연계 프로젝트

### 📜 Contents

1.  [Overview](#-overview)
2.  [서비스 화면](#-서비스-화면)
3.  [사용 설명서](#-사용-설명서)
4.  [개발 환경](#-개발-환경)
5.  [기술 특이점](#-기술-특이점)
6.  [기획 및 설계](#-기획-및-설계)
7.  [Conventions](#-conventions)
8.  [팀 소개](#-팀-소개)

## 👀 Overview

- 다양한 언어(Java, C, C#)에 대응하여 엄격한 소스코드 정적 검사를 통해 신뢰성 있는 빌드 환경과 서버 상태에 대한 관리 기능 및 시각화된 자료를 제공해주는 서비스

## 💻 서비스 화면

### Plugin을 통하여 Job 생성

- 팀 DaeGuOps에서 직접 제작한 Plugin을 이용하여 Job을 생성할 수 있습니다.

### 생성된 Job에 대하여 빌드 실행

- 내용

### 빌드 결과물

- 내용

## 📋사용 설명서

작성 중

## 👨‍💻 개발 환경

- Management Tool

  - 형상 관리 : Gitlab
  - 이슈 관리 : Jira
  - 커뮤니케이션 : Mattermost, Webex, Notion

- Server

  - Ubuntu `20.0.4`
  - Docker `20.10.21`
  - Jenkins `2.387.1`
  - Ansible `core 2.12.10`
  - Kubernetes `v1.26.3`

- Monitoring System

  - Grafana
  - Prometheus

- IDE
  - IntelliJ

## 💡 기술 특이점

- 다양한 언어(Java, C, C#)에 대한 일관된 빌드 환경 제공
- SonarQube를 이용한 소스코드 정적 검사로 코드의 Quality 검증 자동화
- 동시다발적인 빌드 요청에 대하여 priority를 통한 효율적인 분산 처리 수행
- Kubernetes를 통하여 클러스터 내 워커노드 관리 및 복구
- Ansible playbook과 Jenkins CLI를 이용하여 빌드 Pipeline 뿐만 아니라 신규 빌드 서버에 대한 초기 설정까지 자동화
- Grafana & Prometheus를 이용하여 빌드 서버 모니터링 및 시각화

## 🛠️ 기획 및 설계

### ✒️ 요구사항 정의 및 기능 명세

![image](https://user-images.githubusercontent.com/89143804/229289934-10fa6994-7100-4479-8fca-59b6f1cd235b.png)

### 🎨 아키텍처 구성도

![image](https://user-images.githubusercontent.com/89143804/229290404-ded8a4aa-e05e-43b3-af08-64cfb16356e9.png)

## 🦹‍ 팀 소개

### 👨‍👩‍👦‍👦 DaeguOps (Daejeon + Gumi + DevOps)

|                       임영묵                       |                       김성태                       |                          김성한                           |                          양희제                           |                       장재욱                       |                       한상우                       |
| :------------------------------------------------: | :------------------------------------------------: | :-------------------------------------------------------: | :-------------------------------------------------------: | :------------------------------------------------: | :------------------------------------------------: |
|                      [tmp]()                       |                      [tmp]()                       |            [s-ggul](https://github.com/s-ggul)            |         [heejeyang](https://github.com/HeeJeYang)         |                      [tmp]()                       |                      [tmp]()                       |
| ![](https://avatars.githubusercontent.com/u/0?v=4) | ![](https://avatars.githubusercontent.com/u/0?v=4) | ![](https://avatars.githubusercontent.com/u/80890062?v=4) | ![](https://avatars.githubusercontent.com/u/89143804?v=4) | ![](https://avatars.githubusercontent.com/u/0?v=4) | ![](https://avatars.githubusercontent.com/u/0?v=4) |
|                        tmp                         |                        tmp                         |                            tmp                            |                     즐겁게, 후회없이                      |                        tmp                         |                        tmp                         |

## 📐 팀원 역할

- 임영묵 (팀장)

  - SonarQube 정적 검사
  - 전체 Pipeline 통합

- 김성태

  - Pipeline의 Build Stage 작성
  - Build용 컨테이너 제작

- 김성한

  - Jenkins Plugin 개발
  - Build Job 생성 로직

- 양희제

  - Kubernetes 활용한 인프라 구성
  - Ansible script를 통한 초기 세팅

- 장재욱

  - Jenkins Plugin 개발
  - Permission 부여 로직

- 한상우 (발표자)
  - 동일 Build 요청 감지 로직
  - Job별 우선순위 부여 로직

![header](https://capsule-render.vercel.app/api?type=waving&color=auto&height=300&section=footer&text=Thank you&fontSize=90)
