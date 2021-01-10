/* VU Amsterdam, Social AI Group
 * Bilgin Avenoðlu, 10/03/2020 */

package sic;

import java.io.Serializable;

import org.redisson.api.RCascadeType;
import org.redisson.api.annotation.RCascade;
import org.redisson.api.annotation.REntity;
import org.redisson.api.annotation.RId;
import org.redisson.liveobject.resolver.UUIDGenerator;

@REntity
public class EndPoint implements Serializable {
	@RId(generator = UUIDGenerator.class)
	private String endPointID;
	private ProtocolType protocolType;
	private String ipNumber;
	private String webURI;
	private String portNumber;
	private String topicID;

	public EndPoint() {

	}

	public EndPoint(String endPointID) {
		super();
		this.endPointID = endPointID;
	}

	public String getEndPointID() {
		return this.endPointID;
	}

	public ProtocolType getProtocolType() {
		return this.protocolType;
	}

	public void setProtocolType(ProtocolType protocolType) {
		this.protocolType = protocolType;
	}

	public String getIpNumber() {
		return ipNumber;
	}

	public void setIpNumber(String ipNumber) {
		this.ipNumber = ipNumber;
	}

	public String getWebURI() {
		return webURI;
	}

	public void setWebURI(String webURI) {
		this.webURI = webURI;
	}

	public String getPortNumber() {
		return portNumber;
	}

	public void setPortNumber(String portNumber) {
		this.portNumber = portNumber;
	}

	public String getTopicID() {
		return topicID;
	}

	public void setTopicID(String topicID) {
		this.topicID = topicID;
	}
}
