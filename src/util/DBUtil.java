package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {

    /** NOTE: You should change the following settings to your local database, i.e., url, name, user, password
     * We use MySQL Community Server --version 8.0.15 */

    public static String url = "jdbc:mysql://localhost:3306/tkde_example?"
            + "useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=UTC&autoReconnect=true";
    public static String name = "com.mysql.cj.jdbc.Driver";
    public static String user = "root";
    public static String password = "123456";
    public Connection conn = null;
  
    public DBUtil() {
        try {
            Class.forName(name);
            conn = DriverManager.getConnection(url,user, password);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            this.conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
