package org.semux.api.v1_1_0.model;

import java.util.ArrayList;
import java.util.List;
import org.semux.api.v1_1_0.model.ApiHandlerResponse;
import org.semux.api.v1_1_0.model.TransactionType;
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

public class GetPendingTransactionsResponse extends ApiHandlerResponse {
  
  @ApiModelProperty(value = "")
  private List<TransactionType> result = null;
 /**
   * Get result
   * @return result
  **/
  @JsonProperty("result")
  public List<TransactionType> getResult() {
    return result;
  }

  public void setResult(List<TransactionType> result) {
    this.result = result;
  }

  public GetPendingTransactionsResponse result(List<TransactionType> result) {
    this.result = result;
    return this;
  }

  public GetPendingTransactionsResponse addResultItem(TransactionType resultItem) {
    this.result.add(resultItem);
    return this;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetPendingTransactionsResponse {\n");
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

