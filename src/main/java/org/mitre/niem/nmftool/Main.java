package org.mitre.niem.nmftool;

import jakarta.xml.bind.JAXBException;
import java.io.File;

/**
 *
 * @author Scott
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws JAXBException {
        File f = new File("src/test/resources/ClaimModel.xml");
        Model result = Model.readXML(f);
    }
    
}
