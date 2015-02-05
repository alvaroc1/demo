package controllers

import java.sql.{PreparedStatement, ResultSet, Timestamp}
import org.joda.time.DateTime

// had to take care of variance when expanding to include "get" method
trait SqlTypeIn[-T] {
  def set (stmt: PreparedStatement, idx: Int, value: T): Unit
}
trait SqlTypeOut[+T] {
  def get (rs: ResultSet, idx: Int): T
  def get (rs: ResultSet, name: String): T
}

// TYPE CLASS
trait SqlType[T] extends SqlTypeIn[T] with SqlTypeOut[T]

trait SqlHelpers {
  
  // PIMP STATEMENT
  implicit class StatementOps (stmt: PreparedStatement) {
    def set [T:SqlTypeIn](idx: Int, value: T): Unit = {
      implicitly[SqlTypeIn[T]].set(stmt, idx, value)
    }
    
    def params (params: Param[_]*) = {
      for ((param, idx) <- params.zipWithIndex) {
        param.set(stmt, idx+1)
      }
    }
  }
  
  // PIMP RESULTSET
  implicit class ResultSetOps (rs: ResultSet) {
    def get [T:SqlTypeOut] (idx: Int) = implicitly[SqlTypeOut[T]].get(rs, idx)
    def get [T:SqlTypeOut] (name: String) = implicitly[SqlTypeOut[T]].get(rs, name)
  }
  
  // PIMP T (pair T with it's SqlTypeIn[T])
  implicit class Param[T:SqlTypeIn] (value: T) {
    def set (stmt: PreparedStatement, idx: Int) = implicitly[SqlTypeIn[T]].set(stmt, idx, value)
  }
  
  // TYPE CLASS INSTANCES:
  implicit val intSqlType: SqlType[Int] = new SqlType[Int] {
    def set (stmt: PreparedStatement, idx: Int, value: Int) = stmt.setInt(idx, value)
    def get (rs: ResultSet, idx: Int) = rs.getInt(idx)
    def get (rs: ResultSet, name: String) = rs.getInt(name)
  }
  implicit val stringSqlType: SqlType[String] = new SqlType[String] {
    def set (stmt: PreparedStatement, idx: Int, value: String) = stmt.setString(idx, value)
    def get (rs: ResultSet, idx: Int) = rs.getString(idx)
    def get (rs: ResultSet, name: String) = rs.getString(name)
  }
  implicit val timestampSqlType: SqlType[Timestamp] = new SqlType[Timestamp] {
    def set (stmt: PreparedStatement, idx: Int, value: Timestamp) = stmt.setTimestamp(idx, value)
    def get (rs: ResultSet, idx: Int) = rs.getTimestamp(idx)
    def get (rs: ResultSet, name: String) = rs.getTimestamp(name)
  }
  implicit val jodaDateTimeSqlType: SqlType[DateTime] = new SqlType[DateTime] {
    def set (stmt: PreparedStatement, idx: Int, value: DateTime) = stmt.setTimestamp(idx, new Timestamp(value.getMillis))
    def get (rs: ResultSet, idx: Int) = new DateTime(rs.getTimestamp(idx))
    def get (rs: ResultSet, name: String) = new DateTime(rs.getTimestamp(name))
  }
  implicit val statusSqlType: SqlType[UserStatus] = new SqlType[UserStatus] {
    def set (stmt: PreparedStatement, idx: Int, value: UserStatus) = 
      stmt.setString(
        idx, 
        value match {
          case Active => "active"
          case Inactive => "inactive"
        }
      )
    def get (rs: ResultSet, idx: Int) = 
      rs.getString(idx) match {
        case "active" => Active
        case "inactive" => Inactive
      }
    def get (rs: ResultSet, name: String) = 
      rs.getString(name) match {
        case "active" => Active
        case "inactive" => Inactive
      }
  }

}
