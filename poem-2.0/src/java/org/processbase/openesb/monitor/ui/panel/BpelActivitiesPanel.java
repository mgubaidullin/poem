package org.processbase.openesb.monitor.ui.panel;

import com.sun.caps.management.api.bpel.BPELManagementService;
import com.sun.caps.management.api.bpel.BPELManagementService.ActivityStatus;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Window.Notification;
import com.vaadin.ui.themes.Reindeer;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.processbase.openesb.monitor.POEM;


import org.processbase.openesb.monitor.POEMConstants;
import org.processbase.openesb.monitor.db.DBManager;
import org.processbase.openesb.monitor.db.DBManager.ConnectionSource;
import org.processbase.openesb.monitor.ui.template.TableExecButton;
import org.processbase.openesb.monitor.ui.template.TreeTablePanel;
import org.processbase.openesb.monitor.ui.window.BpelInstanceWindow;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author mgubaidullin
 */
public class BpelActivitiesPanel extends TreeTablePanel implements Property.ValueChangeListener {

    private String instanceId = null;
    private String suName = null;
    private String bpelName = null;
    private String target;
    private ConnectionSource connectionSource;
    public HierarchicalContainer activitiesContainer = new HierarchicalContainer();
    private Document bpelDocument = null;
    private List<BPELManagementService.ActivityStatus> activities = null;
    private HashMap<BPELManagementService.ActivityStatus, String> actNames = new HashMap<BPELManagementService.ActivityStatus, String>();
    private Button btnExport = null;

    public BpelActivitiesPanel(String instanceId, String suName, String bpelName, String target, ConnectionSource connectionSource) {
        super("Activities");
        this.target = target;
        this.connectionSource = connectionSource;
        this.instanceId = instanceId;
        this.suName = suName;
        this.bpelName = bpelName;
        buttonBar.setHeight("30px");

        btnExport = new Button("Export", this);
        buttonBar.addComponent(btnExport);

        initContainer();
        initTableUI();
        prepareBPEL();
        refreshInstanceData();
    }

    public void initContainer() {
        activitiesContainer.addContainerProperty("id", String.class, null);
        activitiesContainer.addContainerProperty("type", TableExecButton.class, null);
        activitiesContainer.addContainerProperty("name", String.class, null);
        activitiesContainer.addContainerProperty("iteration", Integer.class, null);
        activitiesContainer.addContainerProperty("startTime", String.class, null);
        activitiesContainer.addContainerProperty("endTime", String.class, null);
        activitiesContainer.addContainerProperty("lasted", String.class, null);
        activitiesContainer.addContainerProperty("status", String.class, null);
        activitiesContainer.addContainerProperty("icon", ThemeResource.class, null);
    }

    @Override
    public void initTableUI() {
        treeTable.setContainerDataSource(activitiesContainer);
        treeTable.setItemIconPropertyId("icon");
        treeTable.setVisibleColumns(new Object[]{"type", "name", "id", "iteration", "startTime", "endTime", "lasted", "status"});
        treeTable.setColumnWidth("type", 100);
        treeTable.setColumnExpandRatio("name", 1);
        treeTable.setSortContainerPropertyId("startTime");
        treeTable.setSortAscending(false);
        treeTable.sort();
        treeTable.setFooterVisible(true);
    }

