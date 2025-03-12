/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2025 The MITRE Corporation.
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
 * An abstract class for an object in a CMF model.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class CMFObject {
    public static final int CMF_CLASS = 1;
    public static final int CMF_DATAPROP = 2;
    public static final int CMF_DATATYPE = 3;
    public static final int CMF_LIST = 4;
    public static final int CMF_MODEL = 5;
    public static final int CMF_NAMESPACE = 6;
    public static final int CMF_OBJECTPROP = 7;
    public static final int CMF_RESTRICTION = 8;
    public static final int CMF_UNION = 9;   
    public static final int CMF_ANYPROP = 10;
    
    // Override these functions in subclasses
    public String uri ()            { return ""; }
    public int getType ()           { return -1; }
    public boolean isClassType ()   { return false; }
    public boolean isDatatype ()    { return false; }
    public boolean isNamespace ()   { return false; }
    public boolean isProperty ()    { return false; }
    public String cmfElement ()     { return "CMFObject"; }

    public void setContent (String text) { }
    
    // Every object asks its parent class to add the child object.  If the parent
    // doesn't know how, then the object does it.
    public boolean addChild (String eln, String loc, CMFObject child) throws CMFException { return false; }

    // Every object returns true if it knows how to handle adding a child of this type.
    // An object at the inheritance bottom raises an error if it doesn't know how.
    public boolean addToAnyProperty (String eln, String loc, AnyProperty p) { return false; }
    public boolean addToAugmentRecord (String eln, String loc, AugmentRecord r) { return false; }
    public boolean addToCodeListBinding (String eln, String loc, CodeListBinding b) { return false; }
    public boolean addToComponent (String eln, String loc, Component c) { return false; }
    public boolean addToClassType (String eln, String loc, ClassType c) { return false; }
    public boolean addToDataProperty (String eln, String loc, DataProperty p) { return false; }
    public boolean addToDatatype (String eln, String loc, Datatype dt) { return false; }
    public boolean addToFacet (String eln, String loc, Facet f) { return false; }
    public boolean addToListType (String eln, String loc, ListType lt) { return false; }
    public boolean addToProperty (String eln, String loc, Property p) { return false; }
    public boolean addToPropertyAssociation (String eln, String loc, PropertyAssociation h) { return false; }
    public boolean addToLocalTerm (String eln, String loc, LocalTerm l) { return false; }
    public boolean addToModel (String eln, String loc, Model m) throws CMFException { return false; }
    public boolean addToNamespace (String eln, String loc, Namespace ns) throws CMFException { return false; }
    public boolean addToObjectProperty (String eln, String loc, ObjectProperty op) { return false; }    
    public boolean addToRestriction (String eln, String loc, Restriction r) { return false; }
    public boolean addToUnion (String eln, String loc, Union u) { return false; }

}
