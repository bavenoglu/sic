package sic.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Predicate;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.protobuf.SicDataMessages;
import org.apache.camel.component.protobuf.SicDataMessages.SicDataMessage;
import org.apache.camel.component.redis.RedisConstants;
import org.apache.camel.dataformat.protobuf.ProtobufDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.main.Main;
import org.apache.camel.model.BeanDefinition;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.MarshalDefinition;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.ProcessDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.SetHeaderDefinition;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.model.UnmarshalDefinition;
import org.apache.camel.model.language.SimpleExpression;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.DefaultRegistry;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.google.protobuf.InvalidProtocolBufferException;

import sic.ConfigUtils;
import sic.DataProcessor;
import sic.ResultProcessor;
import sic.serializer.RedisByteArraySerializer;
import sic.serializer.RedisObjectByteSerializer;
import sic.serializer.SicDataMessageRedisSerializer;
import org.apache.camel.support.ExpressionAdapter;

public class CamelManual3 {

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

			ProcessDefinition pd = new ProcessDefinition(new DataProcessor());
			pdMock.add(pd);

//			ProtobufDataFormat format = new ProtobufDataFormat(SicDataMessages.SicDataMessage.getDefaultInstance());
//			DataFormatDefinition dfd = new DataFormatDefinition(format);
//			MarshalDefinition ud = new MarshalDefinition(dfd);
//			pdMock.add(ud);

			ToDefinition toMock = new ToDefinition();
			String toMockstr = "mock:result?retainLast=30"; // "file://c:/tmp";
			toMock.setUri(toMockstr);
			pdMock.add(toMock);

			Predicate prd1 = new Predicate() {
				@Override
				public boolean matches(Exchange exchange) {
					if (exchange.getProperty("UID", String.class).compareTo("Robot1") == 0)
						return true;
					return false;
				}
			};
			Predicate prd2 = new Predicate() {
				@Override
				public boolean matches(Exchange exchange) {
					if (exchange.getProperty("UID", String.class).compareTo("Robot2") == 0)
						return true;
					return false;
				}
			};

			ChoiceDefinition choice = new ChoiceDefinition();
			choice.when(prd1).to("direct:Red1");
			// choice.when(prd2).to("direct:Red2");
			choice.endChoice();

			pdMock.add(choice);

			rdMock.setOutputs(pdMock);

			mcc.addRouteDefinition(rdMock);

			RouteDefinition rdRed1 = new RouteDefinition();
			rdRed1.setExchangePattern(ExchangePattern.InOut);
			rdRed1.setAutoStartup("true");
			rdRed1.setErrorHandlerFactoryIfNull(new DefaultErrorHandlerBuilder());
			rdRed1.setTrace("true");
			FromDefinition fromRed1 = new FromDefinition("direct:Red1");

			List<ProcessorDefinition<?>> pdRed1 = new ArrayList<ProcessorDefinition<?>>();

//			ProtobufDataFormat format1 = new ProtobufDataFormat(SicDataMessages.SicDataMessage.getDefaultInstance());
//			DataFormatDefinition dfd1 = new DataFormatDefinition(format1);
//			UnmarshalDefinition ud1 = new UnmarshalDefinition(dfd1);
//			pdRed1.add(ud1);

//			ProcessDefinition pd1 = new ProcessDefinition(new ResultProcessor());
//			pdRed1.add(pd1);

//			ToDefinition toMock1 = new ToDefinition();
//			String toMockstr1 = "file://c:/tmp/emo"; // "mock:result?retainLast=30"; // "file://c:/tmp";
//			toMock1.setUri(toMockstr1);
//			pdRed1.add(toMock1);

			ToDefinition toRed1 = new ToDefinition();
			String toRed1str = "spring-redis://" + redisHost + ":6379?command=PUBLISH&redisTemplate=#standardTemplate";
			toRed1.setUri(toRed1str);
			SetHeaderDefinition channelRed1 = new SetHeaderDefinition(RedisConstants.CHANNEL,
					constant("EmotionRecognitionRcvTopic"));
			SetHeaderDefinition messageRed1 = new SetHeaderDefinition(RedisConstants.MESSAGE, new ExpressionAdapter() {
				public Object evaluate(Exchange exchange, Class type) {
					SicDataMessage sicMessageIn = (SicDataMessage) exchange.getIn().getBody();

					//System.out.println(sicMessageIn.getUID());
//					File file = new File("c:\\tmp\\outputfile");
//					FileOutputStream fos = null;
//					try {
//						fos = new FileOutputStream(file);
//						sicMessageIn.writeTo(fos);
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
					return sicMessageIn.toByteArray();
				}

				public String toString() {
					return "";
				}
			});
			// new SimpleExpression("${exchange.getIn().getBody()}"));

			pdRed1.add(channelRed1);
			pdRed1.add(messageRed1);
			pdRed1.add(toRed1);

			rdRed1.setInput(fromRed1);
			rdRed1.setOutputs(pdRed1);
			mcc.addRouteDefinition(rdRed1);

//			RouteDefinition rdRed2 = new RouteDefinition();
//			rdRed2.setExchangePattern(ExchangePattern.InOut);
//			rdRed2.setAutoStartup("true");
//			rdRed2.setErrorHandlerFactoryIfNull(new DefaultErrorHandlerBuilder());
//			rdRed2.setTrace("true");
//			FromDefinition fromRed2 = new FromDefinition("direct:Red2");
//			ToDefinition toRed2 = new ToDefinition();
//			String toRed2str = "spring-redis://" + redisHost + ":6379?command=PUBLISH&redisTemplate=#byteArrayTemplate";
//			toRed2.setUri(toRed2str);
//			SetHeaderDefinition channelRed2 = new SetHeaderDefinition(RedisConstants.CHANNEL,
//					constant("FaceRecognitionRcvTopic"));
//			SetHeaderDefinition messageRed2 = new SetHeaderDefinition(RedisConstants.MESSAGE,
//					new SimpleExpression("${exchange.getIn().getBody()}"));
//			List<ProcessorDefinition<?>> pdRed2 = new ArrayList<ProcessorDefinition<?>>();
//						
//			pdRed2.add(channelRed2);
//			pdRed2.add(messageRed2);
//			pdRed2.add(toRed2);
//
//			rdRed2.setInput(fromRed2);
//			rdRed2.setOutputs(pdRed2);
//			mcc.addRouteDefinition(rdRed2);

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