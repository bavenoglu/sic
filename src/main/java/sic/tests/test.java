package sic.tests;

import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.support.DefaultRegistry;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import sic.ConfigUtils;
import sic.redis.GetRedisService;

public class test {

	public static void main(String[] args) throws Exception {
		
		GetRedisService redisService = new GetRedisService();
		RedissonClient redisson = redisService.getRedisson();
		Properties properties = ConfigUtils.getConfiguration("sic");
		String redisServer = properties.getProperty("redisserver");
		
		DefaultRegistry registry = new DefaultRegistry();
		RedisTemplate<?, ?> template = new RedisTemplate<Object, Object>();
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new StringRedisSerializer());
		template.setDefaultSerializer(new StringRedisSerializer());
		registry.bind("customTemplate", template);
		registry.bind("stringSerializer", new StringRedisSerializer());
		 
		//template.setConnectionFactory(new RedissonConnectionFactory(redisson));
		
		template.afterPropertiesSet();
		CamelContext context = new DefaultCamelContext(registry);
		//context.getRegistry().
		ModelCamelContext mcc = context.adapt(ModelCamelContext.class);
		mcc.addRouteDefinition(
				new RouteDefinition().from("spring-redis://192.168.56.1:6379?command=SUBSCRIBE&channels=Service2SndTopic&redisTemplate=#customTemplate&serializer=#stringSerializer")
				.to("http://localhost:5000/Result"));
				/*
				.process(new Processor() {

					public void process(Exchange exchange) throws Exception {
						Message in = exchange.getIn();
						//System.out.println(in.getBody());
						redisson.getTopic("Service2RcvTopic").publish(in.getBody());
					}}));
				
                //.to("spring-redis://192.168.56.1:6379?channels=Service2RcvTopic"));
				*/


		context.start();

		Thread.sleep(Long.MAX_VALUE);
		redisson.shutdown();
		context.stop();
	}

}
