package io.jenkins.plugins.sample;

import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.TopLevelItem;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

@Extension
public class PipelineGeneratorAction implements RootAction {
    @Override
    public String getIconFileName() {
        return "plugin.png";
    }

    @Override
    public String getDisplayName() {
        return "Pipeline Generator";
    }

    @Override
    public String getUrlName() {
        TopLevelItem jobItem = Jenkins.get().getItem(getCurrentUserId());
        return "job/"+getCurrentUserId()+"-folder/job/"+getCurrentUserId()+"-PipelineGenerator/configure";
    }

    private String getCurrentUserId() {
        return Jenkins.getAuthentication().getName();
    }
}
