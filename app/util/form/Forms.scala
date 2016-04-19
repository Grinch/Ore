package util.form

import models.project.Channel
import play.api.data.Form
import play.api.data.Forms._

/**
  * Collection of forms used in this application.
  */
object Forms {

  /**
    * Submits a new Channel for a Project.
    */
  lazy val ChannelEdit = Form(mapping(
    "channel-input" -> text
      .verifying("Invalid channel name.", Channel.isValidName(_)),
    "channel-color-input" -> text
      .verifying("Invalid channel color.", c => Channel.Colors.exists(_.hex.equalsIgnoreCase(c)))
  )(ChannelData.apply)(ChannelData.unapply))

  /**
    * Submits changes to a [[models.project.Project]]'s
    * [[models.user.ProjectRole]]s.
    */
  lazy val MemberRoles = Form(mapping(
    "users" -> list(number),
    "roles" -> list(text)
  )(ProjectRoleSetBuilder.apply)(ProjectRoleSetBuilder.unapply))

  /**
    * Submits changes on a documentation page.
    */
  lazy val PageEdit = Form(tuple("name" -> text, "content" -> text))

  /**
    * Submits settings changes for a Project.
    */
  lazy val ProjectSave = Form(mapping(
    "category" -> text,
    "issues" -> text,
    "source" -> text,
    "description" -> text
  )(ProjectSettings.apply)(ProjectSettings.unapply))

  /**
    * Submits a name change for a project.
    */
  lazy val ProjectRename = Form(single("name" -> text))

  /**
    * Submits a tagline change for a User.
    */
  lazy val UserTagline = Form(single("tagline" -> text))

  /**
    * Submits a change to a Version's description.
    */
  lazy val VersionDescription = Form(single("description" -> text))

}