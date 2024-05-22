/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2024 The MITRE Corporation.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.mitre.niem.cmf.Model;

/**
 * Check assertions against a Model object created by ModelXMLReaderTest
 * or ModelFromXSDTest.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class CheckModel {
    
    // Test the model build from xsd?/components.xsd or read from cmf/compnents.cmf
    // Test name, namespace, documentation, and deprecated
    // Test class, property, datatype
    public static void checkComponents (Model m) {
        var ct = m.getClassType("test:Test1Type");
        var dt = m.getDatatype("test:Test2DataType");
        var p1 = m.getProperty("test:AnElement");
        var p2 = m.getProperty("test:AnotherElement");
        
        assertEquals("Test1Type", ct.getName());
        assertEquals("http://example.com/components/", ct.getNamespaceURI());
        assertEquals("Test1Type doc string #1", ct.getDocumentation());
        assertTrue(ct.isDeprecated());
        
        assertEquals("Test2DataType", dt.getName());
        assertEquals("http://example.com/components/", dt.getNamespaceURI());
        assertEquals("Test2DataType doc string", dt.getDocumentation());
        assertTrue(dt.isDeprecated());

        assertEquals("AnElement", p1.getName());
        assertEquals("http://example.com/components/", p1.getNamespaceURI());
        assertEquals("AnElement doc string #1", p1.getDocumentation());
        assertTrue(p1.isDeprecated());
        
        assertEquals("AnotherElement", p2.getName());
        assertEquals("http://example.com/components/", p2.getNamespaceURI());
        assertEquals("AnotherElement doc string #1", p2.getDocumentation());
        assertFalse(p2.isDeprecated());        
    }
    
    // Test the model build from xsd?/datatypes.xsd or read from cmf/datatypes.cmf
    // Test ListDatatype and orderedProperty
    // Test UnionDatatype
    public static void checkDatatypes (Model m) {
       var dtFlt = m.getDatatype("xs:float"); assertNotNull(dtFlt);
       var dtInt = m.getDatatype("xs:integer"); assertNotNull(dtInt);
       var dtLst = m.getDatatype("test:ListType"); assertNotNull(dtLst);
       var dtLs2 = m.getDatatype("test:List2Type"); assertNotNull(dtLs2);
       assertEquals(dtInt, dtLst.getListOf()); 
       assertTrue(dtLst.getOrderedItems());
       assertFalse(dtLs2.getOrderedItems());
       
       var udt = m.getDatatype("test:UnionType"); assertNotNull(udt);
       assertThat(udt.unionOf()).containsExactlyInAnyOrder(dtFlt, dtInt);
    }
}
