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
        jenkinsPipeline += "  agent any\n";
        jenkinsPipeline += "    environment {\n";
        jenkinsPipeline += "        GIT_URL = \"" + gitUrl + "\"\n" ;
        jenkinsPipeline += "        BUILD_PATH = '"+buildPath+"'\n";
        jenkinsPipeline += "    }\n";
        jenkinsPipeline += "  parameters {\n";
        jenkinsPipeline += "    string(name: 'BUILD_ENV', defaultValue: '" + buildEnv + "', description: 'Build environment')\n";
        jenkinsPipeline += "    string(name: 'LANGUAGE', defaultValue: '" + language + "', description: 'Programming language')\n";
        jenkinsPipeline += "    string(name: 'GIT_BRANCH', defaultValue: '" + branch + "', description: 'Programming language')\n";
        jenkinsPipeline += "    string(name: 'COMMIT_HASH', defaultValue: '" + commitHash + "', description: 'Programming language')\n";
        jenkinsPipeline += "  }\n";

        try (BufferedReader br = new BufferedReader(new FileReader("src/main/resources/pipeline.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                jenkinsPipeline += (line + "\n");
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
            System.out.println(e);
        }

        System.out.println(jenkinsPipeline);
        return jenkinsPipeline;
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
