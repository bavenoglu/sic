/* VU Amsterdam, Social AI Group
 * Bilgin Avenoðlu, 10/03/2020 */

package sic.serializer;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

public class RedisByteStringSerializer implements RedisSerializer<String> {
	static final byte[] EMPTY_ARRAY = new byte[0];

	public byte[] serialize(String t) throws SerializationException {
		if (t == null) {
            return EMPTY_ARRAY;
        }

        try {
            return t.getBytes();
        } catch (Exception ex) {
            return EMPTY_ARRAY;
            //TODO add logic here to only return EMPTY_ARRAY for known conditions
            // else throw the SerializationException
            // throw new SerializationException("Cannot serialize", ex);
        }
	}

	public String deserialize(byte[] bytes) throws SerializationException {
		if (isEmpty(bytes)) {
            return null;
        }

        try {
            return bytes.toString();
        } catch (Exception ex) {
            throw new SerializationException("Cannot deserialize", ex);
        }
	}
	private boolean isEmpty(byte[] data) {
        return (data == null || data.length == 0);
    }

}
