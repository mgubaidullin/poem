package org.processbase.openesb.monitor.ui.panel;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.processbase.openesb.monitor.ui.window.BpelInstanceWindow;
import com.sun.caps.management.api.bpel.BPELManagementService.BPInstanceInfo;
import com.sun.caps.management.api.bpel.BPELManagementService.BPInstanceQueryResult;
import com.sun.caps.management.api.bpel.BPELManagementService.BPStatus;
import com.sun.caps.management.api.bpel.BPELManagementService.SortColumn;
import com.sun.caps.management.api.bpel.BPELManagementService.SortOrder;
import com.sun.enterprise.tools.admingui.util.AMXUtil;
import com.sun.jbi.ui.common.ServiceAssemblyInfo;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.validator.IntegerValidator;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.DateField;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.PopupDateField;
import com.vaadin.ui.Select;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window.Notification;
import com.vaadin.ui.themes.Reindeer;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.management.ObjectName;
import org.processbase.openesb.monitor.POEM;
import org.processbase.openesb.monitor.POEMConstants;
import org.processbase.openesb.monitor.ui.template.TableExecButton;
import org.processbase.openesb.monitor.db.DBManager;


import org.processbase.openesb.monitor.db.NotificationEvent;
import org.processbase.openesb.monitor.ui.template.TablePanel;

/**
 *
 * @author mgubaidullin
 */
public class AlertDBPanel extends TablePanel implements Property.ValueChangeListener {

    private NativeSelect severitySelect = new NativeSelect("Severity");
    private TextField searchID = new TextField("Search ID");
    private TextField rowCount = new TextField("Row count", "10");
    private NativeSelect suSelect = new NativeSelect("Service Unit");
    private NativeSelect piSelect = new NativeSelect("BPEL ID");
    private GridLayout infoPanel = new GridLayout(9, 4);
    private NativeSelect jdbcSelect = new NativeSelect("JDBC Pool Resource");
    private Button refreshJdbcBtn = new Button();
    private PopupDateField startTime = new PopupDateField("Start time");
    private PopupDateField endTime = new PopupDateField("End time");
    private List<ServiceAssemblyInfo> serveceAssembliesInfoList;
    public IndexedContainer biContainer = new IndexedContainer();
    private String jndiJDBCPoolResourceName;

    public AlertDBPanel() {
        super("Alerts");
        buttonBar.setHeight("100px");

        jdbcSelect.setWidth("200px");
        jdbcSelect.setNullSelectionAllowed(false);
        jdbcSelect.setImmediate(true);
        jdbcSelect.addListener(new Property.ValueChangeListener() {

            public void valueChange(ValueChangeEvent event) {
                jndiJDBCPoolResourceName = (String) event.getProperty().getValue();
                refreshBtn.setStyleName(Reindeer.BUTTON_DEFAULT);
            }
        });
        infoPanel.addComponent(jdbcSelect, 2, 0);
        refreshJdbcBtn.setStyleName(Button.STYLE_LINK);
        refreshJdbcBtn.setDescription("Refresh jdbc list");
        refreshJdbcBtn.setIcon(new ThemeResource("icons/reload.png"));
        refreshJdbcBtn.addListener((Button.ClickListener) this);
        infoPanel.addComponent(refreshJdbcBtn, 3, 0);
        infoPanel.setComponentAlignment(refreshJdbcBtn, Alignment.MIDDLE_LEFT);


        suSelect.setWidth("250px");
        suSelect.setNullSelectionAllowed(false);
        suSelect.setImmediate(true);
        suSelect.addListener(new Property.ValueChangeListener() {

            public void valueChange(ValueChangeEvent event) {
                refreshBpelProcessIds();
                refreshBtn.setStyleName(Reindeer.BUTTON_DEFAULT);
            }
        });
        infoPanel.addComponent(suSelect, 4, 0);

        piSelect.setWidth("250px");
        piSelect.setNullSelectionAllowed(true);
        piSelect.setImmediate(true);
        piSelect.addListener((Property.ValueChangeListener) this);
        infoPanel.addComponent(piSelect, 5, 0, 6, 0);

        severitySelect.setContainerDataSource(getSeverityList());
        severitySelect.setItemCaptionMode(Select.ITEM_CAPTION_MODE_PROPERTY);
        severitySelect.setItemCaptionPropertyId("Name");
        severitySelect.setNullSelectionAllowed(true);
        severitySelect.setImmediate(true);
        severitySelect.addListener((Property.ValueChangeListener) this);
        infoPanel.addComponent(severitySelect, 5, 1);

        rowCount.addValidator(new IntegerValidator("Row count must be a number between 1 and 1000"));
        rowCount.setWidth("100px");
        rowCount.setImmediate(true);
        rowCount.addListener((Property.ValueChangeListener) this);
        infoPanel.addComponent(rowCount, 6, 1);

        searchID.setWidth("200px");
        infoPanel.addComponent(searchID, 7, 0, 7, 0);

        Calendar date = Calendar.getInstance();
        startTime.setResolution(DateField.RESOLUTION_MIN);
        startTime.setValue(date.getTime());
        infoPanel.addComponent(startTime, 7, 1, 7, 1);

        endTime.setResolution(DateField.RESOLUTION_MIN);
        date.add(Calendar.HOUR, -1);
        endTime.setValue(date.getTime());
        infoPanel.addComponent(endTime, 8, 1, 8, 1);

        infoPanel.setMargin(false);
        infoPanel.setSpacing(true);
        buttonBar.addComponent(infoPanel, 0);

        // should be after UI definition
        refreshJDBCPoolResourceList();

        initContainer();
        initTableUI();
    }

