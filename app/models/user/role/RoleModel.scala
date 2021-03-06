package models.user.role

import java.sql.Timestamp

import com.google.common.base.Preconditions._
import db.impl.RoleTable
import db.impl.model.OreModel
import db.impl.model.common.Hideable
import db.impl.table.ModelKeys._
import models.user.User
import ore.Visitable
import ore.permission.role.Role
import ore.permission.role.RoleTypes.RoleType

/**
  * Represents a [[Role]] in something like a [[models.project.Project]] or
  * [[models.user.Organization]].
  *
  * @param id           Model ID
  * @param createdAt    Timestamp instant of creation
  * @param userId       ID of User this role belongs to
  * @param _roleType    Type of Role
  * @param _isAccepted  True if has been accepted
  */
abstract class RoleModel(override val id: Option[Int],
                         override val createdAt: Option[Timestamp],
                         override val userId: Int,
                         private var _roleType: RoleType,
                         private var _isAccepted: Boolean = false,
                         private var _isVisible: Boolean = true)
                         extends OreModel(id, createdAt)
                           with Role with Hideable { self =>

  override type M <: RoleModel { type M = self.M }
  override type T <: RoleTable[M]

  /**
    * Returns the subject of this Role.
    *
    * @return Subject of Role
    */
  def subject: Visitable

  /**
    * Sets this role's [[RoleType]].
    *
    * @param _roleType Role type to set
    */
  def roleType_=(_roleType: RoleType) = {
    checkNotNull(_roleType, "null role type", "")
    this._roleType = _roleType
    if (isDefined) update(RoleType)
  }

  /**
    * Sets whether this role has been accepted by the [[User]] it belongs to.
    *
    * @param accepted True if role accepted
    */
  def setAccepted(accepted: Boolean) = Defined {
    this._isAccepted = accepted
    update(IsAccepted)
  }

  /**
    * Returns true if this role has been accepted by the [[User]] it belongs to.
    *
    * @return True if accepted by user
    */
  def isAccepted: Boolean = this._isAccepted

  /**
    * Sets whether this role should be displayed to the end user.
    *
    * @param visible True if visible
    */
  def setVisible(visible: Boolean) = Defined {
    this._isVisible = visible
    update(IsVisible)
  }

  /**
    * Returns true if this role should be displayed to the end user.
    *
    * @return True if model is visible
    */
  def isVisible: Boolean = this._isVisible

  override def roleType = this._roleType

}

object RoleModel {

  implicit def ordering[A <: RoleModel]: Ordering[A] = Ordering.by(_.roleType.trust)

}
