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
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.processbase.openesb.monitor.POEM;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 *
 * @author marat
 */
public class DBManager {

    public enum ConnectionSource {

        CLUSTER, JDBC
    };
    public static String SQL_COUNT_ =
            "SELECT COUNT(1) PCOUNT , TO_CHAR(MI.STARTTIME, 'DD.MM.YYYY') PDATE, MI.BPELID, MI.STATUS" + " FROM MONITORBPELINSTANCE MI" + " WHERE STARTTIME >= ? " + " GROUP BY TO_CHAR(MI.STARTTIME, 'DD.MM.YYYY'), MI.BPELID, MI.STATUS " + "ORDER BY TO_CHAR(MI.STARTTIME, 'DD.MM.YYYY')";
    public static String SQL_COUNT_BY_STATUS =
            "SELECT count(1) PCOUNT, STATUS FROM MONITORBPELINSTANCE WHERE STARTTIME >= ? GROUP BY STATUS";

    public Connection getConnectionByJdbcPoolName(String jdbcName) {
        Connection con = null;
        try {
            InitialContext context = new InitialContext();
            DataSource ds = (DataSource) context.lookup(jdbcName);
            con = ds.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return con;
    }

    public Connection getConnection(String name, ConnectionSource connectionSource) {
        Connection con = null;
        try {
            InitialContext context = new InitialContext();
            //Look up our data source
            String bpelDatabaseJNDI = null;
            switch (connectionSource) {
                case CLUSTER:
                    if (POEM.getCurrent().isClusterSupported) {
                        bpelDatabaseJNDI = POEM.getCurrent().jbiAdminCommands.getComponentConfiguration("sun-bpel-engine", name).getProperty("DatabaseNonXAJNDIName");
                    } else {
                        bpelDatabaseJNDI = POEM.getCurrent().jbiAdminCommands.getComponentConfiguration("sun-bpel-engine", JBIAdminCommands.SERVER_TARGET_KEY).getProperty("DatabaseNonXAJNDIName");
                    }
                    break;
                case JDBC:
                    bpelDatabaseJNDI = name;
                    break;
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

    public InputStream getBPEL(String suName, String bpelName, String name, ConnectionSource connectionSource) {
//        System.out.println("DEBUG GETBPEL " + suName + " " +bpelName + " " + clusterName);
        InputStream result = null;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        File file = null;
        try {
            connection = getConnection(name, connectionSource);
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

    public InputStream findBPEL(String bpelName, String name, ConnectionSource connectionSource) {
//        System.out.println("DEBUG FINDBPEL " + " " +bpelName + " " + clusterName);
        InputStream result = null;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        File file = null;
        try {
            connection = getConnection(name, connectionSource);
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
            SortColumn sortColumn, SortOrder sortOrder, String name, ConnectionSource connectionSource) {
        return getBPELInstances(bpelName, bPStatus, searchID, rowCount, sortColumn, sortOrder, name, null, null, connectionSource);
    }

    public BPInstanceQueryResult getBPELInstances(
            String bpelName, BPStatus bPStatus, String searchID, Integer rowCount,
            SortColumn sortColumn, SortOrder sortOrder, String name, Timestamp startTime, Timestamp endTime, ConnectionSource connectionSource) {
        BPInstanceQueryResult result = new BPInstanceQueryResult();
        result.bpInstnaceList = new ArrayList<BPInstanceInfo>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        File file = null;
        try {
            ArrayList<Object> params = new ArrayList<Object>();
            connection = getConnection(name, connectionSource);
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
                if (value instanceof String) {
                    ps.setString(x, (String) params.get(x - 1));
                } else if (value instanceof Timestamp) {
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

    public ArrayList<String> getSUList(String name, ConnectionSource connectionSource) throws SQLException {
        System.out.println("DEBUG getSUList ");
        ArrayList<String> result = new ArrayList<String>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        File file = null;
        try {
            connection = getConnection(name, connectionSource);
            ps = connection.prepareStatement("SELECT SUNAME FROM SERVICEUNIT");
            rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    result.add(rs.getString("SUNAME"));
                }
                rs.close();
            }
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

    public HashMap<String, String> getBPELList(String suName, String name, ConnectionSource connectionSource) {
//        System.out.println("DEBUG FINDBPEL " + " " +bpelName + " " + clusterName);
        HashMap<String, String> result = new HashMap<String, String>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        File file = null;
        try {
            connection = getConnection(name, connectionSource);
            ps = connection.prepareStatement("SELECT SUZIPARCHIVE FROM SERVICEUNIT WHERE SUNAME = ?");
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
                        if (zipEntry.getName().endsWith(".bpel")) {
                            String bpelName = zipEntry.getName().replace("/sun-bpel-engine/", "").replace(".bpel", "");
                            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                            DocumentBuilder builder = factory.newDocumentBuilder();
                            InputStream is = zipFile.getInputStream(zipEntry);
                            Document bpelDocument = builder.parse(is);
                            NodeList process = bpelDocument.getElementsByTagName("process");
                            StringBuilder bpelId = new StringBuilder();
                            bpelId.append("{").append(process.item(0).getAttributes().getNamedItem("targetNamespace").getNodeValue()).append("}");
                            bpelId.append(bpelName);
                            System.out.println(bpelId.toString());
                            result.put(bpelId.toString(), bpelName);
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
}
