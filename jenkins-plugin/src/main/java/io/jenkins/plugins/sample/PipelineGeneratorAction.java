package io.jenkins.plugins.sample;

import hudson.Extension;
import hudson.model.RootAction;

@Extension
public class PipelineGeneratorAction implements RootAction {

    @Override
    public String getIconFileName() {
        return "clipboard.png";
    }

    @Override
    public String getDisplayName() {
        return "Pipeline Generator";
    }

    @Override
    public String getUrlName() {
        return "http://localhost:8080/jenkins/job/new%20pl%20generator/configure";
    }
}
