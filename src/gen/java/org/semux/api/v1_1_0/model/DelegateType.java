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

public class DelegateType  {
  
  @ApiModelProperty(value = "")
  private String address = null;

  @ApiModelProperty(value = "")
  private String name = null;

  @ApiModelProperty(value = "")
  private String registeredAt = null;

  @ApiModelProperty(value = "")
  private String votes = null;

  @ApiModelProperty(value = "")
  private String blocksForged = null;

  @ApiModelProperty(value = "")
  private String turnsHit = null;

  @ApiModelProperty(value = "")
  private String turnsMissed = null;
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

  public DelegateType address(String address) {
    this.address = address;
    return this;
  }

 /**
   * Get name
   * @return name
  **/
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DelegateType name(String name) {
    this.name = name;
    return this;
  }

 /**
   * Get registeredAt
   * @return registeredAt
  **/
  @JsonProperty("registeredAt")
 @Pattern(regexp="^\\d+$")  public String getRegisteredAt() {
    return registeredAt;
  }

  public void setRegisteredAt(String registeredAt) {
    this.registeredAt = registeredAt;
  }

  public DelegateType registeredAt(String registeredAt) {
    this.registeredAt = registeredAt;
    return this;
  }

 /**
   * Get votes
   * @return votes
  **/
  @JsonProperty("votes")
 @Pattern(regexp="^\\d+$")  public String getVotes() {
    return votes;
  }

  public void setVotes(String votes) {
    this.votes = votes;
  }

  public DelegateType votes(String votes) {
    this.votes = votes;
    return this;
  }

 /**
   * Get blocksForged
   * @return blocksForged
  **/
  @JsonProperty("blocksForged")
 @Pattern(regexp="^\\d+$")  public String getBlocksForged() {
    return blocksForged;
  }

  public void setBlocksForged(String blocksForged) {
    this.blocksForged = blocksForged;
  }

  public DelegateType blocksForged(String blocksForged) {
    this.blocksForged = blocksForged;
    return this;
  }

 /**
   * Get turnsHit
   * @return turnsHit
  **/
  @JsonProperty("turnsHit")
 @Pattern(regexp="^\\d+$")  public String getTurnsHit() {
    return turnsHit;
  }

  public void setTurnsHit(String turnsHit) {
    this.turnsHit = turnsHit;
  }

  public DelegateType turnsHit(String turnsHit) {
    this.turnsHit = turnsHit;
    return this;
  }

 /**
   * Get turnsMissed
   * @return turnsMissed
  **/
  @JsonProperty("turnsMissed")
 @Pattern(regexp="^\\d+$")  public String getTurnsMissed() {
    return turnsMissed;
  }

  public void setTurnsMissed(String turnsMissed) {
    this.turnsMissed = turnsMissed;
  }

  public DelegateType turnsMissed(String turnsMissed) {
    this.turnsMissed = turnsMissed;
    return this;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DelegateType {\n");
    
    sb.append("    address: ").append(toIndentedString(address)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    registeredAt: ").append(toIndentedString(registeredAt)).append("\n");
    sb.append("    votes: ").append(toIndentedString(votes)).append("\n");
    sb.append("    blocksForged: ").append(toIndentedString(blocksForged)).append("\n");
    sb.append("    turnsHit: ").append(toIndentedString(turnsHit)).append("\n");
    sb.append("    turnsMissed: ").append(toIndentedString(turnsMissed)).append("\n");
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

