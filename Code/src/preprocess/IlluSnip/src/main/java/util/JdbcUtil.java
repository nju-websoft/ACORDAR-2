package util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class JdbcUtil {
    private static String URL_REMOTE;
    private static String URL_LOCAL;
    private static String JDBC_DRIVER;
    private static String USER_NAME_REMOTE;
    private static String PASSWORD_REMOTE;
    private static String USER_NAME_LOCAL;
    private static String PASSWORD_LOCAL;
    private static Connection connection_remote = null;
    private static Connection connection_local = null;
    /*
     * 静态代码块，类初始化时加载数据库驱动
     */
    static {
        try {
            // 加载dbinfo.properties配置文件
            InputStream in = JdbcUtil.class.getClassLoader()
                    .getResourceAsStream("JavaUtil.properties");
            Properties properties = new Properties();
            properties.load(in);

            // 获取驱动名称、url、用户名以及密码
            JDBC_DRIVER = properties.getProperty("JDBC_DRIVER");

            URL_REMOTE = properties.getProperty("URL_REMOTE");
            USER_NAME_REMOTE = properties.getProperty("USER_NAME_REMOTE");
            PASSWORD_REMOTE = properties.getProperty("PASSWORD_REMOTE");

            URL_LOCAL = properties.getProperty("URL_LOCAL");
            USER_NAME_LOCAL = properties.getProperty("USER_NAME_LOCAL");
            PASSWORD_LOCAL = properties.getProperty("PASSWORD_LOCAL");

            // 加载驱动
            Class.forName(JDBC_DRIVER);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    public static Connection getConnection(int type)
    {
        Integer failedCnt = 0;
        while(failedCnt < 3)
        {
            try
            {
                //URL = URL.replace("needreplace", Utils.GlobalVariances.Database_name);
                if(type == GlobalVariances.REMOTE){
                    return DriverManager.getConnection(URL_REMOTE, USER_NAME_REMOTE, PASSWORD_REMOTE);
                }else{
                    return DriverManager.getConnection(URL_LOCAL, USER_NAME_LOCAL, PASSWORD_LOCAL);
                }

            } catch (SQLException e)
            {
                e.printStackTrace();
                ++ failedCnt;
                continue;
            }
        }
        return null;
    }

//    public static Connection getLocalConnection()
//    {
//        Integer failedCnt = 0;
//        while(failedCnt < 3)
//        {
//            try
//            {
//                //URL = URL.replace("needreplace", Utils.GlobalVariances.Database_name);
//                return DriverManager.getConnection(URL_LOCAL, USER_NAME_LOCAL, PASSWORD_REMOTE);
//            } catch (SQLException e)
//            {
//                e.printStackTrace();
//                ++ failedCnt;
//                continue;
//            }
//        }
//        return null;
//    }


    public static void closeConnection(Connection conn){
        if(conn != null){
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void closeStaticConnection(){
        if(connection_remote != null){
            try {
                connection_remote.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void close(Connection connection, Statement statement, ResultSet resultSet) {
        if (connection != null)
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        if (statement != null)
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        if (resultSet != null)
            try {
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
    }
}