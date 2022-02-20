package com.essbase.japi;

import java.io.BufferedReader;

import java.io.InputStreamReader;

import java.sql.Connection;
import java.sql.Date;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Scanner;

public class AuditDataGenerator {
    //static final String dbUrl = "jdbc:oracle:thin:@SLC15AWD:1521:orcl";
    static String dbUrl = "jdbc:oracle:thin:@slc05pqh.us.oracle.com:1521:epm";

    static String user = "PBCS_REG";
    static String pwd = "welcome1";
    static final Calendar calendar = new GregorianCalendar();
    static int noOfRecords = 1200000;
    //static int reportCount = 100000;
    static int batchCount = 1000;
    static int rndFactor = 100;

    public static void main(String[] args) {
        Connection conn = null;

        String action = "insert";

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter dbUrl: ");
        dbUrl=scanner.nextLine();
        System.out.println("Enter user: ");
        user=scanner.nextLine();
        System.out.println("Enter password: ");
        pwd= scanner.nextLine();
        System.out.println("Enter no Of records: ");
        //System.out.println(dbUrl+" "+user +" "+dbUrl+" "+noOfRecords);
        noOfRecords= Integer.parseInt(scanner.nextLine());
        //System.out.println(dbUrl+" "+user +" "+dbUrl+" "+noOfRecords);

        try {
            //System.out.println(dbUrl+" "+user +" "+dbUrl+" "+noOfRecords);
            conn = connectToBugDB(user, pwd, dbUrl);

            if ("insert".equalsIgnoreCase(action)) {
                insertRecords(conn);
            }
            //            }
            //            else if ("before6months".equalsIgnoreCase(action)) {
            //                testSelectOlderSixMonths(conn);
            //            }
            //            else if ("export".equalsIgnoreCase(action)) {
            //                testExport(conn);
            //            }
            //            else {
            //                testSelectAll(conn);
            //            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            close(conn);
        }
    }

    /*
    private static void testYear() {
        int[] map = new int[25];

        for(int i = 0; i < 1000000; i++) {
            map[getYear()]++;
        }

        for(int i = 0; i < map.length; i++) {
            System.out.println("i=" + String.valueOf(i + 1) + ":" + map[i]);
        }
    }
    */

    private static void insertRecords(Connection conn) throws SQLException {
        PreparedStatement ps = null;

        conn.setAutoCommit(false);

        ps = getPreparedStatement(conn);

        int batCount = batchCount;
        int pctComplete = 0;

        for (int i = 1; i <= noOfRecords; i++) {
            insert(ps, i);
            int pcComplete = (int)((i * 100L) / noOfRecords);
            if (pcComplete != pctComplete) {
                System.out.println(String.valueOf(pctComplete) + "% complete: Inserted " + String.valueOf(i) + " Records");
                pctComplete = pcComplete;
            }

            if (--batCount <= 0) {
                ps.executeBatch();
                conn.commit();
                batCount = batchCount;
            }
        }

        ps.executeBatch();
        conn.commit();
        conn.setAutoCommit(true);
        ps.close();
    }


    private static PreparedStatement getPreparedStatement(Connection conn) throws SQLException {
        String sql = "insert into HSP_AUDIT_RECORDS (TYPE,ID_1,ID_2,USER_NAME,TIME_POSTED,ACTION,PROPERTY,OLD_VAL,NEW_VAL) VALUES (?,?,?,?,(sysdate-?),?,?,?,?)";
        return conn.prepareCall(sql);
    }


    private static void insert(PreparedStatement ps, int days) throws SQLException {
        double daysEarlierFromLaterToday = 0;


        ps.setString(1, "Security");
        ps.setString(2, "SRC1");
        ps.setString(3, "cell intersection");
        ps.setString(4, "epm_default_cloud_admin");
        ps.setDouble(5, daysEarlierFromLaterToday);
        ps.setString(6, "manual");
        ps.setString(7, "test");
        ps.setString(8, "40");
        ps.setString(9, "40");

        //        ps.execute();
        ps.addBatch();
    }

    private static Connection connectToBugDB(String user, String pwd, String url) throws Exception {
        Driver d = (Driver)Class.forName("oracle.jdbc.OracleDriver").newInstance();
        DriverManager.registerDriver(d);
        return (DriverManager.getConnection(url, user, pwd));
    }

    private static int getYear() {
        return (int)((Math.random() * rndFactor) % 25.0);
    }

    private static int getMonth() {
        return (int)((Math.random() * rndFactor) % 12.0);
    }

    private static int getDay() {
        return (int)((Math.random() * rndFactor) % 31.0);
    }

    private static void close(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public static String NumberToWords(long records) {
        int million = (int)(records / 1000000);
        records = records % 1000000;
        int hundthousands = (int)(records / 1000);
        records = records % 1000;

        //        int hundreds = (int) (records / 100);
        //        records = records % 100;

        //        int tens = (int) (records / 10);
        //        records = records % 10;

        String text = "";

        if (million > 0) {
            text += String.valueOf(million) + " million ";
        }

        if (hundthousands > 0) {
            text += String.valueOf(hundthousands) + " thousand ";
        }
        if (records > 0) {
            text += String.valueOf(records);
        }

        return (text);
    }

    public static String TimeToWords(long time) {
        int ms = (int)(time % 1000);
        int secs = 0;
        int mins = 0;

        time /= 1000;
        if (time > 59) {
            secs = (int)(time % 60);
            time /= 60;
        } else {
            secs = (int)time;
            time = 0;
        }

        if (time > 59) {
            mins = (int)(time % 60);
            time /= 60;
        } else {
            mins = (int)time;
            time = 0;
        }

        String text = "";

        if (time > 0) {
            text = String.valueOf(time) + " hours ";
        }
        if (mins > 0) {
            text += String.valueOf(mins) + " minutes ";
        }
        if (secs > 0) {
            text += String.valueOf(secs) + " seconds ";
        }
        if (ms > 0) {
            text += String.valueOf(ms) + " milliseconds ";
        }

        return (text);
    }

}

