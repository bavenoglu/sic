/* VU Amsterdam, Social AI Group
 * Bilgin Avenoðlu, 10/03/2020
 *  */

package sic;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Predicate;
import org.apache.camel.RecipientList;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.protobuf.SicDataMessages.SicDataMessage;
import org.apache.camel.component.redis.RedisConstants;
import org.apache.camel.dataformat.protobuf.ProtobufDataFormat;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.MarshalDefinition;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.MulticastDefinition;
import org.apache.camel.model.ProcessDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.SetHeaderDefinition;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.model.UnmarshalDefinition;
import org.apache.camel.model.language.SimpleExpression;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.redisson.Redisson;
import org.redisson.api.RCascadeType;
import org.redisson.api.RKeys;
import org.redisson.api.RLiveObjectService;
import org.redisson.api.RMap;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.annotation.RCascade;
import org.redisson.api.annotation.REntity;
import org.redisson.api.annotation.RId;
import org.redisson.api.annotation.RIndex;
import org.redisson.api.condition.Conditions;
import org.redisson.config.Config;
import org.redisson.liveobject.resolver.UUIDGenerator;
import org.slf4j.LoggerFactory;
import sic.redis.GetRedisService;

@REntity
public class Device implements Serializable {
	private static org.slf4j.Logger log = LoggerFactory.getLogger(Device.class);
	@RId(generator = UUIDGenerator.class)
	private String deviceID;
	private String deviceName;
	@RIndex
	private User user;
	@RCascade(RCascadeType.DELETE)
	private List<Sensor> sensors;
	private boolean isConnected;

	public Device() {

	}

	public Device(String deviceID) throws IOException {
		super();
		this.deviceID = deviceID;
		this.deviceName = deviceID;
	}

	public String getDeviceID() {
		return this.deviceID;
	}

	public String getDeviceName() {
		return this.deviceName;
	}

