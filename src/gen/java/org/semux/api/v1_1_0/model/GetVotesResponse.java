package org.semux.api.v1_1_0.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class GetVotesResponse extends ApiHandlerResponse {
  
  @ApiModelProperty(value = "")
  private Map<String, String> result = null;
 /**
   * Get result
   * @return result
  **/
  @JsonProperty("result")
  public Map<String, String> getResult() {
    return result;
  }

  public void setResult(Map<String, String> result) {
    this.result = result;
  }

  public GetVotesResponse result(Map<String, String> result) {
    this.result = result;
    return this;
  }

  public GetVotesResponse putResultItem(String key, String resultItem) {
    this.result.put(key, resultItem);
    return this;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetVotesResponse {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    result: ").append(toIndentedString(result)).append("\n");
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

