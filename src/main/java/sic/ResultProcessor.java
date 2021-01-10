/* VU Amsterdam, Social AI Group
 * Bilgin Avenoðlu, 10/03/2020 */

package sic;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.protobuf.SicDataMessages;
import org.apache.camel.component.protobuf.SicDataMessages.SicDataMessage;
import com.google.protobuf.ByteString;

public class ResultProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		SicDataMessage sicMessageIn= (SicDataMessage) exchange.getIn().getBody();
		exchange.getIn().setHeader("UID", sicMessageIn.getUID());
		SicDataMessages.SicDataMessage sicMessageOut = SicDataMessages.SicDataMessage.newBuilder()
				.setUID(exchange.getIn().getHeader("ServiceID").toString()) // this must be setServiceID
				.setBData(sicMessageIn.getBData())
				.build();
		
		exchange.getIn().setBody(sicMessageOut.toByteArray());
	}
}
