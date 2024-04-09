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
package org.mitre.niem.cmf;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class CodeListBinding extends ObjectType {
    private String uri = null;
    private String column = null;
    private boolean isConstraining = false;
    
    public void setURI (String s)               { uri = s; }
    public void setColumm (String s)            { column = s; }    
    public void setIsConstraining (boolean f)   { isConstraining = f; }
    
    public String getURI()              { return uri; }
    public String getColumn()           { return column; }
    public boolean getIsConstraining()  { return isConstraining; }

    public CodeListBinding () { super(); }
    
}