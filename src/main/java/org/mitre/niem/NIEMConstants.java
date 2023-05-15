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
    public static final String CONFORMANCE_ATTRIBUTE_NAME = "conformanceTargets";
    public static final String CMF_NS_URI_PREFIX = "http://reference.niem.gov/specification/cmf/";    
    public static final String CMF_NS_URI = "http://reference.niem.gov/specification/cmf/0.7/";
    public static final String CMF_STRUCTURES_NS_URI = "http://release.niem.gov/niem/structures/5.0/";
    public static final String DEFAULT_NIEM_VERSION = "5.0"; 
    public static final String OWL_NS_URI = "http://www.w3.org/2002/07/owl";
    public static final String RDF_NS_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns";    
    public static final String RDFS_NS_URI = "http://www.w3.org/2000/01/rdf-schema";
    public static final String XML_CATALOG_NS_URI = "urn:oasis:names:tc:entity:xmlns:xml:catalog";
        
    private NIEMConstants () { }

}
