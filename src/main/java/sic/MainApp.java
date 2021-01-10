/* VU Amsterdam, Social AI Group
 * Bilgin Avenoðlu, 10/03/2020 */

package sic;

import org.apache.camel.main.Main;
import org.slf4j.LoggerFactory;

public class MainApp {
	private static org.slf4j.Logger log = LoggerFactory.getLogger(MainApp.class);
	public static void main(String[] args) {
		
		Initializer routeBuilder = new Initializer();
		
		Main main = new Main();
		
		try {
			main.addRoutesBuilder(routeBuilder);
			main.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}