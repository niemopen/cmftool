/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mitre.niem.nmf.impl;

import jakarta.xml.bind.Unmarshaller;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.mitre.niem.nmf.ObjectType;

/**
 *
 * @author SAR
 */
public class ObjectTypeEx extends ObjectType {
    
    static public List<ObjectTypeEx> all = new ArrayList<>();
    
    public ObjectTypeEx () {
        super();
        all.add(this);
    }
    
    public void dump () {
        String olab = this.toString().substring(19);
        System.out.print(String.format("%s: id=%s ref=%s seq=%d\n",
                olab,
                this.getId(),
                this.getRef()==null?"null":this.getRef().toString(),
                this.getSequenceID()
        ));
    }

    public void addToModel (ModelTypeEx m, Set<ObjectTypeEx>seen) {
    }; 
    
    public String debugID () {
        return String.format("Object(0x%x)", this.hashCode());
    }
    
    void afterUnmarshal (Unmarshaller u, Object p) {
        System.out.print(String.format("id=%s  ref=%s\n",
                (id == null ? "null" : id),
                (ref == null ? "null" : ref)
        ));
    }
    
}
