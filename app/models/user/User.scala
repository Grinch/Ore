package models.user

import java.sql.Timestamp

import com.google.common.base.Preconditions._
import db.Named
import db.access.ModelAccess
import db.impl.OrePostgresDriver.api._
import db.impl._
import db.impl.access.{OrganizationBase, UserBase}
import db.impl.model.OreModel
import db.impl.table.ModelKeys._
import models.project.{Flag, Project, Version}
import models.user.role.{OrganizationRole, ProjectRole}
import ore.{OreConfig, Visitable}
import ore.permission._
import ore.permission.role.RoleTypes.{DonorType, RoleType}
import ore.permission.role._
import ore.permission.scope._
import ore.user.Prompts.Prompt
import ore.user.UserOwned
import org.spongepowered.play.discourse.model.DiscourseUser
import play.api.mvc.Request
import security.pgp.PGPPublicKeyInfo
import security.spauth.SpongeUser
import util.StringUtils._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Represents a Sponge user.
  *
  * @param id           External ID provided by authentication.
  * @param createdAt    Date this user first logged onto Ore.
  * @param _name        Full name of user
  * @param _username    Username
  * @param _email       Email
  * @param _tagline     The user configured "tagline" displayed on the user page.
  */
case class User(override val id: Option[Int] = None,
                override val createdAt: Option[Timestamp] = None,
                private var _name: Option[String] = None,
                private var _username: String = null,
                private var _email: Option[String] = None,
                private var _tagline: Option[String] = None,
                private var _globalRoles: List[RoleType] = List(),
                private var _joinDate: Option[Timestamp] = None,
                private var _avatarUrl: Option[String] = None,
                private var _readPrompts: List[Prompt] = List(),
                private var _pgpPubKey: Option[String] = None,
                private var _lastPgpPubKeyUpdate: Option[Timestamp] = None,
                private var _isLocked: Boolean = false)
                extends OreModel(id, createdAt)
                  with UserOwned
                  with ScopeSubject
                  with Named
                  with Visitable {

  override type M = User
  override type T = UserTable

  /**
    * The User's [[PermissionPredicate]]. All permission checks go through
    * here.
    */
  val can: PermissionPredicate = PermissionPredicate(this)
  val cannot: PermissionPredicate = PermissionPredicate(this, not = true)

  /**
    * The User's username
    *
    * @return Username
    */
  def username: String = this._username

  /**
    * Sets this User's username.
    *
    * @param _username Username of User
    */
  def username_=(_username: String) = {
    checkNotNull(_username, "username cannot be null", "")
    this._username = _username
    if (isDefined) update(Name)
  }

  /**
    * Returns this User's email.
    *
    * @return User email
    */
  def email: Option[String] = this._email

  /**
    * Sets this User's email.
    *
    * @param _email User email
    */
  def email_=(_email: String) = {
    this._email = Option(_email)
    if (isDefined) update(Email)
  }

  /**
    * Returns this User's PGP public key, if any. A PGP public key is required
    * for any uploads to Ore.
    *
    * @return PGP public key
    */
  def pgpPubKey: Option[String] = this._pgpPubKey

  /**
    * Sets this User's PGP public key. A PGP public key is required for any
    * uploads to Ore.
    *
    * @param pgpPubKey PGP public key
    */
  def pgpPubKey_=(pgpPubKey: String) = {
    this._pgpPubKey = Option(pgpPubKey)
    if (isDefined) update(PGPPubKey)
  }

  /**
    * Decodes this user's raw PGP public key and returns information about the
    * key.
    *
    * @return Public key information
    */
  def pgpPubKeyInfo: Option[PGPPublicKeyInfo] = this.pgpPubKey.flatMap(PGPPublicKeyInfo.decode)

  /**
    * Returns the last [[Timestamp]] when this User's PGP Public key was
    * updated. This is not set for the first time a User sets a public key but
    * is set for every time after.
    *
    * @return Last time this User updated their public key
    */
  def lastPgpPubKeyUpdate: Option[Timestamp] = this._lastPgpPubKeyUpdate

  /**
    * Sets the last [[Timestamp]] when this User's PGP Public key was updated.
    *
    * @param _lastPgpPubKeyUpdate Last time this User updated their public key
    */
  def lastPgpPubKeyUpdate_=(_lastPgpPubKeyUpdate: Timestamp) = Defined {
    this._lastPgpPubKeyUpdate = Option(_lastPgpPubKeyUpdate)
    update(LastPGPPubKeyUpdate)
  }

  /**
    * Returns true if this user's PGP Public Key is ready for use.
    *
    * @return True if key is ready for use
    */
  def isPgpPubKeyReady: Boolean = this.pgpPubKey.isDefined && this.lastPgpPubKeyUpdate.forall { lastUpdate =>
    val cooldown = this.config.security.getLong("keyChangeCooldown").get
    val minTime = new Timestamp(lastUpdate.getTime + cooldown)
    minTime.before(this.service.theTime)
  }

  /**
    * Returns true if this User's profile is locked.
    *
    * @return True if profile is locked
    */
  def isLocked: Boolean = this._isLocked

  /**
    * Sets whether this User's profile is locked.
    *
    * @param _isLocked True if profile is locked
    */
  def setLocked(_isLocked: Boolean) = {
    this._isLocked = _isLocked
    if (isDefined) update(IsLocked)
  }

  /**
    * Returns this User's full name.
    *
    * @return Full name of user
    */
  def fullName: Option[String] = this._name

  /**
    * Sets this User's full name.
    *
    * @param _fullName Full name of user
    */
  def fullName_=(_fullName: String) = {
    this._name = Option(_fullName)
    if (isDefined) update(FullName)
  }

  /**
    * Returns the date this User joined any Sponge services.
    *
    * @return Join date
    */
  def joinDate: Option[Timestamp] = this._joinDate

  /**
    * Sets the Timestamp instant when this User joined Sponge for the first
    * time.
    *
    * @param _joinDate Sponge join date
    */
  def joinDate_=(_joinDate: Timestamp) = {
    this._joinDate = Option(_joinDate)
    if (isDefined) update(JoinDate)
  }

  /**
    * Returns this User's avatar url.
    *
    * @return Avatar url
    */
  def avatarUrl: Option[String] = this._avatarUrl

  /**
    * Sets this User's avatar url.
    *
    * @param _avatarUrl Avatar url
    */
  def avatarUrl_=(_avatarUrl: String) = {
    this._avatarUrl = Option(_avatarUrl)
    if (isDefined) update(AvatarUrl)
  }

  /**
    * Returns this user's tagline.
    *
    * @return User tagline
    */
  def tagline: Option[String] = this._tagline

  /**
    * Sets this User's "tagline" that is displayed on the User page.
    *
    * @param _tagline Tagline to display
    */
  def tagline_=(_tagline: String) = {
    checkArgument(_tagline.length <= this.config.users.getInt("max-tagline-len").get, "tagline too long", "")
    this._tagline = Option(nullIfEmpty(_tagline))
    if (isDefined) update(Tagline)
  }

  /**
    * Returns this user's global [[RoleType]]s.
    *
    * @return Global RoleTypes
    */
  def globalRoles: Set[RoleType] = this._globalRoles.toSet

  /**
    * Sets the [[RoleTypes]]s that this User has globally.
    *
    * @param _globalRoles Roles to set
    */
  def globalRoles_=(_globalRoles: Set[RoleType]) = {
    var roles = _globalRoles
    if (roles == null)
      roles = Set.empty
    this._globalRoles = roles.toList
    if (isDefined) update(GlobalRoles)
  }

  /**
    * Returns the highest level [[DonorType]] this User has.
    *
    * @return Highest level donor type
    */
  def donorType: Option[DonorType] = {
    this.globalRoles.toList
      .filter(_.isInstanceOf[DonorType])
      .map(_.asInstanceOf[DonorType])
      .sortBy(_.id).lastOption
  }

  /**
    * Returns this User's highest level of Trust.
    *
    * @return Highest level of trust
    */
  def trustIn(scope: Scope = GlobalScope): Trust = Defined {
    scope match {
      case GlobalScope =>
        this.globalRoles.map(_.trust).toList.sorted.lastOption.getOrElse(Default)
      case pScope: ProjectScope =>
        pScope.project.memberships.getTrust(this)
      case oScope: OrganizationScope =>
        oScope.organization.memberships.getTrust(this)
      case _ =>
        throw new RuntimeException("unknown scope: " + scope)
    }
  }

  /**
    * Returns the Projects that this User has starred.
    *
    * @param page Page of user stars
    * @return     Projects user has starred
    */
  def starred(page: Int = -1): Seq[Project] = Defined {
    val starsPerPage = this.config.users.getInt("stars-per-page").get
    val limit = if (page < 1) -1 else starsPerPage
    val offset = (page - 1) * starsPerPage
    this.schema.getAssociation[ProjectStarsTable, Project](classOf[ProjectStarsTable], this)
      .sorted(ordering = _.name, limit = limit, offset = offset)
  }

  /**
    * Returns true if this User is the currently authenticated user.
    *
    * @return True if currently authenticated user
    */
  def isCurrent(implicit request: Request[_]): Boolean = {
    checkNotNull(request, "null request", "")
    this.service.getModelBase(classOf[UserBase]).current.exists { user =>
        user.equals(this) || (this.isOrganization && this.toOrganization.owner.user.equals(user))
    }
  }

  /**
    * Fills the mutable field in this User with the specified User's
    * non-missing mutable fields.
    *
    * @param user User to fill with
    * @return     This user
    */
  def fill(user: DiscourseUser): User = {
    if (user == null)
      return this
    this.username = user.username
    user.createdAt.foreach(this.joinDate_=)
    user.email.foreach(this.email_=)
    user.fullName.foreach(this.fullName_=)
    user.avatarTemplate.foreach(this.avatarUrl_=)
    this.globalRoles = user.groups
      .flatMap(group => RoleTypes.values.find(_.roleId == group.id).map(_.asInstanceOf[RoleType]))
      .toSet[RoleType]
    this
  }

  /**
    * Fills this User with the information SpongeUser provides.
    *
    * @param user SpongeUser
    * @return     This user
    */
  def fill(user: SpongeUser)(implicit config: OreConfig): User = {
    if (user == null)
      return this
    this.username = user.username
    this.email = user.email
    user.avatarUrl.map { url =>
      if (!url.startsWith("http")) {
        val baseUrl = config.security.getString("api.url").get
        baseUrl + url
      } else
        url
    }.foreach(this.avatarUrl = _)
    this
  }

  /**
    * Pulls information from the forums and updates this User.
    *
    * @return This user
    */
  def pullForumData(): User = {
    this.forums.await(this.forums.fetchUser(this.name).recover {
      case e: Exception => None // couldn't connect, ignore
    }).foreach(fill)
    this
  }

  /**
    * Pulls information from the forums and updates this User.
    *
    * @return This user
    */
  def pullSpongeData(): User = this.auth.getUser(this.name).map(fill).getOrElse {
    throw new Exception("user doesn't exist on SpongeAuth?")
  }

  /**
    * Returns all [[Project]]s owned by this user.
    *
    * @return Projects owned by user
    */
  def projects = this.schema.getChildren[Project](classOf[Project], this)

  /**
    * Returns the Project with the specified name that this User owns.
    *
    * @param name   Name of project
    * @return       Owned project, if any, None otherwise
    */
  def getProject(name: String): Option[Project] = this.projects.find(equalsIgnoreCase(_.name, name))

  /**
    * Returns a [[ModelAccess]] of [[ProjectRole]]s.
    *
    * @return ProjectRoles
    */
  def projectRoles = this.schema.getChildren[ProjectRole](classOf[ProjectRole], this)

  /**
    * Returns the [[Organization]]s that this User owns.
    *
    * @return Organizations user owns
    */
  def ownedOrganizations = this.schema.getChildren[Organization](classOf[Organization], this)

  /**
    * Returns the [[Organization]]s that this User belongs to.
    *
    * @return Organizations user belongs to
    */
  def organizations
  = this.schema.getAssociation[OrganizationMembersTable, Organization](classOf[OrganizationMembersTable], this)

  /**
    * Returns a [[ModelAccess]] of [[OrganizationRole]]s.
    *
    * @return OrganizationRoles
    */
  def organizationRoles = this.schema.getChildren[OrganizationRole](classOf[OrganizationRole], this)

  /**
    * Returns true if this User is also an organization.
    *
    * @return True if organization
    */
  def isOrganization: Boolean = Defined {
    this.service.getModelBase(classOf[OrganizationBase]).exists(_.id === this.id.get)
  }

  /**
    * Converts this User to an [[Organization]].
    *
    * @return Organization
    */
  def toOrganization: Organization = Defined {
    this.service.getModelBase(classOf[OrganizationBase]).get(this.id.get)
      .getOrElse(throw new IllegalStateException("user is not an organization"))
  }

  /**
    * Returns the [[Project]]s that this User is watching.
    *
    * @return Projects user is watching
    */
  def watching = this.schema.getAssociation[ProjectWatchersTable, Project](classOf[ProjectWatchersTable], this)

  /**
    * Sets the "watching" status on the specified project.
    *
    * @param project Project to update status on
    * @param watching True if watching
    */
  def setWatching(project: Project, watching: Boolean) = {
    checkNotNull(project, "null project", "")
    checkArgument(project.isDefined, "undefined project", "")
    val contains = this.watching.contains(project)
    if (watching) {
      if (!contains)
        this.watching.add(project)
    } else if (contains)
      this.watching.remove(project)
  }

  /**
    * Returns the [[Flag]]s submitted by this User.
    *
    * @return Flags submitted by user
    */
  def flags = this.schema.getChildren[Flag](classOf[Flag], this)

  /**
    * Returns true if the User has an unresolved [[Flag]] on the specified
    * [[Project]].
    *
    * @param project  Project to check
    * @return         True if has pending flag on Project
    */
  def hasUnresolvedFlagFor(project: Project): Boolean = {
    checkNotNull(project, "null project", "")
    checkArgument(project.isDefined, "undefined project", "")
    this.flags.exists(f => f.projectId === project.id.get && !f.isResolved)
  }

  /**
    * Returns this User's notifications.
    *
    * @return User notifications
    */
  def notifications = this.schema.getChildren[Notification](classOf[Notification], this)

  /**
    * Sends a [[Notification]] to this user.
    *
    * @param notification Notification to send
    * @return Future result
    */
  def sendNotification(notification: Notification) = {
    checkNotNull(notification, "null notification", "")
    this.config.debug("Sending notification: " + notification, -1)
    this.service.access[Notification](classOf[Notification]).add(notification.copy(userId = this.id.get))
  }

  /**
    * Returns true if this User has any unread notifications.
    *
    * @return True if has unread notifications
    */
  def hasUnreadNotifications: Boolean = Defined {
    val flags = this.service.access[Flag](classOf[Flag])
    val versions = this.service.access[Version](classOf[Version])
    val hasFlags = (this can ReviewFlags in GlobalScope) && flags.filterNot(_.isResolved).nonEmpty
    val hasReview = (this can ReviewProjects in GlobalScope) && versions.filterNot(_.isReviewed).nonEmpty
    val hasNotifications = this.notifications.filterNot(_.read).nonEmpty
    hasFlags || hasReview || hasNotifications
  }

  /**
    * Returns a set of [[Prompt]]'s that have been read by this User.
    *
    * @return Prompts read by User
    */
  def readPrompts: Set[Prompt] = this._readPrompts.toSet

  /**
    * Marks a [[Prompt]] as read by this User.
    *
    * @param prompt Prompt to mark as read
    */
  def markPromptRead(prompt: Prompt) = {
    checkNotNull(prompt, "null prompt", "")
    this._readPrompts = (this.readPrompts + prompt).toList
    if (isDefined) update(ReadPrompts)
  }

  override val name = this.username
  override val url = this.username
  override val scope = GlobalScope
  override def userId = this.id.get
  override def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(createdAt = theTime)

}

object User {

  /**
    * Creates a new [[User]] from the specified [[DiscourseUser]].
    *
    * @param user User to convert
    * @return     Ore User
    */
  @deprecated("use fromSponge instead")
  def fromDiscourse(user: DiscourseUser) = User().fill(user).copy(id = Some(user.id))

  /**
    * Create a new [[User]] from the specified [[SpongeUser]].
    *
    * @param user User to convert
    * @return     Ore user
    */
  def fromSponge(user: SpongeUser)(implicit config: OreConfig) = User().fill(user).copy(id = Some(user.id))

}
