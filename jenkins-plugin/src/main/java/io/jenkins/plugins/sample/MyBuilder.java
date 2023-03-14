package io.jenkins.plugins.sample;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;

//@Extension
public class MyBuilder extends Builder {
    private long time;

    @DataBoundConstructor
    public MyBuilder(long time){
        this.time=time;
    }
    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("yaho yaho "+time+ "colllllllllllll");
        Thread.sleep(time);
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckTime(@QueryParameter Long time)
                throws IOException, ServletException {
            try{
                Long.valueOf(time);
                if(time<0){
                    return FormValidation.error("Hey!!!!!! Please enter a positive number!!");
                }else{
                    return FormValidation.ok();
                }
            }catch (NumberFormatException e){
                return FormValidation.error("Hey!! Please enter a number!!!!!!! ");
            }
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return aClass == FreeStyleProject.class;
        }

        @Override
        public String getDisplayName() {
            return "My new Builder!!!";
        }

    }
}
