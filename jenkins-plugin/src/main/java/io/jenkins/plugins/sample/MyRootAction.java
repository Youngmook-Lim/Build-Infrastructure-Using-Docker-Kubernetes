package io.jenkins.plugins.sample;

import hudson.Extension;
import hudson.model.RootAction;

@Extension
public class MyRootAction implements RootAction {

    @Override
    public String getIconFileName() {
        return "clipboard.png";
    }

    @Override
    public String getDisplayName() {
        return "TEST sidebar new menu";
    }

    @Override
    public String getUrlName() {
        return "https://www.naver.com/";
    }
}
