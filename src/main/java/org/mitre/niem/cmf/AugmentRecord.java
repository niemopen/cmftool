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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class for <cmf:NamespaceAugmentingRecord>
 * Records all the augmentations in a namespace.  If every augmentation was an
 * AugmentationType, we could just use ClassType for this, but they aren't so we
 * can't. 
 * 
 * An index of -1 for an element indicates a non-augmentation element substituted
 * for an augmentation point.
 * 
 * An index of -1 for an attribute indicates an attribute augmentation, not part
 * of an augmentation type.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class AugmentRecord extends ObjectType implements Comparable<AugmentRecord> {
    static final Logger LOG = LogManager.getLogger(AugmentRecord.class);
        
    private ClassType classType = null;         // the augmented Class
    private Property property = null;           // the agumenting Property
    private int indexInType = -1;               // index of Property in augmentation type, or -1 if no such type
    private int minQ = 0;                       // minOccurs
    private int maxQ = 0;                       // maxOccurs
    private boolean maxUnbounded = false;       // unbounded
    private boolean orderedProperty = false;    // is this property ordered?
    private String globalAugComp = null;        // QName of global component augmented (eg. structures:ObjectType)
    
    public void setClassType (ClassType c)      { classType = c; }    
    public void setProperty (Property p)        { property = p; }
    public void setIndexInType( int i)          { indexInType = i; }
    public void setMinOccurs (int m)            { minQ = m; }
    public void setMaxOccurs (int m)            { maxQ = m; }
    public void setMaxUnbounded (boolean f)     { maxUnbounded = f; }
    public void setOrderedProperties (boolean f)  { orderedProperty = f; }
    
    public ClassType getClassType()             { return classType; }
    public Property getProperty ()              { return property; }
    public int indexInType ()                   { return indexInType; }
    public int minOccurs ()                     { return minQ; }
    public int maxOccurs ()                     { return maxQ; } 
    public boolean maxUnbounded ()              { return maxUnbounded; }
    public boolean orderedProperties ()         { return orderedProperty; }
    
    public String getGlobalAugmented ()         { return globalAugComp; }
    public void setGlobalAugmented (String s)   { globalAugComp = s; }
    
    // Global augmentation codes 
    public static final int AUG_NONE   = 0;     // not a global augmentation
    public static final int AUG_ASSOC  = 1;     // a global augmentation for associations
    public static final int AUG_OBJECT = 2;     // a global augmentation for objects
    public static final int AUG_SIMPLE = 3;     // a global augmentation for simple content
    public static final int AUG_NUMCODES = 4;   // this many kinds of augmentations
    
    public int getGlobalAug () {
        if (null == globalAugComp) return AUG_NONE;
        var cname = globalAugComp.substring(globalAugComp.indexOf(":")+1);
        if (cname.startsWith("Association")) return AUG_ASSOC;
        if (cname.startsWith("Object")) return AUG_OBJECT;
        if (cname.startsWith("Simple")) return AUG_SIMPLE;
        LOG.error("unknown global augmented component {}", globalAugComp);
        return AUG_NONE;
    }
        
    public AugmentRecord () {}
    
    @Override
    public int compareTo (AugmentRecord o) {
        int rv = 0;
        if (null != this.classType && null != o.classType) rv = this.classType.compareTo(o.classType);
        if (0 == rv) rv = this.property.getQName().compareTo(o.property.getQName());
        if (0 == rv) rv = this.globalAugComp.compareTo(o.globalAugComp);
        return rv;
    }
}
