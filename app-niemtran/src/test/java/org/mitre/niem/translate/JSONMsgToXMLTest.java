/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2026 The MITRE Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mitre.niem.translate;

import com.google.gson.JsonParser;
import java.io.File;
import java.io.StringWriter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ModelXMLReader;
import org.mitre.niem.xml.XMLWriter;

/**
 *
 * @author Scott Renner
 */
public class JSONMsgToXMLTest {
    
    private static final String SRC_D = "src/test/resources";
    
    private Model model(String cmfName) throws Exception {
        return new ModelXMLReader().readFiles(new File(SRC_D, cmfName));
    }
    
    private String convert(Model m, String json) throws Exception {
        var j2x = new JSONMsgToXML(m);
        var jsn = JsonParser.parseString(json).getAsJsonObject();
        var doc = j2x.convert(jsn);
        var xw  = new XMLWriter();
        var sw  = new StringWriter();
        xw.writeXML(doc, sw);
        return sw.toString();
    }
    
    private void assertConvertsTo(Model m, String json, String xml) throws Exception {
        var actual = convert(m, json);
        assertEquals(xml, actual);
    }
    
    private void assertFails(Model m, String json, String msgPart) {
        var ex = assertThrows(Exception.class, () -> convert(m, json));
        assertTrue(ex.getMessage().contains(msgPart), ex.getMessage());
    }

    // Tests list datatypes, repeatable properties, literal classes
    @Test
    public void testList() throws Exception {
        var m = model("list.cmf");
        var json = """
                   {
                     "t:ObjectElement": {
                       "t:SimpleElement": [ "1 2 3", "4 5" ],
                       "t:ListDataElement": [
                         [ 6, 7, 8 ],
                         [ 6, 7, 88 ]
                       ],
                       "t:ListObjectElement": {
                         "t:myAtt": "foo",
                         "t:ListObjectLiteral": [ 9, 10, 11 ]
                       }
                     },
                     "@context": { "t": "http://example.com/Test/" }
                   }
                   """;
        var xml = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <t:ObjectElement 
                    xmlns:t="http://example.com/Test/">
                    <t:SimpleElement>1 2 3</t:SimpleElement>
                    <t:SimpleElement>4 5</t:SimpleElement>
                    <t:ListDataElement>6 7 8</t:ListDataElement>
                    <t:ListDataElement>6 7 88</t:ListDataElement>
                    <t:ListObjectElement t:myAtt="foo">9 10 11</t:ListObjectElement>
                  </t:ObjectElement>
                  """;
        assertConvertsTo(m, json, xml);
    }
    
    @Test
    public void testAugmentationsSingleNamespace() throws Exception {
        var m = model("aug.cmf");
        var json = """
                   {
                     "nc:PersonEducation": {
                       "nc:EducationDescriptionText": [ "Text0", "Text1" ],
                       "t:TestAugElement": "TestAugElement",
                       "t:myAtt": "silly",
                       "t:CommentDestinationText": "CommentText0",
                       "nc:CommentText": "OtherComment"
                     }
                   }
                   """;
        var xml = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <nc:PersonEducation
                    xmlns:nc="https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"
                    xmlns:t="http://example.com/test/"
                    t:myAtt="silly">
                    <nc:EducationDescriptionText>Text0</nc:EducationDescriptionText>
                    <nc:EducationDescriptionText>Text1</nc:EducationDescriptionText>
                    <t:TestAugElement>TestAugElement</t:TestAugElement>
                    <t:EducationAugmentation>
                      <t:CommentDestinationText>CommentText0</t:CommentDestinationText>
                      <nc:CommentText>OtherComment</nc:CommentText>
                    </t:EducationAugmentation>
                  </nc:PersonEducation>
                  """;
        assertConvertsTo(m, json, xml);
    }
    
