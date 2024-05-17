/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2023 The MITRE Corporation.
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

import static org.mitre.niem.NIEMConstants.DEFAULT_NIEM_VERSION;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.NamespaceKind;
import static org.mitre.niem.cmf.NamespaceKind.NSK_OTHERNIEM;
import org.mitre.niem.cmf.Property;

/**
 *
 * Generates a NIEM 6 schema document pile from a CMF model.
 * Works for most models based on NIEM 3, 4, and 5 namespaces.
 * Won't work for those models with metadata or adapters.  FIXME??
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToSrcXSD extends ModelToXSD {

    public ModelToSrcXSD (Model m) { super(m); }
    
    // Convert NIEM 6 message schema ctarg to subset schema ctarg.
    @Override
    protected String genConformanceTargets (Namespace ns) {
        var ctas = super.genConformanceTargets(ns);
        ctas.replaceAll("#MessageSchemaDocument", "#SubsetSchemaDocument");
        return ctas;
    }  

    // 
    @Override
    protected String fixSchemaVersion (String nsuri) {
        var ns = m.getNamespaceByURI(nsuri);
        var kind = ns.getKind();
        var rv   = ns.getSchemaVersion();
        if (kind > NSK_OTHERNIEM) return rv;// not a model schema, leave @version alone 
        if (null == rv) return "source";    // "source" is the default @version
        if (rv.startsWith("message")) rv = "source" + rv.substring(7);
        return rv;
    } 
    
    @Override
    protected String getArchitecture ()     { return "NIEM6"; }  
    
    // In a source schema all properties are nillable unless refcode NONE is specified.
    @Override
    protected boolean isPropertyNillable (Property p) {
        if (p.isAttribute()) return false;
        if (p.isAbstract())  return false;
        var rc = p.getReferenceCode();
        if (null == rc) return true;
        return !"NONE".equals(rc);
    }
}
