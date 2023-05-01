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
package org.mitre.niem.cmf;

/**
 * A class for recording some properties of an XML Schema document.
 * We extract these values when building a Model from NIEM XSD.
 * You can set these values as desired when building a Model from scratch.
 * Values are used when generating NIEM XSD from a Model.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class SchemaDocument extends ObjectType {
    private String targetNS = null;
    private String targetPrefix = null;
    private String confTargs = null;
    private String filepath = null;
    private String niemVersion = null;
    private String schemaVersion = null;
    private String language = null;
    
    public void setTargetPrefix (String s)  { targetPrefix = s; }
    public void setTargetNS (String s)      { targetNS = s; }
    public void setConfTargets (String s)   { confTargs = s; }
    public void setFilePath (String s)      { filepath = s; }
    public void setNIEMversion (String s)   { niemVersion = s; }
    public void setSchemaVersion (String s) { schemaVersion = s; }
    public void setLanguage (String s)      { language = s; }
    
    public String targetPrefix()            { return targetPrefix; }
    public String targetNS()                { return targetNS; }
    public String confTargets ()            { return confTargs; }
    public String filePath ()               { return filepath; }
    public String niemVersion ()            { return niemVersion; }
    public String schemaVersion ()          { return schemaVersion; }
    public String language ()               { return language; }
    
    public SchemaDocument () { super(); }
    
    public SchemaDocument (String prefix, String ns, String ct, String fp, String nv, String sv, String lang) {
        super();
        targetPrefix = prefix;
        targetNS = ns;
        confTargs = ct;
        filepath = fp;
        niemVersion = nv;
        schemaVersion = sv;
        language = lang;
    }
}
