package org.mitre.niem.json;

import com.google.gson.annotations.SerializedName;
import org.mitre.niem.cmf.Facet;

import java.util.ArrayList;

public class JSONDefinitionEnum {

  public JSONDefinitionEnum(Facet facet){
    if ($enum == null) $enum = new ArrayList<>();

    $enum.add(facet.getStringVal());
    description = facet.getDefinition();
  }

  @SerializedName("enum")
  public ArrayList<String> $enum;
  public String description;
}
