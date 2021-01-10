/* VU Amsterdam, Social AI Group
 * Bilgin Avenoðlu, 10/03/2020 */

package sic.db;

import java.io.IOException;
import org.redisson.api.RLiveObjectService;
import org.redisson.api.RedissonClient;
import sic.Device;
import sic.ServiceRequest;
import sic.User;
import sic.EndPoint;
import sic.ProtocolType;
import sic.Sensor;
import sic.Service;
import sic.redis.GetRedisService;

public class BatchDeviceInsert {

	public static void main(String[] args) throws IOException {
		GetRedisService redisService = new GetRedisService();
		RedissonClient redisson = redisService.getRedisson();
		RLiveObjectService liveObjectService = redisson.getLiveObjectService();

		Service searchedService1 = liveObjectService.get(Service.class, "FaceRecognition");
		if (searchedService1 != null)
			liveObjectService.delete(searchedService1);
		Service searchedService2 = liveObjectService.get(Service.class, "EmotionRecognition");
		if (searchedService2 != null)
			liveObjectService.delete(searchedService2);
		Service searchedService3 = liveObjectService.get(Service.class, "GoogleSpeechToText");
		if (searchedService3 != null)
			liveObjectService.delete(searchedService3);
		
		User searchedDevice0 = liveObjectService.get(User.class, "Bilgin");
		if (searchedDevice0 != null)
			liveObjectService.delete(searchedDevice0);

		Device searchedDevice1 = liveObjectService.get(Device.class, "Robot1");
		if (searchedDevice1 != null)
			liveObjectService.delete(searchedDevice1);
		Device searchedDevice2 = liveObjectService.get(Device.class, "Robot2");
		if (searchedDevice2 != null)
			liveObjectService.delete(searchedDevice2);
/*
		Service service1 = new Service("FaceRecognition");
		service1 = liveObjectService.merge(service1);
			EndPoint rcvEndPointSrv1 = new EndPoint("FaceRecognitionRcv");
			rcvEndPointSrv1.setProtocolType(ProtocolType.REDIS);
			rcvEndPointSrv1.setIpNumber("130.37.53.113");
			rcvEndPointSrv1.setPortNumber("6379");
			rcvEndPointSrv1 = liveObjectService.merge(rcvEndPointSrv1);
			service1.setRcvEndPoint(rcvEndPointSrv1);
			EndPoint sndEndPointSrv1 = new EndPoint("FaceRecognitionSnd");
			sndEndPointSrv1.setProtocolType(ProtocolType.REDIS);
			sndEndPointSrv1.setIpNumber("130.37.53.113");
			sndEndPointSrv1.setPortNumber("6379");
			sndEndPointSrv1 = liveObjectService.merge(sndEndPointSrv1);
			service1.setSndEndPoint(sndEndPointSrv1);

		Service service2 = new Service("EmotionRecognition");
		service2 = liveObjectService.merge(service2);
			EndPoint rcvEndPointSrv2 = new EndPoint("EmotionRecognitionRcv");
			rcvEndPointSrv2.setProtocolType(ProtocolType.REDIS);
			rcvEndPointSrv2.setIpNumber("130.37.53.113");
			rcvEndPointSrv2.setPortNumber("6379");
			rcvEndPointSrv2 = liveObjectService.merge(rcvEndPointSrv2);
			service2.setRcvEndPoint(rcvEndPointSrv2);
			EndPoint sndEndPointSrv2 = new EndPoint("EmotionRecognitionSnd");
			sndEndPointSrv2.setProtocolType(ProtocolType.REDIS);
			sndEndPointSrv2.setIpNumber("130.37.53.113");
			sndEndPointSrv2.setPortNumber("6379");
			sndEndPointSrv2 = liveObjectService.merge(sndEndPointSrv2);
			service2.setSndEndPoint(sndEndPointSrv2);
*/			
		Service service3 = new Service("GoogleSpeechToText");
		service3 = liveObjectService.merge(service3);
				EndPoint rcvEndPointSrv3 = new EndPoint("GoogleSpeechToTextRcv");
				rcvEndPointSrv3.setProtocolType(ProtocolType.REDIS);
				rcvEndPointSrv3.setIpNumber("130.37.53.113");
				rcvEndPointSrv3.setPortNumber("6379");
				rcvEndPointSrv3 = liveObjectService.merge(rcvEndPointSrv3);
				service3.setRcvEndPoint(rcvEndPointSrv3);
				EndPoint sndEndPointSrv3 = new EndPoint("GoogleSpeechToTextSnd");
				sndEndPointSrv3.setProtocolType(ProtocolType.REDIS);
				sndEndPointSrv3.setIpNumber("130.37.53.113");
				sndEndPointSrv3.setPortNumber("6379");
				sndEndPointSrv3 = liveObjectService.merge(sndEndPointSrv3);
				service3.setSndEndPoint(sndEndPointSrv3);
				
		User user1 = new User("Bilgin");
		user1 = liveObjectService.merge(user1);

			Device robot1 = new Device("BilginRobot1");
			robot1 = liveObjectService.merge(robot1);
			robot1.setUser(user1);
			robot1.setIsConnected(false);
			user1.getDevices().add(robot1);
/*	
				Sensor robot1Sensor1 = new Sensor("BilginRobot1Camera1");
				robot1Sensor1 = liveObjectService.merge(robot1Sensor1);
				robot1Sensor1.setDevice(robot1);
				robot1.getSensors().add(robot1Sensor1);
		
					EndPoint rob1Sen1SndEndPoint = new EndPoint("BilginRobot1Camera1Snd");
					rob1Sen1SndEndPoint.setProtocolType(ProtocolType.REDIS);
					rob1Sen1SndEndPoint.setIpNumber("130.37.53.113");
					rob1Sen1SndEndPoint.setPortNumber("6379");
					rob1Sen1SndEndPoint = liveObjectService.merge(rob1Sen1SndEndPoint);
					robot1Sensor1.setSndEndPoint(rob1Sen1SndEndPoint);
	
					ServiceRequest rob1Sen1SrvReq1 = new ServiceRequest("BilginRobot1Camera1FaceRecognition");
					rob1Sen1SrvReq1 = liveObjectService.merge(rob1Sen1SrvReq1);
					rob1Sen1SrvReq1.setService(service1);
					rob1Sen1SrvReq1.setSensor(robot1Sensor1);
					robot1Sensor1.getServiceRequests().add(rob1Sen1SrvReq1);
			
						EndPoint rob1Sen1SerReq1RcvEndPoint = new EndPoint("BilginRobot1Camera1FaceRecognitionRcv1");
						rob1Sen1SerReq1RcvEndPoint.setProtocolType(ProtocolType.HTTP);
						rob1Sen1SerReq1RcvEndPoint.setWebURI("http://130.37.53.111:80/BilginRobot1Camera1FaceRecognitionRcv1");
						rob1Sen1SerReq1RcvEndPoint = liveObjectService.merge(rob1Sen1SerReq1RcvEndPoint);
						rob1Sen1SrvReq1.getSenSrvReqRcvEndPoints().add(rob1Sen1SerReq1RcvEndPoint);
				
					ServiceRequest rob1Sen1SrvReq2 = new ServiceRequest("BilginRobot1Camera1EmotionRecognition");
					rob1Sen1SrvReq2 = liveObjectService.merge(rob1Sen1SrvReq2);
					rob1Sen1SrvReq2.setService(service2);
					rob1Sen1SrvReq2.setSensor(robot1Sensor1);
					robot1Sensor1.getServiceRequests().add(rob1Sen1SrvReq2);
			
						EndPoint rob1Sen1SerReq2RcvEndPoint = new EndPoint("BilginRobot1Camera1EmotionRecognitionRcv1");
						rob1Sen1SerReq2RcvEndPoint.setProtocolType(ProtocolType.REDIS);
						rob1Sen1SerReq2RcvEndPoint.setIpNumber("130.37.53.113");
						rob1Sen1SerReq2RcvEndPoint.setPortNumber("6379");
						rob1Sen1SerReq2RcvEndPoint = liveObjectService.merge(rob1Sen1SerReq2RcvEndPoint);
						rob1Sen1SrvReq2.getSenSrvReqRcvEndPoints().add(rob1Sen1SerReq2RcvEndPoint);
						
						EndPoint rob1Sen1SerReq2RcvEndPoint1 = new EndPoint("BilginRobot1Camera1EmotionRecognitionRcv2");
						rob1Sen1SerReq2RcvEndPoint1.setProtocolType(ProtocolType.HTTP);
						rob1Sen1SerReq2RcvEndPoint1.setWebURI("http://130.37.53.111:80/BilginRobot1Camera1EmotionRecognitionRcv2");
						rob1Sen1SerReq2RcvEndPoint1 = liveObjectService.merge(rob1Sen1SerReq2RcvEndPoint1);
						rob1Sen1SrvReq2.getSenSrvReqRcvEndPoints().add(rob1Sen1SerReq2RcvEndPoint1);
*/
				Sensor robot1Sensor2 = new Sensor("BilginRobot1Mic1");
				robot1Sensor2 = liveObjectService.merge(robot1Sensor2);
				robot1Sensor2.setDevice(robot1);
				robot1.getSensors().add(robot1Sensor2);
		
					EndPoint rob1Sen2SndEndPoint = new EndPoint("BilginRobot1Mic1Snd");
					rob1Sen2SndEndPoint.setProtocolType(ProtocolType.REDIS);
					rob1Sen2SndEndPoint.setIpNumber("130.37.53.113");
					rob1Sen2SndEndPoint.setPortNumber("6379");
					rob1Sen2SndEndPoint = liveObjectService.merge(rob1Sen2SndEndPoint);
					robot1Sensor2.setSndEndPoint(rob1Sen2SndEndPoint);	
					
					ServiceRequest rob1Sen2SrvReq1 = new ServiceRequest("BilginRobot1Mic1GoogleSpeechToText");
					rob1Sen2SrvReq1 = liveObjectService.merge(rob1Sen2SrvReq1);
					rob1Sen2SrvReq1.setService(service3);
					rob1Sen2SrvReq1.setSensor(robot1Sensor2);
					robot1Sensor2.getServiceRequests().add(rob1Sen2SrvReq1);
			
						EndPoint rob1Sen2SerReq1RcvEndPoint = new EndPoint("BilginRobot1Mic1GoogleSpeechToTextRcv1");
						rob1Sen2SerReq1RcvEndPoint.setProtocolType(ProtocolType.REDIS);
						rob1Sen2SerReq1RcvEndPoint.setIpNumber("130.37.53.113");
						rob1Sen2SerReq1RcvEndPoint.setPortNumber("6379");
						rob1Sen2SerReq1RcvEndPoint = liveObjectService.merge(rob1Sen2SerReq1RcvEndPoint);
						rob1Sen2SrvReq1.getSenSrvReqRcvEndPoints().add(rob1Sen2SerReq1RcvEndPoint);
/*				
				Sensor robot1Sensor3 = new Sensor("BilginRobot1Mic2");
				robot1Sensor3 = liveObjectService.merge(robot1Sensor3);
				robot1Sensor3.setDevice(robot1);
				robot1.getSensors().add(robot1Sensor3);
		
					EndPoint rob1Sen3SndEndPoint = new EndPoint("BilginRobot1Mic2Snd");
					rob1Sen3SndEndPoint.setProtocolType(ProtocolType.REDIS);
					rob1Sen3SndEndPoint.setIpNumber("130.37.53.113");
					rob1Sen3SndEndPoint.setPortNumber("6379");
					rob1Sen3SndEndPoint = liveObjectService.merge(rob1Sen3SndEndPoint);
					robot1Sensor3.setSndEndPoint(rob1Sen3SndEndPoint);	
					
					ServiceRequest rob1Sen3SrvReq1 = new ServiceRequest("BilginRobot1Mic2GoogleSpeechToText");
					rob1Sen3SrvReq1 = liveObjectService.merge(rob1Sen3SrvReq1);
					rob1Sen3SrvReq1.setService(service3);
					rob1Sen3SrvReq1.setSensor(robot1Sensor3);
					robot1Sensor3.getServiceRequests().add(rob1Sen3SrvReq1);
			
						EndPoint rob1Sen3SerReq1RcvEndPoint = new EndPoint("BilginRobot1Mic2GoogleSpeechToTextRcv1");
						rob1Sen3SerReq1RcvEndPoint.setProtocolType(ProtocolType.REDIS);
						rob1Sen3SerReq1RcvEndPoint.setIpNumber("130.37.53.113");
						rob1Sen3SerReq1RcvEndPoint.setPortNumber("6379");
						rob1Sen3SerReq1RcvEndPoint = liveObjectService.merge(rob1Sen3SerReq1RcvEndPoint);
						rob1Sen3SrvReq1.getSenSrvReqRcvEndPoints().add(rob1Sen3SerReq1RcvEndPoint);
*/
/*	
 			
			Device robot2 = new Device("BilginRobot2");
			robot2 = liveObjectService.merge(robot2);
			robot2.setUser(user1);
			robot2.setIsConnected(false);
			user1.getDevices().add(robot2);
	
				Sensor robot2Sensor1 = new Sensor("BilginRobot2Camera2");
				robot2Sensor1 = liveObjectService.merge(robot2Sensor1);
				robot2Sensor1.setDevice(robot2);
				robot2.getSensors().add(robot2Sensor1);
		
					EndPoint rob2Sen1SndEndPoint = new EndPoint("BilginRobot2Camera2Snd");
					rob2Sen1SndEndPoint.setProtocolType(ProtocolType.REDIS);
					rob2Sen1SndEndPoint.setIpNumber("130.37.53.113");
					rob2Sen1SndEndPoint.setPortNumber("6379");
					rob2Sen1SndEndPoint = liveObjectService.merge(rob2Sen1SndEndPoint);
					robot2Sensor1.setSndEndPoint(rob2Sen1SndEndPoint);
			
					ServiceRequest rob2Sen1SrvReq1 = new ServiceRequest("BilginRobot2Camera2FaceRecognition");
					rob2Sen1SrvReq1 = liveObjectService.merge(rob2Sen1SrvReq1);
					rob2Sen1SrvReq1.setService(service1);
					rob2Sen1SrvReq1.setSensor(robot2Sensor1);
					robot2Sensor1.getServiceRequests().add(rob2Sen1SrvReq1);
			
						EndPoint rob2Sen1SrReq1RcvEndPoint = new EndPoint("BilginRobot2Camera2FaceRecognitionRcv1");
						rob2Sen1SrReq1RcvEndPoint.setProtocolType(ProtocolType.HTTP);
						rob2Sen1SrReq1RcvEndPoint.setWebURI("http://130.37.53.112:82/BilginRobot2Camera2FaceRecognitionRcv1");
						rob2Sen1SrReq1RcvEndPoint = liveObjectService.merge(rob2Sen1SrReq1RcvEndPoint);
						rob2Sen1SrvReq1.getSenSrvReqRcvEndPoints().add(rob2Sen1SrReq1RcvEndPoint);
			
					ServiceRequest rob2Sen1SrvReq2 = new ServiceRequest("BilginRobot2Camera2EmotionRecognition");
					rob2Sen1SrvReq2 = liveObjectService.merge(rob2Sen1SrvReq2);
					rob2Sen1SrvReq2.setService(service2);
					rob2Sen1SrvReq2.setSensor(robot2Sensor1);
					robot2Sensor1.getServiceRequests().add(rob2Sen1SrvReq2);
			
						EndPoint rob2Sen1SrvReq2RcvEndPoint = new EndPoint("BilginRobot2Camera2EmotionRecognitionRcv1");
						rob2Sen1SrvReq2RcvEndPoint.setProtocolType(ProtocolType.REDIS);
						rob2Sen1SrvReq2RcvEndPoint.setIpNumber("130.37.53.113");
						rob2Sen1SrvReq2RcvEndPoint.setPortNumber("6379");
						rob2Sen1SrvReq2RcvEndPoint = liveObjectService.merge(rob2Sen1SrvReq2RcvEndPoint);
						rob2Sen1SrvReq2.getSenSrvReqRcvEndPoints().add(rob2Sen1SrvReq2RcvEndPoint);
*/
		System.out.println("Database is Created!");

		redisson.shutdown();
	}
}
