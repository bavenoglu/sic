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
public class User implements Serializable {
	@RId(generator = UUIDGenerator.class)
	private String userID;
	@RCascade(RCascadeType.DELETE)
	private List<Device> devices;
	
	protected User() {
		
	}

	public User(String userID) {
		super();
		this.userID = userID;
	}

	public String getUserID() {
		return this.userID;
	}
	
	public List<Device> getDevices() {
		return this.devices;
	}

	public void setDevices(List<Device> devices) {
		this.devices = devices;
	}
}
