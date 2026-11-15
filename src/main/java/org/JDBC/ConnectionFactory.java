package org.JDBC;
import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.util.Properties;
import java.sql.*;

//prepare a connedction factory for jdbc connection

public class ConnectionFactory {
    private static Connection conn;

    private ConnectionFactory() {
    }

    public static Connection getConnection() {
        InputStream inpS = ConnectionFactory.class.getClassLoader().getResourceAsStream("db.properties");
        Properties prop = new Properties();
        try {
            prop.load(inpS);
        } catch ( IOException e) {
            System.out.println(e);
        }
        String url = prop.getProperty("db.url");
        String driver = prop.getProperty("db.driver");
        String user = prop.getProperty("db.user");
        String password = prop.getProperty("db.password");

        try{
            Class.forName(driver);
            System.out.println("Driver Loaded");
        }catch (ClassNotFoundException e){
            System.out.println(e);
        }
        try {
      //      conn = DriverManager.getConnection("jdbc.mysql://localhost:3306/vkydb", "root", "root");
        conn=DriverManager.getConnection(url,user,password);
//        return conn;
        } catch (SQLException e) {
            System.out.println("Connection to DB failed");
        }
        return conn;
    }
}


