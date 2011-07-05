package mobi.pruss.archosbuttons;

import java.io.DataOutputStream;
import android.util.Log;

public class Root {
	private DataOutputStream rootCommands;
	private Process rootShell;
	
	public Root() {
		this(false);
	}

	public Root(boolean output) {
		try {
			if (output) {
				rootShell = Runtime.getRuntime().exec("su");
			}
			else {
				String[] cmds = { "sh", "-c", "su > /dev/null 2> /dev/null" };				
				rootShell = Runtime.getRuntime().exec(cmds);
			}
			
			rootCommands = new DataOutputStream(rootShell.getOutputStream());
		}
		catch (Exception e) {
			rootCommands = null;
		}
	}
	
	public static boolean test() {
		try {
			Process p = Runtime.getRuntime().exec("su");
			DataOutputStream out = new DataOutputStream(p.getOutputStream());
			out.close();
			if(p.waitFor() != 0) {
				return false;
			}
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}
	
	public void close() {
		if (rootCommands != null) {
			try {
				rootCommands.close();
			}
			catch (Exception e) {
			}
			rootCommands = null;
		}
		if (rootShell != null) {
			try {
				rootShell.destroy();
			}
			catch (Exception e) {
			}
			rootShell = null;
		}
	}
	
	public void exec( String s ) {
		try {
			Log.e("root", s);
			rootCommands.writeBytes(s + "\n");
			rootCommands.flush();
		}
		catch (Exception e) {
			Log.e("Error executing",s);
		}
	}
}
