/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.processbase.openesb.monitor;

import com.sun.caps.management.api.bpel.BPELManagementService;
import com.sun.caps.management.api.bpel.BPELManagementServiceFactory;
import com.sun.jbi.ui.client.JBIAdminCommandsClientFactory;
import com.sun.jbi.ui.common.JBIAdminCommands;
import com.sun.jbi.ui.common.ServiceAssemblyInfo;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 *
 * @author marat
 */
public class Test {

    public static void main(String[] args) throws Exception {
        JMXServiceURL jmxUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + "10.2.3.19" + ":" + "8686" + "/jmxrmi");
        Map environment = new HashMap();
        environment.put(JMXConnector.CREDENTIALS, new String[]{"admin", "adminadmin"});
        JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxUrl, environment);
        MBeanServerConnection jmxMBeanServerCon = jmxConnector.getMBeanServerConnection();

        JBIAdminCommands jbiAdminCommands = JBIAdminCommandsClientFactory.getInstance(jmxMBeanServerCon);

        BPELManagementService bpelManagementService = BPELManagementServiceFactory.getBPELManagementServiceLocal(jmxMBeanServerCon);

        String xmlQueryResults = jbiAdminCommands.listServiceAssemblies("sun-bpel-engine", JBIAdminCommands.DOMAIN_TARGET_KEY);
        List x = ServiceAssemblyInfo.readFromXmlTextWithProlog(xmlQueryResults);
        System.out.println(
                bpelManagementService.getBPELProcessIds("*", null));
    }
}
