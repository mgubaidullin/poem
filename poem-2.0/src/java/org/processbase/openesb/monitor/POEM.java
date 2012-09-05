package org.processbase.openesb.monitor;

/**
 *
 * @author mgubaidullin
 */
import javax.jms.Queue;
import org.processbase.openesb.monitor.ui.window.LoginWindow;
import org.processbase.openesb.monitor.ui.window.MainWindow;
import com.sun.caps.management.api.bpel.BPELManagementService;
import com.sun.caps.management.api.bpel.BPELManagementServiceFactory;
import com.sun.enterprise.tools.admingui.util.AMXUtil;
import com.sun.jbi.ui.client.JBIAdminCommandsClientFactory;
import com.sun.jbi.ui.common.JBIAdminCommands;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import com.vaadin.service.ApplicationContext.TransactionListener;

import com.vaadin.Application;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.terminal.gwt.server.WebApplicationContext;
import java.util.HashMap;
import java.util.Map;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.InitialContext;
import org.processbase.openesb.monitor.db.DBManager;

public class POEM extends Application implements TransactionListener {

    private static ThreadLocal<POEM> currentApplication = new ThreadLocal<POEM>();
    private MainWindow mainWindow;
    public JMXConnector jmxConnector;
    public MBeanServerConnection jmxMBeanServerConnection;
    public JBIAdminCommands jbiAdminCommands;
    public BPELManagementService bpelManagementService;
    public IndexedContainer targets = new IndexedContainer();
    public DBManager dbManager = new DBManager();
    public boolean isClusterSupported = false;
    

    @Override
    public void init() {
        setTheme("reindeermods");
        WebApplicationContext applicationContext = (WebApplicationContext) this.getContext();
        this.setLocale(applicationContext.getBrowser().getLocale());
        try {

            LoginWindow loginWindow = new LoginWindow();
            setMainWindow(loginWindow);
            loginWindow.address.setValue(this.getURL().getHost());

        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(POEM.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
//        if (!Constants.LOADED) {
//            Constants.loadConstants();
//        }
        if (getContext() != null) {
            getContext().addTransactionListener(this);
        }
    }

    private void prepareSharedUI() {
        if (POEM.getCurrent().isClusterSupported) {
            
        }
        
    }

    public void authenticate(String address, String port, String login, String password) throws NamingException, Exception {

        JMXServiceURL jmxUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + address + ":" + port + "/jmxrmi");
        Map environment = new HashMap();
        environment.put(JMXConnector.CREDENTIALS, new String[]{login, password});
        jmxConnector = JMXConnectorFactory.connect(jmxUrl, environment);
        jmxMBeanServerConnection = jmxConnector.getMBeanServerConnection();

        POEM.getCurrent().jbiAdminCommands = JBIAdminCommandsClientFactory.getInstance(jmxMBeanServerConnection);

        POEM.getCurrent().bpelManagementService = BPELManagementServiceFactory.getBPELManagementServiceLocal(jmxMBeanServerConnection);

        POEM.getCurrent().isClusterSupported = AMXUtil.supportCluster();

        
//        System.out.println("--------------------");
//        for (String jms : AMXUtil.getDomainConfig().getConnectorResourceConfigMap().keySet()){
//            InitialContext context = new InitialContext();
//            ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup(jms);
//            Destination destination = (Destination) context.lookup("jms/processbaseEventDR");
//
//            Connection conn = connectionFactory.createConnection();
//            Session session = conn.createSession(true, Session.SESSION_TRANSACTED);
//
//            QueueBrowser myBrowser = session.createBrowser((Queue) destination);
//            System.out.println(jms + " - " + connectionFactory.getClass().getCanonicalName());
//        }
//
//        for (String jms : AMXUtil.getDomainConfig().getAdminObjectResourceConfigMap().keySet()){
//            System.out.println(jms);
//
//        }

//        System.out.println(AMXUtil.getDomainConfig().getJDBCConnectionPoolConfigMap());

        prepareSharedUI();

        mainWindow = new MainWindow();
        setMainWindow(mainWindow);
    }

    /**
     * @return the current application instance
     */
    public static POEM getCurrent() {
        return currentApplication.get();
    }

    /**
     * Set the current application instance
     */
    public static void setCurrent(POEM application) {
        if (getCurrent() == null) {
            currentApplication.set(application);
        }
    }

    @Override
    public void transactionEnd(Application application, Object transactionData) {
        currentApplication.remove();
    }

    @Override
    public void transactionStart(Application application, Object transactionData) {
        POEM.setCurrent(this);

    }
}
