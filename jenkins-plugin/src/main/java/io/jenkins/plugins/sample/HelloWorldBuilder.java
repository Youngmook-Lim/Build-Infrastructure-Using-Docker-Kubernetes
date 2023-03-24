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
import java.io.IOException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import hudson.model.TopLevelItem;

import java.util.*;
import io.jenkins.cli.shaded.org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.interceptor.RequirePOST;
import hudson.security.ProjectMatrixAuthorizationStrategy;
import hudson.security.Permission;
import hudson.security.AuthorizationMatrixProperty;
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy;
import com.michelin.cio.hudson.plugins.rolestrategy.Role;
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType;

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

    public String generateScript() {
        String jenkinsPipeline = "pipeline {\n";
        jenkinsPipeline += "  agent any\n";
        jenkinsPipeline += " environment {\n";
        jenkinsPipeline += "        GIT_URL = \"https://github.com/SonarSource/sonar-scanning-examples.git\"\n" ;
        jenkinsPipeline += "        GIT_BRANCH = \"master\"\n" ;
        jenkinsPipeline += "        BUILD_PATH = 'sonarqube-scanner-gradle/gradle-basic'\n" ;
        jenkinsPipeline += "        HOST_BIND_MOUNT = '/home/ubuntu/tmp/workspace'\n" ;
        jenkinsPipeline += "        SONAR_LOGIN = 'admin'\n" ;
        jenkinsPipeline += "        SONAR_PASSWORD = 'admin'\n" ;
        jenkinsPipeline += "        SONAR_PORT = 9000\n" ;
        jenkinsPipeline += "        JENKINS_PORT = 8080\n" ;
        jenkinsPipeline += "        AUTHENTICATION_ID = 'jenkins_api_token123'\n" ;
        jenkinsPipeline += "    }\n";
        jenkinsPipeline += "  parameters {\n";
        jenkinsPipeline += "    string(name: 'gitUrl', defaultValue: '" + gitUrl + "', description: 'Git URL')\n";
        jenkinsPipeline += "    string(name: 'buildEnv', defaultValue: '" + buildEnv + "', description: 'Build environment')\n";
        jenkinsPipeline += "    string(name: 'language', defaultValue: '" + language + "', description: 'Programming language')\n";
        jenkinsPipeline += "    string(name: 'branch', defaultValue: '" + branch + "', description: 'Programming language')\n";
        jenkinsPipeline += "    string(name: 'commitHash', defaultValue: '" + commitHash + "', description: 'Programming language')\n";
        jenkinsPipeline += "    string(name: 'buildPath', defaultValue: '" + buildPath + "', description: 'Programming language')\n";
        jenkinsPipeline += "  }\n";
        jenkinsPipeline += "  stages {\n";
        jenkinsPipeline += "    stage('Print Git URL') {\n";
        jenkinsPipeline += "      steps {\n";
        jenkinsPipeline += "        echo \"Git URL: ${params.gitUrl}\"\n";
        jenkinsPipeline += "      }\n";
        jenkinsPipeline += "    }\n";
        jenkinsPipeline += "    stage('Print Build Environment') {\n";
        jenkinsPipeline += "      steps {\n";
        jenkinsPipeline += "        echo \"Build Environment: ${params.buildEnv}\"\n";
        jenkinsPipeline += "      }\n";
        jenkinsPipeline += "    }\n";
        jenkinsPipeline += "    stage('Print Programming Language') {\n";
        jenkinsPipeline += "      steps {\n";
        jenkinsPipeline += "        echo \"Programming Language: ${params.language}\"\n";
        jenkinsPipeline += "      }\n";
        jenkinsPipeline += "    }\n";
        jenkinsPipeline += "    stage('Print branch') {\n";
        jenkinsPipeline += "      steps {\n";
        jenkinsPipeline += "        echo \"branch: ${params.branch}\"\n";
        jenkinsPipeline += "      }\n";
        jenkinsPipeline += "    }\n";
        jenkinsPipeline += "    stage('Print commit hash') {\n";
        jenkinsPipeline += "      steps {\n";
        jenkinsPipeline += "        echo \"commit hash: ${params.commitHash}\"\n";
        jenkinsPipeline += "      }\n";
        jenkinsPipeline += "    }\n";
        jenkinsPipeline += "    stage('Print Build Path') {\n";
        jenkinsPipeline += "      steps {\n";
        jenkinsPipeline += "        echo \"commit hash: ${params.buildPath}\"\n";
        jenkinsPipeline += "      }\n";
        jenkinsPipeline += "    }\n";
        jenkinsPipeline += "  }\n";
        jenkinsPipeline += "}";

        return jenkinsPipeline;
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

        // commitHash가 없는 경우에는 latest 처리 => 이거 해결해야할지도??

        String jobName = run.getParent().getDisplayName();

        // Gets the logged in username.
        String currentUsername=jobName;

        // check job name duplication
        TopLevelItem jobItem = jenkinsInstance.getItem(name);
        if (jobItem != null) {
            listener.getLogger().println("Job with this name already exists: " + name);
            WorkflowJob job = jenkinsInstance.createProject(WorkflowJob.class, name);
            job.makeDisabled(true);
            return;
        }

        // Create a new Pipeline Job
        try {
            TopLevelItem item = jenkinsInstance.createProject(WorkflowJob.class, currentUsername+"-"+name);
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
