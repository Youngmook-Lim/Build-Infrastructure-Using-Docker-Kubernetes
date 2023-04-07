# S003_대구옵스팀 한상우입니다.

---

## 프로젝트에 임하는 자세
<aside>
💡 요행을 바라지 않는다.
</aside>

<aside>
💡 내가 할 수 있는 일에 최선을 다한다.
</aside>


---
### 구현한 기능

1. **jenkins 빌드 작업별 우선순위 부여**
    1. **사용기술**: Priority Sorter Plugin, Multi-Branch priority sorter Plugin
    2. **구체적인 구현 내용**:
        1. 우선순위 번호가 작을 수록 우선순위가 높다.
        2. 빌드하는 프로젝트 언어별 우선순위 부여
            1. (예:) 자바는 3  /  C++은 1 우선순위 번호 부여
            2. Pipeline Job Name을 기준으로 우선순위를 부여함.
        3. 외부 요인으로 빌드 실패 시 해당 작업을 최우선 순위를 부여하고 재빌드 진행.
            1. 재빌드 요청 시 Build Parameter를 설정하여 최우선 순위 부여.
            

1. **Commit Hash를 기준으로 동일한 빌드 요청 필터링**
    1. **사용기술**: Git Plugin, Jenkins API
    2. **구체적인 동작 흐름**:
        1. 빌드 작업을 진행하기 위해 사용자로부터 Commit Hash를 입력 받는다.
        (Commit Hash가 입력되지 않으면 가장 최근 Commit Hash를 기준으로 빌드한다.)
        2. 입력받은 Commit Hash가 빌드 중인 Jenkins Job 빌드 이력에 있는지 확인한다.
            1. 빌드 이력이 존재하고 그 빌드 결과가 성공일 경우
                
                ⇒ 빌드 중단
                
            2. 빌드 이력 존재 (X) or 빌드 결과가 실패 
                
                ⇒ 이어서 빌드 진행
                
        3. 그 후, git clone하여 빌드 진행


### 동일 빌드를 감지하는 jenkinsfile

```groovy
def targetCommit

pipeline {
    agent {
        label 'agent'
    }

    environment {
        GIT_URL = 'https://github.com/miracle3070/jenkins-build-test'
        AUTHENTICATION_ID = 'jenkins_api_token123'
    }

    parameters {
        // string(name: 'gitUrl', defaultValue: 'test', description: 'Git URL')
        // string(name: 'buildEnv', defaultValue: 'gradle', description: 'Build environment')
        // string(name: 'language', defaultValue: 'java', description: 'Programming language')
        string(name: 'branch', defaultValue: 'master', description: 'Input git branch name')
        string(name: 'commitHash', defaultValue: '', description: 'Input commit hash to build')
    }

    stages { 
        stage('Check Commit Hash') {
            steps {
                script {
                    targetCommit = params.commitHash.trim()
                    if(targetCommit == '') { // commit hash 값이 입력되지 않았을 경우.
                        // 사용자 지정 브랜치에서 가장 최근 commit hash 값을 얻어옴.
                        targetCommit = sh(script: "git ls-remote ${GIT_URL} ${params.branch} | awk '{print \$1}'", returnStdout: true).trim()
                        if(targetCommit == '') {    // 브랜치 이름이 존재하지 않을 경우 빌드 중지
                            echo "Can't find target branch's commit hash"
                            currentBuild.result = 'ABORTED'
                            error("Build was aborted because the ${params.branch} branch doesn't exist.")
                        }
                        echo "targetCommit: ${targetCommit}"
                        echo "Latest commit hash for branch ${params.branch}: ${targetCommit}"
                    } else {
                        echo "User provided commit hash: ${targetCommit}"
                    }

                    def buildStatusUrl = "${env.JENKINS_URL}/job/${env.JOB_NAME}/api/json?tree=allBuilds[result,actions[buildsByBranchName[*[*]]]]"
                    def buildStatusResponse = httpRequest(url: buildStatusUrl, authentication: AUTHENTICATION_ID, acceptType: 'APPLICATION_JSON')
                    def buildStatusJson = readJSON text: buildStatusResponse.content

                    def commitBuilt = false
                    for (build in buildStatusJson.allBuilds) {
                        def branchBuildInfo = build.actions.find { it.buildsByBranchName }
                        if (branchBuildInfo) {
                            def commitInfo = branchBuildInfo.buildsByBranchName.values().find { it.revision.SHA1.startsWith(targetCommit) }
                            if (commitInfo && build.result == 'SUCCESS') {
                                commitBuilt = true
                                break
                            }
                        }
                    }

                    if (commitBuilt) {
                        echo "Target commit ${targetCommit} was built successfully. Stopping the build."
                        currentBuild.result = 'ABORTED'
                        error("Build was aborted because the target commit ${targetCommit} has already been built successfully.")
                    } else {
                        echo "Target commit ${targetCommit} was not built or not built successfully. Continuing the build."
                    }
                }
            }
        }

        stage('Clone Repository') {
            steps {
                script {
                    // Replace 'BRANCH_NAME' with the desired branch name
                    def BRANCH_NAME = params.branch

                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "${BRANCH_NAME}"]],
                        doGenerateSubmoduleConfigurations: false,
                        extensions: [[$class: 'CloneOption', noTags: false, shallow: false, depth: 0, reference: '']],
                        submoduleCfg: [],
                        userRemoteConfigs: [[url: "${GIT_URL}", refspec: "+refs/heads/${BRANCH_NAME}:refs/remotes/origin/${BRANCH_NAME}"]]
                    ])

                    sh "git checkout ${targetCommit}"
                }
            }
        }

        stage('Build') {
            steps {
                // Your build steps go here
                echo "Building the project..."
            }
        }

        stage('Test') {
            steps {
                // Your test steps go here
                echo "Testing the project..."
            }
        }

        stage('Deploy') {
            steps {
                // Your deploy steps go here
                echo "Deploying the project..."
            }
        }
    }
}
```

