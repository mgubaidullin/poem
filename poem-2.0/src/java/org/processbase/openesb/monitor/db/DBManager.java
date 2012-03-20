package org.processbase.openesb.monitor.db;

import com.sun.caps.management.api.bpel.BPELManagementService;
import com.sun.caps.management.api.bpel.BPELManagementService.BPInstanceInfo;
import com.sun.caps.management.api.bpel.BPELManagementService.BPInstanceQueryResult;
import com.sun.caps.management.api.bpel.BPELManagementService.BPStatus;
import com.sun.caps.management.api.bpel.BPELManagementService.SortColumn;
import com.sun.caps.management.api.bpel.BPELManagementService.SortOrder;
import com.sun.jbi.ui.common.JBIAdminCommands;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
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
//        System.out.println("DEBUG GETBPEL " + suName + " " +bpelName + " " + clusterName);
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
//                        System.out.println(zipEntry.getName());
                        if (zipEntry.getName().equals("/sun-bpel-engine/" + bpelName + ".bpel")) {
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

    public InputStream findBPEL(String bpelName, String clusterName) {
//        System.out.println("DEBUG FINDBPEL " + " " +bpelName + " " + clusterName);
        InputStream result = null;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        File file = null;
        try {
            connection = getConnection(clusterName);
            ps = connection.prepareStatement("SELECT SUZIPARCHIVE FROM SERVICEUNIT");
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
//                        System.out.println(zipEntry.getName());
                        if (zipEntry.getName().equals("/sun-bpel-engine/" + bpelName + ".bpel")) {
                            result = zipFile.getInputStream(zipEntry);
                            break;
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

    public BPInstanceQueryResult getBPELInstances(
            String bpelName, BPStatus bPStatus, String searchID, Integer rowCount,
            SortColumn sortColumn, SortOrder sortOrder, String clusterName) {
        return getBPELInstances(bpelName, bPStatus, searchID, rowCount, sortColumn, sortOrder, clusterName, null, null);
    }

    public BPInstanceQueryResult getBPELInstances(
            String bpelName, BPStatus bPStatus, String searchID, Integer rowCount,
            SortColumn sortColumn, SortOrder sortOrder, String clusterName, Timestamp startTime, Timestamp endTime) {
        BPInstanceQueryResult result = new BPInstanceQueryResult();
        result.bpInstnaceList = new ArrayList<BPInstanceInfo>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        File file = null;
        try {
            ArrayList<Object> params = new ArrayList<Object>();
            connection = getConnection(clusterName);
            StringBuilder select = new StringBuilder("SELECT * FROM MONITORBPELINSTANCE ");
            if (bpelName != null || searchID != null || bPStatus != null || startTime != null || endTime != null) {
                select.append(" WHERE ");
            }
            if (bpelName != null) {
                select.append(params.size() > 0 ? " AND " : "").append("BPELID = ?");
                params.add(bpelName);
            }
            if (searchID != null) {
                select.append(params.size() > 0 ? " AND " : "").append("INSTANCEID = ?");
                params.add(searchID);
            }
            if (bPStatus != null) {
                select.append(params.size() > 0 ? " AND " : "").append("STATUS = ?");
                params.add(bPStatus.toString());
            }
            if (startTime != null) {
                select.append(params.size() > 0 ? " AND " : "").append("STARTTIME >= ?");
                params.add(startTime);
            }
            if (endTime != null) {
                select.append(params.size() > 0 ? " AND " : "").append("ENDTIME <= ?");
                params.add(endTime);
            }
            select.append(" ORDER BY ").append(sortColumn.toString()).append(" ").append(sortOrder.toString());
            System.out.println(select.toString());
            ps = connection.prepareStatement(select.toString());
            for (int x = 1; x < params.size() + 1; x++) {
                Object value = params.get(x - 1);
                System.out.println(value);
                if (value instanceof String){
                    ps.setString(x, (String) params.get(x - 1));
                } else if (value instanceof Timestamp){
                    ps.setTimestamp(x, (Timestamp) params.get(x - 1));
                }
            }
            rs = ps.executeQuery();
            if (rs != null) {
                int i = 0;
                while (i < rowCount && rs.next()) {
                    BPInstanceInfo bpinfo = new BPInstanceInfo();
                    bpinfo.id = rs.getString(2);
                    bpinfo.bpelId = rs.getString(3);
                    bpinfo.status = BPELManagementService.BPStatus.valueOf(rs.getString(4));
                    bpinfo.startTime = rs.getTimestamp(5);
                    bpinfo.endTime = rs.getTimestamp(6);
                    bpinfo.lastUpdateTime = rs.getTimestamp(7);
                    Timestamp lastTime = (((Timestamp) bpinfo.endTime == null) ? new Timestamp(Calendar.getInstance().getTimeInMillis()) : (Timestamp) bpinfo.endTime);
                    bpinfo.lasted = (float) ((lastTime.getTime() - ((Timestamp) bpinfo.startTime).getTime()) / 1000.0);
                    result.bpInstnaceList.add(bpinfo);
                    i++;
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
}
