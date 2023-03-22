package io.jenkins.plugins.sample;

import hudson.Launcher;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import io.jenkins.cli.shaded.org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import com.cloudbees.hudson.plugins.folder.Folder;

import javax.servlet.ServletException;
import java.io.IOException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import hudson.model.TopLevelItem;
import org.kohsuke.stapler.interceptor.RequirePOST;

public class HelloWorldBuilder extends Builder implements SimpleBuildStep {

    private final String name;
    private final String gitUrl;
    private final String language;
    private final String buildEnv;
    private final String branch;

    @DataBoundConstructor
    public HelloWorldBuilder(String gitUrl, String name, String language, String buildEnv, String branch) {
        this.name = name;
        this.gitUrl = gitUrl;
        this.language = language;
        this.buildEnv = buildEnv;
        this.branch = branch;
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

    public String generateScript() {
        String jenkinsPipeline = "pipeline {\n";
        jenkinsPipeline += "  agent any\n";
        jenkinsPipeline += "  parameters {\n";
        jenkinsPipeline += "    string(name: 'gitUrl', defaultValue: '" + gitUrl + "', description: 'Git URL')\n";
        jenkinsPipeline += "    string(name: 'buildEnv', defaultValue: '" + buildEnv + "', description: 'Build environment')\n";
        jenkinsPipeline += "    string(name: 'language', defaultValue: '" + language + "', description: 'Programming language')\n";
        jenkinsPipeline += "    string(name: 'language', defaultValue: '" + branch + "', description: 'Programming language')\n";
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
        jenkinsPipeline += "  }\n";
        jenkinsPipeline += "}";
        return jenkinsPipeline;
    }

    private Folder getOrCreateUserFolder(String username) throws IOException {
        Jenkins jenkinsInstance = Jenkins.get();
        TopLevelItem folderItem = jenkinsInstance.getItem(username);

        if (folderItem instanceof Folder) {
            return (Folder) folderItem;
        } else if (folderItem == null) {
            // CloudBees Folders Plugin을 사용하여 사용자 이름에 해당하는 폴더를 생성합니다.
            Folder folder = new Folder(jenkinsInstance, username);
            jenkinsInstance.add(folder, username);
            folder.save();
            return folder;
        } else {
            throw new IOException("이름이 " + username + "인 폴더를 생성할 수 없습니다. 해당 이름을 가진 다른 항목이 이미 존재합니다.");
        }
    }

    private String getCurrentUserId() {
        return Jenkins.getAuthentication().getName();
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        Jenkins jenkinsInstance = Jenkins.get();

        if(name.equals("") || gitUrl.equals("") || language.equals("") || buildEnv.equals("") || branch.equals("")) {
            listener.getLogger().println("build history에 실패했습니다. 필수 입력 값이 비어 있습니다.");
            WorkflowJob job = jenkinsInstance.createProject(WorkflowJob.class, name);
            job.makeDisabled(true);
            return;
        }

        // 로그인한 사용자 이름을 가져옵니다.
        String currentUsername = getCurrentUserId();

        // 사용자 이름에 해당하는 폴더를 가져오거나 생성합니다.
        Folder userFolder;
        try {
            userFolder = getOrCreateUserFolder(currentUsername);
        } catch (IOException e) {
            e.printStackTrace(listener.error("사용자 폴더를 가져오거나 생성하는데 실패했습니다."));
            return;
        }

        // Check if the job already exists
        TopLevelItem jobItem = userFolder.getItem(name);
        if (jobItem != null) {
            listener.getLogger().println("Job with this name already exists: " + name);
            WorkflowJob job = jenkinsInstance.createProject(WorkflowJob.class, name);
            job.makeDisabled(true);
            return;
        }

        // Create a new Pipeline Job
        try {
            TopLevelItem item = userFolder.createProject(WorkflowJob.class, name);
            if (item instanceof WorkflowJob) {
                WorkflowJob job = (WorkflowJob) item;
                job.setDefinition(new CpsFlowDefinition(generateScript(), true));
                job.save();
                job.scheduleBuild2(0).waitForStart();
            } else {
                listener.getLogger().println("새 파이프라인 작업을 생성하는데 실패했습니다.");
            }
        } catch (Exception e) {
            e.printStackTrace(listener.error("새 파이프라인 작업을 생성하는데 실패했습니다."));
        }
    }

    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

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
