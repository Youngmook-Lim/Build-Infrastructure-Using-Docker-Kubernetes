package io.jenkins.plugins.sample;

import hudson.Launcher;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.*;
import hudson.util.FormValidation;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import hudson.model.TopLevelItem;
import com.cloudbees.hudson.plugins.folder.Folder;

public class HelloWorldBuilder extends Builder implements SimpleBuildStep {

    private final String name;
    private final String gitUrl;
    private final String language;
    private final String buildEnv;
    private final String branch;
    private final String commitHash;
    private final String buildPath;


    @DataBoundConstructor
    public HelloWorldBuilder(String gitUrl, String name, String language, String buildEnv, String branch, String commitHash, String buildPath) {
        this.name = name;
        this.gitUrl = gitUrl;
        this.language = language;
        this.buildEnv = buildEnv;
        this.branch = branch;
        this.commitHash = commitHash;
        this.buildPath = buildPath;
    }

    public String getName() {
        return name;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public String getLanguage() {
        return language;
    }

    public String getBuildEnv() {
        return buildEnv;
    }

    public String getBranch() {
        return branch;
    }
    public String getCommitHash() {
        return commitHash;
    }
    public String getBuildPath(){
        return buildPath;
    }

    public String generateScript(){
        String jenkinsPipeline = "";
        jenkinsPipeline = "pipeline {\n";
        jenkinsPipeline += "  agent {\n";
        jenkinsPipeline += "       label 'agent'\n";
        jenkinsPipeline += "  }\n";
        if(buildEnv.equals("maven")){
            jenkinsPipeline += "  tools {\n";
            jenkinsPipeline += "       maven 'maven'\n";
            jenkinsPipeline += "  }\n";
        }
        jenkinsPipeline += "    environment {\n";
        jenkinsPipeline += "        GIT_URL = \"" + gitUrl + "\"\n";
        jenkinsPipeline += "        BUILD_PATH = '"+buildPath+"'\n";
        jenkinsPipeline += "        BUILD_RESULT_PATH = '"+ getBuildResPath(buildEnv) +"'\n";
        jenkinsPipeline += "    }\n";
        jenkinsPipeline += "  parameters {\n";
        jenkinsPipeline += "    string(name: 'BUILD_ENV', defaultValue: '" + buildEnv + "', description: 'Build environment')\n";
        jenkinsPipeline += "    string(name: 'LANGUAGE', defaultValue: '" + language + "', description: 'Programming language')\n";
        jenkinsPipeline += "    string(name: 'GIT_BRANCH', defaultValue: '" + branch + "', description: 'Programming language')\n";
        jenkinsPipeline += "    string(name: 'COMMIT_HASH', defaultValue: '" + commitHash + "', description: 'Programming language')\n";
        jenkinsPipeline += "  }\n";
        jenkinsPipeline += getRestScript();

        System.out.println(jenkinsPipeline);
        return jenkinsPipeline;
    }

    private String getBuildResPath(String buildEnv) {
        switch (buildEnv){
            case "maven":
                return "build/libs/*.jar";
            case "gradle":
                return "target/*.jar";
        }
        return "nothing";
    }

    private Folder getUserFolder(String username) throws IOException {
        Jenkins jenkinsInstance = Jenkins.get();
        TopLevelItem folderItem = jenkinsInstance.getItem(username+"-folder");
        return (Folder) folderItem;
    }


    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {

        Jenkins jenkinsInstance = Jenkins.get();
        if(name.equals("") || gitUrl.equals("") || language.equals("") || buildEnv.equals("") || branch.equals("") || buildPath.equals("")) {
            listener.getLogger().println("The build failed. A required input value is empty.");
            WorkflowJob job = jenkinsInstance.createProject(WorkflowJob.class, name);
            job.makeDisabled(true);
            return;
        }

        String jobName = run.getParent().getDisplayName();

        // Gets the logged in username.
        String currentUsername=jobName.split("-")[0];
        Folder userFolder;
        try {
            userFolder = getUserFolder(currentUsername);
        } catch (IOException e) {
            e.printStackTrace(listener.error("사용자 폴더를 가져오거나 생성하는데 실패했습니다."));
            return;
        }

        // check job name duplication
        TopLevelItem jobItem = userFolder.getItem(currentUsername+"-"+name);
        if (jobItem != null) {
            listener.getLogger().println("Job with this name already exists: " + name);
//            WorkflowJob job = jenkinsInstance.createProject(WorkflowJob.class, name);
//            job.makeDisabled(true);
            // ???
            return;
        }

        // Create a new Pipeline Job
        try {
            TopLevelItem item = userFolder.createProject(WorkflowJob.class, currentUsername+"-"+language+"-"+name);
            if (item instanceof WorkflowJob) {
                WorkflowJob job = (WorkflowJob) item;
                job.setDefinition(new CpsFlowDefinition(generateScript(), true));
                job.save();
                job.scheduleBuild2(0).waitForStart();
            } else {
                listener.getLogger().println("Creating a new pipeline job failed.");
            }
        } catch (Exception e) {
            e.printStackTrace(listener.error("Creating a new pipeline job failed."));
        }
    }

    public String getRestScript(){
        return  "  stages {\n" +
                "        stage('Set Environment Variables') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    withCredentials([\n" +
                "                        string(credentialsId: 'sonar_login', variable: 'SONAR_LOGIN'),\n" +
                "                        string(credentialsId: 'sonar_password', variable: 'SONAR_PASSWORD'),\n" +
                "                        string(credentialsId: 'sonar_port', variable: 'SONAR_PORT'),\n" +
                "                        string(credentialsId: 'host_bind_mount', variable: 'HOST_BIND_MOUNT'),\n" +
                "                    ]) {\n" +
                "                        env.SONAR_LOGIN = \"${SONAR_LOGIN}\"\n" +
                "                        env.SONAR_PASSWORD = \"${SONAR_PASSWORD}\"\n" +
                "                        env.SONAR_PORT = \"${SONAR_PORT}\"\n" +
                "                        env.HOST_BIND_MOUNT = \"${HOST_BIND_MOUNT}\"\n" +
                "\n" +
                "                        // parameters -> env\n" +
                "                        env.BUILD_ENV = \"${params.BUILD_ENV}\"\n" +
                "                        env.LANGUAGE = \"${params.LANGUAGE}\"\n" +
                "                        env.GIT_BRANCH = \"${params.GIT_BRANCH}\"\n" +
                "                        env.COMMIT_HASH = \"${params.COMMIT_HASH}\"\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('Check Commit Hash') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    env.TARGET_COMMIT = \"${COMMIT_HASH}\".trim()\n" +
                "                    if(\"${TARGET_COMMIT}\" == '') {\n" +
                "                        env.TARGET_COMMIT = sh(script: \"git ls-remote ${GIT_URL} ${params.GIT_BRANCH} | awk '{print \\$1}'\", returnStdout: true).trim()\n" +
                "                        if(\"${TARGET_COMMIT}\" == '') {\n" +
                "                            echo \"Can't find target branch's commit hash\"\n" +
                "                            currentBuild.result = 'ABORTED'\n" +
                "                            error(\"Build was aborted because the ${GIT_BRANCH} branch doesn't exist.\")\n" +
                "                        }\n" +
                "                        echo \"TARGET_COMMIT: ${TARGET_COMMIT}\"\n" +
                "                        echo \"Latest commit hash for branch ${GIT_BRANCH}: ${TARGET_COMMIT}\"\n" +
                "                    } else {\n" +
                "                        echo \"User provided commit hash: ${TARGET_COMMIT}\"\n" +
                "                    }\n" +
                "\n" +
                "                    def buildStatusUrl = \"${JENKINS_URL}/job/${JOB_NAME}/api/json?tree=allBuilds[result,actions[buildsByBranchName[*[*]]]]\"\n" +
                "                    def buildStatusResponse = httpRequest(url: buildStatusUrl, authentication: \"authentication_id\", acceptType: 'APPLICATION_JSON')\n" +
                "                    def buildStatusJson = readJSON text: buildStatusResponse.content\n" +
                "\n" +
                "                    def commitBuilt = false\n" +
                "                    for (build in buildStatusJson.allBuilds) {\n" +
                "                        def branchBuildInfo = build.actions.find { it.buildsByBranchName }\n" +
                "                        if (branchBuildInfo) {\n" +
                "                            def commitInfo = branchBuildInfo.buildsByBranchName.values().find { it.revision.SHA1.startsWith(\"${TARGET_COMMIT}\") }\n" +
                "                            if (commitInfo && build.result == 'SUCCESS') {\n" +
                "                                commitBuilt = true\n" +
                "                                break\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "\n" +
                "                    if (commitBuilt) {\n" +
                "                        echo \"Target commit ${TARGET_COMMIT} was built successfully. Stopping the build.\"\n" +
                "                        currentBuild.result = 'ABORTED'\n" +
                "                        error(\"Build was aborted because the target commit ${TARGET_COMMIT} has already been built successfully.\")\n" +
                "                    } else {\n" +
                "                        echo \"Target commit ${TARGET_COMMIT} was not built or not built successfully. Continuing the build.\"\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "\n" +
                "        stage('Clone Repository') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    dir(\"repo\") {\n" +
                "                        checkout([\n" +
                "                            $class: 'GitSCM',\n" +
                "                            branches: [[name: \"${GIT_BRANCH}\"]],\n" +
                "                            doGenerateSubmoduleConfigurations: false,\n" +
                "                            extensions: [[$class: 'CloneOption', noTags: false, shallow: false, depth: 0, reference: '']],\n" +
                "                            submoduleCfg: [],\n" +
                "                            userRemoteConfigs: [[url: \"${GIT_URL}\", refspec: \"+refs/heads/${GIT_BRANCH}:refs/remotes/origin/${GIT_BRANCH}\"]]\n" +
                "                        ])\n" +
                "\n" +
                "                        sh \"git checkout ${TARGET_COMMIT}\"\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('Transfer Script to Agent') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    // Switch to the master agent\n" +
                "                    node('master') {\n" +
                "                        dir(\"../scripts/${LANGUAGE}/${BUILD_ENV}\") {\n" +
                "                            stash name: \"${LANGUAGE}-${BUILD_ENV}\", includes: \"*.sh\"\n" +
                "                        }\n" +
                "                    }\n" +
                "\n" +
                "                    // Switch back to the agent node and unstash the script file\n" +
                "                    dir('scripts') {\n" +
                "                        unstash \"${LANGUAGE}-${BUILD_ENV}\"\n" +
                "                        sh \"pwd\"\n" +
                "                        sh \"ls\"\n" +
                "                        sh \"chmod +x *.sh\"\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('Sonarqube dependency injection') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    if (\"${BUILD_ENV}\" == 'gradle') {\n" +
                "                        sh \"scripts/injection.sh ${WORKSPACE}/repo/${BUILD_PATH}/build.gradle\"\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('Launch Sonarqube container') {\n" +
                "            steps {\n" +
                "                sh 'docker run --rm -d --name sonarqube -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true -p ${SONAR_PORT}:9000 sonarqube:latest'\n" +
                "                script {\n" +
                "                    env.SONAR_HOST = \"http://${sh(script:'docker exec sonarqube hostname -I', returnStdout: true).trim()}:${SONAR_PORT}\"\n" +
                "                    waitForSonarQube(\"${SONAR_HOST}\", 300)\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('Get Sonarqube token') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    def tokenOutput = sh(script: '''\n" +
                "                        USER_LOGIN=${SONAR_LOGIN}\n" +
                "                        TOKEN_NAME=\"My Jenkins Token\"\n" +
                "                        MAX_RETRIES=16\n" +
                "                        RETRY_INTERVAL=5\n" +
                "\n" +
                "                        for i in $(seq 1 $MAX_RETRIES); do\n" +
                "                            echo \"Attempt #$i to get token...\"\n" +
                "                            TOKEN=$(curl -s -u \"${SONAR_LOGIN}:${SONAR_PASSWORD}\" -X POST \"${SONAR_HOST}/api/user_tokens/generate\" \\\n" +
                "                                -d \"name=${TOKEN_NAME}\" \\\n" +
                "                                -d \"login=${USER_LOGIN}\" \\\n" +
                "                                | sed -n 's/.*\\\"token\\\":\\\"\\\\([^\\\"]*\\\\)\\\".*/\\\\1/p')\n" +
                "                            if [ -n \"$TOKEN\" ]; then\n" +
                "                                echo \"Token: ${TOKEN}\"\n" +
                "                                break\n" +
                "                            else\n" +
                "                                echo \"Token not received, waiting for $RETRY_INTERVAL seconds before retrying...\"\n" +
                "                                sleep $RETRY_INTERVAL\n" +
                "                            fi\n" +
                "                        done\n" +
                "\n" +
                "                        if [ -z \"$TOKEN\" ]; then\n" +
                "                            echo \"Failed to get token after $MAX_RETRIES attempts.\"\n" +
                "                            exit 1\n" +
                "                        fi\n" +
                "\n" +
                "                        echo \"Token: ${TOKEN}\"\n" +
                "                    ''', returnStdout: true).trim()\n" +
                "\n" +
                "                    env.SONAR_TOKEN = tokenOutput.substring(tokenOutput.lastIndexOf(\"Token: \") + 7)\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('Sonarqube Scan') {\n" +
                "            steps {\n" +
                "                dir(\"${WORKSPACE}\") {\n" +
                "                    script {\n" +
                "                        def repositoryName = sh(script: \"basename -s .git ${GIT_URL}\", returnStdout: true).trim()\n" +
                "\n" +
                "                        sh \"./scripts/sonar.sh ${SONAR_HOST} ${SONAR_LOGIN} ${SONAR_PASSWORD} ${TARGET_COMMIT} ${repositoryName} ${WORKSPACE}/repo/${BUILD_PATH} ${HOST_BIND_MOUNT}/${JOB_NAME}/repo/${BUILD_PATH}\"\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('Sonarqube Quality Gate') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    // Wait for analysis report to be processed by SonarQubeff\n" +
                "                    def waitTime = 0\n" +
                "                    def maxWaitTime = 60\n" +
                "                    def analysisStatus = 'NONE'\n" +
                "                    while (analysisStatus == 'NONE' && waitTime < maxWaitTime) {\n" +
                "                        def jsonAnalysis = sh(script: \"curl -s ${SONAR_HOST}/api/qualitygates/project_status?projectKey=${TARGET_COMMIT} -u ${SONAR_LOGIN}:${SONAR_PASSWORD}\", returnStdout: true).trim()\n" +
                "                        analysisStatus = new groovy.json.JsonSlurper().parseText(jsonAnalysis).projectStatus.status\n" +
                "                        if (analysisStatus == 'NONE') {\n" +
                "                            echo 'Analysis report is still being processed by SonarQube, waiting for 10 seconds...'\n" +
                "                            sleep(10)\n" +
                "                            waitTime += 10\n" +
                "                        }\n" +
                "                    }\n" +
                "\n" +
                "                    if (waitTime >= maxWaitTime) {\n" +
                "                        error('Timeout waiting for analysis report to be processed by SonarQube')\n" +
                "                    }\n" +
                "\n" +
                "                    // Check quality gate status\n" +
                "                    if (analysisStatus == 'WARN') {\n" +
                "                        echo \"Quality gate status: WARN. Aborting...\"\n" +
                "                        exit pipeline\n" +
                "                    } else if (analysisStatus == 'ERROR') {\n" +
                "                        echo \"Quality gate status: ERROR. Aborting...\"\n" +
                "                        exit pipeline\n" +
                "                    }\n" +
                "                    echo \"********** Quality gate status: ${analysisStatus} **********\"\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('Save Sonarqube Report') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    def metrics = 'ncloc,coverage,violations,complexity,bugs,vulnerabilities,code_smells,sqale_index,alert_status,reliability_rating,security_rating'\n" +
                "\n" +
                "                    def apiUrl = \"${SONAR_HOST}/api/measures/component?component=${TARGET_COMMIT}&metricKeys=${metrics}\"\n" +
                "\n" +
                "                    def jsonData = sh(script: \"curl -s -u ${SONAR_LOGIN}:${SONAR_PASSWORD} '${apiUrl}'\", returnStdout: true).trim()\n" +
                "\n" +
                "                    def qualityGateApiUrl = \"${SONAR_HOST}/api/qualitygates/project_status?projectKey=${TARGET_COMMIT}\"\n" +
                "\n" +
                "                    def qualityGateJsonData = sh(script: \"curl -s -u ${SONAR_LOGIN}:${SONAR_PASSWORD} '${qualityGateApiUrl}'\", returnStdout: true).trim()\n" +
                "\n" +
                "                    def reportData = readJSON text: jsonData\n" +
                "\n" +
                "                    def qualityGateData = readJSON text: qualityGateJsonData\n" +
                "\n" +
                "                    def qualityGateStatus = qualityGateData['projectStatus']['status']\n" +
                "\n" +
                "                    def tableRows = reportData['component']['measures'].collect { measure ->\n" +
                "                        \"\"\"\n" +
                "                            <tr>\n" +
                "                                <td>${measure['metric']}</td>\n" +
                "                                <td>${measure['value']}</td>\n" +
                "                            </tr>\n" +
                "                        \"\"\"\n" +
                "                    }.join(\"\")\n" +
                "\n" +
                "                    def html = \"\"\"\n" +
                "                        <html>\n" +
                "                            <head>\n" +
                "                                <title>SonarQube Report</title>\n" +
                "                                <style>\n" +
                "                                    body {\n" +
                "                                        font-family: Arial, sans-serif;\n" +
                "                                    }\n" +
                "                                    h1 {\n" +
                "                                        font-size: 24px;\n" +
                "                                    }\n" +
                "                                    h2 {\n" +
                "                                        font-size: 20px;\n" +
                "                                        margin-bottom: 10px;\n" +
                "                                    }\n" +
                "                                    table {\n" +
                "                                        border-collapse: collapse;\n" +
                "                                        width: 100%;\n" +
                "                                    }\n" +
                "                                    th, td {\n" +
                "                                        border: 1px solid #ddd;\n" +
                "                                        padding: 8px;\n" +
                "                                        text-align: left;\n" +
                "                                    }\n" +
                "                                    th {\n" +
                "                                        background-color: #f2f2f2;\n" +
                "                                        font-weight: bold;\n" +
                "                                    }\n" +
                "                                </style>\n" +
                "                            </head>\n" +
                "                            <body>\n" +
                "                                <h1>SonarQube Report</h1>\n" +
                "                                <h2>SonarQube Quality Gate Status: ${qualityGateStatus}</h2>\n" +
                "                                <h2>Report Data</h2>\n" +
                "                                <table>\n" +
                "                                    <thead>\n" +
                "                                        <tr>\n" +
                "                                            <th>Metric</th>\n" +
                "                                            <th>Value</th>\n" +
                "                                        </tr>\n" +
                "                                    </thead>\n" +
                "                                    <tbody>\n" +
                "                                        ${tableRows}\n" +
                "                                    </tbody>\n" +
                "                                </table>\n" +
                "                            </body>\n" +
                "                        </html>\n" +
                "                    \"\"\"\n" +
                "                    writeFile file: 'sonarqube-report.html', text: html\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('Save Report') {\n" +
                "            steps {\n" +
                "                archiveArtifacts artifacts: 'sonarqube-report.html', fingerprint: true\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('Build') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    dir(\"${WORKSPACE}\") {\n" +
                "                        sh \"scripts/build.sh ${HOST_BIND_MOUNT} ${JOB_NAME} ${BUILD_PATH}\"\n" +
                "                    }\n" +
                "\n" +
                "                }\n" +
                "            }\n" +
                "            post {\n" +
                "                failure {\n" +
                "                    script {\n" +
                "                        echo \"BUILD FAILED!\"\n" +
                "                        if (params.RETRY) {\n" +
                "                            exit pipeline\n" +
                "                        }\n" +
                "                        dir(\"${WORKSPACE}\") {\n" +
                "                            if (sh(returnStatus: true, script: \"scripts/fail_analysis.sh\") == 1 || !fileExists('NORMAL_FAIL')) {\n" +
                "                                build(job: env.JOB_NAME, parameters: [\n" +
                "                                string(name: 'BUILD_ENV', value: env.BUILD_ENV),\n" +
                "                                string(name: 'LANGUAGE', value: env.LANGUAGE),\n" +
                "                                string(name: 'GIT_BRANCH', value: env.GIT_BRANCH),\n" +
                "                                string(name: 'COMMIT_HASH', value: env.COMMIT_HASH),\n" +
                "                                string(name: 'RETRY', value: 'TRUE'),\n" +
                "                                string(name: 'BuildPriority', value: '1')\n" +
                "                                ], wait: false)\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('deploy artifacts') {\n" +
                "            steps {\n" +
                "                archiveArtifacts artifacts: \"repo/${BUILD_PATH}/${BUILD_RESULT_PATH}\", followSymlinks: false\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    post {\n" +
                "        always {\n" +
                "          script {\n" +
                "            sh 'docker stop sonarqube'\n" +
                "            cleanWs deleteDirs: true\n" +
                "          }\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "def waitForSonarQube(sonarQubeUrl, timeout) {\n" +
                "    def running = false\n" +
                "\n" +
                "    for (int i = 0; i < timeout && !running; i += 10) {\n" +
                "        echo \"Attempting to connect to SonarQube at ${sonarQubeUrl}\"\n" +
                "        try {\n" +
                "            sh(script: \"curl --max-time 10 --retry 0 --retry-max-time 10 --retry-connrefused --fail --silent ${sonarQubeUrl}/api/system/status\", returnStdout: true)\n" +
                "            running = true\n" +
                "        } catch (Exception e) {\n" +
                "            sleep(10)\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    if (!running) {\n" +
                "        error(\"SonarQube did not start within the expected time.\")\n" +
                "    }\n" +
                "}\n";
    }

    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public FormValidation doCheckBuildPath(@QueryParameter String buildPath) throws IOException, ServletException {
            if (buildPath.length() == 0) {
                return FormValidation.error(Messages.HelloWorldBuilder_DescriptorImpl_errors_missingGitURL());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckGitUrl(@QueryParameter String gitUrl) throws IOException, ServletException {
            if (gitUrl.length() == 0) {
                return FormValidation.error(Messages.HelloWorldBuilder_DescriptorImpl_errors_missingGitURL());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckName(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error(Messages.HelloWorldBuilder_DescriptorImpl_errors_missingName());
            if (value.length() < 4)
                return FormValidation.warning(Messages.HelloWorldBuilder_DescriptorImpl_warnings_tooShort());
            return FormValidation.ok();
        }

        public FormValidation doCheckBranch(@QueryParameter String value) throws IOException, ServletException {
            if(value.length() == 0) return FormValidation.error(Messages.HelloWorldBuilder_DescriptorImpl_errors_missingBranch());

            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.HelloWorldBuilder_DescriptorImpl_DisplayName();
        }

    }
}
