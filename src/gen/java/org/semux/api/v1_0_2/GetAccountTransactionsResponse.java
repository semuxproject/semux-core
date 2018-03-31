package org.semux.api.v1_0_2;

import java.util.ArrayList;
import java.util.List;
import org.semux.api.v1_0_2.ApiHandlerResponse;
import org.semux.api.v1_0_2.TransactionType;

import io.swagger.annotations.ApiModelProperty;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetAccountTransactionsResponse extends ApiHandlerResponse {
  
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

  public GetAccountTransactionsResponse result(List<TransactionType> result) {
    this.result = result;
    return this;
  }

  public GetAccountTransactionsResponse addResultItem(TransactionType resultItem) {
    this.result.add(resultItem);
    return this;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetAccountTransactionsResponse {\n");
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

