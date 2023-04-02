![header](https://capsule-render.vercel.app/api?type=waving&color=auto&height=300&section=header&text=Connect Jenkins Master and Agents&fontSize=45)

## jenkins-cli를 활용한 jenkins-agent 추가

### 1. jenkins cli를 사용하기위해 다운로드합니다.

```sh
curl -l <젠킨스URL>/jnlpJars/jenkins-cli.jar --output jenkins-cli.jar
```

### 2. 젠킨스에 로그인할 관리자 계정의 정보를 jenkins-cli-credential에 저장합니다.

```sh
touch jenkins-cli-credential
echo 'JENKINS_URL=<젠킨스주소>' >> jenkins-cli-credential
echo 'JENKINS_USER=<아이디>' >> jenkins-cli-credential
echo 'JENKINS_PASSWORD=<비밀번호>' >> jenkins-cli-credential
```

### 3. 스크립트 본체를 작성합니다.

```add-agent.sh
#!/bin/sh

# load credential
. ./jenkins-cli-credential

# Define variables
NODE_NAME=$3
NODE_DESCRIPTION=$3
NODE_REMOTE_ROOT=/home/jenkins
NODE_LABELS=$4
REMOTE_FS=/home/jenkins

# Define SSH agent variables
SSH_ADDRESS=$1
SSH_PORT=$2
CREDENTIAL_ID=$5

# create node using Jenkins CLI
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

### 4. 스크립트를 실행합니다.

```sh
sh add-agent.sh <agent 주소> <agent ssh 포트> <agent 이름> <agent 레이블> <credential ID>
```

- 완료 시 지정된 구성의 새 Jenkins Agent 노드가 생성되고 SSH를 통해 Jenkins 서버에 연결됩니다.정상적으로 완료되었다면 [다음 단계로 이동(업뎃 필요)]()해주세요.
