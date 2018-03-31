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

public class AccountType  {
  
  @ApiModelProperty(value = "")
  private String address = null;

  @ApiModelProperty(value = "")
  private String available = null;

  @ApiModelProperty(value = "")
  private String locked = null;

  @ApiModelProperty(value = "")
  private String nonce = null;

  @ApiModelProperty(value = "")
  private Integer transactionCount = null;
 /**
   * Get address
   * @return address
  **/
  @JsonProperty("address")
  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public AccountType address(String address) {
    this.address = address;
    return this;
  }

 /**
   * Get available
   * @return available
  **/
  @JsonProperty("available")
  public String getAvailable() {
    return available;
  }

  public void setAvailable(String available) {
    this.available = available;
  }

  public AccountType available(String available) {
    this.available = available;
    return this;
  }

 /**
   * Get locked
   * @return locked
  **/
  @JsonProperty("locked")
  public String getLocked() {
    return locked;
  }

  public void setLocked(String locked) {
    this.locked = locked;
  }

  public AccountType locked(String locked) {
    this.locked = locked;
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

  public AccountType nonce(String nonce) {
    this.nonce = nonce;
    return this;
  }

 /**
   * Get transactionCount
   * @return transactionCount
  **/
  @JsonProperty("transactionCount")
  public Integer getTransactionCount() {
    return transactionCount;
  }

  public void setTransactionCount(Integer transactionCount) {
    this.transactionCount = transactionCount;
  }

  public AccountType transactionCount(Integer transactionCount) {
    this.transactionCount = transactionCount;
    return this;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AccountType {\n");
    
    sb.append("    address: ").append(toIndentedString(address)).append("\n");
    sb.append("    available: ").append(toIndentedString(available)).append("\n");
    sb.append("    locked: ").append(toIndentedString(locked)).append("\n");
    sb.append("    nonce: ").append(toIndentedString(nonce)).append("\n");
    sb.append("    transactionCount: ").append(toIndentedString(transactionCount)).append("\n");
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