    @Test
    public void testAugmentationsMultipleNamespaces() throws Exception {
        var m = model("aug.cmf");
        var json = """
                   {
                     "nc:PersonEducation": {
                       "nc:EducationDescriptionText": [ "Hello", "Dolly" ],
                       "t:TestAugElement": "TestAugElement",
                       "j:EducationTotalYearsText": "20",
                       "t:myAtt": "silly",
                       "t:CommentDestinationText": "My comment",
                       "nc:CommentText": "OtherComment"
                     }
                   }
                   """;
        var xml = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <nc:PersonEducation
                    xmlns:j="https://docs.oasis-open.org/niemopen/ns/model/domains/justice/6.0/"
                    xmlns:nc="https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"
                    xmlns:t="http://example.com/test/"
                    t:myAtt="silly">
                    <nc:EducationDescriptionText>Hello</nc:EducationDescriptionText>
                    <nc:EducationDescriptionText>Dolly</nc:EducationDescriptionText>
                    <t:TestAugElement>TestAugElement</t:TestAugElement>
                    <j:EducationAugmentation>
                      <j:EducationTotalYearsText>20</j:EducationTotalYearsText>
                    </j:EducationAugmentation>
                    <t:EducationAugmentation>
                      <t:CommentDestinationText>My comment</t:CommentDestinationText>
                      <nc:CommentText>OtherComment</nc:CommentText>
                    </t:EducationAugmentation>
                  </nc:PersonEducation>
                  """;
        assertConvertsTo(m, json, xml);
    }
    
    @Test
    public void testDupAugment() throws Exception {
        var m = model("augdup.cmf");
        var json = """
                   {
                     "nc:PersonEducation": {
                       "nc:EducationDescriptionText": [ "Text0", "Text1" ],
                       "t:TestAugElement": "TestAugElement",
                       "t:myAtt": "silly",
                       "t:CommentDestinationText": "CommentText0",
                       "j:EducationTotalYearsText": "20",
                       "nc:CommentText": "OtherComment"
                     }
                   }
                   """;
        var xml = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <nc:PersonEducation
                    xmlns:j="https://docs.oasis-open.org/niemopen/ns/model/domains/justice/6.0/"
                    xmlns:nc="https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"
                    xmlns:t="http://example.com/test/"
                    t:myAtt="silly">
                    <nc:EducationDescriptionText>Text0</nc:EducationDescriptionText>
                    <nc:EducationDescriptionText>Text1</nc:EducationDescriptionText>
                    <t:TestAugElement>TestAugElement</t:TestAugElement>
                    <j:EducationAugmentation>
                      <j:EducationTotalYearsText>20</j:EducationTotalYearsText>
                    </j:EducationAugmentation>
                    <t:EducationAugmentation>
                      <t:CommentDestinationText>CommentText0</t:CommentDestinationText>
                      <nc:CommentText>OtherComment</nc:CommentText>
                    </t:EducationAugmentation>
                  </nc:PersonEducation>
                  """;
        assertConvertsTo(m, json, xml);
    }
    
