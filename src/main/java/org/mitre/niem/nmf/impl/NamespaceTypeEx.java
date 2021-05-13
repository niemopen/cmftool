/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mitre.niem.nmf.impl;

import java.util.Set;
import org.mitre.niem.nmf.NamespaceType;

/**
 *
 * @author SAR
 */
public class NamespaceTypeEx extends NamespaceType {
    
    public void addToModel(ModelTypeEx m, Set<ObjectTypeEx>seen) {
        if (seen.contains(this)) {
            return;
        }
        m.addNamespace(this.namespaceURI, this);
    }    
}
