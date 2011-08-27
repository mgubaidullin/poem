package org.processbase.openesb.monitor.ui.panel;

import java.sql.Connection;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.PopupDateField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window.Notification;
import com.vaadin.ui.themes.Reindeer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.processbase.openesb.monitor.POEM;
import org.processbase.openesb.monitor.db.DBManager;
import org.processbase.openesb.monitor.ui.template.TabPanel;

/**
 *
 * @author mgubaidullin
 */
public class DashboardPanel extends TabPanel implements Property.ValueChangeListener {

    private PopupDateField startDate = new PopupDateField("Start DateTime");
    private Connection connection;
    private GridLayout dashboardPanel = new GridLayout(3, 2);
    VerticalLayout vl = new VerticalLayout();

    public DashboardPanel() {
        super("Dashboard");
        buttonBar.setHeight("38px");

        startDate.setResolution(PopupDateField.RESOLUTION_MIN);
        startDate.setValue(new Date());
        buttonBar.addComponent(startDate, 0);

        
        vl.setSizeFull();
        this.layout.addComponent(vl);
        layout.setExpandRatio(vl,1);
        dashboardPanel.setSizeFull();
        vl.addComponent(dashboardPanel);


    }

    @Override
    public void buttonClick(ClickEvent event) {
        try {
            if (event.getButton().equals(refreshBtn)) {
                refreshDashboard();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            getWindow().showNotification("Error", ex.getMessage(), Notification.TYPE_ERROR_MESSAGE);
        }
    }

    public void valueChange(ValueChangeEvent event) {
        refreshBtn.setStyleName(Reindeer.BUTTON_DEFAULT);
    }

    private void refreshDashboard() {
    }


    private void getProcessesByStatus() {
        connection = POEM.getCurrent().dbManager.getConnection(null);
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(DBManager.SQL_COUNT_BY_STATUS);
//            cstmt.setString(1, sClientId);
//            cstmt.setString(2, "     ");
//            cstmt.registerOutParameter(2, java.sql.Types.VARCHAR);
            ResultSet rs = null;
            rs = pstmt.executeQuery();

            while (rs.next()) {
                System.out.println(rs.getString(1) + " " + rs.getString(2));
            }

            if (rs != null) {
                rs.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DashboardPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                connection.close();
            } catch (SQLException ex) {
                Logger.getLogger(DashboardPanel.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }
}
