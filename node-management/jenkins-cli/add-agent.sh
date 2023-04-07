#!/bin/sh

# credential을 불러온다.
. ./jenkins-cli-credential

# 새로 생성할 노드 정보
NODE_NAME=$3
NODE_DESCRIPTION=$3
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
