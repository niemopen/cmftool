/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2025 The MITRE Corporation.
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
package org.mitre.niem.xml;

import java.io.File;
import java.net.URI;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.niem.xml.XMLSchemaDocument.getDocumentation;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLSchemaDocumentTest {
    
    private final static File resDF   = new File("src/test/resources");
    private final static File xsDocF  = new File(resDF, "xsd/xsDocTest.xsd");
    private final static File goodXsF =  new File(resDF, "xsd/goodXsTest.xsd");
    
    @Test
    public void testFileConstructor () throws Exception {
        var xsd = new XMLSchemaDocument(xsDocF);
    }
    
    @Test
    public void testTargetNamespace () throws Exception {
        var xsd = new XMLSchemaDocument(xsDocF);
        var tns = xsd.targetNamespace();
        assertEquals("http://example.com/test/", tns);
    }
    
    @Test
    public void testVersion () throws Exception {
        var xsd = new XMLSchemaDocument(xsDocF);
        var tns = xsd.version();
        assertEquals("1", tns);
    }
    
    @Test
    public void testDom() throws Exception {
        var xsd = new XMLSchemaDocument(xsDocF);
        assertNotNull(xsd.dom());
    }

    @Test
    public void testEval() throws Exception {
        var xsd = new XMLSchemaDocument(xsDocF);
        var nds = xsd.evalForNodes("/*/@targetNamespace");
        assertEquals(1, nds.getLength());
    }
    
    @Test
    public void testImportElements () throws Exception {
        var xsd  = new XMLSchemaDocument(goodXsF);
        var imps = xsd.importElements();
        assertEquals(3, imps.getLength());
    }
    
    @Test
    public void testGetDocumentation () throws Exception {
        var xsd  = new XMLSchemaDocument(goodXsF);
        var dom  = xsd.dom();
        var root = dom.getDocumentElement();
        var docL = getDocumentation(root);
        assertThat(docL)
            .hasSize(2)
            .satisfiesExactlyInAnyOrder(
                obj -> { assertThat("en-US".equals(obj.lang()));
                         assertThat("XMLSchema test".equals(obj.text()));
                },
                obj -> { assertThat("fr".equals(obj.lang()));
                         assertThat(obj.text().contains("pas ma valise"));
                }
            );
    }

    @Test
    public void testNsdecls() throws Exception {
        var xsd = new XMLSchemaDocument(xsDocF);
        var nsd = xsd.nsdecls();
        assertThat(nsd)
                .hasSize(4)
                .satisfiesExactlyInAnyOrder(
                    obj -> {
                        assertThat(obj.prefix().equals("ct"));
                        assertThat(obj.ns().equals("https://docs.oasis-open.org/niemopen/ns/specification/conformanceTargets/6.0/"));
                        assertThat(obj.line() == 4);
                        assertThat(obj.depth() == 0);                        
                    },
                    obj -> {
                        assertThat(obj.prefix().equals("xs"));
                        assertThat(obj.ns().equals("http://www.w3.org/2001/XMLSchema"));
                        assertThat(obj.line() == 5);
                        assertThat(obj.depth() == 0);                        
                    },
                    obj -> {
                        assertThat(obj.prefix().equals("ct"));
                        assertThat(obj.ns().equals("https://example.com/bogus-ct/"));
                        assertThat(obj.line() == 13);
                        assertThat(obj.depth() == 1);                        
                    },
                    obj -> {
                        assertThat(obj.prefix().equals("foo"));
                        assertThat(obj.ns().equals("http://example.com/foo/"));
                        assertThat(obj.line() == 13);
                        assertThat(obj.depth() == 1);                        
                    }
                );
    }
    
}
