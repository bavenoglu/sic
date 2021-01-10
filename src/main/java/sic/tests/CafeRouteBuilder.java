/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sic.tests;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.AsEndpointUri;
import org.apache.camel.support.DefaultRegistry;
public class CafeRouteBuilder extends RouteBuilder {
    
    public static void main(String[] args) throws Exception {
        CafeRouteBuilder builder = new CafeRouteBuilder();
        builder.runCafeRouteDemo();
    }
        
    public void runCafeRouteDemo() throws Exception {
        DefaultCamelContext camelContext = new DefaultCamelContext();
        camelContext.addRoutes(this);
        camelContext.start();

        ProducerTemplate template = camelContext.createProducerTemplate();
        
        template.sendBody("direct:cafe", "hello");
        Thread.sleep(6000);
        camelContext.stop();
        
    }
    @Override
    public void configure() {
    	String to1 = "direct:foo";
    	String to2 = "direct:bar";
    	String to3 = "direct:baz";
    	String to4 = "mock:result";
    	
		from("direct:cafe")
        .multicast()
            .stopOnException().to(to1,to2,to3,to4)
        .end()
        .to("mock:result");

        from("direct:foo").to("stream:out");
        from("direct:bar").to("mock:bar");
        from("direct:baz").to("mock:baz");

    }
    
}
