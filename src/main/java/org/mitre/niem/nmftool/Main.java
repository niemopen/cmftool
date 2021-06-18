package org.mitre.niem.nmftool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.mitre.niem.nmf.Model;
import org.mitre.niem.xsd.ModelXMLReader;
import org.mitre.niem.xsd.ModelXMLWriter;
import org.xml.sax.SAXException;

/**
 *
 * @author Scott
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        File f = new File("src/test/resources/ClaimModel.xml");
        FileInputStream fis = new FileInputStream(f);
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(fis);
        
        ModelXMLWriter mw = new ModelXMLWriter();
        mw.writeXML(m, System.out);
    }
    
}
