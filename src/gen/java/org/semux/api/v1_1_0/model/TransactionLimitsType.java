package org.semux.api.v1_1_0.model;

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

public class TransactionLimitsType  {
  
  @ApiModelProperty(value = "")
  private Integer maxTransactionDataSize = null;

  @ApiModelProperty(value = "")
  private String minTransactionFee = null;

  @ApiModelProperty(value = "")
  private String minDelegateBurnAmount = null;
 /**
   * Get maxTransactionDataSize
   * @return maxTransactionDataSize
  **/
  @JsonProperty("maxTransactionDataSize")
  public Integer getMaxTransactionDataSize() {
    return maxTransactionDataSize;
  }

  public void setMaxTransactionDataSize(Integer maxTransactionDataSize) {
    this.maxTransactionDataSize = maxTransactionDataSize;
  }

  public TransactionLimitsType maxTransactionDataSize(Integer maxTransactionDataSize) {
    this.maxTransactionDataSize = maxTransactionDataSize;
    return this;
  }

 /**
   * Get minTransactionFee
   * @return minTransactionFee
  **/
  @JsonProperty("minTransactionFee")
 @Pattern(regexp="^\\d+$")  public String getMinTransactionFee() {
    return minTransactionFee;
  }

  public void setMinTransactionFee(String minTransactionFee) {
    this.minTransactionFee = minTransactionFee;
  }

  public TransactionLimitsType minTransactionFee(String minTransactionFee) {
    this.minTransactionFee = minTransactionFee;
    return this;
  }

 /**
   * Get minDelegateBurnAmount
   * @return minDelegateBurnAmount
  **/
  @JsonProperty("minDelegateBurnAmount")
 @Pattern(regexp="^\\d+$")  public String getMinDelegateBurnAmount() {
    return minDelegateBurnAmount;
  }

  public void setMinDelegateBurnAmount(String minDelegateBurnAmount) {
    this.minDelegateBurnAmount = minDelegateBurnAmount;
  }

  public TransactionLimitsType minDelegateBurnAmount(String minDelegateBurnAmount) {
    this.minDelegateBurnAmount = minDelegateBurnAmount;
    return this;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TransactionLimitsType {\n");
    
    sb.append("    maxTransactionDataSize: ").append(toIndentedString(maxTransactionDataSize)).append("\n");
    sb.append("    minTransactionFee: ").append(toIndentedString(minTransactionFee)).append("\n");
    sb.append("    minDelegateBurnAmount: ").append(toIndentedString(minDelegateBurnAmount)).append("\n");
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

