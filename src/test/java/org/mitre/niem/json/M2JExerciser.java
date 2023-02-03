package org.mitre.niem.json;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.SchemaDocument;
import org.mitre.niem.xsd.ModelXMLReader;
import org.mitre.niem.xsd.ModelXMLWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

import static org.apache.logging.log4j.message.MapMessage.MapFormat.JSON;

public class M2JExerciser {
  @Test
  public void ReadCmfFile(){
    String objFile = "c:\\temp\\testoutfile.json";
    PrintWriter ow = new PrintWriter(System.out);
    if (!"".equals(objFile)) {
      try {
        File of = new File(objFile);
        ow = new PrintWriter(of);
      } catch (FileNotFoundException ex) {
        System.err.println(String.format("Can't write to output file %s: %s", objFile, ex.getMessage()));
        System.exit(1);
      }
    }

    Model m = null;
    File ifile = new File("c:\\temp\\CrashDriver.cmf");
    FileInputStream is = null;
    try {
      is = new FileInputStream(ifile);
    } catch (FileNotFoundException ex) {
      System.err.println(String.format("Error reading model file: %s", ex.getMessage()));
      System.exit(1);
    }
    ModelXMLReader mr = new ModelXMLReader();
    m = mr.readXML(is);
    var j = new ModelToJSON(m);
    j.writeJSON();

    System.out.println(new Gson().toJson(j.getJsonSchema().getSchemas()));
    System.out.println();


    if (null == m) {
      List<String> msgs = mr.getMessages();
      System.err.print("Could not construct model object:");
      if (1 == msgs.size()) System.err.print(msgs.get(0));
      else msgs.forEach((xm) -> { System.err.print("\n  "+xm); });
      System.err.println();
      System.exit(1);
    }

    // Write the NIEM model instance to the output stream
    ModelXMLWriter mw = new ModelXMLWriter();
    try {
      mw.writeXML(m, ow);
      ow.close();
    } catch (TransformerException ex) {
      System.err.println(String.format("Output error: %s", ex.getMessage()));
      System.exit(1);
    } catch (ParserConfigurationException ex) {
      // CAN'T HAPPEN
    }

  }
}
