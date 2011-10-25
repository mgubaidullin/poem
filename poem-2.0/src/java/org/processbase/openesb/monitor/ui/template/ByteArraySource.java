/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.processbase.openesb.monitor.ui.template;

import com.vaadin.terminal.StreamResource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;


/**
 *
 * @author mgubaidullin
 */
public class ByteArraySource implements StreamResource.StreamSource {

    public byte[] byteArray = null;

    public ByteArraySource(byte[] byteArray) {
        super();
        this.byteArray = byteArray;
    }

    @Override
    public InputStream getStream() {
        try {
            return new ByteArrayInputStream(byteArray);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
