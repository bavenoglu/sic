package sic.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Expression;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.protobuf.SicDataMessages.SicDataMessage;
import org.apache.camel.component.redis.RedisConstants;
import org.apache.camel.component.redis.RedisEndpoint;
import org.apache.camel.dataformat.protobuf.ProtobufDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.language.simple.SimpleLanguage;
import org.apache.camel.language.tokenizer.TokenizeLanguage;
import org.apache.camel.main.Main;
import org.apache.camel.model.BeanDefinition;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.ConvertBodyDefinition;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.DynamicRouterDefinition;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.MarshalDefinition;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.MulticastDefinition;
import org.apache.camel.model.OtherwiseDefinition;
import org.apache.camel.model.ProcessDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.PropertyDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.SetBodyDefinition;
import org.apache.camel.model.SetHeaderDefinition;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.model.UnmarshalDefinition;
import org.apache.camel.model.WhenDefinition;
import org.apache.camel.model.language.SimpleExpression;
import org.apache.camel.reifier.RouteReifier;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.DefaultRegistry;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import sic.ConfigUtils;
import sic.ResultProcessor;
import sic.serializer.RedisByteArraySerializer;
import sic.serializer.RedisObjectByteSerializer;
import sic.serializer.SicDataMessageRedisSerializer;

public class CamelManual2 {

	public static void main(String[] args) throws Exception {
		ManualRoutes routeBuilder = new ManualRoutes();
		Main main = new Main();
		try {
			main.addRoutesBuilder(routeBuilder);
			main.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class ManualRoutes extends RouteBuilder {
		@Override
		public void configure() throws Exception {
			Properties properties = ConfigUtils.getConfiguration("sic");
			String redisHost = properties.getProperty("redis.host");
			String redisPort = properties.getProperty("redis.port");
			ModelCamelContext mcc = getContext().adapt(ModelCamelContext.class);
			((DefaultCamelContext) mcc).setRegistry(prepareRedisTemplate());

			RouteDefinition rdMock = new RouteDefinition();
			rdMock.setExchangePattern(ExchangePattern.InOut);
			rdMock.setAutoStartup("true");
			rdMock.setErrorHandlerFactoryIfNull(new DefaultErrorHandlerBuilder());
			rdMock.setTrace("true");
			rdMock.setId("rdMock");

			String fromRedis = "spring-redis://" + redisHost
					+ ":6379?command=SUBSCRIBE&channels=Robot1Camera1SndTopic&redisTemplate=#byteArrayTemplate&serializer=#byteArraySerializer";
			FromDefinition from = new FromDefinition(fromRedis);
			rdMock.setInput(from);
		
			List<ProcessorDefinition<?>> pdMock = new ArrayList<ProcessorDefinition<?>>();

			ProtobufDataFormat format = new ProtobufDataFormat(SicDataMessage.getDefaultInstance());
			DataFormatDefinition dfd = new DataFormatDefinition(format);
			UnmarshalDefinition ud = new UnmarshalDefinition(dfd);
			pdMock.add(ud);

			ProcessDefinition pd = new ProcessDefinition(new ResultProcessor());
			pdMock.add(pd);
//
//			ToDefinition toMock = new ToDefinition();
//			String toMockstr = "mock:result?retainLast=30"; // "file://c:/tmp";
//			toMock.setUri(toMockstr);
//			pdMock.add(toMock);
			
			ToDefinition toRed1 = new ToDefinition();
			toRed1.setId("toRed1");
			String toRed1str = "spring-redis://" + redisHost + ":6379?command=PUBLISH&redisTemplate=#byteArrayTemplate";
			toRed1.setUri(toRed1str);
			SetHeaderDefinition channelRed1 = new SetHeaderDefinition(RedisConstants.CHANNEL,
					constant("EmotionRecognitionRcvTopic"));
			SetHeaderDefinition messageRed1 = new SetHeaderDefinition(RedisConstants.MESSAGE,
					new SimpleExpression(body()));
			pdMock.add(channelRed1);
			pdMock.add(messageRed1);
			pdMock.add(toRed1);

			ToDefinition toRed2 = new ToDefinition();
			toRed2.setId("toRed2");
			String toRed2str = "spring-redis://" + redisHost + ":6379?command=PUBLISH&redisTemplate=#byteArrayTemplate";
			toRed2.setUri(toRed2str);
			SetHeaderDefinition channelRed2 = new SetHeaderDefinition(RedisConstants.CHANNEL,
					constant("FaceRecognitionRcvTopic"));
			SetHeaderDefinition messageRed2 = new SetHeaderDefinition(RedisConstants.MESSAGE,
					new SimpleExpression("${exchange.getIn().getBody()}"));
			pdMock.add(channelRed2);
			pdMock.add(messageRed2);
			pdMock.add(toRed2);

			Predicate prd1 = new Predicate() {
				@Override
				public boolean matches(Exchange exchange) {
					if (exchange.getProperty("deviceID", String.class).compareTo("Robot1") == 0)
						return true;
					return false;
				}
			};
			Predicate prd2 = new Predicate() {
				@Override
				public boolean matches(Exchange exchange) {
					if (exchange.getProperty("deviceID", String.class).compareTo("Robot2") == 0)
						return true;
					return false;
				}
			};
			
					
			ChoiceDefinition choice = new ChoiceDefinition();
			choice.when(prd1).to(toRed1str);
			choice.when(prd2).to(toRed2str);
			choice.endChoice();
			
			pdMock.add(choice);

			rdMock.setOutputs(pdMock);

			mcc.addRouteDefinition(rdMock);

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

		registry.bind("byteArrayTemplate", template);
		registry.bind("byteArraySerializer", new RedisByteArraySerializer());
		registry.bind("stringSerializer", new StringRedisSerializer());
		registry.bind("objectSerializer", new RedisObjectByteSerializer());
		registry.bind("jdkSerializer", new JdkSerializationRedisSerializer());
		registry.bind("sicMessageSerializer", new SicDataMessageRedisSerializer());

		LettuceConnectionFactory lcf1 = new LettuceConnectionFactory(
				new RedisStandaloneConfiguration(redisHost, redisPort));
		lcf1.afterPropertiesSet();

		RedisTemplate<?, ?> template1 = new RedisTemplate<>();
		template1.setKeySerializer(new StringRedisSerializer());
		template1.setValueSerializer(new RedisByteArraySerializer());
		template1.setConnectionFactory(lcf1);
		template1.afterPropertiesSet();
		registry.bind("standardTemplate", template1);
		// registry.bind("resultRouteBean", new ResultRouteBean());
		return registry;
	}
}