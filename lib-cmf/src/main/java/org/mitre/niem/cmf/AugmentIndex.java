/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2026 The MITRE Corporation.
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

import java.util.List;
import java.util.Set;
import org.mitre.niem.utility.MapToList;

/**
 * A class for indexing the augmentation records in a model by
 * augmenting namespace and augmented class.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class AugmentIndex {
    
    private Model m;

    public AugmentIndex (Model m) { this.m = m; }
    
    // Returns a map from the URI of each class augmented by this namespace
    // to a list of the augmentations for that class in this namespace.
    public MapToList<String,AugmentRecord> nsClassAugRecs (Namespace ns) {
        return null;
    }
    
    // Returns a list of all augmentations in the model for the specified class.
    public List<AugmentRecord> classAugRecs (ClassType ct) {
        return null;
    }
    
    // A property can augment a class without being part of an augmentation type.
    // Returns the augmentation point URI for such a property. 
    // Returns null for other properties.   
    public String propertyAugPointU (Property p) {
        return null;
    }
    
    // Returns a set of property objects that require a reference attribute
    // in an XSD message schema.
    public Set<Property> referenceAttributes () {
        return null;
    }
    
    // Returns a list of attribute augmentation records (that are not part of
    // an augmentaton type) for the specified class URI or global augmentation code.
    public List<AugmentRecord> attributeAugmentations (String uriOrCode) {
        return null;
    }
    
    public Set<String> augmentationElementURIs (String augPointU) {
        return null;
    }
}
