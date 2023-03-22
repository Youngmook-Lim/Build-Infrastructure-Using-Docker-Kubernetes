package io.jenkins.plugins.sample;

import hudson.Extension;
import hudson.model.RootAction;
import jenkins.model.Jenkins;

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
        return "job/"+getCurrentUserId()+"-pg/configure";
    }

    private String getCurrentUserId() {
        return Jenkins.getAuthentication().getName();
    }
}
