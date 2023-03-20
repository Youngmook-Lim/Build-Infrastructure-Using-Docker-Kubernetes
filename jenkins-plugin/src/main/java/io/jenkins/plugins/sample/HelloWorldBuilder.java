package io.jenkins.plugins.sample;

import hudson.Launcher;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Cause;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.StreamTaskListener;
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

public class HelloWorldBuilder extends Builder implements SimpleBuildStep {

    private final String name;
    private final String gitUrl;
    private final String language;
    private final String buildEnv;

    @DataBoundConstructor
    public HelloWorldBuilder(String gitUrl, String name, String language, String buildEnv) {
        this.name = name;
        this.gitUrl = gitUrl;
        this.language = language;
        this.buildEnv = buildEnv;
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

    public String generateScript() {
        String jenkinsPipeline = "pipeline {\n";
        jenkinsPipeline += "  agent any\n";
        jenkinsPipeline += "  parameters {\n";
        jenkinsPipeline += "    string(name: 'gitUrl', defaultValue: '" + gitUrl + "', description: 'Git URL')\n";
        jenkinsPipeline += "    string(name: 'buildEnv', defaultValue: '" + buildEnv + "', description: 'Build environment')\n";
        jenkinsPipeline += "    string(name: 'language', defaultValue: '" + language + "', description: 'Programming language')\n";
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

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
////        String pipelineScript = "pipeline { agent any; stages { stage('Build') { steps { sh 'echo \"Hello, Jenkins!\"' } } } }";
//        Jenkins jenkinsInstance = Jenkins.get();
//        // 파이프라인 작업 생성
//        WorkflowJob job = new WorkflowJob(jenkinsInstance, name);
//
//        // 파이프라인 정의 적용
//        job.setDefinition(new CpsFlowDefinition(generateScript(), true));
//        // 작업 저장 (추가)
//        job.save();
//        // 파이프라인 작업 예약
//        try {
//            job.scheduleBuild2(0).waitForStart();
//        } catch (Exception e) {
//        }

        Jenkins jenkinsInstance = Jenkins.get();

        // Check if the job already exists
        if (jenkinsInstance.getItem(name) != null) {
            listener.getLogger().println("Job with this name already exists: " + name);
            return;
        }

        // Create a new Pipeline Job
        try {
            TopLevelItem item = jenkinsInstance.createProject(WorkflowJob.class, name);
            if (item instanceof WorkflowJob) {
                WorkflowJob job = (WorkflowJob) item;
                job.setDefinition(new CpsFlowDefinition(generateScript(), true));
                job.save();
                job.scheduleBuild2(0).waitForStart();
            } else {
                listener.getLogger().println("Failed to create a new pipeline job.");
            }
        } catch (Exception e) {
            e.printStackTrace(listener.error("Failed to create a new pipeline job."));
        }
    }

    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckName(@QueryParameter String value, @QueryParameter boolean useFrench)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error(Messages.HelloWorldBuilder_DescriptorImpl_errors_missingName());
            if (value.length() < 4)
                return FormValidation.warning(Messages.HelloWorldBuilder_DescriptorImpl_warnings_tooShort());
            if (!useFrench && value.matches(".*[éáàç].*")) {
                return FormValidation.warning(Messages.HelloWorldBuilder_DescriptorImpl_warnings_reallyFrench());
            }
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
