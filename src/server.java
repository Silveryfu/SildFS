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

		arg_parser.parse(args);
		conf_reader.read(arg_parser.getPrimary());

		sild = new SildMain(conf_reader.getIp(), conf_reader.getPort(),
				arg_parser.getDir());

		sild.printParam();
	}
}
