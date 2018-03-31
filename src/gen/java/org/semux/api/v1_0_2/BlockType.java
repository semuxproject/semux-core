package org.semux.api.v1_0_2;

import java.util.ArrayList;
import java.util.List;
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

public class BlockType  {
  
  @ApiModelProperty(value = "")
  private String hash = null;

  @ApiModelProperty(value = "")
  private String number = null;

  @ApiModelProperty(value = "")
  private Integer view = null;

  @ApiModelProperty(value = "")
  private String coinbase = null;

  @ApiModelProperty(value = "")
  private String parentHash = null;

  @ApiModelProperty(value = "")
  private String timestamp = null;

  @ApiModelProperty(value = "")
  private String date = null;

  @ApiModelProperty(value = "")
  private String transactionsRoot = null;

  @ApiModelProperty(value = "")
  private String resultsRoot = null;

  @ApiModelProperty(value = "")
  private String stateRoot = null;

  @ApiModelProperty(value = "")
  private String data = null;

  @ApiModelProperty(value = "")
  private List<TransactionType> transactions = null;
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

  public BlockType hash(String hash) {
    this.hash = hash;
    return this;
  }

 /**
   * Get number
   * @return number
  **/
  @JsonProperty("number")
  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  public BlockType number(String number) {
    this.number = number;
    return this;
  }

 /**
   * Get view
   * @return view
  **/
  @JsonProperty("view")
  public Integer getView() {
    return view;
  }

  public void setView(Integer view) {
    this.view = view;
  }

  public BlockType view(Integer view) {
    this.view = view;
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

  public BlockType coinbase(String coinbase) {
    this.coinbase = coinbase;
    return this;
  }

 /**
   * Get parentHash
   * @return parentHash
  **/
  @JsonProperty("parentHash")
  public String getParentHash() {
    return parentHash;
  }

  public void setParentHash(String parentHash) {
    this.parentHash = parentHash;
  }

  public BlockType parentHash(String parentHash) {
    this.parentHash = parentHash;
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

  public BlockType timestamp(String timestamp) {
    this.timestamp = timestamp;
    return this;
  }

 /**
   * Get date
   * @return date
  **/
  @JsonProperty("date")
  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public BlockType date(String date) {
    this.date = date;
    return this;
  }

 /**
   * Get transactionsRoot
   * @return transactionsRoot
  **/
  @JsonProperty("transactionsRoot")
  public String getTransactionsRoot() {
    return transactionsRoot;
  }

  public void setTransactionsRoot(String transactionsRoot) {
    this.transactionsRoot = transactionsRoot;
  }

  public BlockType transactionsRoot(String transactionsRoot) {
    this.transactionsRoot = transactionsRoot;
    return this;
  }

 /**
   * Get resultsRoot
   * @return resultsRoot
  **/
  @JsonProperty("resultsRoot")
  public String getResultsRoot() {
    return resultsRoot;
  }

  public void setResultsRoot(String resultsRoot) {
    this.resultsRoot = resultsRoot;
  }

  public BlockType resultsRoot(String resultsRoot) {
    this.resultsRoot = resultsRoot;
    return this;
  }

 /**
   * Get stateRoot
   * @return stateRoot
  **/
  @JsonProperty("stateRoot")
  public String getStateRoot() {
    return stateRoot;
  }

  public void setStateRoot(String stateRoot) {
    this.stateRoot = stateRoot;
  }

  public BlockType stateRoot(String stateRoot) {
    this.stateRoot = stateRoot;
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

  public BlockType data(String data) {
    this.data = data;
    return this;
  }

 /**
   * Get transactions
   * @return transactions
  **/
  @JsonProperty("transactions")
  public List<TransactionType> getTransactions() {
    return transactions;
  }

  public void setTransactions(List<TransactionType> transactions) {
    this.transactions = transactions;
  }

  public BlockType transactions(List<TransactionType> transactions) {
    this.transactions = transactions;
    return this;
  }

  public BlockType addTransactionsItem(TransactionType transactionsItem) {
    this.transactions.add(transactionsItem);
    return this;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BlockType {\n");
    
    sb.append("    hash: ").append(toIndentedString(hash)).append("\n");
    sb.append("    number: ").append(toIndentedString(number)).append("\n");
    sb.append("    view: ").append(toIndentedString(view)).append("\n");
    sb.append("    coinbase: ").append(toIndentedString(coinbase)).append("\n");
    sb.append("    parentHash: ").append(toIndentedString(parentHash)).append("\n");
    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    transactionsRoot: ").append(toIndentedString(transactionsRoot)).append("\n");
    sb.append("    resultsRoot: ").append(toIndentedString(resultsRoot)).append("\n");
    sb.append("    stateRoot: ").append(toIndentedString(stateRoot)).append("\n");
    sb.append("    data: ").append(toIndentedString(data)).append("\n");
    sb.append("    transactions: ").append(toIndentedString(transactions)).append("\n");
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

