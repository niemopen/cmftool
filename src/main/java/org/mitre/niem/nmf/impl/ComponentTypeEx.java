/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mitre.niem.nmf.impl;

import java.util.Set;
import org.mitre.niem.nmf.ComponentType;

/**
 *
 * @author SAR
 */
public abstract class ComponentTypeEx extends ComponentType {
    
    public String uri() {
        String ncn   = this.getName();
        String nsuri = this.getNamespace().getNamespaceURI();
        String curi;
        if (nsuri.endsWith("#")) {
           curi = nsuri + ncn;
        }
        else {
            curi = nsuri + "#" + ncn;
        }
        return curi;
    }

}
