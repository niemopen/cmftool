/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2022 The MITRE Corporation.
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
package org.mitre.niem.xsd;

import static org.assertj.core.api.Assertions.assertThat;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLSchemaDocumentTest {
    
    public XMLSchemaDocumentTest () {
    }

    @Test
    public void testParse () throws Exception {
        XMLSchemaDocument sd = new XMLSchemaDocument("src/test/resources/xsd/xmlschemadoc/niem-core.xsd", "");
        
        assertEquals(8, sd.namespaceDecls().size());
        assertThat(sd.namespaceDecls())
                .hasSize(8)
                .extracting(XMLNamespaceDeclaration::decPrefix)
                .contains("appinfo", "ct", "nc", "niem-xs", "structures", "xs", "xsi", "foo");
        assertThat(sd.namespaceDecls())
                .filteredOn(nsd -> nsd.decPrefix().equals("foo"))
                .extracting(nsd -> nsd.elementDepth())
                .containsOnly(2);
        
        assertThat(sd.externalImports())
                .hasSize(1)
                .containsOnly("http://www.opengis.net/gml/3.2");
        
        assertThat(sd.appinfo())
                .hasSize(3)
                .extracting(a -> a.attLname()).containsOnly("deprecated", "orderedPropertyIndicator")
                ;
        for (Appinfo a : sd.appinfo()) {
            if ("orderedPropertyIndicator".equals(a.attLname())) {
                var ceqn = a.componentEQN();
                var eeqn = a.elementEQN();
                assertEquals(Pair.with("http://release.niem.gov/niem/niem-core/5.0/", "PersonNameType"), ceqn);
                assertEquals(Pair.with("http://example.com/redefined/nc/", "PersonGivenName"), eeqn);
            }
        }
        String targetNS = sd.targetNamespace();
        for (var nsd : sd.namespaceDecls()) {
            assertEquals(targetNS, nsd.targetNS());
        }
        
        assertEquals("http://release.niem.gov/niem/niem-core/5.0/", targetNS);
        assertEquals("http://reference.niem.gov/niem/specification/naming-and-design-rules/5.0/#ReferenceSchemaDocument", sd.conformanceTargets());
        assertEquals("5.0", sd.niemVersion());
        assertEquals("99", sd.schemaVersion());
        assertEquals("documentation for NIEM Core.", sd.documentation());
        assertEquals(2, sd.schemaKind());
    }
    
    @Test
    public void testIsCore () throws Exception {
        XMLSchemaDocument sd = new XMLSchemaDocument("src/test/resources/xsd/niem/niem-core.xsd", "");
        assertEquals(2, sd.schemaKind());
    }
    
    @Test
    public void testIsDomain () throws Exception {
        XMLSchemaDocument sd = new XMLSchemaDocument("src/test/resources/xsd/xmlschemadoc/hs.xsd", "");
        assertEquals(1, sd.schemaKind());
    }
    
    @Test
    public void testIsExtension () throws Exception {
        XMLSchemaDocument sd = new XMLSchemaDocument("src/test/resources/xsd/xmlschemadoc/CrashDriver.xsd", "");
        assertEquals(0, sd.schemaKind());
    }    
    
    @Test
    public void testIsBuiltin () throws Exception {
        XMLSchemaDocument sd = new XMLSchemaDocument("src/test/resources/xsd/niem/utility/appinfo.xsd", "");
        assertEquals(4, sd.schemaKind());
    }    
    
    @Test
    public void testIsOtherNIEM () throws Exception {
        XMLSchemaDocument sd = new XMLSchemaDocument("src/test/resources/xsd/externals-niem/adapters/geospatial.xsd", "");
        assertEquals(3, sd.schemaKind());
    }
    
    @Test
    public void testIsXML () throws Exception {
        XMLSchemaDocument sd = new XMLSchemaDocument("src/test/resources/xsd/niem/external/xml.xsd", "");
        assertEquals(6, sd.schemaKind());
    }

    @Test
    public void testNoPrefix () throws Exception {
        XMLSchemaDocument sd = new XMLSchemaDocument("src/test/resources/xsd/noprefix.xsd", "");
        assertEquals("http://example.com/noprefix/", sd.targetNamespace());
    }
}
