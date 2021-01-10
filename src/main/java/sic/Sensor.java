/* VU Amsterdam, Social AI Group
 * Bilgin Avenoðlu, 10/03/2020 */

package sic;

import java.io.Serializable;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.redisson.api.RCascadeType;
import org.redisson.api.annotation.RCascade;
import org.redisson.api.annotation.REntity;
import org.redisson.api.annotation.RId;
import org.redisson.api.annotation.RIndex;
import org.redisson.liveobject.resolver.UUIDGenerator;

@REntity
public class Sensor implements Serializable {
	@RId(generator = UUIDGenerator.class)
	private String sensorID;
	private String sensorName;
	private String sensorDataType; // camera, microphone, speaker, screen, gps, accelerometer, gyroscope, proximity, touchscreen, fingerprint, pedometer, barcode, qrcode
	@RIndex
	private Device device;
	@RCascade(RCascadeType.DELETE)
	private List<ServiceRequest> serviceRequests;
	@RCascade(RCascadeType.DELETE)
	private EndPoint sndEndPoint;
	
	protected Sensor() {
		
	}

	public Sensor(String sensorID) {
		super();
		this.sensorID = sensorID;
		this.sensorName = sensorID;
	}

	public String getSensorID() {
		return this.sensorID;
	}

	public String getSensorName() {
		return this.sensorName;
	}

	public void setSensorName(String sensorName) {
		this.sensorName = sensorName;
	}
	
	public Device getDevice() {
		return this.device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public String getSensorDataType() {
		return this.sensorDataType;
	}

	public void setSensorDataType(String sensorDataType) {
		this.sensorDataType = sensorDataType;
	}

	public List<ServiceRequest> getServiceRequests() {
		return this.serviceRequests;
	}

	public void setSndEndPoint(EndPoint sndEndPoint) {
		this.sndEndPoint = sndEndPoint;
	}

	public EndPoint getSndEndPoint() {
		return sndEndPoint;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject getSndEndPointJSON(JSONObject endPointJSON) {
		endPointJSON.put("Protocol", this.getSndEndPoint().getProtocolType());
		endPointJSON.put("Web URI", this.getSndEndPoint().getWebURI());
		endPointJSON.put("IP Number", this.getSndEndPoint().getIpNumber());
		endPointJSON.put("Port Number", this.getSndEndPoint().getPortNumber());
		endPointJSON.put("Topic Name", this.getSndEndPoint().getTopicID());

		return endPointJSON;
	}

	@SuppressWarnings("unchecked")
	public JSONArray getSrvRcvEndPointsJSON(JSONArray endPointsJSON) {
		for (ServiceRequest srvReq : this.getServiceRequests()) {
			JSONArray endPointJSON = new JSONArray();
			for (EndPoint epsrvReq : srvReq.getSenSrvReqRcvEndPoints()) {
				JSONObject rcvJSON = new JSONObject();
				rcvJSON.put("Service Name", srvReq.getService().getServiceName());
				rcvJSON.put("Protocol", epsrvReq.getProtocolType());
				rcvJSON.put("Web URI", epsrvReq.getWebURI());
				rcvJSON.put("IP Number", epsrvReq.getIpNumber());
				rcvJSON.put("Port Number", epsrvReq.getPortNumber());
				rcvJSON.put("Topic Name", epsrvReq.getTopicID());
				endPointJSON.add(rcvJSON);
			}
			endPointsJSON.add(endPointJSON);
		}
		return endPointsJSON;
	}

}
