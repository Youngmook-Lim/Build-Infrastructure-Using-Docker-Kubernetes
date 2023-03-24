import jenkins.model.Jenkins
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy
import com.michelin.cio.hudson.plugins.rolestrategy.Role
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType
import org.jenkinsci.plugins.rolestrategy.permissions.PermissionHelper
import hudson.security.AuthorizationStrategy
import hudson.security.Permission
import hudson.model.Project
import hudson.model.User
import hudson.security.PermissionGroup

Jenkins jenkins = Jenkins.get()
authorizationStrategy=jenkins.getAuthorizationStrategy()
rbas=(RoleBasedAuthorizationStrategy) authorizationStrategy

def users = User.getAll()

ItemRoleMap = rbas.getRoleMaps()[RoleType.Project]
Set<Permission> userPermissions = Permission.getAll().toSet()

users.each { user ->
    def itemRolePattern = "${user.getId()}-.*"
    def itemRoleName = "${user.getId()}"
    def itemRole = new Role(itemRoleName, itemRolePattern, userPermissions)
    ItemRoleMap.addRole(itemRole)
    ItemRoleMap.assignRole(itemRole,user.getId())
}

jenkins.setAuthorizationStrategy(rbas)
