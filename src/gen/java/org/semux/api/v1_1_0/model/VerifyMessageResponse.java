package org.semux.api.v1_1_0.model;

import org.semux.api.v1_1_0.model.ApiHandlerResponse;
import javax.validation.constraints.*;

import io.swagger.annotations.ApiModelProperty;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VerifyMessageResponse extends ApiHandlerResponse {
  
  @ApiModelProperty(value = "")
  private Boolean validSignature = null;
 /**
   * Get validSignature
   * @return validSignature
  **/
  @JsonProperty("validSignature")
  public Boolean isValidSignature() {
    return validSignature;
  }

  public void setValidSignature(Boolean validSignature) {
    this.validSignature = validSignature;
  }

  public VerifyMessageResponse validSignature(Boolean validSignature) {
    this.validSignature = validSignature;
    return this;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VerifyMessageResponse {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    validSignature: ").append(toIndentedString(validSignature)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private static String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

