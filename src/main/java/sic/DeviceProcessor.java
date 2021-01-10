/* VU Amsterdam, Social AI Group
 * Bilgin Avenoðlu, 10/03/2020 */

package sic;

import javax.servlet.http.HttpServletRequest;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.simple.JSONObject;
import org.redisson.api.RLiveObjectService;
import org.redisson.api.RedissonClient;
import org.slf4j.LoggerFactory;

import sic.redis.GetRedisService;

public class DeviceProcessor implements Processor {
	CamelContext context;
	RedissonClient redisson;
	private static org.slf4j.Logger log = LoggerFactory.getLogger(DeviceProcessor.class);
	
	public DeviceProcessor(CamelContext context, RedissonClient redisson) {
		super();
		this.context = context;
		this.redisson = redisson;
		
	}

	public void process(Exchange exchange) throws Exception {

		String body = exchange.getIn().getBody(String.class);
		HttpServletRequest req = exchange.getIn().getBody(HttpServletRequest.class);
		String deviceID = req.getParameter("DeviceID");
		log.info("Device connection request from " + deviceID);
		
	    Device device = redisson.getLiveObjectService().get(Device.class, deviceID);
	    GetRedisService redisService = new GetRedisService();
		RedissonClient redissonDevice = redisService.getRedisson();
	    device.connect(context, redissonDevice);
	    JSONObject returnedJSON = device.returnConnectionInfo();
		
	    exchange.getOut().setBody(returnedJSON);
	}

}
