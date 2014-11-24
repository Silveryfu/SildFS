import com.sildfs.server.SildMain;
import com.sildfs.tool.SildArgParser;
import com.sildfs.tool.SildConfReader;

/**
 * A wrapper for SildFS primary server It uses SildArgParser to read the input
 * parameters for the server; Uses the SildConfReader to read the primary.txt
 * file to set other parameters for primary server
 * 
 * @author dif
 */

public class server {
	public static void main(String[] args) {
		SildArgParser arg_parser = new SildArgParser();
		SildConfReader conf_reader = new SildConfReader();
		SildMain sild;

		// Parse input arguments
		arg_parser.parse(args);

		if (arg_parser.isPrimary()) {
			/* Start a primary server.
			 * Do sanity check for primary.txt
			 */
			arg_parser.checkFromFile();

			// Read primary configuration file
			conf_reader.read(arg_parser.getPrimaryFile());

			// Create a SildFS primary server
			sild = new SildMain(conf_reader.getIp(),
					conf_reader.getPort(), arg_parser.getDir());
			sild.setReplica(false);

			// Start service
			sild.startService();

		} else if (arg_parser.isReplica()) {
			/* Start a replica server.
			 * Do sanity check for primary.txt
			 */
			arg_parser.checkFromFile();
			arg_parser.checkReplica();
			
			// Start a server from command line input
			String dir = arg_parser.getDir();
			String ip = arg_parser.getIp();
			int port = arg_parser.getPort();

			if (ip == null && port == 0) {
				sild = new SildMain(dir);
			} else if (ip == null && port != 0) {
				sild = new SildMain(port, dir);
			} else if (port == 0 && ip != null) {
				sild = new SildMain(ip, dir);
			} else {
				sild = new SildMain(ip, port, dir);
			}
			sild.startService();
		} else if (arg_parser.isReboot()) {
			/* Start a rebooted server.
			 * Do sanity check for primary.txt
			 */
			arg_parser.checkFromFile();

		} else {
			/* Start a server without replication.
			 * Do sanity check for primary.txt
			 */
			arg_parser.checkPlain();
			// Start a server from command line input
			String dir = arg_parser.getDir();
			String ip = arg_parser.getIp();
			int port = arg_parser.getPort();

			// Enumerate the possibilities, setting up server parameters
			if (ip == null && port == 0) {
				sild = new SildMain(dir);
			} else if (ip == null && port != 0) {
				sild = new SildMain(port, dir);
			} else if (port == 0 && ip != null) {
				sild = new SildMain(ip, dir);
			} else {
				sild = new SildMain(ip, port, dir);
			}
			sild.startService();
		}
	}
}
