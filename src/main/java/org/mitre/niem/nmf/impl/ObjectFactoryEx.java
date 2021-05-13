/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mitre.niem.nmf.impl;

import org.mitre.niem.nmf.ClassType;
import org.mitre.niem.nmf.DataPropertyType;
import org.mitre.niem.nmf.DatatypeType;
import org.mitre.niem.nmf.ModelType;
import org.mitre.niem.nmf.NamespaceType;
import org.mitre.niem.nmf.ObjectFactory;
import org.mitre.niem.nmf.ObjectPropertyType;

/**
 *
 * @author SAR
 */
public class ObjectFactoryEx extends ObjectFactory {
    
    @Override
    public ClassType createClassType() {
        return new ClassTypeEx();
    }

    @Override
    public DataPropertyType createDataPropertyType() {
        return new DataPropertyTypeEx();
    }

    @Override
    public DatatypeType createDatatypeType() {
        return new DatatypeTypeEx();
    }    
    @Override
    public ModelType createModelType() {
        return new ModelTypeEx();
    } 
    
    @Override
    public NamespaceType createNamespaceType() {
        return new NamespaceTypeEx();
    }

    @Override
    public ObjectPropertyType createObjectPropertyType() {
        return new ObjectPropertyTypeEx();
    }  
}
