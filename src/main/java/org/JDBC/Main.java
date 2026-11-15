package org.JDBC;

import java.sql.Connection;
import java.sql.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
//Create TABLE Employee(EMP_ID NUMERIC, EMP_NAME VARCHAR(50), DEPT VARCHAR(10), SALARY DOUBLE);
// using basic statement here for queries.
public class Main {
    public static void main(String[] args) {
        Connection conn = ConnectionFactory.getConnection();
        Statement stmt = null;
        ResultSet rset = null;
        try {
            stmt = conn.createStatement();
             rset = stmt.executeQuery("select * from Employee");
        } catch (SQLException e) {
            System.out.println(e);
        }

        try {
             while (rset.next()) {

                 System.out.println(rset.getInt("EMP_ID")
                         + " / " + rset.getString("EMP_NAME")
                         + " / " + rset.getString("DEPT")
                         + " / " + rset.getDouble("SALARY"));

             }
            } catch (SQLException e) {
                System.out.println(e);
            }
        }

    }
