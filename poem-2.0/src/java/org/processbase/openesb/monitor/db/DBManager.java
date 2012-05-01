package org.processbase.openesb.monitor.db;

import com.sun.caps.management.api.bpel.BPELManagementService;
import com.sun.caps.management.api.bpel.BPELManagementService.BPInstanceInfo;
import com.sun.caps.management.api.bpel.BPELManagementService.BPInstanceQueryResult;
import com.sun.caps.management.api.bpel.BPELManagementService.BPStatus;
import com.sun.caps.management.api.bpel.BPELManagementService.SortColumn;
import com.sun.caps.management.api.bpel.BPELManagementService.SortOrder;
import com.sun.jbi.ui.common.JBIAdminCommands;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.sql.rowset.serial.SerialClob;
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

    /** default process scope ID */
    private static final Long DEFAULT_PROCESS_SCOPE_ID = new Long(-2);
    /** default process branch ID */
    private static final Long DEFAULT_PROCESS_BRANCH_ID = new Long(-1);
    private static final Long NULL_FAULTED_SCOPE_ID = new Long(-3);
    private static final String SIMPLE_TYPE_NOTE_PREFIX = "Searchable. Type: ";
    private static final String BP_PROCESS = "/bpws:process";

    public enum ConnectionSource {

        CLUSTER, JDBC
    };
    public static String SQL_COUNT_ =
            "SELECT COUNT(1) PCOUNT , TO_CHAR(MI.STARTTIME, 'DD.MM.YYYY') PDATE, MI.BPELID, MI.STATUS" + " FROM MONITORBPELINSTANCE MI" + " WHERE STARTTIME >= ? " + " GROUP BY TO_CHAR(MI.STARTTIME, 'DD.MM.YYYY'), MI.BPELID, MI.STATUS " + "ORDER BY TO_CHAR(MI.STARTTIME, 'DD.MM.YYYY')";
    public static String SQL_COUNT_BY_STATUS =
            "SELECT count(1) PCOUNT, STATUS FROM MONITORBPELINSTANCE WHERE STARTTIME >= ? GROUP BY STATUS";
    private static final String GET_ACTIVITY_STATUS_BY_INSTANCE = "SELECT " + SchemaConstants.ACTIVITY_ID + ", " + SchemaConstants.ACTIVITY_XPATH + ", " + SchemaConstants.ITERATION + ", " + SchemaConstants.STATUS + ", " + SchemaConstants.START_TIME + ", " + SchemaConstants.END_TIME + " FROM MONITORBPELACTIVITY  WHERE " + SchemaConstants.INSTANCE_ID + " = ? ORDER BY " + SchemaConstants.START_TIME + " ASC";
    private static final String GET_PROCESS_SINGLE_SIMPLE_VAR_QUERY = "SELECT " + SchemaConstants.VAR_NAME + ", " + SchemaConstants.VAR_ID + ", " + SchemaConstants.VAR_TYPE + " FROM MONITORSIMPLEVARIABLE WHERE " + SchemaConstants.INSTANCE_ID + " = ? AND " + SchemaConstants.VAR_NAME + " = ? AND " + SchemaConstants.SCOPE_ID + " = " + DEFAULT_PROCESS_SCOPE_ID;
    private static final String GET_PROCESS_SINGLE_VAR_QUERY = "SELECT " + SchemaConstants.VAR_NAME + ", " + SchemaConstants.VAR_ID + " FROM MONITORBPELVARIABLE WHERE " + SchemaConstants.IS_FAULT + " = 'N' AND " + SchemaConstants.INSTANCE_ID + " = ? AND " + SchemaConstants.VAR_NAME + " = ? AND " + SchemaConstants.SCOPE_ID + " = " + DEFAULT_PROCESS_SCOPE_ID;
    private static final String GET_INNER_SIMPLE_VAR_COMMON = "SELECT v." + SchemaConstants.VAR_NAME + ", v." + SchemaConstants.VAR_ID + ", a." + SchemaConstants.ACTIVITY_XPATH + ", v." + SchemaConstants.VAR_TYPE + " FROM MONITORSIMPLEVARIABLE v, MONITORBPELACTIVITY a WHERE v." + SchemaConstants.SCOPE_ID + " = a." + SchemaConstants.ACTIVITY_ID + " AND v." + SchemaConstants.INSTANCE_ID + " = a." + SchemaConstants.INSTANCE_ID + " AND v." + SchemaConstants.INSTANCE_ID + " = ?";
    private static final String GET_INNER_SINGLE_SIMPLE_VAR_QUERY = GET_INNER_SIMPLE_VAR_COMMON + " AND v." + SchemaConstants.VAR_NAME + " = ? AND v." + SchemaConstants.SCOPE_ID + " <> " + DEFAULT_PROCESS_SCOPE_ID;
    private static final String GET_INNER_VAR_COMMON = "SELECT v." + SchemaConstants.VAR_NAME + ", v." + SchemaConstants.VAR_ID + ", a." + SchemaConstants.ACTIVITY_XPATH + " FROM MONITORBPELVARIABLE v, MONITORBPELACTIVITY a WHERE v." + SchemaConstants.SCOPE_ID + " = a." + SchemaConstants.ACTIVITY_ID + " AND v." + SchemaConstants.INSTANCE_ID + " = a." + SchemaConstants.INSTANCE_ID + " AND v." + SchemaConstants.IS_FAULT + " = 'N' AND v." + SchemaConstants.INSTANCE_ID + " = ?";
    private static final String GET_INNER_SINGLE_VAR_QUERY = GET_INNER_VAR_COMMON + " AND v." + SchemaConstants.VAR_NAME + " = ? AND v." + SchemaConstants.SCOPE_ID + " <> " + DEFAULT_PROCESS_SCOPE_ID;
    private static final String GET_INNER_SIMPLE_VAR_QUERY = GET_INNER_SIMPLE_VAR_COMMON + " AND v." + SchemaConstants.SCOPE_ID + " <> " + DEFAULT_PROCESS_SCOPE_ID;
    private static final String GET_PROCESS_SIMPLE_VAR_QUERY = "SELECT " + SchemaConstants.VAR_NAME + ", " + SchemaConstants.VAR_ID + ", " + SchemaConstants.VAR_TYPE + " FROM MONITORSIMPLEVARIABLE WHERE " + SchemaConstants.INSTANCE_ID + " = ? AND " + SchemaConstants.SCOPE_ID + " = " + DEFAULT_PROCESS_SCOPE_ID;
    private static final String GET_INNER_VAR_QUERY = GET_INNER_VAR_COMMON + " AND v." + SchemaConstants.IS_FAULT + " = 'N' AND v." + SchemaConstants.SCOPE_ID + " <> " + DEFAULT_PROCESS_SCOPE_ID;
    private static final String GET_PROCESS_VAR_QUERY = "SELECT " + SchemaConstants.VAR_NAME + ", " + SchemaConstants.VAR_ID + " FROM MONITORBPELVARIABLE WHERE " + SchemaConstants.IS_FAULT + " = 'N' AND " + SchemaConstants.INSTANCE_ID + " = ? AND " + SchemaConstants.SCOPE_ID + " = " + DEFAULT_PROCESS_SCOPE_ID;
    private static final String GET_SIMPLE_VARIABLE_QUERY = "SELECT " + SchemaConstants.STR_VALUE + " FROM MONITORSIMPLEVARIABLE WHERE " + SchemaConstants.INSTANCE_ID + " = ? AND " + SchemaConstants.VAR_ID + " = ?";
    private static final String GET_BPEL_VARIABLE_QUERY = "SELECT " + SchemaConstants.VAR_VALUE + " FROM MONITORBPELVARIABLE WHERE " + SchemaConstants.INSTANCE_ID + " = ? AND " + SchemaConstants.VAR_ID + " = ?";

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
            System.out.println("connectionSource = " + name);
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

    public List<BPELManagementService.ActivityStatus> getBPELInstanceActivityStatus(String instanceId, String name, ConnectionSource connectionSource) throws Exception {
        List<BPELManagementService.ActivityStatus> result = new ArrayList<BPELManagementService.ActivityStatus>();
        Connection connection = null;
        PreparedStatement ps = null;
        PreparedStatement query = null;
        ResultSet resultSet = null;
        try {
            connection = getConnection(name, connectionSource);

            query = connection.prepareStatement(GET_ACTIVITY_STATUS_BY_INSTANCE);
            query.setString(1, instanceId);
            resultSet = query.executeQuery();
            while (resultSet.next()) {
                Map<String, Object> bpinfo = new HashMap<String, Object>();
                bpinfo.put("activityId", Long.valueOf(resultSet.getLong(1)));
                bpinfo.put("activityXpath", resultSet.getString(2));
                bpinfo.put("iteration", resultSet.getInt(3));
                bpinfo.put("status", resultSet.getString(4));
                bpinfo.put("startTime", resultSet.getTimestamp(5));
                bpinfo.put("endTime", resultSet.getTimestamp(6));
                Timestamp lastTime = ((Timestamp) bpinfo.get("endTime") == null ? new Timestamp(
                        Calendar.getInstance().getTimeInMillis())
                        : (Timestamp) bpinfo.get("endTime"));
                float lasted = (float) ((lastTime.getTime() - ((Timestamp) bpinfo.get("startTime")).getTime()) / 1000.0);
                bpinfo.put("lasted", new Float(lasted));
                BPELManagementService.ActivityStatus activityStatus = new BPELManagementService.ActivityStatus();
                activityStatus.activityId = String.valueOf(resultSet.getLong(1));
                activityStatus.activityXpath = resultSet.getString(2);
                activityStatus.endTime = resultSet.getTimestamp(6);
                activityStatus.iteration = resultSet.getInt(3);
                activityStatus.lasted = (float) ((lastTime.getTime() - ((Timestamp) bpinfo.get("startTime")).getTime()) / 1000.0);
                activityStatus.startTime = resultSet.getTimestamp(5);
                activityStatus.status = BPELManagementService.ActivityStatus.Status.valueOf(resultSet.getString(4));
                result.add(activityStatus);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    public List<BPELManagementService.VarInfo> listBPELVaraibles(String instanceId, String varName, String name, ConnectionSource connectionSource) throws Exception {
        if (varName == null) {
            return listBPELVaraibles(instanceId, name, connectionSource);
        }

        PreparedStatement scopeStmt = null;
        PreparedStatement scopeSimpleStmt = null;
        PreparedStatement processStmt = null;
        PreparedStatement processSimpleStmt = null;
        ResultSet rs = null;

        List<BPELManagementService.VarInfo> result = new ArrayList<BPELManagementService.VarInfo>();
        Connection connection = null;

        try {
            connection = getConnection(name, connectionSource);

            // Simple variables NOT in default scope
            scopeSimpleStmt = connection.prepareStatement(GET_INNER_SINGLE_SIMPLE_VAR_QUERY);
            System.out.println(GET_INNER_SINGLE_SIMPLE_VAR_QUERY);
            scopeSimpleStmt.setString(1, instanceId);
            scopeSimpleStmt.setString(2, varName);
            rs = scopeSimpleStmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> varinfo = new HashMap<String, Object>();
                varinfo.put("varName", rs.getString(1));
                varinfo.put("varId", Long.valueOf(rs.getLong(2)));
                varinfo.put("xpath", rs.getString(3));
                String notes = SIMPLE_TYPE_NOTE_PREFIX + getVariableTypeFromCode(rs.getString(4));
                varinfo.put("notes", notes);
                result.add(getVarInfo(varinfo));
            }
            // Complex variables NOT in default scope
            scopeStmt = connection.prepareStatement(GET_INNER_SINGLE_VAR_QUERY);
            scopeStmt.setString(1, instanceId);
            scopeStmt.setString(2, varName);
            rs = scopeStmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> varinfo = new HashMap<String, Object>();
                varinfo.put("varName", rs.getString(1));
                varinfo.put("varId", Long.valueOf(rs.getLong(2)));
                varinfo.put("xpath", rs.getString(3));
                varinfo.put("notes", "");
                result.add(getVarInfo(varinfo));
            }
            // Simple variables in default scope
            processSimpleStmt = connection.prepareStatement(GET_PROCESS_SINGLE_SIMPLE_VAR_QUERY);
            processSimpleStmt.setString(1, instanceId);
            processSimpleStmt.setString(2, varName);
            rs = processSimpleStmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> varinfo = new HashMap<String, Object>();
                varinfo.put("varName", rs.getString(1));
                varinfo.put("varId", Long.valueOf(rs.getLong(2)));
                varinfo.put("xpath", BP_PROCESS);
                String notes = SIMPLE_TYPE_NOTE_PREFIX + getVariableTypeFromCode(rs.getString(3));
                varinfo.put("notes", notes);
                result.add(getVarInfo(varinfo));
            }
            // Complex variables in default scope
            processStmt = connection.prepareStatement(GET_PROCESS_SINGLE_VAR_QUERY);
            processStmt.setString(1, instanceId);
            processStmt.setString(2, varName);
            rs = processStmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> varinfo = new HashMap<String, Object>();
                varinfo.put("varName", rs.getString(1));
                varinfo.put("varId", Long.valueOf(rs.getLong(2)));
                varinfo.put("xpath", BP_PROCESS);
                varinfo.put("notes", "");
                result.add(getVarInfo(varinfo));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (scopeStmt != null) {
                    scopeStmt.close();
                }
                if (scopeSimpleStmt != null) {
                    scopeSimpleStmt.close();
                }
                if (processStmt != null) {
                    processStmt.close();
                }
                if (processSimpleStmt != null) {
                    processSimpleStmt.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        return result;
    }

    public List<BPELManagementService.VarInfo> listBPELVaraibles(String instanceId, String name, ConnectionSource connectionSource) throws Exception {
        PreparedStatement scopeStmt = null;
        PreparedStatement scopeSimpleStmt = null;
        PreparedStatement processStmt = null;
        PreparedStatement processSimpleStmt = null;
        ResultSet rs = null;

        List<BPELManagementService.VarInfo> result = new ArrayList<BPELManagementService.VarInfo>();
        Connection connection = null;

        try {
            connection = getConnection(name, connectionSource);

            // Simple variables NOT in default scope.
            scopeSimpleStmt = connection.prepareStatement(GET_INNER_SIMPLE_VAR_QUERY);
            scopeSimpleStmt.setString(1, instanceId);
            rs = scopeSimpleStmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> varinfo = new HashMap<String, Object>();
                varinfo.put("varName", rs.getString(1));
                varinfo.put("varId", Long.valueOf(rs.getLong(2)));
                varinfo.put("xpath", rs.getString(3));
                String notes = SIMPLE_TYPE_NOTE_PREFIX + getVariableTypeFromCode(rs.getString(4));
                varinfo.put("notes", notes);
                result.add(getVarInfo(varinfo));
            }
            // Complex variables NOT in default scope
            scopeStmt = connection.prepareStatement(GET_INNER_VAR_QUERY);
            scopeStmt.setString(1, instanceId);
            rs = scopeStmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> varinfo = new HashMap<String, Object>();
                varinfo.put("varName", rs.getString(1));
                varinfo.put("varId", Long.valueOf(rs.getLong(2)));
                varinfo.put("xpath", rs.getString(3));
                varinfo.put("notes", "");
                result.add(getVarInfo(varinfo));
            }
            // Simple variables in default scope
            processSimpleStmt = connection.prepareStatement(GET_PROCESS_SIMPLE_VAR_QUERY);
            processSimpleStmt.setString(1, instanceId);
            rs = processSimpleStmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> varinfo = new HashMap<String, Object>();
                varinfo.put("varName", rs.getString(1));
                varinfo.put("varId", Long.valueOf(rs.getLong(2)));
                varinfo.put("xpath", BP_PROCESS);
                String notes = SIMPLE_TYPE_NOTE_PREFIX + getVariableTypeFromCode(rs.getString(3));
                varinfo.put("notes", notes);
                result.add(getVarInfo(varinfo));
            }
            // Complex variables in default scope
            processStmt = connection.prepareStatement(GET_PROCESS_VAR_QUERY);
            processStmt.setString(1, instanceId);
            rs = processStmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> varinfo = new HashMap<String, Object>();
                varinfo.put("varName", rs.getString(1));
                varinfo.put("varId", Long.valueOf(rs.getLong(2)));
                varinfo.put("xpath", BP_PROCESS);
                varinfo.put("notes", "");
                result.add(getVarInfo(varinfo));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (scopeStmt != null) {
                    scopeStmt.close();
                }
                if (scopeSimpleStmt != null) {
                    scopeSimpleStmt.close();
                }
                if (processStmt != null) {
                    processStmt.close();
                }
                if (processSimpleStmt != null) {
                    processSimpleStmt.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        return result;
    }

    public String getVariableValue(String instanceId, Long varId, String name, ConnectionSource connectionSource) throws Exception {

        PreparedStatement query = null;
        PreparedStatement querySimple = null;
        ResultSet resultSet = null;
        Connection connection = null;

        try {
            connection = getConnection(name, connectionSource);

            querySimple = connection.prepareStatement(GET_SIMPLE_VARIABLE_QUERY);
            querySimple.setString(1, instanceId);
            querySimple.setLong(2, varId);
            resultSet = querySimple.executeQuery();
            if (resultSet.next()) {
                String obtained = resultSet.getString(1);
                return obtained;
            }
            // The variable was not found in the simple variable table. Do the query against the complex
            // variable table.
            query = connection.prepareStatement(GET_BPEL_VARIABLE_QUERY);
            query.setString(1, instanceId);
            query.setLong(2, varId);
            resultSet = query.executeQuery();
            if (resultSet.next()) {
                Clob obtained = resultSet.getClob(1);
                if (obtained != null) {
                    SerialClob clob = new SerialClob(obtained);
                    return getValue(clob);
                } else {
                    return null;
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (query != null) {
                    query.close();
                }
                if (querySimple != null) {
                    querySimple.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        return null;
    }

    private static String getValue(SerialClob clob) throws Exception {
        Reader retVal = null;
        try {
            retVal = clob.getCharacterStream();
            BufferedReader br = new BufferedReader(retVal);
//            boolean read = true;
            StringBuffer sb = new StringBuffer();
            String s = null;
            while ((s = br.readLine()) != null) {
                sb.append(s);
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            throw e;
        }
    }

    private BPELManagementService.VarInfo getVarInfo(Map<String, Object> varinfo) {
        BPELManagementService.VarInfo var = new BPELManagementService.VarInfo();
        var.varId = Long.valueOf(varinfo.get("varId").toString());
        var.varName = varinfo.get("varName").toString();
        var.xpath = varinfo.get("xpath").toString();
        var.notes = varinfo.get("notes").toString();
        return var;
    }

    private String getVariableTypeFromCode(String code) {
        if (code == null) {
            return "";
        } else if (code.equals(SimpleVarType.String.getCode())) {
            return SimpleVarType.String.toString();
        } else if (code.equals(SimpleVarType.Numeric.getCode())) {
            return SimpleVarType.Numeric.toString();
        } else if (code.equals(SimpleVarType.Boolean.getCode())) {
            return SimpleVarType.Boolean.toString();
        } else if (code.equals(SimpleVarType.Date.getCode())) {
            return SimpleVarType.Date.toString();
        }
        return "";
    }

    public enum SimpleVarType {

        String("S"),
        Numeric("N"),
        Boolean("B"),
        Date("D");
        String mCode;

        SimpleVarType(String code) {
            mCode = code;
        }

        public String getCode() {
            return mCode;
        }
    }
}
