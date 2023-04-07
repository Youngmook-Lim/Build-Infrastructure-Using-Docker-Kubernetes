# jenkins-cli를 활용한 jenkins-agent 추가

1. jenkins cli를 사용하기위해 다운로드 받는다.
```sh
curl -l <젠킨스URL>/jnlpJars/jenkins-cli.jar --output jenkins-cli.jar
```

2. 젠킨스에 로그인할 관리자 계정의 정보를 jenkins-cli-credential에 저장한다.
```sh
touch jenkins-cli-credential
echo 'JENKINS_URL=<젠킨스주소>' >> jenkins-cli-credential
echo 'JENKINS_USER=<아이디>' >> jenkins-cli-credential
echo 'JENKINS_PASSWORD=<비밀번호>' >> jenkins-cli-credential
```

3. 스크립트 본체를 작성한다.
```add-agent.sh
#!/bin/sh

# credential을 불러온다.
. ./jenkins-cli-credential

# 새로 생성할 노드 정보
NODE_NAME=$3
NODE_DESCRIPTION=$3
NODE_REMOTE_ROOT=/home/jenkins
NODE_LABELS=$4
REMOTE_FS=/home/jenkins

# SSH 에이전트 정보
SSH_ADDRESS=$1
SSH_PORT=$2
CREDENTIAL_ID=$5

# Jenkins CLI를 사용하여 새로운 노드를 생성합니다.
java -jar jenkins-cli.jar -s $JENKINS_URL -auth "$JENKINS_USER:$JENKINS_PASSWORD" create-node $NODE_NAME << EOF
<slave>
  <name>$NODE_NAME</name>
  <description>$NODE_NAME</description>
  <remoteFS>$REMOTE_FS</remoteFS>
  <numExecutors>1</numExecutors>
  <mode>NORMAL</mode>
  <retentionStrategy class="hudson.slaves.RetentionStrategy\$Always"/>
  <launcher class="hudson.plugins.sshslaves.SSHLauncher" plugin="ssh-slaves@2.877.v365f5eb_a_b_eec">
    <host>$SSH_ADDRESS</host>
    <port>$SSH_PORT</port>
    <credentialsId>$CREDENTIAL_ID</credentialsId>
    <launchTimeoutSeconds>60</launchTimeoutSeconds>
    <maxNumRetries>10</maxNumRetries>
    <retryWaitTime>15</retryWaitTime>
    <sshHostKeyVerificationStrategy class="hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy"/>
    <tcpNoDelay>true</tcpNoDelay>
  </launcher>
  <label>$NODE_LABELS</label>
  <nodeProperties />
</slave>
EOF
```

4. 스크립트를 실행한다.
```sh
sh add-agent.sh <agent 주소> <agent ssh 포트> <agent 이름> <agent 레이블> <credential ID>
```


---

C/C++ cmake(CMakeLists.txt가 있을때)

이미지 alpine 기반
```Dockerfile
FROM alpine
RUN apk add --no-cache gcc g++ clang make cmake
```

```sh
docker build -t c-cpp .
```

buildpath에서 실행
```sh
docker run -v ./:/root/workspace c-cpp sh -c "cd /root/workspace;mkdir build && cd build;cmake ..;make;rm CMakeCache.txt Makefile cmake_install.cmake;rm -rf CMakeFiles;"
```

buildpath/build 안에있는 모든파일 artifacts archieve로 배포 하면 끝

C/C++ make(Makefile이 있을때)

buildpath에서 실행
```
docker run -v ./:/root/workspace c-cpp sh -c "cd /root/workspace;make;"
```

buildpath에 있는 