	public User getUser() {
		return this.user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public boolean getIsConnected() {
		return this.isConnected;
	}

	public void setIsConnected(boolean isConnected) {
		this.isConnected = isConnected;
	}

	public List<Sensor> getSensors() {
		return this.sensors;
	}

	@SuppressWarnings("unchecked")
	public JSONObject returnConnectionInfo() {
		JSONObject deviceObj = new JSONObject();
		deviceObj.put("DeviceID", this.getDeviceID());

		JSONArray sensorsJSON = new JSONArray();

		for (Sensor sen : this.getSensors()) {
			JSONObject senJSON = new JSONObject();
			senJSON.put("SensorID", sen.getSensorID());

			JSONArray rcvJSON = new JSONArray();
			sen.getSrvRcvEndPointsJSON(rcvJSON);

			JSONObject sndJSON = new JSONObject();
			sen.getSndEndPointJSON(sndJSON);

			senJSON.put("Sending Endpoint", sndJSON);
			senJSON.put("Receiving Endpoints", rcvJSON);

			sensorsJSON.add(senJSON);
		}
		deviceObj.put("Sensors", sensorsJSON);

		log.info("Connection information is prepared as JSON ");
		return deviceObj;
	}

	public void dataSending(CamelContext context) throws Exception {
		Properties properties = ConfigUtils.getConfiguration("sic");
		String sicBaseWebURI = properties.getProperty("sicBaseWebURI");
		String UID = "";
		String routeID = "";
		log.info("Data sending routes are preparing ...");
		for (Sensor sen : this.getSensors()) {
			routeID = sen.getSndEndPoint().getEndPointID();
			if (sen.getSndEndPoint().getProtocolType().compareTo(ProtocolType.HTTP) == 0) {
				sen.getSndEndPoint().setWebURI("jetty://" + sicBaseWebURI + routeID);
				log.info("Route -" + routeID + "- from Web URI -" + sen.getSndEndPoint().getWebURI()
						+ "- to requested services of -" + sen.getSensorID() + "- is preparing.");
			} else if (sen.getSndEndPoint().getProtocolType().compareTo(ProtocolType.REDIS) == 0) {
				sen.getSndEndPoint().setTopicID(routeID);
				log.info("Route -" + routeID + "- from Redis topic -" + routeID + "- to requested services of -"
						+ sen.getSensorID() + "- is preparing.");

			}
			List<EndPoint> toEndPoints = new ArrayList<EndPoint>();
			for (ServiceRequest serReq : sen.getServiceRequests()) {
				toEndPoints.add(serReq.getService().getRcvEndPoint());
			}
			UID = sen.getSensorID();
			createDataRoutes(context, UID, routeID, sen.getSndEndPoint(), toEndPoints);
		}
	}

	public void createDataRoutes(CamelContext context, String UID, String routeID, EndPoint fromEndPoint,
			List<EndPoint> toEndPoints) throws Exception {
		ModelCamelContext mcc = context.adapt(ModelCamelContext.class);
		String fromLog = "";
		String toLog = "";
		if (context.getRoute(routeID) != null) {
			context.getRouteController().stopRoute(routeID);
			log.warn("Route " + routeID + " is stopped.");
		}

		RouteDefinition rdFromEndPoint = new RouteDefinition();
		rdFromEndPoint.setId(routeID);
		rdFromEndPoint.setExchangePattern(ExchangePattern.InOut);
		rdFromEndPoint.setAutoStartup("true");
		rdFromEndPoint.setErrorHandlerFactoryIfNull(new DefaultErrorHandlerBuilder());
		rdFromEndPoint.setTrace("true");

		// From EndPoint
		String fromURI = "";
		if (fromEndPoint.getProtocolType().compareTo(ProtocolType.HTTP) == 0) {
			fromURI = fromEndPoint.getWebURI();
			fromLog = "Web URI -" + fromURI;
		} else if (fromEndPoint.getProtocolType().compareTo(ProtocolType.REDIS) == 0) {
			fromURI = "spring-redis://" + fromEndPoint.getIpNumber() + ":" + fromEndPoint.getPortNumber()
					+ "?command=SUBSCRIBE&channels=" + fromEndPoint.getTopicID()
					+ "&redisTemplate=#byteTemplate&serializer=#byteSerializer";
			fromLog = "Redis topic -" + fromEndPoint.getTopicID();
		}
		FromDefinition fromEP = new FromDefinition(fromURI);

		if (toEndPoints.size() == 1) {
			for (EndPoint ep : toEndPoints) {
				ToDefinition toToEndPoint = new ToDefinition();
				List<ProcessorDefinition<?>> pdTo = new ArrayList<ProcessorDefinition<?>>();

				SetHeaderDefinition uid = new SetHeaderDefinition("UID", UID);
				pdTo.add(uid);

				ProcessDefinition pd = new ProcessDefinition(new DataProcessor());
				pdTo.add(pd);

				String toToURI = "";
				if (ep.getProtocolType().compareTo(ProtocolType.HTTP) == 0) {
					toToURI = ep.getWebURI();
					toLog = "Web URI -" + toToURI;
				} else if (ep.getProtocolType().compareTo(ProtocolType.REDIS) == 0) {
					toToURI = "spring-redis://" + ep.getIpNumber() + ":" + ep.getPortNumber()
							+ "?command=PUBLISH&redisTemplate=#byteTemplate&serializer=#byteSerializer";

					SetHeaderDefinition channel = new SetHeaderDefinition(RedisConstants.CHANNEL, ep.getTopicID());
					SetHeaderDefinition message = new SetHeaderDefinition(RedisConstants.MESSAGE,
							new SimpleExpression("${exchange.getIn().getBody()}"));
					pdTo.add(channel);
					pdTo.add(message);
					toLog = "Redis topic -" + ep.getTopicID();
				}
				toToEndPoint.setUri(toToURI);
				pdTo.add(toToEndPoint);
				rdFromEndPoint.setInput(fromEP);
				rdFromEndPoint.setOutputs(pdTo);
				mcc.addRouteDefinition(rdFromEndPoint);
				log.info("Route -" + routeID + "- is created from " + fromLog + "- to " + toLog);
			}
		} else {
			ToDefinition toMock = new ToDefinition();
			String toMockstr = "mock:result?retainLast=50";
			toMock.setUri(toMockstr);
			List<ProcessorDefinition<?>> pdMock = new ArrayList<ProcessorDefinition<?>>();

//			ProtobufDataFormat format = new ProtobufDataFormat(SicDataMessage.getDefaultInstance());
//			DataFormatDefinition dfd = new DataFormatDefinition(format);
//			MarshalDefinition mad = new MarshalDefinition(dfd);
//			pdMock.add(mad);
//
			SetHeaderDefinition uid = new SetHeaderDefinition("UID", UID);
			pdMock.add(uid);

			ProcessDefinition pd = new ProcessDefinition(new DataProcessor());
			pdMock.add(pd);

			pdMock.add(toMock);
			rdFromEndPoint.setInput(fromEP);
			rdFromEndPoint.setOutputs(pdMock);

			MulticastDefinition md = rdFromEndPoint.multicast();
			for (EndPoint ep : toEndPoints) {
				md.to("direct:" + ep.getEndPointID());
				log.info("Multicast from " + fromLog + "- to -direct:" + ep.getEndPointID());
			}
			md.parallelProcessing();
			md.setTimeout(1000L);

			mcc.addRouteDefinition(rdFromEndPoint);
			log.info("Route -" + routeID + "- is created from " + fromLog + "- to mock:result");

			for (EndPoint ep : toEndPoints) {
				if (context.getRoute(ep.getEndPointID()) != null) {
					context.getRouteController().stopRoute(ep.getEndPointID());
					log.warn("Route " + routeID + " is stopped.");
				}
				RouteDefinition rdToEndPoint = new RouteDefinition();
				rdToEndPoint.setId(routeID + ep.getEndPointID());
				rdToEndPoint.setExchangePattern(ExchangePattern.InOut);
				rdToEndPoint.setAutoStartup("true");
				rdToEndPoint.setErrorHandlerFactoryIfNull(new DefaultErrorHandlerBuilder());
				rdToEndPoint.setTrace("true");

				FromDefinition fromToEndPoint = new FromDefinition("direct:" + routeID + ep.getEndPointID());

				ToDefinition toToEndPoint = new ToDefinition();
				List<ProcessorDefinition<?>> pdTo = new ArrayList<ProcessorDefinition<?>>();
				String toToURI = "";
				if (ep.getProtocolType().compareTo(ProtocolType.HTTP) == 0) {
					toToURI = ep.getWebURI();
					toLog = "Web URI -" + toToURI;
				} else if (ep.getProtocolType().compareTo(ProtocolType.REDIS) == 0) {
					toToURI = "spring-redis://" + ep.getIpNumber() + ":" + ep.getPortNumber()
							+ "?command=PUBLISH&redisTemplate=#byteTemplate&serializer=#byteSerializer";
					SetHeaderDefinition channel = new SetHeaderDefinition(RedisConstants.CHANNEL, ep.getTopicID());
					SetHeaderDefinition message = new SetHeaderDefinition(RedisConstants.MESSAGE,
							new SimpleExpression("${exchange.getIn().getBody()}"));
					pdTo.add(channel);
					pdTo.add(message);
					toLog = "Redis topic -" + ep.getTopicID();
				}
				toToEndPoint.setUri(toToURI);
				pdTo.add(toToEndPoint);
				rdToEndPoint.setInput(fromToEndPoint);
				rdToEndPoint.setOutputs(pdTo);
				mcc.addRouteDefinition(rdToEndPoint);
				log.info("Route -" + ep.getEndPointID() + "- is created from -direct:" + ep.getEndPointID() + "- to "
						+ toLog);
			}
		}
	}

	public void resultReceiving(CamelContext context, RedissonClient redisson) throws Exception {
		Properties properties = ConfigUtils.getConfiguration("sic");
		log.info("Result receiving routes are preparing ...");
		for (Sensor sen : this.getSensors()) {
			for (ServiceRequest serReq : sen.getServiceRequests()) {
				log.info("Processing -" + serReq.getServiceRequestID());
				for (EndPoint endSerReq : serReq.getSenSrvReqRcvEndPoints()) {
					if (endSerReq.getProtocolType().compareTo(ProtocolType.HTTP) == 0) {
						// nothing to do. The Web address is entered to the db (from the sic web site)
						// by device owners.
						if (endSerReq.getWebURI() == null)
							log.error("Web URI for receiving results coming from service -"
									+ serReq.getService().getServiceID() + "- must be entered.");
					} else if (endSerReq.getProtocolType().compareTo(ProtocolType.REDIS) == 0) {
						String redisTopic = endSerReq.getEndPointID();
						endSerReq.setTopicID(redisTopic);
					}
				}
				String routeID = serReq.getService().getServiceID();
				String serviceID = serReq.getService().getServiceID();
				createResultRoutes(context, redisson, serviceID, routeID, serReq.getService().getSndEndPoint(), serReq);
			}
		}
	}

	public void createResultRoutes(CamelContext context, RedissonClient redisson, String serviceID, String routeID,
			EndPoint fromEndPoint, ServiceRequest serReq) throws Exception {
		ModelCamelContext mcc = context.adapt(ModelCamelContext.class);
		String fromLog = "";
		String toLog = "";

		Collection<ServiceRequest> allServiceRequests = redisson.getLiveObjectService().find(ServiceRequest.class,
				Conditions.eq("service", serReq.getService().getServiceID()));

		Collection<ServiceRequest> serviceRequests = new ArrayList();
		for (ServiceRequest sr : allServiceRequests) {
			if (sr.getSensor().getDevice().getIsConnected())
				serviceRequests.add(sr);
		}

		if (context.getRoute(routeID) != null) {
			context.getRouteController().stopRoute(routeID);
			log.warn("Route -" + routeID + "- is stopped.");
		}

		RouteDefinition rdFromService = new RouteDefinition();
		rdFromService.setId(routeID);
		rdFromService.setExchangePattern(ExchangePattern.InOut);
		rdFromService.setAutoStartup("true");
		rdFromService.setErrorHandlerFactoryIfNull(new DefaultErrorHandlerBuilder());
		rdFromService.setTrace("true");

		String fromURI = "";
		if (fromEndPoint.getProtocolType().compareTo(ProtocolType.HTTP) == 0) {
			fromURI = fromEndPoint.getWebURI();
			fromLog = "Web URI -" + fromURI;
		} else if (fromEndPoint.getProtocolType().compareTo(ProtocolType.REDIS) == 0) {
			fromURI = "spring-redis://" + fromEndPoint.getIpNumber() + ":" + fromEndPoint.getPortNumber()
					+ "?command=SUBSCRIBE&channels=" + fromEndPoint.getTopicID()
					+ "&redisTemplate=#byteTemplate&serializer=#byteSerializer";
			fromLog = "Redis topic -" + fromEndPoint.getTopicID();
		}
		FromDefinition fromService = new FromDefinition(fromURI);

//		ToDefinition toMock = new ToDefinition();
//		String toMockstr = "mock:result?retainLast=50";
//		toMock.setUri(toMockstr);
		List<ProcessorDefinition<?>> pdService = new ArrayList<ProcessorDefinition<?>>();

		ProtobufDataFormat format = new ProtobufDataFormat(SicDataMessage.getDefaultInstance());
		DataFormatDefinition dfd = new DataFormatDefinition(format);
		UnmarshalDefinition mad = new UnmarshalDefinition(dfd);
		pdService.add(mad);

		SetHeaderDefinition uid = new SetHeaderDefinition("ServiceID", serviceID);
		pdService.add(uid);

		ProcessDefinition pd = new ProcessDefinition(new ResultProcessor());
		pdService.add(pd);

//		pdService.add(toMock);
		rdFromService.setInput(fromService);

		ChoiceDefinition choice = new ChoiceDefinition();

		for (ServiceRequest sr : serviceRequests) {
			Predicate prd = new Predicate() {
				@Override
				public boolean matches(Exchange exchange) {
					System.out.println(exchange.getIn().getHeader("UID", String.class));
					System.out.println(sr.getSensor().getSensorID());
					if (exchange.getIn().getHeader("UID", String.class).compareTo(sr.getSensor().getSensorID()) == 0)
						return true;
					return false;
				}
			};
			choice.when(prd).to("direct:" + sr.getServiceRequestID());
			log.info("Service request -direct:" + sr.getServiceRequestID() + "- is added to choice.");
		}
		choice.endChoice();
		pdService.add(choice);

		rdFromService.setOutputs(pdService);

		mcc.addRouteDefinition(rdFromService);
		log.info("Route -" + routeID + "- is created from " + fromLog + "- to service requests.");

		for (ServiceRequest sr : serviceRequests) {
			if (context.getRoute(sr.getServiceRequestID()) != null) {
				context.getRouteController().stopRoute("direct:" + sr.getServiceRequestID());
				log.warn("Route -" + sr.getServiceRequestID() + "- is stopped.");
			}
			RouteDefinition rdToSerReq = new RouteDefinition();
			rdToSerReq.setId(sr.getServiceRequestID());
			rdToSerReq.setExchangePattern(ExchangePattern.InOut);
			rdToSerReq.setAutoStartup("true");
			rdToSerReq.setErrorHandlerFactoryIfNull(new DefaultErrorHandlerBuilder());
			rdToSerReq.setTrace("true");

			FromDefinition fromSerReq = new FromDefinition("direct:" + sr.getServiceRequestID());

			if (sr.getSenSrvReqRcvEndPoints().size() == 1) {
				for (EndPoint ep : sr.getSenSrvReqRcvEndPoints()) {
					ToDefinition toToEndPoint = new ToDefinition();
					List<ProcessorDefinition<?>> pdTo = new ArrayList<ProcessorDefinition<?>>();
					String toToURI = "";
					if (ep.getProtocolType().compareTo(ProtocolType.HTTP) == 0) {
						toToURI = ep.getWebURI();
						toLog = "Web URI -" + toToURI;
					} else if (ep.getProtocolType().compareTo(ProtocolType.REDIS) == 0) {
						toToURI = "spring-redis://" + ep.getIpNumber() + ":" + ep.getPortNumber()
								+ "?command=PUBLISH&redisTemplate=#byteTemplate&serializer=#byteSerializer";
						SetHeaderDefinition channel = new SetHeaderDefinition(RedisConstants.CHANNEL, ep.getTopicID());
						SetHeaderDefinition message = new SetHeaderDefinition(RedisConstants.MESSAGE,
								new SimpleExpression("${exchange.getIn().getBody()}"));
						pdTo.add(channel);
						pdTo.add(message);
						toLog = "Redis topic -" + ep.getTopicID();
					}
					toToEndPoint.setUri(toToURI);
					pdTo.add(toToEndPoint);
					rdToSerReq.setInput(fromSerReq);
					rdToSerReq.setOutputs(pdTo);
					mcc.addRouteDefinition(rdToSerReq);
					log.info("Route -" + sr.getServiceRequestID() + "- is created from -direct:"
							+ sr.getServiceRequestID() + "- to " + toLog);
				}
			} else {
				ToDefinition toMult = new ToDefinition();
				String toMultstr = "mock:result?retainLast=50";
				toMult.setUri(toMultstr);
				List<ProcessorDefinition<?>> pdMult = new ArrayList<ProcessorDefinition<?>>();

				pdMult.add(toMult);
				rdToSerReq.setInput(fromSerReq);
				rdToSerReq.setOutputs(pdMult);

				MulticastDefinition md = rdToSerReq.multicast();
				for (EndPoint ep : sr.getSenSrvReqRcvEndPoints()) {
					md.to("direct:" + ep.getEndPointID());
					log.info("Multicast from -direct:" + sr.getServiceRequestID() + "- to -direct:"
							+ ep.getEndPointID());
				}
				md.parallelProcessing();
				md.setTimeout(1000L);

				mcc.addRouteDefinition(rdToSerReq);
				log.info("Route -" + sr.getServiceRequestID() + "- is created from -direct:" + sr.getServiceRequestID()
						+ "- to service request receive enpoints.");

				for (EndPoint ep : sr.getSenSrvReqRcvEndPoints()) {
					if (context.getRoute(ep.getEndPointID()) != null) {
						context.getRouteController().stopRoute(ep.getEndPointID());
						log.warn("Route -" + ep.getEndPointID() + "- is stopped.");
					}
					RouteDefinition rdToEndPoint = new RouteDefinition();
					rdToEndPoint.setId(ep.getEndPointID());
					rdToEndPoint.setExchangePattern(ExchangePattern.InOut);
					rdToEndPoint.setAutoStartup("true");
					rdToEndPoint.setErrorHandlerFactoryIfNull(new DefaultErrorHandlerBuilder());
					rdToEndPoint.setTrace("true");

					FromDefinition fromToEndPoint = new FromDefinition("direct:" + ep.getEndPointID());

					ToDefinition toToEndPoint = new ToDefinition();
					List<ProcessorDefinition<?>> pdTo = new ArrayList<ProcessorDefinition<?>>();
					String toToURI = "";
					if (ep.getProtocolType().compareTo(ProtocolType.HTTP) == 0) {
						toToURI = ep.getWebURI();
						toLog = "Web URI -" + toToURI;
					} else if (ep.getProtocolType().compareTo(ProtocolType.REDIS) == 0) {
						toToURI = "spring-redis://" + ep.getIpNumber() + ":" + ep.getPortNumber()
								+ "?command=PUBLISH&redisTemplate=#byteTemplate&serializer=#byteSerializer";
						SetHeaderDefinition channel = new SetHeaderDefinition(RedisConstants.CHANNEL, ep.getTopicID());
						SetHeaderDefinition message = new SetHeaderDefinition(RedisConstants.MESSAGE,
								new SimpleExpression("${exchange.getIn().getBody()}"));
						pdTo.add(channel);
						pdTo.add(message);
						toLog = "Redis topic -" + ep.getTopicID();
					}
					toToEndPoint.setUri(toToURI);
					pdTo.add(toToEndPoint);
					rdToEndPoint.setInput(fromToEndPoint);
					rdToEndPoint.setOutputs(pdTo);
					mcc.addRouteDefinition(rdToEndPoint);
					log.info("Route -" + ep.getEndPointID() + "- is created from -direct:" + ep.getEndPointID()
							+ "- to " + toLog);
				}
			}
		}
	}

	public void connect(CamelContext context, RedissonClient redisson) throws Exception {
		this.setIsConnected(true);
		dataSending(context);
		resultReceiving(context, redisson);

	}

	public void disconnect(CamelContext context, RedissonClient redisson) throws Exception {
		this.setIsConnected(false);
	}
}
