/* VU Amsterdam, Social AI Group
 * Bilgin Avenoðlu, 10/03/2020 */

package sic.serializer;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.lang.Nullable;

public class RedisByteArraySerializer implements RedisSerializer<byte[]> {

	

	@Nullable
	@Override
	public byte[] serialize(@Nullable byte[] bytes) throws SerializationException {
		return bytes;
	}

	@Nullable
	@Override
	public byte[] deserialize(@Nullable byte[] bytes) throws SerializationException {
		return bytes;
	}
}
