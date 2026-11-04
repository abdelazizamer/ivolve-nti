# Jenkins Lab 20: Users and Role-Based Authorization

This lab creates two Jenkins users and gives them different global roles:

- `user1` receives the `admin` role.
- `user2` receives the `read-only` role.

The existing trusted administrator also keeps the `admin` role so administrative access is preserved.

## 1. Sign in as the current Jenkins administrator

Open the local Jenkins instance (commonly `http://localhost:8080`) and sign in with the existing administrator account. Keep this session open until both new users have been verified.

## 2. Confirm the Role Strategy plugin is installed

Go to **Manage Jenkins → Plugins → Installed plugins** and confirm **Role-based Authorization Strategy** is installed and enabled. In **Manage Jenkins → Security**, confirm that **Role-Based Strategy** is available under **Authorization**. Do not switch to it before role assignments are prepared.

## 3. Create the Jenkins users

Go to **Manage Jenkins → Users → Create User** and create:

- Username and full name: `user1`
- Username and full name: `user2`

Choose test passwords and keep them private. Do not put passwords in screenshots, repository files, or chat.

## 4. Configure roles and assignments together

The role editor did not accept role creation before the strategy was active. To avoid switching to an empty authorization strategy, use **Manage Jenkins → Script Console** to create the role maps, assign users, and activate the strategy in one operation.

Replace `<CURRENT_ADMIN_USERNAME>` below with the exact username of the existing trusted administrator, then paste the script into the Script Console and click **Run**:

```groovy
import com.michelin.cio.hudson.plugins.rolestrategy.AuthorizationType
import com.michelin.cio.hudson.plugins.rolestrategy.PermissionEntry
import com.michelin.cio.hudson.plugins.rolestrategy.Role
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType
import hudson.model.Item
import hudson.model.View
import hudson.security.Permission
import jenkins.model.Jenkins
import java.util.HashSet

def jenkins = Jenkins.get()
def strategy = new RoleBasedAuthorizationStrategy()
def globalRoles = strategy.getRoleMap(RoleType.Global)

def adminPermissions = new HashSet<Permission>()
adminPermissions.add(Jenkins.ADMINISTER)
def adminRole = new Role("admin", adminPermissions)
globalRoles.addRole(adminRole)
globalRoles.assignRole(adminRole, new PermissionEntry(AuthorizationType.USER, "<CURRENT_ADMIN_USERNAME>"))
globalRoles.assignRole(adminRole, new PermissionEntry(AuthorizationType.USER, "user1"))

def readOnlyPermissions = new HashSet<Permission>()
readOnlyPermissions.add(Jenkins.READ)
readOnlyPermissions.add(Item.READ)
readOnlyPermissions.add(View.READ)
def readOnlyRole = new Role("read-only", readOnlyPermissions)
globalRoles.addRole(readOnlyRole)
globalRoles.assignRole(readOnlyRole, new PermissionEntry(AuthorizationType.USER, "user2"))

jenkins.setAuthorizationStrategy(strategy)
jenkins.save()

println("Role-Based Strategy configured. The trusted admin and user1 have admin; user2 has read-only.")
```

The Script Console can change Jenkins security. Run only code you understand and keep the trusted administrator username correct.

## 5. Verify the roles and assignments in Jenkins

Open **Manage Jenkins → Role Management → Manage Roles**. Confirm:

- `admin`: **Overall → Administer**
- `read-only`: **Overall → Read**, **Job → Read**, and **View → Read**

Then open **Role Management → Assign Roles** and confirm:

- The trusted administrator and `user1` are assigned `admin`.
- `user2` is assigned `read-only`.
- `anonymous` and `authenticated` have no roles assigned.

## 6. Verify each user

Keep the trusted administrator session open while testing.

1. In a private/incognito browser window, sign in as `user1`. Confirm **Manage Jenkins** is available.
2. Sign out, then sign in as `user2`. Confirm the home page and jobs are readable, while **Manage Jenkins**, **Configure**, and **Build Now** are unavailable. Do not trigger a build as part of the check.

## 7. Completion checklist

- `user1` can access Jenkins administration.
- `user2` can read jobs but cannot administer, configure, or build them.
- The existing trusted administrator still has the `admin` role.
- Never commit passwords, private keys, or API tokens.


<img width="1919" height="922" alt="image" src="https://github.com/user-attachments/assets/1284d08a-8bd2-437a-bb4a-0cfa4b508465" />
<img width="1919" height="927" alt="Screenshot 2026-11-04 161017" src="https://github.com/user-attachments/assets/322d95a8-851c-4b8c-80c4-772e3bbdd553" />

