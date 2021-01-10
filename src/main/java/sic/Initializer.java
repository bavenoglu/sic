/* VU Amsterdam, Social AI Group
 * Bilgin Avenoðlu, 10/03/2020 */

package sic;

import java.util.Properties;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.support.DefaultRegistry;
import org.redisson.api.RKeys;
import org.redisson.api.RLiveObjectService;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.slf4j.LoggerFactory;

import sic.redis.GetRedisService;
import sic.serializer.SicRedisTemplate;

public class Initializer extends RouteBuilder {
	private static org.slf4j.Logger log = LoggerFactory.getLogger(Initializer.class);
	@Override
	public void configure() throws Exception {
		final Properties properties = ConfigUtils.getConfiguration("sic");
		String sicBaseWebURI = properties.getProperty("sicBaseWebURI");
		String sicDeviceConnectionURI = properties.getProperty("sicDeviceConnectionURI");
		
		GetRedisService redisService = new GetRedisService();
		RedissonClient redisson = redisService.getRedisson();
		
		SicRedisTemplate.prepareRedisTemplate(getContext());
				
		HttpComponent httpComponent = getContext().getComponent("http", HttpComponent.class); 
		//log.info("HTTP component is added to Apache Camel");
		
		// Topics, web URIs and routes are created for registered services in db.
		initializeServices(getContext(), redisson);
		
		// sic's listener web URI for device connection.
		// All devices must call this service first to connect.
		// Before calling this service, device information must be inserted to db.
		from("jetty://" + sicBaseWebURI + sicDeviceConnectionURI).process(new DeviceProcessor(getContext(), redisson));
	}
	
	public static void initializeServices(CamelContext context, RedissonClient redisson) throws Exception {
				
		RKeys keysDevice = redisson.getKeys();
	    Iterable<String> allDeviceKeys = keysDevice.getKeysByPattern("*sic.Device:deviceID:*");
	    for(String key : allDeviceKeys)
	    {
	    	RMap<Object, Object> buckets = redisson.getMap(key);
			Device searchedDevice = redisson.getLiveObjectService().get(Device.class, buckets.get("deviceName"));
			searchedDevice.setIsConnected(false);
	    }
		// All objects for Service class in Redis are found and registered to sic
		RKeys keys = redisson.getKeys();
	    Iterable<String> allKeys = keys.getKeysByPattern("*sic.Service:*");
	    
	    for(String key : allKeys)
	    {
			RMap<Object, Object> buckets = redisson.getMap(key);
			Service searchedService = redisson.getLiveObjectService().get(Service.class, buckets.get("serviceName"));
			GetRedisService redisService = new GetRedisService();
			RedissonClient redissonService = redisService.getRedisson();
			log.info("Service -" + searchedService.getServiceName() + "- is initializing ....");
			searchedService.register(context, redissonService);
			log.info(searchedService.getServiceID() + " service is registered.");
	    }
	}
}