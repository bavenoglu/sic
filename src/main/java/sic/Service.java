/* VU Amsterdam, Social AI Group
 * Bilgin Avenoðlu, 10/03/2020 */

package sic;

import java.io.Serializable;
import java.util.Properties;
import org.apache.camel.CamelContext;
import org.redisson.api.RCascadeType;
import org.redisson.api.RLiveObjectService;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.annotation.RCascade;
import org.redisson.api.annotation.REntity;
import org.redisson.api.annotation.RId;
import org.redisson.api.annotation.RIndex;
import org.redisson.liveobject.resolver.UUIDGenerator;
import org.slf4j.LoggerFactory;
import sic.redis.GetRedisService;

@REntity
public class Service implements Serializable {

	private static org.slf4j.Logger log = LoggerFactory.getLogger(Service.class);
	@RId(generator = UUIDGenerator.class)
	private String serviceID;
	@RIndex
	private String serviceName;
	@RCascade(RCascadeType.DELETE)
	private EndPoint rcvEndPoint;
	@RCascade(RCascadeType.DELETE)
	private EndPoint sndEndPoint;

	public Service() {
		
	}

	public Service(String serviceID) {
		super();
		this.serviceID = serviceID;
		this.serviceName = serviceID;
	}

	public String getServiceID() {
		return serviceID;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public EndPoint getRcvEndPoint() {
		return rcvEndPoint;
	}

	public void setRcvEndPoint(EndPoint rcvEndPoint) {
		this.rcvEndPoint = rcvEndPoint;
	}

	public EndPoint getSndEndPoint() {
		return sndEndPoint;
	}

	public void setSndEndPoint(EndPoint sndEndPoint) {
		this.sndEndPoint = sndEndPoint;
	}

	public void dataReceiving(CamelContext context, RedissonClient redisson) throws Exception {
		if (this.getRcvEndPoint().getProtocolType().compareTo(ProtocolType.HTTP) == 0) {
			// nothing to do. The Web address is entered to the db (from the sic web site)
			// by service providers.
			if (this.getRcvEndPoint().getWebURI() == null)
				log.error("Web address for receiving data for service -" + this.getServiceID() + "- must be entered.");
			else
				log.info("Web URI -" + this.getRcvEndPoint().getWebURI() + "- is ready for receiving data.");
		} else if (this.getRcvEndPoint().getProtocolType().compareTo(ProtocolType.REDIS) == 0) {
			this.getRcvEndPoint().setTopicID(this.getRcvEndPoint().getEndPointID());
			log.info("Redis topic -" + this.getRcvEndPoint().getTopicID() + "- is created for receiving data.");
		}

	}

	public void resultSending(CamelContext context, RedissonClient redisson) throws Exception {
		Properties properties = ConfigUtils.getConfiguration("sic");
		String sicBaseWebURI = properties.getProperty("sicBaseWebURI");

		if (this.getSndEndPoint().getProtocolType().compareTo(ProtocolType.HTTP) == 0) {
			this.getSndEndPoint().setWebURI("jetty://" + sicBaseWebURI + this.getSndEndPoint().getEndPointID());
			log.info("Web URI -" + this.getSndEndPoint().getWebURI() + "- is created for sending results.");
		} else if (this.getSndEndPoint().getProtocolType().compareTo(ProtocolType.REDIS) == 0) {
			this.getSndEndPoint().setTopicID(this.getSndEndPoint().getEndPointID());
			log.info("Redis topic -" + this.getSndEndPoint().getTopicID() + "- is created for sending results.");
		}
	}

	public void register(CamelContext context, RedissonClient redisson) throws Exception {

		dataReceiving(context, redisson);
		resultSending(context, redisson);

	}
}
