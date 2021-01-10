package sic.tests;

import org.apache.camel.Exchange;

public class TestMe {
	public void LetsTest(Exchange ex) {
		ex.getIn().setBody(ex.getIn().getBody(byte[].class));
	}
}
