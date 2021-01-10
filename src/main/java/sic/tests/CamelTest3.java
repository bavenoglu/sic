package sic.tests;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import org.redisson.Redisson;
import org.redisson.api.RKeys;
import org.redisson.api.RLiveObjectService;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.api.condition.Conditions;
import org.redisson.config.Config;

import sic.ConfigUtils;
import sic.EndPoint;
import sic.ProtocolType;
import sic.Sensor;
import sic.Service;
import sic.ServiceRequest;

public class CamelTest3 {
	final static CountDownLatch latch = new CountDownLatch(1);

	public static void main(String[] args) throws Exception {
		Properties properties = ConfigUtils.getConfiguration("sic");
		String redisHost = properties.getProperty("redis.host");
		String redisPort = properties.getProperty("redis.port");
		Config config = new Config();
		config.useSingleServer().setAddress("redis://" + redisHost + ":" + redisPort);
		//.setPassword(ConfigUtils.getConfiguration("sic").getProperty("redis.password"));
		RedissonClient redisson = Redisson.create(config);
		
		// Main main = new Main();
		RLiveObjectService liveObjectService = redisson.getLiveObjectService();
		
		RKeys keys = redisson.getKeys();
	    Iterable<String> allKeys = keys.getKeysByPattern("*sic.Service:*");
	    
	    for(String key : allKeys)
	    {
			RMap<Object, Object> buckets = redisson.getMap(key);
			Service searchedService = liveObjectService.get(Service.class, buckets.get("serviceName"));
			
			Collection<ServiceRequest> sr = liveObjectService.find(ServiceRequest.class, Conditions.eq("service",  searchedService.getServiceID()));
			
			for(ServiceRequest s : sr) {
				for (EndPoint ep : s.getSenSrvReqRcvEndPoints()){
				System.out.println(ep.getProtocolType());
				System.out.println(s.getSensor());//.getDevice().getDeviceID());
				Sensor searchedSensor = liveObjectService.get(Sensor.class, s.getSensor().getSensorID());
				System.out.println(searchedSensor.getSensorID());
				System.out.println(searchedSensor.getDevice().getDeviceID());
				}
			}
	    }

		redisson.shutdown();

		 

//		try {
//			main.run();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

	}
	public String routeTo(Collection<ServiceRequest> sr) throws IOException {
		String routeString = "";

		for (ServiceRequest serReq : sr) {
			if (serReq.getService().getRcvEndPoint().getProtocolType()
					.compareTo(ProtocolType.HTTP) == 0) {
				if (routeString != null && !routeString.isEmpty())
					routeString += ",";
				routeString += serReq.getService().getRcvEndPoint().getWebURI();
			} else if (serReq.getService().getRcvEndPoint().getProtocolType()
					.compareTo(ProtocolType.REDIS) == 0) {
				if (routeString != null && !routeString.isEmpty())
					routeString += ",";
				routeString += "spring-redis://" //+ redisHost + ":" + redisPort
						+ "?redisTemplate=#byteTemplate";
			}
		}
		return routeString;
	}
	/*
	 * public static Boolean stopRoute(CamelContext context, String routeID) {
	 * Thread stop = null; if (stop == null) { stop = new Thread() {
	 * 
	 * @Override public void run() { try {
	 * context.getRouteController().stopRoute(routeID); } catch (Exception e) {
	 * 
	 * } finally { latch.countDown(); } } }; } stop.start(); try { latch.await(2,
	 * TimeUnit.SECONDS); } catch (InterruptedException e) { return false; } return
	 * true; }
	 */
}
