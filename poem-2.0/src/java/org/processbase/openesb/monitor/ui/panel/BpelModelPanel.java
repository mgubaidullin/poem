package org.processbase.openesb.monitor.ui.panel;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.themes.Reindeer;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.processbase.openesb.monitor.POEM;


import org.processbase.openesb.monitor.ui.template.TreeTablePanel;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author mgubaidullin
 */
public class BpelModelPanel extends TreeTablePanel implements Property.ValueChangeListener {

    private String instanceId = null;
    private String suName = null;
    private String bpelName = null;
    private String target;
    public HierarchicalContainer activitiesContainer = new HierarchicalContainer();
    private Document bpelDocument = null;

    public BpelModelPanel(String instanceId, String suName, String bpelName, String target) {
        super("Model");
        this.target = target;
        this.instanceId = instanceId;
        this.suName = suName;
        this.bpelName = bpelName;
        buttonBar.setHeight("30px");
        treeTable.setStyleName("specialtable");
        treeTable.setSelectable(true);
        treeTable.setMultiSelect(false);
        treeTable.setImmediate(true);

        initContainer();
        initTableUI();
        prepareBPEL();
        refreshInstanceData();
    }

    public void initContainer() {
//        activitiesContainer.addContainerProperty("id", String.class, null);
        activitiesContainer.addContainerProperty("type", String.class, null);
        activitiesContainer.addContainerProperty("name", String.class, null);
        activitiesContainer.addContainerProperty("attributes", String.class, null);
//        activitiesContainer.addContainerProperty("startTime", String.class, null);
//        activitiesContainer.addContainerProperty("endTime", String.class, null);
//        activitiesContainer.addContainerProperty("lasted", String.class, null);
//        activitiesContainer.addContainerProperty("status", String.class, null);
        activitiesContainer.addContainerProperty("icon", ThemeResource.class, null);
    }

    @Override
    public void initTableUI() {
        treeTable.setContainerDataSource(activitiesContainer);
        treeTable.setItemIconPropertyId("icon");
//        treeTable.setVisibleColumns(new Object[]{"type", "name", "id",  "iteration", "startTime", "endTime", "lasted", "status"});
        treeTable.setVisibleColumns(new Object[]{"type", "name", "attributes"});
        treeTable.setColumnWidth("type", 250);
        treeTable.setColumnWidth("name", 100);
        treeTable.setColumnExpandRatio("attributes", 1);
//        treeTable.setSortContainerPropertyId("startTime");
        treeTable.setSortDisabled(true);
//        treeTable.sort();
        treeTable.setFooterVisible(true);
    }

    public void refreshInstanceData() {
        activitiesContainer.removeAllItems();
        try {
            addRow(bpelDocument.getFirstChild(), null);
            treeTable.setColumnFooter("name", "count = " + activitiesContainer.size());
            treeTable.setPageLength(activitiesContainer.size());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // set children allowed
        for (Object itemId : activitiesContainer.getItemIds()) {
            if (activitiesContainer.hasChildren(itemId)) {
                activitiesContainer.setChildrenAllowed(itemId, true);
            } else {
                activitiesContainer.setChildrenAllowed(itemId, false);
            }
            treeTable.setCollapsed(itemId, false);
        }

        refreshBtn.setStyleName("reindeermods");
    }

    private void addRow(Node node, Node parentNode) {
//        System.out.println(node.getNodeName() + ",  parent=" + (parentNode != null ? parentNode.getNodeName() : "") + ", id=" + getNodeId(node));
        Item woItem = activitiesContainer.addItem(node);
        woItem.getItemProperty("type").setValue(node.getNodeName());
        ThemeResource themeResource = new ThemeResource("bpel_images/" + node.getNodeName().toUpperCase() + ".png");
        woItem.getItemProperty("icon").setValue(themeResource);
        if (node.getAttributes().getNamedItem("name") != null) {
            woItem.getItemProperty("name").setValue(node.getAttributes().getNamedItem("name").getNodeValue());
        }
        // set parent
        if (parentNode != null) {
            activitiesContainer.setParent(node, parentNode);
        }
        // set attributes|value
        if (node.getNodeName().equals("documentation")) {
            woItem.getItemProperty("attributes").setValue(node.getTextContent());
        } else {
            NamedNodeMap map = node.getAttributes();
            if (map.getLength() > 0) {
                StringBuilder attributes = new StringBuilder();
                for (int i = 0; i < map.getLength(); i++) {
                    attributes.append(map.item(i)).append(" ");
                }
                woItem.getItemProperty("attributes").setValue(attributes.toString());
            } else {
                if (node.getNodeName().equals("from") || node.getNodeName().equals("to")) {
                    woItem.getItemProperty("attributes").setValue(node.getTextContent());
                }
            }
        }

        if (node.hasChildNodes() && node.getChildNodes().getLength() > 0) {
            NodeList nodeList = node.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node subNode = nodeList.item(i);
                if (subNode.getNodeType() == Node.ELEMENT_NODE) {
                    addRow(subNode, node);
                }
            }
        }
    }

    private String getNodeId(Node node) {
        if (node.getAttributes().getNamedItem("name") != null) {
            return (node.getNodeName() + " " + node.getAttributes().getNamedItem("name"));
        } else {
            return (node.getNodeName());
        }
    }

    private void prepareBPEL() {
        try {
            // prepare document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//            factory.setNamespaceAware(true); // never forget this!
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream is = POEM.getCurrent().dbManager.getBPEL(suName, bpelName.split("}")[1], target);
            if (is == null) {
                is = POEM.getCurrent().dbManager.findBPEL(bpelName.split("}")[1], target);
            }
            bpelDocument = builder.parse(is);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void buttonClick(ClickEvent event) {

        if (event.getButton().equals(refreshBtn)) {
            refreshInstanceData();
        }
    }

    public void valueChange(ValueChangeEvent event) {
        refreshBtn.setStyleName(Reindeer.BUTTON_DEFAULT);
    }
}
