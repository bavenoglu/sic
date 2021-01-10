package sic.tests;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.redis.RedisConstants;
import org.apache.camel.main.Main;
import org.slf4j.LoggerFactory;

import sic.ConfigUtils;
import sic.serializer.SicRedisTemplate;


public class CamelTest2 {
	private static org.slf4j.Logger log = LoggerFactory.getLogger(CamelTest2.class);

	public static void main(String[] args) throws Exception {
		Properties properties = ConfigUtils.getConfiguration("sic");
		String redisHost = properties.getProperty("redis.host");
		String redisPort = properties.getProperty("redis.port");
		Main main = new Main();
		
		
		main.addRoutesBuilder(new RouteBuilder() {
			@Override
			public void configure() throws Exception {
				
				SicRedisTemplate.prepareRedisTemplate(getContext());
				
				String routeTo = "spring-redis://" + redisHost + ":" + redisPort + "?redisTemplate=#byteTemplate";//&serializer=#byteSerializer";
				//from("spring-redis://" + redisHost + ":" + redisPort
				//		+ "?command=SUBSCRIBE&channels=Robot1CameraSndTopic&redisTemplate=#byteTemplate&serializer=#byteSerializer")	
				from("jetty:http://0.0.0.0:8000/Robot2Camera2SndPoint")
				.process(exchange -> {
					Map<String, Object> values = new HashMap<String, Object>();
					values.put("FaceRecognitionRcvTopic", exchange.getIn().getBody());
					values.put("EmotionRecognitionRcvTopic", exchange.getIn().getBody());
					exchange.getIn().setHeader(RedisConstants.VALUES, values);
				})
				.recipientList().method(this, "routeTo").parallelProcessing().timeout(500);
			}
		});
		
		try {
			main.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
