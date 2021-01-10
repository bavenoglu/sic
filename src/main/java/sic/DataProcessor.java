/* VU Amsterdam, Social AI Group
 * Bilgin Avenoðlu, 10/03/2020 */

package sic;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.protobuf.SicDataMessages;
import com.google.protobuf.ByteString;

public class DataProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		SicDataMessages.SicDataMessage sicMessageOut = SicDataMessages.SicDataMessage.newBuilder()
				.setUID(exchange.getIn().getHeader("UID").toString())
				.setBData(ByteString.copyFrom(exchange.getIn().getBody(byte[].class)))
				.build();
		
		exchange.getIn().setBody(sicMessageOut.toByteArray());
	}
}