    @Test
    public void testAttAugment() throws Exception {
        var m = model("attAug.cmf");
        var json = """
                   {
                     "t:Message": {
                       "nc:Comment": {
                         "nc:CommentText": [
                           {
                             "t:attProp": "abc",
                             "nc:TextLiteral": "Comment"
                           },
                           {
                             "t:attProp": "123",
                             "nc:TextLiteral": "AnotherComment"
                           }
                         ]
                       },
                       "nc:PersonEducation": {
                         "t:attProp": "foo",
                         "nc:EducationDescriptionText": [
                           { "nc:TextLiteral": "PhD" },
                           { "nc:TextLiteral": "Higher and deeper" }
                         ]
                       }
                     }
                   }
                   """;
        var xml = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <t:Message
                    xmlns:nc="https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"
                    xmlns:t="http://example.com/test/">
                    <nc:Comment>
                      <nc:CommentText t:attProp="abc">Comment</nc:CommentText>
                      <nc:CommentText t:attProp="123">AnotherComment</nc:CommentText>
                    </nc:Comment>
                    <nc:PersonEducation t:attProp="foo">
                      <nc:EducationDescriptionText>PhD</nc:EducationDescriptionText>
                      <nc:EducationDescriptionText>Higher and deeper</nc:EducationDescriptionText>
                    </nc:PersonEducation>
                  </t:Message>
                  """;
        assertConvertsTo(m, json, xml);
    }
    
    @Test
    public void testIDRefs() throws Exception {
        var m = model("refs.cmf");
        var json = """
                   {
                     "t:Message": {
                       "t:OneProp": [
                         {
                           "@id": "#I01",
                           "t:DataProperty": "OneDat"
                         },
                         { "@id": "#I01" }
                       ],
                       "t:TwoProp": [
                         {
                           "@id": "#T02",
                           "t:DataProperty": "TwoDat"
                         },
                         {
                           "@id": "#T02",
                           "t:DataProperty": "MoreTwoDat"
                         }
                       ],
                       "t:ThreeProp": [
                         {
                           "@id": "#T03",
                           "t:DataProperty": "ThreeDat"
                         },
                         { "@id": "#T03" }
                       ]
                     },
                     "@context": {
                       "t": "http://example.com/refCode/"
                     }
                   }
                   """;
        var xml = """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <t:Message
                    xmlns:structures="https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/"
                    xmlns:t="http://example.com/test/"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                    <t:OneProp structures:id="I01">
                      <t:DataProperty>OneDat</t:DataProperty>
                    </t:OneProp>
                    <t:OneProp structures:ref="I01" xsi:nil="true"/>
                    <t:TwoProp structures:uri="#T02">
                      <t:DataProperty>TwoDat</t:DataProperty>
                    </t:TwoProp>
                    <t:TwoProp structures:uri="#T02">
                      <t:DataProperty>MoreTwoDat</t:DataProperty>
                    </t:TwoProp>
                    <t:ThreeProp structures:uri="#T03">
                      <t:DataProperty>ThreeDat</t:DataProperty>
                    </t:ThreeProp>
                    <t:ThreeProp structures:uri="#T03"/>
                  </t:Message>               
                  """;
        assertConvertsTo(m, json, xml);
    }
    
    @Test
    public void testContextIgnored() throws Exception {
        var m = model("list.cmf");
        var json = """
                   {
                     "t:ObjectElement": {
                       "t:SimpleElement": [ "1 2 3" ]
                     },
                     "@context": {
                       "t": "http://wrong.example.com/"
                     }
                   }
                   """;
        var xml = convert(m, json);
        assertTrue(xml.contains("<t:ObjectElement"));
        assertTrue(xml.contains("xmlns:t=\"http://example.com/Test/\""));
        assertTrue(xml.contains("<t:SimpleElement>1 2 3</t:SimpleElement>"));
        assertTrue(!xml.contains("http://wrong.example.com/"), xml);
    }
    
    @Test
    public void testNoMessagePropertyKey() throws Exception {
        var m = model("list.cmf");
        var json = """
                   {
                     "@context": {
                       "t": "http://example.com/Test/"
                     }
                   }
                   """;
        assertFails(m, json, "No message property key");
    }
    
    @Test
    public void testTooManyTopLevelPropertyKeys() throws Exception {
        var m = model("list.cmf");
        var json = """
                   {
                     "t:ObjectElement": {},
                     "x:OtherElement": {},
                     "@context": {
                       "t": "http://example.com/Test/"
                     }
                   }
                   """;
        assertFails(m, json, "Too many property keys");
    }
    
    @Test
    public void testTopLevelValueMustBeObject() throws Exception {
        var m = model("list.cmf");
        var json = """
                   {
                     "t:ObjectElement": "not-an-object"
                   }
                   """;
        assertFails(m, json, "Value of t:ObjectElement is not an object");
    }
    
    @Test
    public void testTopLevelMessagePropertyNotInModel() throws Exception {
        var m = model("list.cmf");
        var json = """
                   {
                     "t:NoSuchElement": {}
                   }
                   """;
        assertFails(m, json, "which is not in model");
    }
    
    @Test
    public void testObjectPropertyCannotHavePrimitiveValue() throws Exception {
        var m = model("list.cmf");
        var json = """
                   {
                     "t:ObjectElement": {
                       "t:ListObjectElement": "oops"
                     }
                   }
                   """;
        assertFails(m, json, "Object property t:ListObjectElement can't have primitive value");
    }
    
    @Test
    public void testDataPropertyCannotHaveObjectValue() throws Exception {
        var m = model("list.cmf");
        var json = """
                   {
                     "t:ObjectElement": {
                       "t:SimpleElement": {
                         "bad": "shape"
                       }
                     }
                   }
                   """;
        assertFails(m, json, "Data property t:SimpleElement can't have an object value");
    }
    
    @Test
    public void testNonRepeatableNonListDataPropertyCannotHaveArrayValue() throws Exception {
        var m = model("refs.cmf");
        var json = """
                   {
                     "t:Message": {
                       "t:OneProp": {
                         "t:DataProperty": [ "A", "B" ]
                       }
                     },
                     "@context": {
                       "t": "http://example.com/refCode/"
                     }
                   }
                   """;
        assertFails(m, json, "not repeatable, not a list");
    }
    
    @Test
    public void testNullNotAllowed() throws Exception {
        var m = model("list.cmf");
        var json = """
                   {
                     "t:ObjectElement": {
                       "t:SimpleElement": null
                     }
                   }
                   """;
        assertFails(m, json, "Nulls not allowed in JSON data");
    }
    
    @Test
    public void testLiteralClassMissingLiteralProperty() throws Exception {
        var m = model("list.cmf");
        var json = """
                   {
                     "t:ObjectElement": {
                       "t:ListObjectElement": {
                         "t:myAtt": "foo"
                       }
                     }
                   }
                   """;
        assertFails(m, json, "Literal property");
    }
    
    @Test
    public void testRefAndUriIdHandling() throws Exception {
        var m = model("refs.cmf");
        var json = """
                   {
                     "t:Message": {
                       "t:OneProp": [
                         {
                           "@id": "#I01",
                           "t:DataProperty": "OneDat"
                         },
                         { "@id": "#I01" }
                       ],
                       "t:ThreeProp": [
                         { "@id": "#T03" }
                       ]
                     },
                     "@context": {
                       "t": "http://example.com/refCode/"
                     }
                   }
                   """;
        var xml = convert(m, json);
        assertTrue(xml.contains("structures:id=\"I01\""), xml);
        assertTrue(xml.contains("structures:ref=\"I01\" xsi:nil=\"true\""), xml);
        assertTrue(xml.contains("structures:uri=\"#T03\""), xml);
    }
}