    public void initContainer() {
        biContainer.addContainerProperty("id", TableExecButton.class, null);
        biContainer.addContainerProperty("timestamp", Date.class, null);
        biContainer.addContainerProperty("physicalhostname", String.class, null);
        biContainer.addContainerProperty("componenttype", String.class, null);
        biContainer.addContainerProperty("componentname", String.class, null);
        biContainer.addContainerProperty("severity", String.class, null);
        biContainer.addContainerProperty("messagedetail", String.class, null);
        biContainer.addContainerProperty("deploymentname", String.class, null);
    }

    private void refreshJDBCPoolResourceList() {
        try {
            IndexedContainer jdbcContainer = new IndexedContainer();
            for (String jdbc : AMXUtil.getDomainConfig().getJDBCResourceConfigMap().keySet()) {
                jdbcContainer.addItem(jdbc);
            }
            jdbcSelect.setContainerDataSource(jdbcContainer);
            for (ObjectName objectName : POEM.getCurrent().jmxMBeanServerConnection.queryNames(null, null)) {
                if (objectName.getCanonicalName().equals("EventManagement:name=EventManagementControllerMBean")) {
                    jndiJDBCPoolResourceName = POEM.getCurrent().jmxMBeanServerConnection.getAttribute(objectName, "DBJndiName").toString();
                    jdbcSelect.setValue(jndiJDBCPoolResourceName);
                }
            }

        } catch (Exception ex) {
            Logger.getLogger(AlertDBPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }

    private IndexedContainer getSeverityList() {
        IndexedContainer severityContainer = new IndexedContainer();
        severityContainer.addContainerProperty("Name", String.class, null);
        severityContainer.addItem(1).getItemProperty("Name").setValue("CRITICAL");
        severityContainer.addItem(2).getItemProperty("Name").setValue("MAJOR");
        severityContainer.addItem(3).getItemProperty("Name").setValue("MINOR");
        severityContainer.addItem(4).getItemProperty("Name").setValue("WARNING");
        severityContainer.addItem(5).getItemProperty("Name").setValue("INFO");
        return severityContainer;
    }

    private void refreshBpelProcessIds() {
        piSelect.removeAllItems();
        try {
            if (suSelect.getValue() != null && jdbcSelect.getValue() != null) {
                HashMap<String, String> bpelList = POEM.getCurrent().dbManager.getBPELList(suSelect.getValue().toString(), jdbcSelect.getValue().toString(), DBManager.ConnectionSource.JDBC);
                for (String processId : bpelList.keySet()) {
                    Item item = piSelect.addItem(processId);
                    piSelect.setItemCaption(processId, bpelList.get(processId));
                }
                piSelect.setValue(piSelect.getItemIds().size() > 0 ? piSelect.getItemIds().toArray()[0] : null);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            getWindow().showNotification("Error", ex.getMessage(), Notification.TYPE_ERROR_MESSAGE);
        }

    }

    @Override
    public void initTableUI() {
        table.setContainerDataSource(biContainer);
        table.setColumnWidth("id", 70);
        table.setColumnWidth("messagedetail", 200);
//        table.setColumnExpandRatio("id", 1);
//        table.setSortContainerPropertyId("startTime");
//        table.setSortAscending(false);
//        table.sort();
        table.setFooterVisible(true);
    }

    public void refreshProcessesData() {
        biContainer.removeAllItems();
        try {
            List<NotificationEvent> events = POEM.getCurrent().dbManager.getNotificationEvents((Integer) severitySelect.getValue(), jndiJDBCPoolResourceName, Integer.parseInt(rowCount.getValue().toString()));
            for (NotificationEvent event : events) {
                Item woItem = biContainer.addItem(event);
                woItem.getItemProperty("id").setValue(new TableExecButton(event.getId().toString(), "", null, event, this, POEMConstants.ACTION_INFO));
                woItem.getItemProperty("timestamp").setValue(new Date(event.getTimestamp()));
                woItem.getItemProperty("physicalhostname").setValue(event.getPhysicalhostname());
                woItem.getItemProperty("componenttype").setValue(event.getComponenttype());
                woItem.getItemProperty("componentname").setValue(event.getComponentname());
                woItem.getItemProperty("severity").setValue(event.getSeverity());
                woItem.getItemProperty("messagedetail").setValue(event.getMessagedetail());
                woItem.getItemProperty("deploymentname").setValue(event.getDeploymentname());
            }
            table.setColumnFooter("id", "alerts count = " + table.size());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        refreshBtn.setStyleName("reindeermods");
    }

    public TableExecButton getExecBtn(String description, String iconName, Object t, int action) {
        TableExecButton execBtn = new TableExecButton(description, iconName, t, this, action);
        execBtn.setEnabled(false);
        BPInstanceInfo info = (BPInstanceInfo) t;
        if (execBtn.getAction() == POEMConstants.ACTION_RESUME && info.status.equals(BPStatus.SUSPENDED)) {
            execBtn.setEnabled(true);
        } else if (execBtn.getAction() == POEMConstants.ACTION_SUSPEND && info.status.equals(BPStatus.RUNNING)) {
            execBtn.setEnabled(true);
        } else if (execBtn.getAction() == POEMConstants.ACTION_TERMINATE && info.status.equals(BPStatus.RUNNING)) {
            execBtn.setEnabled(true);
        } else if (execBtn.getAction() == POEMConstants.ACTION_TERMINATE && info.status.equals(BPStatus.SUSPENDED)) {
            execBtn.setEnabled(true);
        }
        return execBtn;
    }

    @Override
    public void buttonClick(ClickEvent event) {
        try {

            if (event.getButton().equals(refreshBtn)) {
                refreshProcessesData();
            } else if (event.getButton() instanceof TableExecButton) {
                TableExecButton teb = (TableExecButton) event.getButton();
                addBpelInstanceWindow((BPInstanceInfo) teb.getTableValue(), jdbcSelect.getValue().toString());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            getWindow().showNotification("Error Open BPEL", ex.getMessage(), Notification.TYPE_ERROR_MESSAGE);
        }
    }

    private void refreshInstance(BPInstanceInfo info, BPStatus status) {
        Item woItem = biContainer.getItem(info);
        woItem.getItemProperty("status").setValue(status);
    }

    private void addBpelInstanceWindow(BPInstanceInfo info, String target) {
        BpelInstanceWindow bpelInstanceWindow = null;
        bpelInstanceWindow = new BpelInstanceWindow(info, suSelect.getValue().toString(), target, DBManager.ConnectionSource.JDBC);
        bpelInstanceWindow.setWidth("90%");
        bpelInstanceWindow.setHeight("90%");
        bpelInstanceWindow.setResizable(false);
        getWindow().addWindow(bpelInstanceWindow);
    }

    public void valueChange(ValueChangeEvent event) {
        refreshBtn.setStyleName(Reindeer.BUTTON_DEFAULT);
    }
}
