package org.semux.api.v1_0_2;

import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PeerType  {
  
  @ApiModelProperty(value = "")
  private String ip = null;

  @ApiModelProperty(value = "")
  private Integer port = null;

  @ApiModelProperty(value = "")
  private Integer networkVersion = null;

  @ApiModelProperty(value = "")
  private String clientId = null;

  @ApiModelProperty(value = "")
  private String peerId = null;

  @ApiModelProperty(value = "")
  private String latestBlockNumber = null;

  @ApiModelProperty(value = "")
  private String latency = null;

  @ApiModelProperty(value = "")
  private List<String> capabilities = null;
 /**
   * Get ip
   * @return ip
  **/
  @JsonProperty("ip")
  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public PeerType ip(String ip) {
    this.ip = ip;
    return this;
  }

 /**
   * Get port
   * @return port
  **/
  @JsonProperty("port")
  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public PeerType port(Integer port) {
    this.port = port;
    return this;
  }

 /**
   * Get networkVersion
   * @return networkVersion
  **/
  @JsonProperty("networkVersion")
  public Integer getNetworkVersion() {
    return networkVersion;
  }

  public void setNetworkVersion(Integer networkVersion) {
    this.networkVersion = networkVersion;
  }

  public PeerType networkVersion(Integer networkVersion) {
    this.networkVersion = networkVersion;
    return this;
  }

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

  public PeerType clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

 /**
   * Get peerId
   * @return peerId
  **/
  @JsonProperty("peerId")
  public String getPeerId() {
    return peerId;
  }

  public void setPeerId(String peerId) {
    this.peerId = peerId;
  }

  public PeerType peerId(String peerId) {
    this.peerId = peerId;
    return this;
  }

 /**
   * Get latestBlockNumber
   * @return latestBlockNumber
  **/
  @JsonProperty("latestBlockNumber")
  public String getLatestBlockNumber() {
    return latestBlockNumber;
  }

  public void setLatestBlockNumber(String latestBlockNumber) {
    this.latestBlockNumber = latestBlockNumber;
  }

  public PeerType latestBlockNumber(String latestBlockNumber) {
    this.latestBlockNumber = latestBlockNumber;
    return this;
  }

 /**
   * Get latency
   * @return latency
  **/
  @JsonProperty("latency")
  public String getLatency() {
    return latency;
  }

  public void setLatency(String latency) {
    this.latency = latency;
  }

  public PeerType latency(String latency) {
    this.latency = latency;
    return this;
  }

 /**
   * Get capabilities
   * @return capabilities
  **/
  @JsonProperty("capabilities")
  public List<String> getCapabilities() {
    return capabilities;
  }

  public void setCapabilities(List<String> capabilities) {
    this.capabilities = capabilities;
  }

  public PeerType capabilities(List<String> capabilities) {
    this.capabilities = capabilities;
    return this;
  }

  public PeerType addCapabilitiesItem(String capabilitiesItem) {
    this.capabilities.add(capabilitiesItem);
    return this;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PeerType {\n");
    
    sb.append("    ip: ").append(toIndentedString(ip)).append("\n");
    sb.append("    port: ").append(toIndentedString(port)).append("\n");
    sb.append("    networkVersion: ").append(toIndentedString(networkVersion)).append("\n");
    sb.append("    clientId: ").append(toIndentedString(clientId)).append("\n");
    sb.append("    peerId: ").append(toIndentedString(peerId)).append("\n");
    sb.append("    latestBlockNumber: ").append(toIndentedString(latestBlockNumber)).append("\n");
    sb.append("    latency: ").append(toIndentedString(latency)).append("\n");
    sb.append("    capabilities: ").append(toIndentedString(capabilities)).append("\n");
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

