package sic.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Expression;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.protobuf.SicDataMessages.SicDataMessage;
import org.apache.camel.component.redis.RedisConstants;
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
import org.apache.camel.model.ProcessDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.PropertyDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.SetBodyDefinition;
import org.apache.camel.model.SetHeaderDefinition;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.model.UnmarshalDefinition;
import org.apache.camel.model.language.SimpleExpression;
import org.apache.camel.reifier.RouteReifier;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.DefaultExchange;
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

public class CamelManual {

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
			
			SetHeaderDefinition devid = new SetHeaderDefinition("deviceID", "Robot1");
			pdMock.add(devid);
			
			ProcessDefinition pd = new ProcessDefinition(new ResultProcessor());
			pdMock.add(pd);
			
			ToDefinition toMock = new ToDefinition();
			String toMockstr = "mock:result?retainLast=30"; // "file://c:/tmp";
			toMock.setUri(toMockstr);
			pdMock.add(toMock);

			rdMock.setOutputs(pdMock);

			MulticastDefinition md = rdMock.multicast();
			md.to("direct:Red1");
			md.to("direct:Red2");
			md.parallelProcessing();
			md.setTimeout(1000L);
			
//			RouteDefinition rdef = mcc.getRouteDefinition("mock:result");
//			RouteReifier.adviceWith(rdef, mcc, new AdviceWithRouteBuilder() {
//				@Override
//				public void configure() throws Exception {
//					
////					if (ex.getIn().getHeader("deviceID", String.class).compareTo("Robot1") == 0)
////						mockEndpointsAndSkip("direct:Red2");
////					else
//						mockEndpointsAndSkip("direct:Red1");
//				}
//			});
			
			mcc.addRouteDefinition(rdMock);
			mcc.getRouteDefinitions().get(0).bean(ResultRouteBean.class, "routeResult");
			
			RouteDefinition rdRed1 = new RouteDefinition();
			rdRed1.setExchangePattern(ExchangePattern.InOut);
			rdRed1.setAutoStartup("true");
			rdRed1.setErrorHandlerFactoryIfNull(new DefaultErrorHandlerBuilder());
			rdRed1.setTrace("true");
			FromDefinition fromRed1 = new FromDefinition("direct:Red1");
			ToDefinition toRed1 = new ToDefinition();
			String toRed1str = "spring-redis://" + redisHost + ":6379?command=PUBLISH&redisTemplate=#byteArrayTemplate";
			toRed1.setUri(toRed1str);
			SetHeaderDefinition channelRed1 = new SetHeaderDefinition(RedisConstants.CHANNEL,
					constant("EmotionRecognitionRcvTopic"));
			SetHeaderDefinition messageRed1 = new SetHeaderDefinition(RedisConstants.MESSAGE,
					body());
			List<ProcessorDefinition<?>> pdRed1 = new ArrayList<ProcessorDefinition<?>>();
			pdRed1.add(channelRed1);
			pdRed1.add(messageRed1);
			pdRed1.add(toRed1);

			rdRed1.setInput(fromRed1);
			rdRed1.setOutputs(pdRed1);
			mcc.addRouteDefinition(rdRed1);

			RouteDefinition rdRed2 = new RouteDefinition();
			rdRed2.setExchangePattern(ExchangePattern.InOut);
			rdRed2.setAutoStartup("true");
			rdRed2.setErrorHandlerFactoryIfNull(new DefaultErrorHandlerBuilder());
			rdRed2.setTrace("true");
			FromDefinition fromRed2 = new FromDefinition("direct:Red2");
			ToDefinition toRed2 = new ToDefinition();
			String toRed2str = "spring-redis://" + redisHost + ":6379?command=PUBLISH&redisTemplate=#byteArrayTemplate";
			toRed2.setUri(toRed2str);
			SetHeaderDefinition channelRed2 = new SetHeaderDefinition(RedisConstants.CHANNEL,
					constant("FaceRecognitionRcvTopic"));
			SetHeaderDefinition messageRed2 = new SetHeaderDefinition(RedisConstants.MESSAGE,
					body());
			List<ProcessorDefinition<?>> pdRed2 = new ArrayList<ProcessorDefinition<?>>();
			pdRed2.add(channelRed2);
			pdRed2.add(messageRed2);
			pdRed2.add(toRed2);

			rdRed2.setInput(fromRed2);
			rdRed2.setOutputs(pdRed2);
			mcc.addRouteDefinition(rdRed2);
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
		registry.bind("resultRouteBean", new ResultRouteBean());
		return registry;
	}
}