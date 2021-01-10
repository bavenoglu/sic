/* VU Amsterdam, Social AI Group
 * Bilgin Avenoðlu, 10/03/2020 */

package sic.redis;

import java.io.IOException;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import sic.ConfigUtils;

public class GetRedisService {
	public RedissonClient getRedisson() throws IOException {
		Config config = new Config();
		config.useSingleServer().setAddress("redis://" + ConfigUtils.getConfiguration("sic").getProperty("redis.host") + ":"
				+ ConfigUtils.getConfiguration("sic").getProperty("redis.port"));
		//.setPassword(ConfigUtils.getConfiguration("sic").getProperty("redis.password"));
		RedissonClient redisson = Redisson.create(config);
		return redisson;
	}
}
