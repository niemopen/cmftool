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
 * A class for <cmf:NamespaceAugmentingRecord>
 * Records all the augmentations in a namespace.  If every augmentation was an
 * AugmentationType, we could just use ClassType for this, but they aren't so we
 * can't. 
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class AugmentRecord extends ObjectType {
    private ClassType classType = null;
    private Property property = null;
    private int indexInType = -1;
    private int minQ = 0;
    private int maxQ = 0;
    private boolean maxUnbounded = false;
    private boolean orderedProperties = false;
    
    public void setClassType (ClassType c)      { classType = c; }    
    public void setProperty (Property p)        { property = p; }
    public void setIndexInType( int i)          { indexInType = i; }
    public void setMinOccurs (int m)            { minQ = m; }
    public void setMaxOccurs (int m)            { maxQ = m; }
    public void setMaxUnbounded (boolean f)     { maxUnbounded = f; }
    public void setOrderedProperties (boolean f)  { orderedProperties = f; }
    
    public ClassType getClassType()             { return classType; }
    public Property getProperty ()              { return property; }
    public int indexInType ()                   { return indexInType; }
    public int minOccurs ()                     { return minQ; }
    public int maxOccurs ()                     { return maxQ; } 
    public boolean maxUnbounded ()              { return maxUnbounded; }
    public boolean orderedProperties ()         { return orderedProperties; }
    
    public AugmentRecord () {}
}