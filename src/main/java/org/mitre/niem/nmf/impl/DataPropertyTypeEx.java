/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mitre.niem.nmf.impl;

import java.util.Set;
import org.mitre.niem.nmf.DataPropertyType;

/**
 *
 * @author SAR
 */
public class DataPropertyTypeEx extends DataPropertyType {
    
    @Override
    public void addToModel(ModelTypeEx m, Set<ObjectTypeEx>seen) {
        if (seen.contains(this)) {
            return;
        }
        String curi = this.uri();
        m.addDataProperty(curi, this);
        if (this.getDatatype() != null) {
            DatatypeTypeEx dt = (DatatypeTypeEx) this.getDatatype();
            dt.addToModel(m, seen);
        }
    }    
}
