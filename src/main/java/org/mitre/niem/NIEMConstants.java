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
package org.mitre.niem;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public final class NIEMConstants {
    public static final String APPINFO_NS_URI = "http://release.niem.gov/niem/appinfo/5.0/";    
    public static final String APPINFO_NS_URI_PREFIX = "http://release.niem.gov/niem/appinfo/";
    public static final String CODE_LIST_INSTANCE_NS_URI_PREFIX = "http://reference.niem.gov/niem/specification/code-lists/";
    public static final String CONFORMANCE_ATTRIBUTE_NAME = "conformanceTargets";
    public static final String CONFORMANCE_TARGET_NS_URI = "http://release.niem.gov/niem/conformanceTargets/3.0/";
    public static final String CONFORMANCE_TARGET_NS_URI_PREFIX = "http://release.niem.gov/niem/conformanceTargets/";
    public static final String CMF_NS_URI_PREFIX = "http://reference.niem.gov/specification/cmf/";    
    public static final String CMF_NS_URI = "http://reference.niem.gov/specification/cmf/0.6/";
    public static final String DEFAULT_NIEM_VERSION = "5.0";
    public static final String NDR_CT_URI_PREFIX = "http://reference.niem.gov/niem/specification/naming-and-design-rules/";   
    public static final String NIEM_CORE_PATTERN = "http://((publication)|(release))\\.niem\\.gov/niem/niem-core/";
    public static final String NIEM_DOMAIN_PATTERN = "http://((publication)|(release))\\.niem\\.gov/niem/domains/";    
    public static final String NIEM_MODEL_PATTERN  = "http://((publication)|(release))\\.niem\\.gov/niem/";
    public static final String NIEM_PUBLICATION_PREFIX = "http://publication.niem.gov/niem/";
    public static final String NIEM_RELEASE_PREFIX = "http://release.niem.gov/niem/";
    public static final String PROXY_NS_URI = "http://release.niem.gov/niem/proxy/niem-xs/5.0/";     
    public static final String PROXY_NS_URI_PREFIX = "http://release.niem.gov/niem/proxy/"; 
    public static final String OWL_NS_URI = "http://www.w3.org/2002/07/owl";
    public static final String RDF_NS_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns";    
    public static final String RDFS_NS_URI = "http://www.w3.org/2000/01/rdf-schema";
    public static final String STRUCTURES_NS_URI = "http://release.niem.gov/niem/structures/5.0/";
    public static final String STRUCTURES_NS_URI_PREFIX = "http://release.niem.gov/niem/structures/";
    public static final String XML_CATALOG_NS_URI = "urn:oasis:names:tc:entity:xmlns:xml:catalog";
    public static final String XML_NS_URI = "http://www.w3.org/XML/1998/namespace";
    public static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";
    public static final String XSD_NS_URI = "http://www.w3.org/2001/XMLSchema";
    public static final String XSI_NS_URI = "http://www.w3.org/2001/XMLSchema-instance";
    
    public final static String[] builtinNamespaceURIPrefix = { 
        APPINFO_NS_URI_PREFIX, 
        CODE_LIST_INSTANCE_NS_URI_PREFIX,
        CONFORMANCE_TARGET_NS_URI_PREFIX,
        PROXY_NS_URI_PREFIX,
        STRUCTURES_NS_URI_PREFIX
    }; 
    
    private NIEMConstants () { }

}
