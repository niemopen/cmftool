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
import org.mitre.niem.cmf.NamespaceKind;
import static org.mitre.niem.cmf.NamespaceKind.NSK_CORE;
import static org.mitre.niem.cmf.NamespaceKind.NSK_OTHERNIEM;

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
    
    // Convert NIEM v3-5 ctargs to NIEM 6.
    // Convert NIEM 6 message schema to subset schema ctarg.
    @Override
    protected String fixConformanceTargets (String ctaStr) {
        if (null == ctaStr) return null;
        if (ctaStr.isBlank()) return "";
        var ctab = new StringBuilder();
        var ctas = ctaStr.split("\\s+");
        var n5pf = NamespaceKind.getCTPrefix("NIEM5");
        var n6pf = NamespaceKind.getCTPrefix("NIEM6");
        var sep  = "";
        for (String cta : ctas) {
            if (cta.startsWith(n5pf)) {
                var targ = NamespaceKind.targetFromCTA(cta);
                cta = n6pf + DEFAULT_NIEM_VERSION + "/" + targ;
            }
            cta = cta.replace("MessageSchemaDocument", "SubsetSchemaDocument");
            ctab.append(sep).append(cta);
            sep = " ";
        }
        ctaStr = ctab.toString();               
        return ctaStr;
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
    
    @Override
    protected String getShareSuffix ()      { return "-src"; }    
}
