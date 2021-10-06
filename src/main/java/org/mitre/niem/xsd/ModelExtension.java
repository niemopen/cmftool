/*
 * NOTICE
 * 
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 * 
 * Copyright 2020-2021 The MITRE Corporation.
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

import java.util.Map;
import java.util.Set;
import static org.mitre.niem.NIEMConstants.APPINFO_NS_URI;
import static org.mitre.niem.NIEMConstants.PROXY_NS_URI;
import static org.mitre.niem.NIEMConstants.STRUCTURES_NS_URI;
import org.mitre.niem.nmf.Namespace;

/**
 * A class for information within a NIEM XML schema that is not part of the
 * Common Model Format
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

// <ModelExtension>
//   <SchemaDocument>
//     <NamespacePrefix>
//     <NamespaceURI>
//     <DocumentFilepathText>
//     <CatalogFilepathText>
//     <ConformanceTargetURI>
//   <AttributeQName>
   

public class ModelExtension {

    private final Namespace ns_appinfo;
    private final Namespace ns_proxy;
    private final Namespace ns_structures;
    private Map<String,String> fpath;
    private Map<String,String> cpath;
    private Map<String,String> ctarg;
    private Set<String> attQNames;
    
    
    public String getCatalogFilepath (String ns)    { return null; }
    public String getConformanceTargets (String ns) { return null; }
    public String getDocumentFilepath (String nsi)  { return null; }
    
    public String getAppinfoPrefix ()    { return ns_appinfo.getNamespacePrefix(); }
    public String getProxyPrefix ()      { return ns_proxy.getNamespacePrefix(); }
    public String getStructuresPrefix () { return ns_structures.getNamespacePrefix(); }
    public String getAppinfoURI ()       { return ns_appinfo.getNamespaceURI(); }
    public String getProxyURI ()         { return ns_proxy.getNamespaceURI(); }
    public String getStructuresURI ()    { return ns_structures.getNamespaceURI(); }
    
    public boolean isAttribute (String qname) { return false; }
    
    public ModelExtension () {
        ns_appinfo = new Namespace();    ns_appinfo.setNamespacePrefix("appinfo");       ns_appinfo.setNamespaceURI(APPINFO_NS_URI);
        ns_proxy = new Namespace();      ns_proxy.setNamespacePrefix("niem-xs");         ns_proxy.setNamespaceURI(PROXY_NS_URI);
        ns_structures = new Namespace(); ns_structures.setNamespacePrefix("structures"); ns_structures.setNamespaceURI(STRUCTURES_NS_URI);
    }
}
