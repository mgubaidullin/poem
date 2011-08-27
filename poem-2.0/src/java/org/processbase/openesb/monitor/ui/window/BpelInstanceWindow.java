package org.processbase.openesb.monitor.ui.window;

import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.Reindeer;
import org.processbase.openesb.monitor.ui.panel.BpelActivitiesPanel;
import org.processbase.openesb.monitor.ui.panel.BpelModelPanel;
import org.processbase.openesb.monitor.ui.panel.BpelVariablesPanel;

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

    public BpelInstanceWindow(String instanceId, String suName, String bpelName, String target) {
        super("Process name" + bpelName + ", id = " + instanceId);
        mainLayout = (VerticalLayout) getContent();
        mainLayout.setMargin(true);
        mainLayout.setStyleName(Reindeer.LAYOUT_WHITE);
        mainLayout.setSizeFull();
        setModal(true);

        tabs.setSizeFull();
        tabs.setStyleName(Reindeer.TABSHEET_MINIMAL);
        mainLayout.addComponent(tabs);
        mainLayout.setExpandRatio(tabs, 1);

        bpelActivitiesPanel = new BpelActivitiesPanel(instanceId, suName, bpelName, target);
        tabs.addComponent(bpelActivitiesPanel);

        bpelModelPanel = new BpelModelPanel(instanceId, suName, bpelName, target);
        tabs.addComponent(bpelModelPanel);

        bpelVariablesPanel = new BpelVariablesPanel(instanceId, null, target);
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

    
}
