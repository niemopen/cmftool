package org.mitre.niem.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.xsd.ModelXMLReader;

public class ModelToJSONSchemaTest {

  String dir = "src/test/java/org/mitre/niem/json/";

  @Test
  public void GenerateJSONSchema() throws IOException {

    Model model = null;
    ModelXMLReader modelXMLReader = new ModelXMLReader();
    File inputFile = new File(dir + "CrashDriver.cmf.xml");
    FileInputStream inputStream = null;

    try {
      inputStream = new FileInputStream(inputFile);
    }
    catch (FileNotFoundException ex) {
      System.err.println(String.format("Error reading model file: %s", ex.getMessage()));
      System.exit(1);
    }

    // Load the model from a CMF file
    model = modelXMLReader.readXML(inputStream);

    // Write the model as JSON Schema
    PrintWriter writer = new PrintWriter(dir + "CrashDriver-actual.schema.json", StandardCharsets.UTF_8);
    ModelToJSON modelToJSON = new ModelToJSON(model);
    String json = modelToJSON.writeJSON(writer);

    writer.close();
    System.out.println(json + "\n");

    String expectedJSONSchema = new String(Files.readAllBytes(Paths.get(dir + "CrashDriver-expected.schema.json")));

    // Note: If the assert fails, compare the expected file with the actual
    // results (the two JSON schema files in this directory)
    assertEquals(expectedJSONSchema, json);

  }
}
