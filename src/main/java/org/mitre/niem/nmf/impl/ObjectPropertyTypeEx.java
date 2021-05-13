/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mitre.niem.nmf.impl;

import java.util.Set;
import org.mitre.niem.nmf.ObjectPropertyType;

/**
 *
 * @author SAR
 */
public class ObjectPropertyTypeEx extends ObjectPropertyType {
    
    @Override
    public void addToModel(ModelTypeEx m, Set<ObjectTypeEx>seen) {
        if (seen.contains(this)) {
            return;
        }
        String curi = this.uri();
        m.addObjectProperty(curi, this);
        if (this.getSubPropertyOf() != null) {
            ObjectPropertyTypeEx sp = (ObjectPropertyTypeEx)this.getSubPropertyOf().getObjectProperty();
            sp.addToModel(m, seen);
        }
        ClassTypeEx c = (ClassTypeEx) this.getClazz();
        if (c != null) {
            c.addToModel(m, seen);
        }
    }
}
