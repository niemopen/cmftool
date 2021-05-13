/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mitre.niem.nmf.impl;

import java.util.List;
import java.util.Set;
import org.mitre.niem.nmf.DatatypeType;

/**
 *
 * @author SAR
 */
public class DatatypeTypeEx extends DatatypeType {
    
    @Override
    public void addToModel(ModelTypeEx m, Set<ObjectTypeEx>seen) {
        if (seen.contains(this)) {
            return;
        }
        String curi = this.uri();
        m.addDatatype(curi, this);
        if (this.getRestrictionOf() != null) {
            DatatypeTypeEx rdt = (DatatypeTypeEx)this.getRestrictionOf().getDatatype();
            if (rdt != null) {
                rdt.addToModel(m, seen);
            }
        }
        if (this.getUnionOf() != null) {
            List<DatatypeType> ul = this.getUnionOf().getDatatype();
            if (ul != null) {
                for (DatatypeType udt : ul) {
                    DatatypeTypeEx udtx = (DatatypeTypeEx) udt;
                    udtx.addToModel(m, seen);
                }
            }
        }
    }    
}
