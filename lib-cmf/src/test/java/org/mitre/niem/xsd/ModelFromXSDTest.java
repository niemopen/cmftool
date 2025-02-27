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
package org.mitre.niem.xsd;

import org.assertj.core.api.Assertions;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Facet;
import org.mitre.niem.xml.LanguageString;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelFromXSDTest {
    private final static String resDN = "src/test/resources/";
    
    public ModelFromXSDTest() {
    }
    
    @Test
    public void testAttributeAugmentation () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/attAugment.xsd");
        var model = mb.createModel(sch);

        assertNull(model.qnToDatatype("test:TCodeList2Type"));
        assertNotNull(model.qnToDatatype("test:TCodeList3Type"));
    }
    
    @Test
    public void testGlobalAttributeAugmentation () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/globalAttAugment.xsd");
        var model = mb.createModel(sch);

        assertNull(model.qnToDatatype("test:TCodeList2Type"));
        assertNull(model.qnToDatatype("test:TCodeList3Type"));
    }
        
    @Test
    public void testCodeListBindings () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/codeListBinding.xsd");
        var model = mb.createModel(sch);
        
        var dt = model.qnToDatatype("test:TCodeList2Type");
        var clb = dt.codeListBinding();
        assertEquals("http://code.list.uri", clb.codeListURI());
        assertEquals("", clb.column());
        assertTrue(clb.isConstraining());

        dt = model.qnToDatatype("test:TCodeList3Type");
        clb = dt.codeListBinding();
        assertEquals("http://list.uri", clb.codeListURI());
        assertEquals("key", clb.column());
        assertFalse(clb.isConstraining());
    }

    @Test
    public void testDatatypes () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/datatypes.xsd");
        var model = mb.createModel(sch);
        
        var dt = model.qnToDatatype("test:List1Type");
        var docL = dt.docL();
        var item = dt.itemType();
        assertThat(docL).extracting(LanguageString::text).containsExactly("List of a named simple type");
        assertEquals("xs:integer", item.qname());
        
        dt = model.qnToDatatype("test:List2Type");
        item = dt.itemType();
        assertEquals("test:UnionType", item.qname());
        
        dt = model.qnToDatatype("test:List3Type");
        item = dt.itemType();
        assertEquals("test:Restrict8Type", item.qname());
        
        assertNull(model.qnToDatatype("test:LiteralType"));
        
        assertNull(model.qnToDatatype("test:Literal2Type"));
        dt = model.qnToDatatype("test:Literal2SimpleType");
        var base = dt.base();
        assertEquals("xs:float", base.qname());

        assertNull(model.qnToDatatype("test:Literal3Type"));
        assertNull(model.qnToDatatype("test:Literal4Type"));
        
        dt = model.qnToDatatype("test:Restrict1Type");
        base = dt.base();
        var fL = dt.facetL();
        assertEquals("xs:token", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactly(
                Assertions.tuple("MinLength", "3"),
                Assertions.tuple("MaxLength", "4"),
                Assertions.tuple("Pattern", "[A-Z]{3}"),
                Assertions.tuple("Enumeration", "FOO"),
                Assertions.tuple("Enumeration", "BAR"));
        assertThat(fL.get(0).docL()).isEmpty();
        assertThat(fL.get(1).docL()).isEmpty();
        assertThat(fL.get(2).docL()).isEmpty();         
        assertThat(fL.get(3).docL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(Assertions.tuple("The FOO token", "en-US"));
        assertThat(fL.get(4).docL()).isEmpty();      
        
        dt = model.qnToDatatype("test:Restrict2Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:integer", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("MinInclusive", "10"),
                Assertions.tuple("MaxInclusive", "20"));

        dt = model.qnToDatatype("test:Restrict3Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:integer", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("MinExclusive", "30"),
                Assertions.tuple("MaxExclusive", "40"));

        dt = model.qnToDatatype("test:Restrict4Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:string", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("MinLength", "3"),
                Assertions.tuple("MaxLength", "4"));

        dt = model.qnToDatatype("test:Restrict5Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:string", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("Length", "3"),
                Assertions.tuple("WhiteSpace", "collapse"));
        

        dt = model.qnToDatatype("test:Restrict6Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:decimal", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("FractionDigits", "2"),
                Assertions.tuple("TotalDigits", "5"));
        

        dt = model.qnToDatatype("test:Restrict7Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:token", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("Enumeration", "GB"),
                Assertions.tuple("Enumeration", "US"));  
        assertThat(fL.get(0).docL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(
                Assertions.tuple("UNITED KINGDOM", "en-US"));
        assertThat(fL.get(1).docL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(
                Assertions.tuple("UNITED STATES", "en-US"),
                Assertions.tuple("LES ETAS-UNIS", "fr"));
    
        dt = model.qnToDatatype("test:Restrict8Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:token", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("Enumeration", "GB"),
                Assertions.tuple("Enumeration", "US"));      
    
        dt = model.qnToDatatype("test:Restrict9Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:integer", base.qname());
        assertThat(fL).isEmpty();
      
        dt = model.qnToDatatype("test:Restrict10Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:integer", base.qname());
        assertThat(fL).isEmpty();
      
        dt = model.qnToDatatype("test:UnionType");
        assertThat(dt.memberL())
            .extracting(Datatype::qname)
            .containsExactlyInAnyOrder("xs:integer", "xs:float", "test:Restrict8Type");
    }

    @Test
    public void testNamespaces () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd/imports.xsd");
        var model = mb.createModel(sch);
        
        var test = model.prefixToNamespaceObj("test");
        var nc   = model.prefixToNamespaceObj("nc");
        var j    = model.prefixToNamespaceObj("j");
        var cli  = model.prefixToNamespaceObj("cli");
        var xml  = model.prefixToNamespaceObj("xml");
        var gml  = model.prefixToNamespaceObj("gml");
        
        assertEquals("http://example.com/test/", test.uri());
        assertEquals("https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/", nc.uri());
        assertEquals("https://docs.oasis-open.org/niemopen/ns/model/domains/justice/6.0/", j.uri());
        assertEquals("https://docs.oasis-open.org/niemopen/ns/specification/code-lists/6.0/instance/", cli.uri());
        assertEquals("http://www.w3.org/XML/1998/namespace", xml.uri());
        assertEquals("http://www.opengis.net/gml/3.2", gml.uri());
        assertThat(gml.impDocL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(Assertions.tuple("Geography Markup Language.", "en-US"));        
        int x = 0;
    }
    
}
