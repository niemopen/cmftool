//
// This file was generated by the Eclipse Implementation of JAXB, v3.0.0 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.05.07 at 03:04:07 PM EDT 
//


package org.mitre.niem.nmf;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import org.mitre.niem.nmf.impl.ComponentTypeEx;


/**
 * A data type for a class.
 * 
 * <p>Java class for ClassType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ClassType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://reference.niem.gov/specification/metamodel/5.0alpha1}ComponentType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{http://reference.niem.gov/specification/metamodel/5.0alpha1}AbstractIndicator" minOccurs="0"/&gt;
 *         &lt;element ref="{http://reference.niem.gov/specification/metamodel/5.0alpha1}ExtensionOf" minOccurs="0"/&gt;
 *         &lt;element ref="{http://reference.niem.gov/specification/metamodel/5.0alpha1}ContentStyleCode" minOccurs="0"/&gt;
 *         &lt;element ref="{http://reference.niem.gov/specification/metamodel/5.0alpha1}ClassAugmentationPoint" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;anyAttribute processContents='lax' namespace='urn:us:gov:ic:ntk urn:us:gov:ic:ism'/&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ClassType", propOrder = {
    "abstractIndicator",
    "extensionOf",
    "contentStyleCode",
    "classAugmentationPoint"
})
public class ClassType
    extends ComponentTypeEx
{

    @XmlElement(name = "AbstractIndicator", nillable = true)
    protected Boolean abstractIndicator;
    @XmlElement(name = "ExtensionOf", nillable = true)
    protected ExtensionOfType extensionOf;
    @XmlElement(name = "ContentStyleCode", nillable = true)
    @XmlSchemaType(name = "string")
    protected ContentStyleCodeSimpleType contentStyleCode;
    @XmlElement(name = "ClassAugmentationPoint")
    protected List<Object> classAugmentationPoint;

    /**
     * Gets the value of the abstractIndicator property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isAbstractIndicator() {
        return abstractIndicator;
    }

    /**
     * Sets the value of the abstractIndicator property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setAbstractIndicator(Boolean value) {
        this.abstractIndicator = value;
    }

    /**
     * Gets the value of the extensionOf property.
     * 
     * @return
     *     possible object is
     *     {@link ExtensionOfType }
     *     
     */
    public ExtensionOfType getExtensionOf() {
        return extensionOf;
    }

    /**
     * Sets the value of the extensionOf property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExtensionOfType }
     *     
     */
    public void setExtensionOf(ExtensionOfType value) {
        this.extensionOf = value;
    }

    /**
     * Gets the value of the contentStyleCode property.
     * 
     * @return
     *     possible object is
     *     {@link ContentStyleCodeSimpleType }
     *     
     */
    public ContentStyleCodeSimpleType getContentStyleCode() {
        return contentStyleCode;
    }

    /**
     * Sets the value of the contentStyleCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link ContentStyleCodeSimpleType }
     *     
     */
    public void setContentStyleCode(ContentStyleCodeSimpleType value) {
        this.contentStyleCode = value;
    }

    /**
     * Gets the value of the classAugmentationPoint property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the classAugmentationPoint property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getClassAugmentationPoint().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * 
     * 
     */
    public List<Object> getClassAugmentationPoint() {
        if (classAugmentationPoint == null) {
            classAugmentationPoint = new ArrayList<Object>();
        }
        return this.classAugmentationPoint;
    }

}