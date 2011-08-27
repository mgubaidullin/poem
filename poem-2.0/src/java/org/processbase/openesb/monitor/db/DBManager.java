package org.processbase.openesb.monitor.db;

import com.sun.jbi.ui.common.JBIAdminCommands;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
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
import org.processbase.openesb.monitor.POEM;

/**
 *
 * @author marat
 */
public class DBManager {

    public static String SQL_COUNT_ =
            "SELECT COUNT(1) PCOUNT , TO_CHAR(MI.STARTTIME, 'DD.MM.YYYY') PDATE, MI.BPELID, MI.STATUS" + " FROM MONITORBPELINSTANCE MI" + " WHERE STARTTIME >= ? " + " GROUP BY TO_CHAR(MI.STARTTIME, 'DD.MM.YYYY'), MI.BPELID, MI.STATUS " + "ORDER BY TO_CHAR(MI.STARTTIME, 'DD.MM.YYYY')";
    public static String SQL_COUNT_BY_STATUS =
            "SELECT count(1) PCOUNT, STATUS FROM MONITORBPELINSTANCE WHERE STARTTIME >= ? GROUP BY STATUS";

    public Connection getConnection(String clusterName) {
        Connection con = null;
        try {
            InitialContext context = new InitialContext();
            //Look up our data source
            String bpelDatabaseJNDI = null;
            if (POEM.getCurrent().isClusterSupported) {
                bpelDatabaseJNDI = POEM.getCurrent().jbiAdminCommands.getComponentConfiguration("sun-bpel-engine", clusterName).getProperty("DatabaseNonXAJNDIName");
            } else {
                bpelDatabaseJNDI = POEM.getCurrent().jbiAdminCommands.getComponentConfiguration("sun-bpel-engine", JBIAdminCommands.SERVER_TARGET_KEY).getProperty("DatabaseNonXAJNDIName");
            }
            DataSource ds = (DataSource) context.lookup(bpelDatabaseJNDI);
            //Allocate and use a connection from the pool
            con = ds.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return con;
    }

    public InputStream getBPEL(String suName, String bpelName, String clusterName) {
//        System.out.println("DEBUG GETBPEL " + suName + " " +bpelName);
        InputStream result = null;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        File file = null;
        try {
            connection = getConnection(clusterName);
            ps = connection.prepareStatement("SELECT SUZIPARCHIVE FROM SERVICEUNIT WHERE SUNAME = :1");
            ps.setString(1, suName);
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
                        if (zipEntry.getName().equals("/sun-bpel-engine/" + bpelName + ".bpel")) {
//                            System.out.println(zipEntry.getName());
                            result = zipFile.getInputStream(zipEntry);
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
        return result;
    }
//    protected List<BPELData> getBPELForSU(String suName) {
//         List<BPELData> bpelList = new ArrayList<BPELData>();
//         try {
//             ResultSet rs = dbConnection.createStatement().executeQuery("SELECT SUZIPARCHIVE FROM ServiceUnit WHERE SUNAME = '" + suName + "'");
//             while (rs.next()) {
//                 Blob suArchiveBlob = (Blob) rs.getBlob("SUZIPARCHIVE");
//                 File file = null;
//                 FileOutputStream fos = null;
//                 int length = 0;
//                 ZipFile zipFile = null;
//                 ZipEntry zipEntry = null;
//                 String zipEntryName = null;
//                 BufferedReader br = null;
//                 InputStreamReader isr = null;
//                 InputStream is = null;
//                 String line = null;
//                 StringBuffer bpelSB = new StringBuffer();
//                 String bpelName = null;
//                 int idx = 0;
//
//                 try {
//                     file = File.createTempFile("suArchive", ".zip");
// //                        System.out.println("*** APH-I1 : Temp File " + file.getAbsolutePath());
// //                        System.out.println("*** APH-I1 : Temp File " + file.getName());
//                     file.deleteOnExit();
//                     length = (int) suArchiveBlob.length();
//                     fos = new FileOutputStream(file);
//                     fos.write(suArchiveBlob.getBytes(1L, length));
//                     zipFile = new ZipFile(file);
// //                        System.out.println("*** APH-I1 : Zip Entries " + zipFile.size());
//                     for (Enumeration<ZipEntry> e = (Enumeration<ZipEntry>) zipFile.entries(); e.hasMoreElements();) {
//                         zipEntry = e.nextElement();
//                         zipEntryName = zipEntry.getName();
// //                            System.out.println("*** APH-I2 : Name = " + zipEntryName);
//                         if (zipEntryName.endsWith(".bpel")) {
//                             bpelSB = new StringBuffer();
//                             is = zipFile.getInputStream(zipEntry);
//                             isr = new InputStreamReader(is);
//                             br = new BufferedReader(isr);
//                             while ((line = br.readLine()) != null) {
// //                                    System.out.println(line);
//                                 bpelSB.append(line + "\n");
//                             }
//                             idx = zipEntryName.lastIndexOf("/");
//                             bpelName = zipEntryName.substring(idx + 1);
// //                                System.out.println("*** APH-I3 : Zip Entry Name " + zipEntryName + " BPEL Name " + bpelName);
// //                                System.out.println("*** APH-I3 : BPEL String "+bpelSB.toString());
//                             bpelList.add(new BPELData(bpelName, bpelSB));
//                         }
//                     }
//                 } catch (Exception ex) {
//                     Logger.getLogger(BpelSvgRetriever.class.getName()).log(Level.SEVERE, null, ex);
//                 } finally {
//                     try {
//                         fos.close();
//                     } catch (Exception e) {
//                     }
//                     try {
//                         is.close();
//                     } catch (Exception e) {
//                     }
//                     file.delete();
//                 }
//             }
//         } catch (Exception ex) {
//             Logger.getLogger(BpelSvgRetriever.class.getName()).log(Level.SEVERE, null, ex);
//         }
//
//
//         return bpelList;
//     }
}
