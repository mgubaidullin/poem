package org.processbase.openesb.monitor.ui.panel;

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
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window.Notification;
import com.vaadin.ui.themes.Reindeer;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.processbase.openesb.monitor.POEM;
import org.processbase.openesb.monitor.POEMConstants;
import org.processbase.openesb.monitor.ui.template.TableExecButton;
import org.processbase.openesb.monitor.db.DBManager;


import org.processbase.openesb.monitor.ui.template.TablePanel;

/**
 *
 * @author mgubaidullin
 */
public class BpelProcessesDBHistoryPanel extends TablePanel implements Property.ValueChangeListener {

    private NativeSelect statusSelect = new NativeSelect("Status");
    private TextField searchID = new TextField("Search ID");
    private TextField rowCount = new TextField("Row count", "10");
    private NativeSelect suSelect = new NativeSelect("Service Unit");
    private NativeSelect piSelect = new NativeSelect("BPEL ID");
    private GridLayout infoPanel = new GridLayout(9, 4);
    private NativeSelect jdbcSelect = new NativeSelect("JDBC Pool Resource");
    private NativeSelect sortColumnSelect = new NativeSelect("Sort Column");
    private NativeSelect sortOrderSelect = new NativeSelect("Sort Order");
    private PopupDateField startTime = new PopupDateField("Start time");
    private PopupDateField endTime = new PopupDateField("End time");
    private Button refreshJdbcBtn = new Button();
    private List<ServiceAssemblyInfo> serveceAssembliesInfoList;
    public IndexedContainer biContainer = new IndexedContainer();

