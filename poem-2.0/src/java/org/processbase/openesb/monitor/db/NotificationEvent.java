/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.processbase.openesb.monitor.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 *
 * @author mgubaidullin
 */
@Entity
@Table(name = "NOTIFICATION_EVENT")
@NamedQueries({
    @NamedQuery(name = "NotificationEvent.findAll", query = "SELECT n FROM NotificationEvent n"),
    @NamedQuery(name = "NotificationEvent.findListOfComponenttype", query = "SELECT DISTINCT n.componenttype FROM NotificationEvent n"),
    @NamedQuery(name = "NotificationEvent.findById", query = "SELECT n FROM NotificationEvent n WHERE n.id = :id"),
    @NamedQuery(name = "NotificationEvent.findByTimestamp", query = "SELECT n FROM NotificationEvent n WHERE n.timestamp = :timestamp"),
    @NamedQuery(name = "NotificationEvent.findByPhysicalhostname", query = "SELECT n FROM NotificationEvent n WHERE n.physicalhostname = :physicalhostname"),
    @NamedQuery(name = "NotificationEvent.findByEnvironmentname", query = "SELECT n FROM NotificationEvent n WHERE n.environmentname = :environmentname"), @NamedQuery(name = "NotificationEvent.findByLogicalhostname", query = "SELECT n FROM NotificationEvent n WHERE n.logicalhostname = :logicalhostname"), @NamedQuery(name = "NotificationEvent.findByServertype", query = "SELECT n FROM NotificationEvent n WHERE n.servertype = :servertype"), @NamedQuery(name = "NotificationEvent.findByServername", query = "SELECT n FROM NotificationEvent n WHERE n.servername = :servername"), @NamedQuery(name = "NotificationEvent.findByComponenttype", query = "SELECT n FROM NotificationEvent n WHERE n.componenttype = :componenttype"), @NamedQuery(name = "NotificationEvent.findByComponentprojectpathname", query = "SELECT n FROM NotificationEvent n WHERE n.componentprojectpathname = :componentprojectpathname"), @NamedQuery(name = "NotificationEvent.findByComponentname", query = "SELECT n FROM NotificationEvent n WHERE n.componentname = :componentname"), @NamedQuery(name = "NotificationEvent.findByEventtype", query = "SELECT n FROM NotificationEvent n WHERE n.eventtype = :eventtype"), @NamedQuery(name = "NotificationEvent.findBySeverity", query = "SELECT n FROM NotificationEvent n WHERE n.severity = :severity"), @NamedQuery(name = "NotificationEvent.findByOperationalstate", query = "SELECT n FROM NotificationEvent n WHERE n.operationalstate = :operationalstate"), @NamedQuery(name = "NotificationEvent.findByMessagecode", query = "SELECT n FROM NotificationEvent n WHERE n.messagecode = :messagecode"), @NamedQuery(name = "NotificationEvent.findByMessagedetail", query = "SELECT n FROM NotificationEvent n WHERE n.messagedetail = :messagedetail"), @NamedQuery(name = "NotificationEvent.findByObservationalstate", query = "SELECT n FROM NotificationEvent n WHERE n.observationalstate = :observationalstate"), @NamedQuery(name = "NotificationEvent.findByDeploymentname", query = "SELECT n FROM NotificationEvent n WHERE n.deploymentname = :deploymentname")})
public class NotificationEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "ID")
    private Integer id;
    @Column(name = "TIMESTAMP")
    private Long timestamp;
    @Column(name = "PHYSICALHOSTNAME")
    private String physicalhostname;
    @Column(name = "ENVIRONMENTNAME")
    private String environmentname;
    @Column(name = "LOGICALHOSTNAME")
    private String logicalhostname;
    @Column(name = "SERVERTYPE")
    private String servertype;
    @Column(name = "SERVERNAME")
    private String servername;
    @Column(name = "COMPONENTTYPE")
    private String componenttype;
    @Column(name = "COMPONENTPROJECTPATHNAME")
    private String componentprojectpathname;
    @Column(name = "COMPONENTNAME")
    private String componentname;
    @Column(name = "EVENTTYPE")
    private String eventtype;
    @Column(name = "SEVERITY")
    private Integer severity;
    @Column(name = "OPERATIONALSTATE")
    private BigInteger operationalstate;
    @Column(name = "MESSAGECODE")
    private String messagecode;
    @Column(name = "MESSAGEDETAIL")
    private String messagedetail;
    @Column(name = "OBSERVATIONALSTATE")
    private BigInteger observationalstate;
    @Column(name = "DEPLOYMENTNAME")
    private String deploymentname;

    public NotificationEvent() {
    }

    public NotificationEvent(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPhysicalhostname() {
        return physicalhostname;
    }

    public void setPhysicalhostname(String physicalhostname) {
        this.physicalhostname = physicalhostname;
    }

    public String getEnvironmentname() {
        return environmentname;
    }

    public void setEnvironmentname(String environmentname) {
        this.environmentname = environmentname;
    }

    public String getLogicalhostname() {
        return logicalhostname;
    }

    public void setLogicalhostname(String logicalhostname) {
        this.logicalhostname = logicalhostname;
    }

    public String getServertype() {
        return servertype;
    }

    public void setServertype(String servertype) {
        this.servertype = servertype;
    }

    public String getServername() {
        return servername;
    }

    public void setServername(String servername) {
        this.servername = servername;
    }

    public String getComponenttype() {
        return componenttype;
    }

    public void setComponenttype(String componenttype) {
        this.componenttype = componenttype;
    }

    public String getComponentprojectpathname() {
        return componentprojectpathname;
    }

    public void setComponentprojectpathname(String componentprojectpathname) {
        this.componentprojectpathname = componentprojectpathname;
    }

    public String getComponentname() {
        return componentname;
    }

    public void setComponentname(String componentname) {
        this.componentname = componentname;
    }

    public String getEventtype() {
        return eventtype;
    }

    public void setEventtype(String eventtype) {
        this.eventtype = eventtype;
    }

    public Integer getSeverity() {
        return severity;
    }

    public void setSeverity(Integer severity) {
        this.severity = severity;
    }

    public BigInteger getOperationalstate() {
        return operationalstate;
    }

    public void setOperationalstate(BigInteger operationalstate) {
        this.operationalstate = operationalstate;
    }

    public String getMessagecode() {
        return messagecode;
    }

    public void setMessagecode(String messagecode) {
        this.messagecode = messagecode;
    }

    public String getMessagedetail() {
        return messagedetail;
    }

    public void setMessagedetail(String messagedetail) {
        this.messagedetail = messagedetail;
    }

    public BigInteger getObservationalstate() {
        return observationalstate;
    }

    public void setObservationalstate(BigInteger observationalstate) {
        this.observationalstate = observationalstate;
    }

    public String getDeploymentname() {
        return deploymentname;
    }

    public void setDeploymentname(String deploymentname) {
        this.deploymentname = deploymentname;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof NotificationEvent)) {
            return false;
        }
        NotificationEvent other = (NotificationEvent) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.processbase.openesb.monitor.db.NotificationEvent[id=" + id + "]";
    }

}
