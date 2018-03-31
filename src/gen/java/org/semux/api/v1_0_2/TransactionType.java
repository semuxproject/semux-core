package org.semux.api.v1_0_2;


import io.swagger.annotations.ApiModelProperty;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TransactionType  {
  
  @ApiModelProperty(value = "")
  private String blockNumber = null;

  @ApiModelProperty(value = "")
  private String hash = null;

  @ApiModelProperty(value = "")
  private String type = null;

  @ApiModelProperty(value = "")
  private String from = null;

  @ApiModelProperty(value = "")
  private String to = null;

  @ApiModelProperty(value = "")
  private String value = null;

  @ApiModelProperty(value = "")
  private String fee = null;

  @ApiModelProperty(value = "")
  private String nonce = null;

  @ApiModelProperty(value = "")
  private String timestamp = null;

  @ApiModelProperty(value = "")
  private String data = null;
 /**
   * Get blockNumber
   * @return blockNumber
  **/
  @JsonProperty("blockNumber")
  public String getBlockNumber() {
    return blockNumber;
  }

  public void setBlockNumber(String blockNumber) {
    this.blockNumber = blockNumber;
  }

  public TransactionType blockNumber(String blockNumber) {
    this.blockNumber = blockNumber;
    return this;
  }

 /**
   * Get hash
   * @return hash
  **/
  @JsonProperty("hash")
  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public TransactionType hash(String hash) {
    this.hash = hash;
    return this;
  }

 /**
   * Get type
   * @return type
  **/
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public TransactionType type(String type) {
    this.type = type;
    return this;
  }

 /**
   * Get from
   * @return from
  **/
  @JsonProperty("from")
  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public TransactionType from(String from) {
    this.from = from;
    return this;
  }

 /**
   * Get to
   * @return to
  **/
  @JsonProperty("to")
  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public TransactionType to(String to) {
    this.to = to;
    return this;
  }

 /**
   * Get value
   * @return value
  **/
  @JsonProperty("value")
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public TransactionType value(String value) {
    this.value = value;
    return this;
  }

 /**
   * Get fee
   * @return fee
  **/
  @JsonProperty("fee")
  public String getFee() {
    return fee;
  }

  public void setFee(String fee) {
    this.fee = fee;
  }

  public TransactionType fee(String fee) {
    this.fee = fee;
    return this;
  }

 /**
   * Get nonce
   * @return nonce
  **/
  @JsonProperty("nonce")
  public String getNonce() {
    return nonce;
  }

  public void setNonce(String nonce) {
    this.nonce = nonce;
  }

  public TransactionType nonce(String nonce) {
    this.nonce = nonce;
    return this;
  }

 /**
   * Get timestamp
   * @return timestamp
  **/
  @JsonProperty("timestamp")
  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public TransactionType timestamp(String timestamp) {
    this.timestamp = timestamp;
    return this;
  }

 /**
   * Get data
   * @return data
  **/
  @JsonProperty("data")
  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public TransactionType data(String data) {
    this.data = data;
    return this;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TransactionType {\n");
    
    sb.append("    blockNumber: ").append(toIndentedString(blockNumber)).append("\n");
    sb.append("    hash: ").append(toIndentedString(hash)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    from: ").append(toIndentedString(from)).append("\n");
    sb.append("    to: ").append(toIndentedString(to)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    fee: ").append(toIndentedString(fee)).append("\n");
    sb.append("    nonce: ").append(toIndentedString(nonce)).append("\n");
    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
    sb.append("    data: ").append(toIndentedString(data)).append("\n");
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

