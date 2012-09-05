/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.processbase.openesb.monitor;

import com.sun.enterprise.tools.admingui.util.AMXUtil;
import com.sun.jbi.ui.client.JBIAdminCommandsClientFactory;
import com.sun.jbi.ui.common.JBIAdminCommands;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author marat
 */
public class Test {

    public static void main(String[] args) throws Exception {
//        JMXServiceURL jmxUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + "esbdvl.hq.bc" + ":" + "8686" + "/jmxrmi");
        JMXServiceURL jmxUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:8686/jmxrmi");
        Map environment = new HashMap();
        environment.put(JMXConnector.CREDENTIALS, new String[]{"admin", "adminadmin"});
        JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxUrl, environment);
        MBeanServerConnection jmxMBeanServerCon = jmxConnector.getMBeanServerConnection();

        System.out.println(new Date(1346790916431L));


        for (ObjectName objectName : jmxMBeanServerCon.queryNames(null, null)) {
            if (objectName.getCanonicalName().equals("EventManagement:name=EventManagementControllerMBean")) {
                System.out.println(jmxMBeanServerCon.getAttribute(objectName, "DBJndiName"));
                System.out.println(jmxMBeanServerCon.getAttribute(objectName, "EMuniqueTableName"));
            }
        }


//        JBIAdminCommands jbiAdminCommands = JBIAdminCommandsClientFactory.getInstance(jmxMBeanServerCon);
//        System.out.println(jbiAdminCommands.getComponentConfiguration("sun-bpel-engine", "esb-clt4").getProperty("DatabaseNonXAJNDIName"));

//        Test test = new Test();
//        test.parce();
//        test.hierarchy();

//        test.getBPEL("AutoPayPaymentComposite-AutoPayPaymentBPEL", "autopayPaymentKCELL");

    }

    private void hierarchy() {
        FileInputStream fis = null;
        try {
            File file = new File("/home/marat/development/openesb/poem-1.0/autopayPaymentKCELL.bpel");
            fis = new FileInputStream(file);
            byte[] bytes = new byte[Long.valueOf(file.length()).intValue()];
            fis.read(bytes);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//            factory.setNamespaceAware(true); // never forget this!
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(bais);

//            System.out.println(doc.getFirstChild().getn);

            printNode(doc.getFirstChild(), null);


        } catch (Exception ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void printNode(Node node, Node parentNode) {
        System.out.println(node.getNodeName() + ",  atrs=" + node.getAttributes().getLength());
        NamedNodeMap map = node.getAttributes();
        if (map.getLength() > 0) {
            for (int i = 0; i < map.getLength(); i++) {
                System.out.println("      " + map.item(i));
            }
        } else {
            System.out.println("      " + node.getTextContent());
        }

        if (node.hasChildNodes()) {

            NodeList nodeList = node.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node subNode = nodeList.item(i);
                if (subNode.getNodeType() == Node.ELEMENT_NODE) {
                    printNode(subNode, node);
                }
            }
        }
    }

    private void parce() {
        FileInputStream fis = null;
        try {
            File file = new File("/home/marat/development/openesb/poem-1.0/autopayPaymentKCELL.bpel");
//            File file = new File("/home/marat/development/openesb/poem-1.0/test.xml");
            fis = new FileInputStream(file);
            byte[] bytes = new byte[Long.valueOf(file.length()).intValue()];
            fis.read(bytes);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//            factory.setNamespaceAware(true); // never forget this!
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(bais);

            System.out.println(doc.getFirstChild().getChildNodes().getLength());

            XPathFactory xFactory = XPathFactory.newInstance();
            XPath xpath = xFactory.newXPath();
            XPathExpression expr = xpath.compile("/bpws:process/bpws:sequence/bpws:if/bpws:else/bpws:assign".replace("bpws:", ""));
            Object result = expr.evaluate(doc, XPathConstants.NODESET);

            NodeList nodes = (NodeList) result;

            System.out.println(nodes.item(0).getAttributes().getNamedItem("name").getNodeValue());
            System.out.println(nodes.item(0).getAttributes().getNamedItem("name").getNodeValue());

            for (int i = 0; i < nodes.getLength(); i++) {
                System.out.println(nodes.item(i).getAttributes().getNamedItem("name").getNodeValue());
            }

        } catch (Exception ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private static Connection getConnection() {
        Connection connection = null;
        try {
            String driverName = "oracle.jdbc.driver.OracleDriver";
            Class.forName(driverName);

            String serverName = "localhost";
            String portNumber = "1521";
            String sid = "maratdb";
            String url = "jdbc:oracle:thin:@" + serverName + ":" + portNumber + ":" + sid;
            String username = "openesb";
            String password = "openesb";
            connection = DriverManager.getConnection(url, username, password);
            return connection;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    public InputStream getBPEL(String suName, String bpelName) {
        System.out.println("DEBUG GETBPEL");
        InputStream result = null;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        File file = null;
        try {
            connection = getConnection();
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
                            System.out.println(zipEntry.getName());
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
}
