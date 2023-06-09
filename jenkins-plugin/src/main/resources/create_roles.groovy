import jenkins.model.Jenkins
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy
import com.michelin.cio.hudson.plugins.rolestrategy.Role
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType
import hudson.security.Permission
import hudson.model.User
import hudson.model.FreeStyleProject
import com.cloudbees.hudson.plugins.folder.Folder

Jenkins jenkins = Jenkins.get()
authorizationStrategy=jenkins.getAuthorizationStrategy()
rbas=(RoleBasedAuthorizationStrategy) authorizationStrategy

def users = User.getAll()

ItemRoleMap = rbas.getRoleMaps()[RoleType.Project]
Set<Permission> userPermissions = Permission.getAll().toSet()

def roleLength = ItemRoleMap.getRoles().size()
def userLength = users.size()

if(roleLength != userLength){
    println "A new user has signed up. Renew authentication."
    users.each { user ->
        def itemRolePattern = "${user.getId()}-.*"
        def userName = "${user.getId()}"
        def itemRole = new Role(userName, itemRolePattern, userPermissions)
        ItemRoleMap.addRole(itemRole)
        ItemRoleMap.assignRole(itemRole,user.getId())
        def jobName= userName+"-PipelineGenerator"
        def folderName= userName+"-folder"
        // Check if freestyle job exists with user's ID as name
        if (!jenkins.getItemByFullName(folderName, Folder.class)) {
            // Create new folder  & job
            def folder = jenkins.createProject(Folder.class, folderName)
            folder.save()
            def job = folder.createProject(FreeStyleProject.class, jobName)
            job.setDescription("Pipeline Generator입니다. Pipeline Generator build step을 수정 후 빌드하세요.")
            job.save()
            println userName +" has signed up"
        }
    }
    jenkins.setAuthorizationStrategy(rbas)
}else{
    println "There are no new users."
}