    public BpelProcessesDBHistoryPanel() {
        super("BPEL History (BETA)");
        buttonBar.setHeight("100px");

        jdbcSelect.setWidth("150px");
        jdbcSelect.setNullSelectionAllowed(false);
        jdbcSelect.setImmediate(true);
        jdbcSelect.addListener(new Property.ValueChangeListener() {

            public void valueChange(ValueChangeEvent event) {
                refreshBpelServiceUnits();
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

        sortColumnSelect.setWidth("100px");
        sortColumnSelect.setNullSelectionAllowed(false);
        sortColumnSelect.setImmediate(true);
        sortColumnSelect.addListener((Property.ValueChangeListener) this);
        for (SortColumn sortColumn : SortColumn.values()) {
            sortColumnSelect.addItem(sortColumn);
        }
        sortColumnSelect.setValue(sortColumnSelect.getItemIds().toArray()[0]);
        infoPanel.addComponent(sortColumnSelect, 2, 1);

        sortOrderSelect.setWidth("100px");
        sortOrderSelect.setNullSelectionAllowed(false);
        sortOrderSelect.setImmediate(true);
        sortOrderSelect.addListener((Property.ValueChangeListener) this);
        for (SortOrder sortOrder : SortOrder.values()) {
            sortOrderSelect.addItem(sortOrder);
        }
        sortOrderSelect.setValue(SortOrder.DESC);
        infoPanel.addComponent(sortOrderSelect, 4, 1);

        statusSelect.addItem(BPStatus.RUNNING);
        statusSelect.addItem(BPStatus.COMPLETED);
        statusSelect.addItem(BPStatus.FAULTED);
        statusSelect.addItem(BPStatus.SUSPENDED);
        statusSelect.addItem(BPStatus.TERMINATED);
        statusSelect.setNullSelectionAllowed(true);
        statusSelect.setImmediate(true);
        statusSelect.addListener((Property.ValueChangeListener) this);
        infoPanel.addComponent(statusSelect, 5, 1);

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
        biContainer.addContainerProperty("bpelId", String.class, null);
        biContainer.addContainerProperty("startTime", String.class, null);
        biContainer.addContainerProperty("endTime", String.class, null);
        biContainer.addContainerProperty("lasted", String.class, null);
        biContainer.addContainerProperty("status", String.class, null);
    }

    private void refreshJDBCPoolResourceList() {
        IndexedContainer jdbcContainer = new IndexedContainer();
        try {
            for (String jdbc : AMXUtil.getDomainConfig().getJDBCResourceConfigMap().keySet()) {
                jdbcContainer.addItem(jdbc);
            }
            jdbcSelect.setContainerDataSource(jdbcContainer);
            jdbcSelect.setValue(jdbcContainer.getIdByIndex(0));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void refreshBpelServiceUnits() {
        try {
            suSelect.removeAllItems();
            IndexedContainer serveceUnitsContainer = new IndexedContainer();
            if (jdbcSelect.getValue() != null) {
                for (String suname : POEM.getCurrent().dbManager.getSUList(jdbcSelect.getValue().toString(), DBManager.ConnectionSource.JDBC)) {
                    serveceUnitsContainer.addItem(suname);
                }
                suSelect.setContainerDataSource(serveceUnitsContainer);
                suSelect.setValue(suSelect.getItemIds().size() > 0 ? suSelect.getItemIds().toArray()[0] : null);
            }
        } catch (SQLException sqlEx) {
            POEM.getCurrent().getMainWindow().showNotification("ERROR", sqlEx.getMessage(), Notification.TYPE_ERROR_MESSAGE);
        }
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
        table.setColumnExpandRatio("bpelId", 1);
//        table.setSortContainerPropertyId("startTime");
//        table.setSortAscending(false);
//        table.sort();
        table.setFooterVisible(true);
    }

    public void refreshProcessesData() {
        biContainer.removeAllItems();
        try {
            BPInstanceQueryResult instances = null;

            if (searchID.getValue() != null && !searchID.getValue().toString().isEmpty()) {
                instances =
                        POEM.getCurrent().dbManager.getBPELInstances(
                        piSelect.getValue() != null ? piSelect.getValue().toString() : null,
                        (BPStatus) statusSelect.getValue(),
                        (String) searchID.getValue(),
                        new Integer(rowCount.getValue().toString()),
                        (SortColumn) sortColumnSelect.getValue(),
                        (SortOrder) sortOrderSelect.getValue(),
                        jdbcSelect.getValue().toString(),
                        new Timestamp(((Date) startTime.getValue()).getTime()),
                        new Timestamp(((Date) endTime.getValue()).getTime()), DBManager.ConnectionSource.JDBC);
            } else {
                instances =
                        POEM.getCurrent().dbManager.getBPELInstances(
                        piSelect.getValue() != null ? piSelect.getValue().toString() : null,
                        (BPStatus) statusSelect.getValue(),
                        null,
                        new Integer(rowCount.getValue().toString()),
                        (SortColumn) sortColumnSelect.getValue(),
                        (SortOrder) sortOrderSelect.getValue(),
                        jdbcSelect.getValue().toString(),
                        new Timestamp(((Date) startTime.getValue()).getTime()),
                        new Timestamp(((Date) endTime.getValue()).getTime()), DBManager.ConnectionSource.JDBC);
            }
            for (BPInstanceInfo info : instances.bpInstnaceList) {
                Item woItem = biContainer.addItem(info);
                woItem.getItemProperty("id").setValue(new TableExecButton(info.id, "", null, info, this, POEMConstants.ACTION_INFO));
                woItem.getItemProperty("bpelId").setValue(info.bpelId.split("}")[1]);
                woItem.getItemProperty("startTime").setValue(info.startTime);
                woItem.getItemProperty("endTime").setValue(info.endTime);
                woItem.getItemProperty("lasted").setValue(info.lasted);
                woItem.getItemProperty("status").setValue(info.status);
            }
            table.setColumnFooter("bpelId", "processes count = " + table.size());
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
            } else if (event.getButton().equals(refreshJdbcBtn)) {
                refreshJDBCPoolResourceList();
                refreshBtn.setStyleName(Reindeer.BUTTON_DEFAULT);
            } else if (event.getButton() instanceof TableExecButton) {
                TableExecButton teb = (TableExecButton) event.getButton();
                addBpelInstanceWindow((BPInstanceInfo) teb.getTableValue(), null);
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
        bpelInstanceWindow = new BpelInstanceWindow(info, suSelect.getValue().toString(), target);
        bpelInstanceWindow.setWidth("90%");
        bpelInstanceWindow.setHeight("90%");
        bpelInstanceWindow.setResizable(false);
        getWindow().addWindow(bpelInstanceWindow);
    }

    public void valueChange(ValueChangeEvent event) {
        refreshBtn.setStyleName(Reindeer.BUTTON_DEFAULT);
    }
}
