package org.processbase.openesb.monitor.db;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 *
 * @author marat
 */
public class DBManager {

    public Connection getConnection() {
        Connection con = null;
        try {
            InitialContext context = new InitialContext();
            //Look up our data source
            DataSource ds = (DataSource) context.lookup("jdbc/bpelseNonXA");
            //Allocate and use a connection from the pool
            con = ds.getConnection();
        } catch (SQLException e) {
            Logger.getLogger(DBManager.class.getName()).log(Level.SEVERE, e.getMessage());
            return null;
        } catch (Exception e) {
            Logger.getLogger(DBManager.class.getName()).log(Level.SEVERE, e.getMessage());
        }
        return con;
    }

    public void getBPEL(String suName) {
        System.out.println("DEBUG GETBPEL ");
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = this.getConnection();
            ps = connection.prepareStatement("SELECT SUZIPARCHIVE FROM SERVICEUNIT WHERE SUNAME = ?");
            ps.setString(1, suName);
            ResultSet rs = null;
            rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    Blob blob = rs.getBlob(1);
                    System.out.println("blob.length() = " + blob.length());
                }
                rs.close();
            }

        } catch (SQLException ex) {
            Logger.getLogger(DBManager.class.getName()).log(Level.SEVERE, ex.getMessage());
        } finally {
            try {
                ps.close();
                connection.close();
            } catch (SQLException ex) {
                Logger.getLogger(DBManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
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
