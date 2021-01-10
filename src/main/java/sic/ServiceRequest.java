/* VU Amsterdam, Social AI Group
 * Bilgin Avenoðlu, 10/03/2020 */

package sic;

import java.io.Serializable;
import java.util.List;

import org.redisson.api.RCascadeType;
import org.redisson.api.annotation.RCascade;
import org.redisson.api.annotation.REntity;
import org.redisson.api.annotation.RId;
import org.redisson.api.annotation.RIndex;
import org.redisson.liveobject.resolver.UUIDGenerator;

@REntity
public class ServiceRequest implements Serializable {
	@RId(generator = UUIDGenerator.class)
	private String serviceRequestID;
	@RIndex
	private Service service;
	@RCascade(RCascadeType.DELETE)
	private List<EndPoint>  senSrvReqRcvEndPoints;
	@RIndex
	private Sensor sensor;
					 
	public ServiceRequest() {

	}

	public ServiceRequest(String serviceRequestID) {
		super();
		this.serviceRequestID = serviceRequestID;
	}

	public String getServiceRequestID() {
		return this.serviceRequestID;
	}

	public Service getService() {
		return this.service;
	}

	public void setService(Service service) {
		this.service = service;
	}

	public Sensor getSensor() {
		return this.sensor;
	}

	public void setSensor(Sensor sensor) {
		this.sensor = sensor;
	}
	
	public List<EndPoint> getSenSrvReqRcvEndPoints() {
		return this.senSrvReqRcvEndPoints;
	}

	public void setSenSrvReqRcvEndPoints(List<EndPoint> senSrvReqRcvEndPoints) {
		this.senSrvReqRcvEndPoints = senSrvReqRcvEndPoints;
	}

}