/* VU Amsterdam, Social AI Group
 * Bilgin Avenoðlu, 10/03/2020 */

package sic.serializer;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

public class RedisObjectByteSerializer implements RedisSerializer<Object> {
	private Converter<Object, byte[]> serializer = new SerializingConverter();
    private Converter<byte[], Object> deserializer = new DeserializingConverter();
    static final byte[] EMPTY_ARRAY = new byte[0];
	public byte[] serialize(Object t) throws SerializationException {
		 if (t == null) {
	            return EMPTY_ARRAY;
	        }

	        try {
	            return serializer.convert(t);
	        } catch (Exception ex) {
	            return EMPTY_ARRAY;
	            //TODO add logic here to only return EMPTY_ARRAY for known conditions
	            // else throw the SerializationException
	            // throw new SerializationException("Cannot serialize", ex);
	        }
	}

	public Object deserialize(byte[] bytes) throws SerializationException {
		if (isEmpty(bytes)) {
            return null;
        }

        try {
            return deserializer.convert(bytes);
        } catch (Exception ex) {
            throw new SerializationException("Cannot deserialize", ex);
        }
	}
	private boolean isEmpty(byte[] data) {
        return (data == null || data.length == 0);
    }

}
