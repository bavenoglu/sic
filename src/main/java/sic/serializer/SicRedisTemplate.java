/* VU Amsterdam, Social AI Group
 * Bilgin Avenoðlu, 10/03/2020 */

package sic.serializer;

import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.DefaultRegistry;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import sic.ConfigUtils;

public class SicRedisTemplate {

	public static Registry prepareRedisTemplate(CamelContext context) throws Exception {
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
		registry.bind("stringSerializer", new StringRedisSerializer());
		registry.bind("objectSerializer", new RedisObjectByteSerializer());

		ModelCamelContext mcc = context.adapt(ModelCamelContext.class);
		((DefaultCamelContext) mcc).setRegistry(registry);

		return registry;
	}
}
