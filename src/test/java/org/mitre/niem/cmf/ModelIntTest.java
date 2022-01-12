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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelIntTest {
    
    Model m; 
    Namespace fish;
    Namespace cats; 
    Namespace dogs;
    ClassType ft;
    Property p;
    Datatype d;
  
        
    public ModelIntTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
        m = new Model();
        fish = new Namespace("fish", "fishNamespace");
        cats = new Namespace("cats", "catNamespace"); 
        dogs = new Namespace("dogs", "dogNamespace");    
        try {
            fish.addToModel(m);
            cats.addToModel(m);
            dogs.addToModel(m);
        } catch (CMFException ex) {
            fail("addToModel should not have thrown CMFException");
        } 
        ft = new ClassType(fish, "FishClass");
        ft.addToModel(m);
        p = new Property(fish, "FishProp");
        p.addToModel(m);       
        d = new Datatype(fish, "FishDatatype");  
        d.addToModel(m);
    }
    
    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testAddToModel () {
        assertEquals(3, m.getNamespaceList().size());
        assertEquals("cats", m.getNamespaceList().get(0).getNamespacePrefix());
        assertEquals("dogNamespace", m.getNamespaceList().get(1).getNamespaceURI());
        assertEquals("fish", m.getNamespaceByURI("fishNamespace").getNamespacePrefix());
        assertEquals("fishNamespace", m.getNamespaceByPrefix("fish").getNamespaceURI()); 
        assertNotNull(m.getComponent("fish:FishClass"));       
        assertNotNull(m.getComponent("fish:FishProp"));     
        assertNotNull(m.getComponent("fish:FishDatatype"));   
        assertNotNull(m.getClassType("fishNamespace", "FishClass"));
        assertNotNull(m.getDatatype("fishNamespace", "FishDatatype"));
        assertNotNull(m.getProperty("fishNamespace", "FishProp"));
        assertNotNull(m.getNamespaceByPrefix("fish"));
        assertNotNull(m.getNamespaceByURI("fishNamespace"));
        assertEquals(3, m.getComponentList().size());
        assertEquals("fish:FishClass", m.getComponentList().get(0).getQName());
        assertNull(m.getComponent("bird:Element"));
        assertNull(m.getNamespaceByPrefix("bird"));
    }
    
    @Test
    public void testSetName () {    
        ft.setName("ZClass");
        assertNull(m.getComponent("fish:FishClass"));
        assertNotNull(m.getComponent("fish:ZClass"));
        assertEquals("fish:ZClass", m.getComponentList().get(2).getQName());
    }
    
    @Test
    public void testSetNamespace () {     
        ft.setNamespace(dogs);
        assertNull(m.getComponent("fish:FishClass"));
        assertNotNull(m.getComponent("dogs:FishClass"));
        assertEquals("dogs:FishClass", m.getComponentList().get(0).getQName());
    }
    
    @Test
    public void testSetNamespacePrefix () {
        assertNotNull(m.getComponent("fish:FishProp"));
        assertNull(m.getComponent("newfish:FishProp"));
        try {
            fish.setNamespacePrefix("newfish");
        } catch (CMFException ex) {
            fail("setNamespacePrefix should not have thrown CMFException");
        }
        assertNull(m.getComponent("fish:FishProp"));
        assertNotNull(m.getComponent("newfish:FishProp"));
        
        CMFException ex = assertThrows(
                CMFException.class, () -> fish.setNamespacePrefix("dogs"),
                "setNamespacePrefix failed to throw CMFException");
        assertTrue(ex.getMessage().contains("Duplicate namespace prefix"));                
    }
    
    @Test
    public void testAddNamespace () {
        Namespace dup = new Namespace("dogs", "moreDogs");
        CMFException ex = assertThrows(
                CMFException.class, () -> dup.addToModel(m),
                "addToModel failed to throw CMFException");
        assertTrue(ex.getMessage().contains("Duplicate namespace prefix"));
    }
    
}
