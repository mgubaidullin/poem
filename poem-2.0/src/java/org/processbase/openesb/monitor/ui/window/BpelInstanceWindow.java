package org.processbase.openesb.monitor.ui.window;

import com.sun.caps.management.api.bpel.BPELManagementService;
import com.sun.caps.management.api.bpel.BPELManagementService.ActivityStatus;
import com.sun.caps.management.api.bpel.BPELManagementService.BPInstanceInfo;
import com.sun.caps.management.api.bpel.BPELManagementService.VarInfo;
import com.vaadin.terminal.StreamResource;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.Reindeer;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.processbase.openesb.monitor.POEM;
import org.processbase.openesb.monitor.db.DBManager.ConnectionSource;
import org.processbase.openesb.monitor.ui.panel.BpelActivitiesPanel;
import org.processbase.openesb.monitor.ui.panel.BpelModelPanel;
import org.processbase.openesb.monitor.ui.panel.BpelVariablesPanel;
import org.processbase.openesb.monitor.ui.template.ByteArraySource;

/**
 *
 * @author marat
 */
public class BpelInstanceWindow extends Window {

    private TabSheet tabs = new TabSheet();
    private VerticalLayout mainLayout;
    private BpelActivitiesPanel bpelActivitiesPanel = null;
    private BpelModelPanel bpelModelPanel = null;
    private BpelVariablesPanel bpelVariablesPanel = null;
    private BPInstanceInfo instanceInfo = null;

    public BpelInstanceWindow(BPInstanceInfo instanceInfo, String suName, String target, ConnectionSource connectionSource) {
        super("Process name" + instanceInfo.bpelId + ", id = " + instanceInfo.id);
        System.out.println("Process name" + instanceInfo.bpelId + ", id = " + instanceInfo.id + ", target ="+ target);
        this.instanceInfo = instanceInfo;
        mainLayout = (VerticalLayout) getContent();
        mainLayout.setMargin(true);
        mainLayout.setStyleName(Reindeer.LAYOUT_WHITE);
        mainLayout.setSizeFull();
        setModal(true);

        tabs.setSizeFull();
        tabs.setStyleName(Reindeer.TABSHEET_MINIMAL);
        mainLayout.addComponent(tabs);
        mainLayout.setExpandRatio(tabs, 1);

        bpelActivitiesPanel = new BpelActivitiesPanel(instanceInfo.id, suName, instanceInfo.bpelId, target, connectionSource);
        tabs.addComponent(bpelActivitiesPanel);

        bpelModelPanel = new BpelModelPanel(instanceInfo.id, suName, instanceInfo.bpelId, target);
        tabs.addComponent(bpelModelPanel);

        bpelVariablesPanel = new BpelVariablesPanel(instanceInfo.id, null, target, connectionSource);
        tabs.addComponent(bpelVariablesPanel);
    }

    public BpelActivitiesPanel getBpelActivitiesPanel() {
        return bpelActivitiesPanel;
    }

    public BpelModelPanel getBpelModelPanel() {
        return bpelModelPanel;
    }

    public BpelVariablesPanel getBpelVariablesPanel() {
        return bpelVariablesPanel;
    }

    public TabSheet getTabs() {
        return tabs;
    }

    public void export() {
        try {
            StringBuilder act = new StringBuilder("POEM PROCESS INSTANCE EXPORT " + new Date(System.currentTimeMillis()));
            act.append("\n");
            // prepare process info
            act.append("BPEL PROCESS \n");
            act.append("id;").append("bpelId;").append("lastUpdateTime;").append("startTime;").append("endTime;").append("lasted;").append("status;").append("\n");
            act.append(instanceInfo.id).append(";");
            act.append(instanceInfo.bpelId).append(";");
            act.append(instanceInfo.lastUpdateTime).append(";");
            act.append(instanceInfo.startTime).append(";");
            act.append(instanceInfo.endTime).append(";");
            act.append(instanceInfo.lasted).append(";");
            act.append(instanceInfo.status).append("\n\n");

            // prepare activities info
            List<BPELManagementService.ActivityStatus> activities = bpelActivitiesPanel.getActivities();
            HashMap<ActivityStatus, String> actNames = bpelActivitiesPanel.getActNames();
            act.append("ACTIVITIES \n");
            act.append("activityId;").append("activityXpath;").append("name;").append("iteration;").append("startTime;").append("endTime;").append("lasted;").append("status").append("\n");
            for (ActivityStatus activity : activities) {
                act.append(activity.activityId).append(";");
                act.append(activity.activityXpath).append(";");
                act.append(actNames.get(activity)).append(";");
                act.append(activity.iteration).append(";");
                act.append(activity.startTime).append(";");
                act.append(activity.endTime).append(";");
                act.append(activity.lasted).append(";");
                act.append(activity.status).append("\n");
            }
            act.append("\n");

            // prepare variables info
            act.append("VARIABLES \n");
            HashMap<VarInfo, String> variables = bpelVariablesPanel.getVariables();
            for (VarInfo varInfo : variables.keySet()) {
                act.append(varInfo.varId).append(";");
                act.append(varInfo.varName).append(";");
                act.append(varInfo.xpath).append(";");
                act.append(varInfo.notes).append("\n");
                act.append(variables.get(varInfo)).append("\n");
            }

            ByteArraySource bas = new ByteArraySource(act.toString().getBytes("UTF-8"));
            StreamResource streamResource = new StreamResource(bas, instanceInfo.id + ".csv", getApplication());
            streamResource.setCacheTime(50000); // no cache (<=0) does not work with IE8
            getWindow().getWindow().open(streamResource, "_new");
        } catch (Exception e) {
            e.printStackTrace();
            showNotification("Export Error", e.getMessage(), Notification.TYPE_ERROR_MESSAGE);
        }
    }
}
