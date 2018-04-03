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

public class InfoType  {
  
  @ApiModelProperty(value = "")
  private String clientId = null;

  @ApiModelProperty(value = "")
  private String coinbase = null;

  @ApiModelProperty(value = "")
  private String latestBlockNumber = null;

  @ApiModelProperty(value = "")
  private String latestBlockHash = null;

  @ApiModelProperty(value = "")
  private Integer activePeers = null;

  @ApiModelProperty(value = "")
  private Integer pendingTransactions = null;
 /**
   * Get clientId
   * @return clientId
  **/
  @JsonProperty("clientId")
  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public InfoType clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

 /**
   * Get coinbase
   * @return coinbase
  **/
  @JsonProperty("coinbase")
  public String getCoinbase() {
    return coinbase;
  }

  public void setCoinbase(String coinbase) {
    this.coinbase = coinbase;
  }

  public InfoType coinbase(String coinbase) {
    this.coinbase = coinbase;
    return this;
  }

 /**
   * Get latestBlockNumber
   * @return latestBlockNumber
  **/
  @JsonProperty("latestBlockNumber")
 @Pattern(regexp="^\\d+$")  public String getLatestBlockNumber() {
    return latestBlockNumber;
  }

  public void setLatestBlockNumber(String latestBlockNumber) {
    this.latestBlockNumber = latestBlockNumber;
  }

  public InfoType latestBlockNumber(String latestBlockNumber) {
    this.latestBlockNumber = latestBlockNumber;
    return this;
  }

 /**
   * Get latestBlockHash
   * @return latestBlockHash
  **/
  @JsonProperty("latestBlockHash")
  public String getLatestBlockHash() {
    return latestBlockHash;
  }

  public void setLatestBlockHash(String latestBlockHash) {
    this.latestBlockHash = latestBlockHash;
  }

  public InfoType latestBlockHash(String latestBlockHash) {
    this.latestBlockHash = latestBlockHash;
    return this;
  }

 /**
   * Get activePeers
   * @return activePeers
  **/
  @JsonProperty("activePeers")
  public Integer getActivePeers() {
    return activePeers;
  }

  public void setActivePeers(Integer activePeers) {
    this.activePeers = activePeers;
  }

  public InfoType activePeers(Integer activePeers) {
    this.activePeers = activePeers;
    return this;
  }

 /**
   * Get pendingTransactions
   * @return pendingTransactions
  **/
  @JsonProperty("pendingTransactions")
  public Integer getPendingTransactions() {
    return pendingTransactions;
  }

  public void setPendingTransactions(Integer pendingTransactions) {
    this.pendingTransactions = pendingTransactions;
  }

  public InfoType pendingTransactions(Integer pendingTransactions) {
    this.pendingTransactions = pendingTransactions;
    return this;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InfoType {\n");
    
    sb.append("    clientId: ").append(toIndentedString(clientId)).append("\n");
    sb.append("    coinbase: ").append(toIndentedString(coinbase)).append("\n");
    sb.append("    latestBlockNumber: ").append(toIndentedString(latestBlockNumber)).append("\n");
    sb.append("    latestBlockHash: ").append(toIndentedString(latestBlockHash)).append("\n");
    sb.append("    activePeers: ").append(toIndentedString(activePeers)).append("\n");
    sb.append("    pendingTransactions: ").append(toIndentedString(pendingTransactions)).append("\n");
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

