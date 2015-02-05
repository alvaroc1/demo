package controllers

import play.api._
import play.api.mvc._
import play.api.db.DB
import play.api.Play.current
import java.sql.PreparedStatement
import org.joda.time.DateTime
import java.sql.Timestamp

sealed abstract class UserStatus
case object Active extends UserStatus
case object Inactive extends UserStatus

object Application extends Controller with SqlHelpers {
  
  def index = Action {
    setup()
    
    DB.withConnection {conn =>
      val stmt = conn.prepareStatement("INSERT INTO users SET id = ?, name = ?, age = ?, status = ?, created_at = ?")
      
      /* original jdbc api for setting params
      stmt.setInt(1, 1)
      stmt.setString(2, "Alvaro")
      stmt.setInt(3, 31)
      stmt.setString(4, "active")
      stmt.setTimestamp(5, new java.sql.Timestamp(DateTime.now.getMillis))
      */
      
      /* after basic pimping
      stmt.set(1, 1)
      stmt.set(2, "Alvaro")
      stmt.set(3, 31)
      stmt.set(4, Active)
      stmt.set(5, DateTime.now)
      */
      
      // Final API:
      stmt.params(
        1, "Alvaro", 31, Active, DateTime.now
      )
    
      stmt.execute()
      
      val selectStmt = conn.prepareStatement("SELECT id, name, status, created_at FROM users")
      
      val rs = selectStmt.executeQuery()
      
      rs.next()
      
      /* original jdbc api for retrieving values
      val id = rs.getInt(1) // by index
      val name = rs.getString("name") // by colname/alias
      val status = rs.getString("status") match {
        case "active" => Active
        case "inactive" => Inactive
      }
      val createdAt = new DateTime(rs.getTimestamp("created_at"))
      */
      
      // Final API:
      val id = rs.get[Int](1) // by index
      val name = rs.get[String]("name") // by colname/alias
      val status = rs.get[UserStatus]("status") // custom type
      val createdAt = rs.get[DateTime]("created_at") // custom_type
      
      println(s"ID: $id")
      println(s"Name: $id")
      println(s"Status: $status")
      println(s"Created: $createdAt")
      
      rs.close

      stmt.close()
    }
    
    Ok(views.html.index("Your new application is ready."))
  }
  
  private def setup () = DB.withConnection {conn =>
    val createStmt = conn.prepareStatement("DROP TABLE IF EXISTS users; CREATE TABLE users (id INT, name VARCHAR, age INT, status VARCHAR, created_at DATETIME)")
    createStmt.execute()
    createStmt.close()
  }

}

