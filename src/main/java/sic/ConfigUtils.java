/* VU Amsterdam, Social AI Group
 * Bilgin Avenoðlu, 10/03/2020 */

package sic;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigUtils {

	public static Properties getConfiguration(String classpathFileName) throws IOException {
		InputStream configStream = ConfigUtils.class.getClassLoader()
				.getResourceAsStream(classpathFileName + ".properties");
		Properties prop = new Properties();
		prop.load(configStream);
		configStream.close();
		return prop;
	}

}