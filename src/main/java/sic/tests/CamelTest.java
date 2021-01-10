package sic.tests;

import java.util.Properties;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.redis.RedisConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.main.Main;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.DefaultRegistry;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import sic.ConfigUtils;
import sic.serializer.RedisByteArraySerializer;


public class CamelTest {
	public static void main(String[] args) throws Exception {
		Main main = new Main();
		
		main.addRoutesBuilder(new RouteBuilder() {
			@Override
			public void configure() throws Exception {
				ModelCamelContext mcc = getContext().adapt(ModelCamelContext.class);
				((DefaultCamelContext) mcc).setRegistry(prepareRedisTemplate());
		        
				from("spring-redis://localhost:6379?command=SUBSCRIBE&channels=Robot1Camera1SndTopic&redisTemplate=#byteTemplate&serializer=#byteSerializer")
//				.process(exchange -> {
//					System.out.println("bla");
//					exchange.getIn().setHeader(RedisConstants.MESSAGE, exchange.getIn().getBody());
//				}) 
				.setHeader(RedisConstants.MESSAGE, body())
				.setHeader(RedisConstants.CHANNEL, constant("EmotionRecognitionRcvTopic"))
				//.to("stream:out")
				.to("spring-redis://localhost:6379?command=PUBLISH&redisTemplate=#standardTemplate&serializer=#byteSerializer");
			}
		});
		
		try {
			main.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static Registry prepareRedisTemplate() throws Exception {
		Properties properties = ConfigUtils.getConfiguration("sic");
		String redisHost = properties.getProperty("redis.host");
		int redisPort = Integer.parseInt(properties.getProperty("redis.port"));

		LettuceConnectionFactory lcf = new LettuceConnectionFactory(
				new RedisStandaloneConfiguration(redisHost, redisPort));
		lcf.afterPropertiesSet();

		DefaultRegistry registry = new DefaultRegistry();

		RedisTemplate<?, ?> template = new RedisTemplate<>();
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new RedisByteArraySerializer());
		template.setConnectionFactory(lcf);
		template.afterPropertiesSet();
		
		registry.bind("byteTemplate", template);
		registry.bind("byteSerializer", new RedisByteArraySerializer());
		
		LettuceConnectionFactory lcf1 = new LettuceConnectionFactory(
				new RedisStandaloneConfiguration(redisHost, redisPort));
		lcf1.afterPropertiesSet();
		
		RedisTemplate<?, ?> template1 = new RedisTemplate<>();
		template1.setKeySerializer(new StringRedisSerializer());
		template1.setValueSerializer(new RedisByteArraySerializer());
		template1.setConnectionFactory(lcf1);
		template1.afterPropertiesSet();
		registry.bind("standardTemplate", template1);
		
		
		return registry;
	}
	

}
