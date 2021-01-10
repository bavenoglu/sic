package sic.tests;

import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.reifier.RouteReifier;

public class ResultRouteBean {
	public void routeResult(Exchange ex) throws Exception {
		ModelCamelContext mcc = ex.getContext().adapt(ModelCamelContext.class);
		
		RouteDefinition rdef = mcc.getRouteDefinitions().get(0);
				
		RouteReifier.adviceWith(rdef, mcc, new AdviceWithRouteBuilder() {
			@Override
			public void configure() throws Exception {
				System.out.println(ex.getProperty("deviceID", String.class));
				if (ex.getProperty("deviceID", String.class).compareTo("Robot1") == 0)
					mockEndpointsAndSkip("direct:Red2");
				else
					mockEndpointsAndSkip("direct:Red1");
			}
		});
	}
}