### 참고 링크

[https://plugins.jenkins.io/PrioritySorter/](https://plugins.jenkins.io/PrioritySorter/)



---


## 일자별 작업 내용
### 2023-03-13 작업 내용
- [x]  [프로젝트] multipass 설치
- [x]  [프로젝트] jenkins Master용 VM 생성
- [x]  [프로젝트] jenkins Master 설치
- [x]  [프로젝트] kubernetes master 서버용 VM 생성

### 2023-03-15 작업 내용
- [x]  [프로젝트] 3개 VM 서버에 ansible-playbook 접근 권한 설정
- [x]  [프로젝트] ansible-playbook을 활용해서 3개의 jenkins agent 서버에 jenkins 설치
    - [x]  [프로젝트] jenkins-master에 agent용 node 등록
- [x]  [프로젝트] jenkins master에서 jenkins agent로 build 테스트

### 2023-03-16 작업 내용
- [x]  jenkins 한 개의 작업을 동시에 여러번 빌드하도록 허용하기
- [x]  jenkins 대기큐 동작 확인

### 2023-03-17 ~ 2023-03-20 작업 내용
- [ ]  PriorityQueue Sorter로 대기큐 정렬 구현

### 2023-03-21 작업 내용
- [x] 커밋 해시값 - 이전에 이미 빌드된 커밋인지 확인
    - [x]  Jenkins에서 이전 커밋 조회 방법 찾기
    - [x]  Jenkins agent에 빌드 요청을 보내기전 master에서 현재 커밋과 비교하는 기능 구현
    - [x]  (이전에 빌드한 커밋으로 판단될 경우) 빌드 중단하도록 만들기

### 2023-03-22 작업 내용
- [x]  이전에 빌드된 커밋인지 확인하는 기능을 script로 만들기
    - [x]  Jenkins Freestyle Job에서 동작하도록 만들기
    - [x]  Jenkins Pipeline Script로 변환 (Jenkinsfile)

### 2023-03-23 ~ 24 작업 내용
- [x]  PriorityQueue Sorter로 대기큐 정렬 구현

### 2023-03-27 ~ 31 작업 내용
- [x]  마이너 버그 수정
- [x]  발표 PPT 제작

### 2023-04-03 ~ 05 작업 내용
- [x]  최종 발표회 준비

### 2023-04-06 작업 내용
- [x]  최종 발표회 프레젠테이션



