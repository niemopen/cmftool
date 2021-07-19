package org.mitre.niem.nmftool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.xerces.xs.XSModel;
import org.mitre.niem.nmf.Model;
import org.mitre.niem.nmf.NMFException;
import org.mitre.niem.xsd.ModelFromXSD;
import org.mitre.niem.xsd.ModelXMLReader;
import org.mitre.niem.xsd.ModelXMLWriter;
import org.mitre.niem.xsd.Schema;
import org.mitre.niem.xsd.SchemaException;
import org.xml.sax.SAXException;

/**
 *
 * @author Scott
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException, URISyntaxException, SchemaException {
//        File f = new File("src/test/resources/Claim-iepd/claim-model.xml");
//        FileInputStream fis = new FileInputStream(f);
//        ModelXMLReader mr = new ModelXMLReader();
//        Model m = mr.readXML(fis);
//        
//        ModelXMLWriter mw = new ModelXMLWriter();
//        mw.writeXML(m, System.out);

        Schema s = Schema.newInstance(
//                "src/test/resources/CrashDriver-iepd/extension/CrashDriver.xsd", 
//                "http://example.com/PrivacyMetadata/1.0/",
//                "src/test/resources/CrashDriver-iepd/xml-catalog.xml"
//                "http://example.org/claim/1/", 
//                "src/test/resources/Claim-iepd/xml-catalog.xml"    
//                "src/test/resources/Test/Code.xsd"
                "src/test/resources/Test/PersonName.xsd"
        );
        try {
            Model m = new ModelFromXSD().createModel(s);
            ModelXMLWriter mw = new ModelXMLWriter();
            mw.writeXML(m, System.out);
        } catch (NMFException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
