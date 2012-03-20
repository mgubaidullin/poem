/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.processbase.openesb.monitor.db;

import com.sun.caps.management.api.bpel.BPELManagementService;
import com.sun.caps.management.api.bpel.BPELManagementService.BPInstanceInfo;
import com.sun.caps.management.api.bpel.BPELManagementService.BPInstanceQueryResult;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;

public class Test {

    public static void main(String[] args) {
        Test test = new Test();
        test.findBPELInfo("{http://enterprise.netbeans.org/bpel/BpelModule1/bpelModule1}bpelModule1", "");
    }

    public BPInstanceQueryResult findBPELInfo(String bpelName, String clusterName) {
        BPInstanceQueryResult result = new BPInstanceQueryResult();
        result.bpInstnaceList = new ArrayList<BPInstanceInfo>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        File file = null;
        try {
            connection = getConnection();
            ps = connection.prepareStatement("SELECT * FROM MONITORBPELINSTANCE WHERE BPELID = ?");
            ps.setString(1, bpelName);
            rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
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

    public static Connection getConnection() {
        String jdbcURL = "jdbc:derby://localhost:1527/bpelseDB";
        Connection conn = null;
        ResultSet rs = null;
        String user = "USR2";
        String passwd = "openesb";

        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
            conn = DriverManager.getConnection(jdbcURL, user, passwd);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return conn;
    }
}
