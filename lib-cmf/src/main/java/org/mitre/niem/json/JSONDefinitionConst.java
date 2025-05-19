package org.mitre.niem.json;

import com.google.gson.annotations.SerializedName;
import org.mitre.niem.cmf.Facet;

public class JSONDefinitionConst {
  @SerializedName("const")
  public String $const;
  public String description;

  public JSONDefinitionConst(Facet facet){
    $const = facet.value();
    description = facet.definition();
  }

}
