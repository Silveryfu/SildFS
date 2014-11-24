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
		SildMain sild_primary, sild_replica;

		// Parse input arguments
		arg_parser.parse(args);

		// Start a SildFS from primary.txt
		if (arg_parser.isFresh()) {
			arg_parser.checkFresh();

			// Read primary configuration file
			conf_reader.read(arg_parser.getPrimary());

			// Create a SildFS primary server
			sild_primary = new SildMain(conf_reader.getIp(),
					conf_reader.getPort(), arg_parser.getDir());
			sild_primary.setReplica(false);

			// Start a server along with a replica
			if (arg_parser.isReplicated()) {
				arg_parser.checkReplica();
				
				
				sild_primary.startService();
				
			} else {
				// Start service without replica
				sild_primary.startService();
			}
		} else if (arg_parser.isReboot()) {
			// Start a rebooted server

		} else {
			arg_parser.checkPlain();
			// Start a server from command line input
			String dir = arg_parser.getDir();
			String ip = arg_parser.getIp();
			int port = arg_parser.getPort();

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
			sild.startService();
		}
	}
}
