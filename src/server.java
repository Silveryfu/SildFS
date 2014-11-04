import com.sildfs.server.SildMain;
import com.sildfs.tool.SildArgParser;

/**
 * A wrapper for SildFS
 *
 * @author dif
 */

public class server {
	public static void main(String[] args) {
		SildArgParser parser = new SildArgParser();

		// Parse input arguments
		parser.parse(args);
		String dir = parser.getDir();
		String ip = parser.getIp();
		int port = parser.getPort();

		// Enumerate the possibilities, setting up server parameters
		SildMain sild;
		if (ip == null && port == 0) {
			sild = new SildMain(dir);
		} else if (ip == null && port != 0) {
			sild = new SildMain(port, dir);
		} else if (port == 0 && ip != null) {
			sild = new SildMain(ip, dir);
		} else {
			sild = new SildMain(ip, port, dir);
		}

		// Start Sild service
		sild.startService();
	}
}
