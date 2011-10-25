package org.processbase.openesb.monitor.ui.panel;

import com.sun.caps.management.api.bpel.BPELManagementService;
import com.sun.caps.management.api.bpel.BPELManagementService.VarInfo;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.Reindeer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.processbase.openesb.monitor.POEM;


import org.processbase.openesb.monitor.ui.template.TablePanel;
import org.vaadin.codemirror.CodeMirror;
import org.vaadin.codemirror.client.ui.CodeStyle;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSException;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;

/**
 *
 * @author mgubaidullin
 */
public class BpelVariablesPanel extends TablePanel implements Property.ValueChangeListener {

    private String instanceId;
    private String target;
    private String varName;
    private IndexedContainer variablesContainer = new IndexedContainer();
    private String variableBody = null;
    private Document document;
    private DocumentBuilder docBuilder;
    private CodeMirror variableSource = new CodeMirror(null);
    private TextField variableText = new TextField();
    private Accordion variableValues = new Accordion();
    private HashMap<BPELManagementService.VarInfo, String> variables = new HashMap<BPELManagementService.VarInfo, String>();

    public BpelVariablesPanel(String instanceId, String varName, String target) {
        super("Variables");
        this.target = target;
        this.instanceId = instanceId;
        this.varName = varName;
        try {
            docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(BpelVariablesPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
        }

        buttonBar.setHeight("30px");

        initContainer();
        initTableUI();
        refreshInstanceData();

        variableText.setSizeFull();

        variableSource.setCodeStyle(CodeStyle.XML);
        variableSource.setSizeFull();

//        variableValues.addTab(variableText, "Text", new ThemeResource("icons/document-txt.png"));
        variableValues.addTab(variableSource, "Source", new ThemeResource("icons/document.png"));
        variableValues.setWidth("800px");
        variableValues.setHeight("100%");
        layout.addComponent(variableValues);
        layout.setExpandRatio(table, 1);
    }

    public void initContainer() {
        variablesContainer.addContainerProperty("varId", String.class, null);
        variablesContainer.addContainerProperty("varName", String.class, null);
        variablesContainer.addContainerProperty("xpath", String.class, null);
        variablesContainer.addContainerProperty("notes", String.class, null);
    }

    @Override
    public void initTableUI() {
        table.setContainerDataSource(variablesContainer);
        table.setSelectable(true);
        table.setMultiSelect(false);
        table.setImmediate(true);
        table.addListener(new Table.ValueChangeListener() {

            public void valueChange(ValueChangeEvent event) {
                VarInfo var = (VarInfo) event.getProperty().getValue();
                try {
                    variableBody = POEM.getCurrent().bpelManagementService.getVariableValue(instanceId, var.varId, target);
//                    System.out.println("variableBody = " + variableBody.getClass().getCanonicalName());
                    setVariableFields();
                } catch (Exception ex) {
                    Logger.getLogger(BpelVariablesPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
                }

            }
        });
    }

    private void setVariableFields() throws SAXException, IOException {
        String xmlString = formatXml(variableBody);
        variableSource.setValue(xmlString);

//        variableText.setValue(variableBody);
//        document = docBuilder.parse(new InputSource(new StringReader(variableBody)));
//        if ("jbi:message".equals(document.getDocumentElement().getTagName())) {
//            Node node = document.getDocumentElement().getFirstChild().getFirstChild();
//            if (node instanceof Element) {
//                Document doc = docBuilder.newDocument();
//                doc.appendChild(doc.adoptNode(node));
//                variableText.setValue(formatXml(doc));
//            } else {
//                variableText.setValue(node.getTextContent());
//            }
//        }
    }

    public void refreshInstanceData() {
        variablesContainer.removeAllItems();
        try {
            List<BPELManagementService.VarInfo> vars =
                    POEM.getCurrent().bpelManagementService.listBPELVaraibles(instanceId, varName, target);
            for (VarInfo var : vars) {
                Item woItem = variablesContainer.addItem(var);
                woItem.getItemProperty("varId").setValue(var.varId);
                woItem.getItemProperty("varName").setValue(var.varName);
                woItem.getItemProperty("xpath").setValue(var.xpath);
                woItem.getItemProperty("notes").setValue(var.notes);
            }
            table.select(variablesContainer.getIdByIndex(0));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        refreshBtn.setStyleName("reindeermods");
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

    public static String formatXml(Document document) {
        try {
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            DOMImplementation domImpl = registry.getDOMImplementation("LS 3.0");
            DOMImplementationLS implLS = (DOMImplementationLS) domImpl;
            LSSerializer dom3Writer = implLS.createLSSerializer();
            dom3Writer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
            dom3Writer.getDomConfig().setParameter("xml-declaration", Boolean.FALSE);
            LSOutput output = implLS.createLSOutput();
            Writer writer = new StringWriter();
            output.setCharacterStream(writer);
            output.setEncoding("UTF-8");
            dom3Writer.write(document, output);
            return writer.toString();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(BpelVariablesPanel.class.getName()).log(Level.WARNING, "formatXml", ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(BpelVariablesPanel.class.getName()).log(Level.WARNING, "formatXml", ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(BpelVariablesPanel.class.getName()).log(Level.WARNING, "formatXml", ex);
        } catch (ClassCastException ex) {
            Logger.getLogger(BpelVariablesPanel.class.getName()).log(Level.WARNING, "formatXml", ex);
        } catch (DOMException ex) {
            Logger.getLogger(BpelVariablesPanel.class.getName()).log(Level.WARNING, "formatXml", ex);
        } catch (LSException ex) {
            Logger.getLogger(BpelVariablesPanel.class.getName()).log(Level.WARNING, "formatXml", ex);
        }
        return null;
    }

    public static String xmlToString(Node node) {
        try {
            Source source = new DOMSource(node);
            StringWriter stringWriter = new StringWriter();
            Result result = new StreamResult(stringWriter);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(source, result);
            return stringWriter.getBuffer().toString();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String formatXml(String xml) {
        try {
            Transformer serializer = SAXTransformerFactory.newInstance().newTransformer();
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            //serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            //serializer.setOutputProperty("{http://xml.customer.org/xslt}indent-amount", "2");
            Source xmlSource = new SAXSource(new InputSource(new ByteArrayInputStream(xml.getBytes())));
            StreamResult res = new StreamResult(new ByteArrayOutputStream());
            serializer.transform(xmlSource, res);
            return new String(((ByteArrayOutputStream) res.getOutputStream()).toByteArray());
        } catch (Exception e) {
            //TODO log error
            return xml;
        }
    }

    public HashMap<VarInfo, String> getVariables() {
        try {
            List<BPELManagementService.VarInfo> vars = POEM.getCurrent().bpelManagementService.listBPELVaraibles(instanceId, varName, target);
            for (VarInfo var : vars) {
                String body = POEM.getCurrent().bpelManagementService.getVariableValue(instanceId, var.varId, target);
                variables.put(var, body);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return variables;
    }
}