    public void refreshInstanceData() {
        activitiesContainer.removeAllItems();
        try {
            if (connectionSource.equals(ConnectionSource.CLUSTER)){
            activities = POEM.getCurrent().bpelManagementService.getBPELInstanceActivityStatus(instanceId, target);
            } else {
                activities = POEM.getCurrent().dbManager.getBPELInstanceActivityStatus(instanceId, target, connectionSource);
            }

            for (ActivityStatus activity : activities) {
                // prepare xpath
                XPathFactory xFactory = XPathFactory.newInstance();
                XPath xpath = xFactory.newXPath();
                XPathExpression expr = xpath.compile(activity.activityXpath.replace("bpws:", ""));
                Object result = expr.evaluate(bpelDocument, XPathConstants.NODESET);
                NodeList nodes = (NodeList) result;
                Node node = nodes.item(0);

//                System.out.println("node = " + node);
                Item woItem = activitiesContainer.addItem(activity);
//                System.out.println("activityId = " + activity.activityId);
                woItem.getItemProperty("id").setValue(activity.activityId);
                TableExecButton button = new TableExecButton(node.getNodeName(), "", null, node, this, POEMConstants.ACTION_INFO);
                woItem.getItemProperty("type").setValue(button);
                woItem.getItemProperty("icon").setValue(new ThemeResource("bpel/" + node.getNodeName() + "16.png"));

                if (node.getAttributes().getNamedItem("name") != null) {
                    woItem.getItemProperty("name").setValue(node.getAttributes().getNamedItem("name").getNodeValue());
                    actNames.put(activity, node.getAttributes().getNamedItem("name").getNodeValue());
                }

                woItem.getItemProperty("iteration").setValue(activity.iteration);
                woItem.getItemProperty("startTime").setValue(activity.startTime);
                woItem.getItemProperty("endTime").setValue(activity.endTime);
                woItem.getItemProperty("lasted").setValue(activity.lasted);
                woItem.getItemProperty("status").setValue(activity.status);
                activitiesContainer.setChildrenAllowed(node, false);
            }
            treeTable.setColumnFooter("name", "count = " + activities.size());
        } catch (Exception ex) {
            ex.printStackTrace();
            getWindow().showNotification("Error refreshInstanceData", ex.getMessage(), Notification.TYPE_ERROR_MESSAGE);
        }

        refreshBtn.setStyleName("reindeermods");
    }

    private void prepareBPEL() {
        try {
            // prepare document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//            factory.setNamespaceAware(true); 
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream is = POEM.getCurrent().dbManager.getBPEL(suName, bpelName.split("}")[1], target, DBManager.ConnectionSource.CLUSTER);
            if (is == null) {
                is = POEM.getCurrent().dbManager.findBPEL(bpelName.split("}")[1], target, DBManager.ConnectionSource.JDBC);
            }
            bpelDocument = builder.parse(is);

            System.out.println(bpelDocument.toString());

        } catch (Exception ex) {
            ex.printStackTrace();
            getWindow().showNotification("Error prepareBPEL", ex.getMessage(), Notification.TYPE_ERROR_MESSAGE);
        }
    }

    @Override
    public void buttonClick(ClickEvent event) {
        if (event.getButton().equals(refreshBtn)) {
            refreshInstanceData();
        } else if (event.getButton().equals(btnExport)) {
            ((BpelInstanceWindow)getWindow()).export();
        } else if (event.getButton() instanceof TableExecButton) {
            TableExecButton button = (TableExecButton) event.getButton();
            if (button.getAction() == POEMConstants.ACTION_INFO) {
                BpelInstanceWindow biw = (BpelInstanceWindow) getWindow();
                Node selectedNode = (Node) button.getTableValue();
                for (Object object : biw.getBpelModelPanel().getTreeTable().getItemIds()) {
                    Node node = (Node) object;
                    if (node.getNodeName().equals(selectedNode.getNodeName())) {
                        if (node.getAttributes().getNamedItem("name") != null && selectedNode.getAttributes().getNamedItem("name") != null && node.getAttributes().getNamedItem("name").getNodeValue().equals(selectedNode.getAttributes().getNamedItem("name").getNodeValue())) {
                            biw.getBpelModelPanel().getTreeTable().select(object);
                        } else if (node.getAttributes().getNamedItem("name") == null && selectedNode.getAttributes().getNamedItem("name") == null) {
                            biw.getBpelModelPanel().getTreeTable().select(object);
                        }
                    }
                }
                biw.getTabs().setSelectedTab(biw.getBpelModelPanel());
            }
        }
    }

    public void valueChange(ValueChangeEvent event) {
        refreshBtn.setStyleName(Reindeer.BUTTON_DEFAULT);
    }

    public List<ActivityStatus> getActivities() {
        return activities;
    }

    public HashMap<ActivityStatus, String> getActNames() {
        return actNames;
    }
}
