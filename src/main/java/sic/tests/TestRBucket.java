package sic.tests;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.redisson.Redisson;
import org.redisson.api.RBatch;
import org.redisson.api.RBucket;
import org.redisson.api.RBuckets;
import org.redisson.api.RKeys;
import org.redisson.api.RLiveObjectService;
import org.redisson.api.RMap;
import org.redisson.api.RType;
import org.redisson.api.RedissonClient;
import org.redisson.api.annotation.RId;
import org.redisson.api.condition.Conditions;
import org.redisson.client.codec.ByteArrayCodec;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;

import sic.Device;
import sic.Service;

public class TestRBucket {

	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		Config config = new Config();
		config.useSingleServer().setAddress("redis://localhost:6379");
		RedissonClient redisson = Redisson.create(config);
		RLiveObjectService liveObjectService  = redisson.getLiveObjectService();
		
		RKeys keys = redisson.getKeys();
	    //Iterable<String> allKeys = keys.getKeysByPattern("*sic.Service:*");
	    Iterable<String> allKeys = keys.getKeysByPattern("*sic.Device:deviceID:*");
	    for(String key : allKeys)
	    {	System.out.println(key);
			RMap<Object, Object> buckets = redisson.getMap(key);
			//System.out.println(buckets.get("serviceID"));
			//System.out.println(buckets.get());//.get(0).getName());
	    	//RBucket<String> bucket = redisson.getBucket("FaceRecognition");
	    	//System.out.println(bucket.getName());
			//Service searchedService1 = liveObjectService.get(Service.class,  buckets.get("serviceName"));
			Device searchedService1 = liveObjectService.get(Device.class,  buckets.get("deviceName"));
			System.out.println(searchedService1.getDeviceID());
			//System.out.println(searchedService1.getServiceID() );
			//searchedService1.register(context);
	    }
	    	
	    redisson.shutdown();
	}

}
