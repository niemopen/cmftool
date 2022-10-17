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
package org.mitre.niem.cmf;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class HasProperty extends ObjectType {
    private Property property = null;
    private int minQ = 1;
    private int maxQ = 1;
    private boolean maxUnbounded = false;
    private boolean orderedProperties = false;
    private Namespace augElementNS              = null;
    private final Set<Namespace> augTypeNS      = new HashSet<>();
    
    public void setProperty (Property p)        { property = p; }
    public void setMinOccurs (int m)            { minQ = m; }
    public void setMaxOccurs (int m)            { maxQ = m; }
    public void setMaxUnbounded (boolean f)     { maxUnbounded = f; }
    public void setOrderedProperties (boolean f)  { orderedProperties = f; }
    public void setAugmentElementNS (Namespace n) { augElementNS = n; }
    
    public Property getProperty ()              { return property; }
    public int minOccurs ()                     { return minQ; }
    public int maxOccurs ()                     { return maxQ; } 
    public boolean maxUnbounded ()              { return maxUnbounded; }
    public boolean orderedProperties ()         { return orderedProperties; }
    public Namespace augmentElementNS ()        { return augElementNS; }
    public Set<Namespace> augmentTypeNS ()      { return augTypeNS; }
    
    public HasProperty () { super();  }    
  
}
