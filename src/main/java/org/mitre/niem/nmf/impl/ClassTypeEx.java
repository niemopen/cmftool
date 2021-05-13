/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mitre.niem.nmf.impl;

import java.util.Set;
import org.mitre.niem.nmf.ClassType;

/**
 *
 * @author SAR
 */
public class ClassTypeEx extends ClassType {
    
    @Override
    public void addToModel(ModelTypeEx m, Set<ObjectTypeEx>seen) {
        if (seen.contains(this)) {
            return;
        }
        String curi = this.uri();
        m.addClass(curi, this);
        if (this.getExtensionOf() != null) {
            ClassTypeEx et = (ClassTypeEx)this.getExtensionOf().getClazz();
            et.addToModel(m, seen);
        }
    }
    
}
