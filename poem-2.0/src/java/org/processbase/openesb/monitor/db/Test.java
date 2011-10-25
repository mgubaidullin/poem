/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.processbase.openesb.monitor.db;

import com.sun.jbi.ui.common.JBIAdminCommands;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.naming.InitialContext;
import javax.sql.DataSource;

public class Test {

    public static void main(String[] args) {

//        System.out.println("DEBUG GETBPEL " + suName + " " +bpelName + " " + clusterName);
        InputStream result = null;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        File file = null;
        try {
            connection = getConnection();
            ps = connection.prepareStatement("SELECT SUZIPARCHIVE FROM SERVICEUNIT WHERE SUNAME = :1");
            ps.setString(1, "KIOSK_CASA-KIOSK");
            rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    Blob suArchiveBlob = rs.getBlob(1);
                    FileOutputStream fos = null;
                    int length = 0;
                    ZipFile zipFile = null;
                    ZipEntry zipEntry = null;

                    file = File.createTempFile("suArchive", ".zip");
                    file.deleteOnExit();
                    length = (int) suArchiveBlob.length();
                    fos = new FileOutputStream(file);
                    fos.write(suArchiveBlob.getBytes(1L, length));
                    zipFile = new ZipFile(file);
                    for (Enumeration<ZipEntry> e = (Enumeration<ZipEntry>) zipFile.entries(); e.hasMoreElements();) {
                        zipEntry = e.nextElement();
                        System.out.println(zipEntry.getName());
                        if (zipEntry.getName().equals("/sun-bpel-engine/" + "BPEL_CL_INFO" + ".bpel")) {
                            result = zipFile.getInputStream(zipEntry);
                            System.out.println("result = " + result);
                        }
                    }
                }
                rs.close();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (connection != null) {
                    connection.close();
                }
                if (file != null) {
                    file.delete();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static Connection getConnection() {
        String jdbcURL = "jdbc:oracle:thin:@localhost:1521:maratdb";
        Connection conn = null;
        ResultSet rs = null;
        String user = "openesb";
        String passwd = "openesb";

        try {
            Class.forName("oracle.jdbc.driver.OracleDriver").newInstance();
            conn = DriverManager.getConnection(jdbcURL, user, passwd);

        } catch (Exception ex) {
        }
        return conn;
    }
}
