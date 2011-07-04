package mobi.pruss.gen8resizer;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mobi.pruss.gen8resizer.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu;  
import android.view.ContextMenu.ContextMenuInfo;  
import android.view.Window;

public class Gen8Resizer extends Activity {
	Resources res;
	
	private static final String models[] = 
	{ "A101IT", "A43", "A70H", "A70H2", "A70S", "A70S2" };
	private static final int defaults[][] = {
		{ 980,0,44,600 },
		{ 0,800,480,54 },
		{ 760,0,40,480 },
		{ 980,0,44,600 },
		{ 760,0,40,480 }, 
		{ 980,0,44,600 }
	};
	private boolean landscape[] = {
			true, false, true, true, true, true
	};
	private String templates[] = {
			"0x12:217_0:0:1:.25:0x12:102_0:.25:1:.5:0x12:229_0:.5:1:.75:0x12:158_0:.75:1:1",
			"0x12:158_0:0:.25:1:0x12:229_.25:0:.5:1:0x12:102_.5:0:.75:1:0x12:217_.75:0:1:1",
			"0x12:217_0:0:1:.25:0x12:102_0:.25:1:.5:0x12:229_0:.5:1:.75:0x12:158_0:.75:1:1",
			"0x12:217_0:0:1:.25:0x12:102_0:.25:1:.5:0x12:229_0:.5:1:.75:0x12:158_0:.75:1:1",
			"0x12:217_0:0:1:.25:0x12:102_0:.25:1:.5:0x12:229_0:.5:1:.75:0x12:158_0:.75:1:1",
			"0x12:217_0:0:1:.25:0x12:102_0:.25:1:.5:0x12:229_0:.5:1:.75:0x12:158_0:.75:1:1"
	};
	private int width;	
	private int model;
	private static final String modelProp="ro.product.model";
	
	private Root root;
	
	private SeekBar barControl;
	private TextView currentValue;
	
	private String makeKeyLine() {
		String s = templates[model];
		String newBar;
		
		if (landscape[model]) {
			int left = defaults[model][0]+defaults[model][2]-width;
			newBar = ":" + left + ":" + defaults[model][1] + ":" + width + ":" + defaults[model][3] + ":";
		}
		else {
			int top = defaults[model][1]+defaults[model][3]-width;
			newBar = ":" + defaults[model][0] + ":" + top + ":" + defaults[model][2] + ":" + width + ":";
		}
		
		return s.replaceAll("_", newBar);		
	}
	
	private int getDefaultWidth() {
		if (landscape[model]) {
			return defaults[model][2];
		}
		else {
			return defaults[model][3];
		}
	}
	
	public void setAndReboot(View w) {
		String key = makeKeyLine();
		root.exec("sed -i \"1s/.*/" + key + "/\" \"" + getFilename() + "\"");
		root.exec("killall zygote");		
	}
	
	public void setDefaults(View v) {
		width = getDefaultWidth();
		barControl.setProgress(width);
	}
		
	private String getProp(String propId, int buflen) {
		try {
			Process p = Runtime.getRuntime().exec("getprop "+propId);
			DataInputStream stream = new DataInputStream(p.getInputStream());
			byte[] buf = new byte[buflen];
			String s;
			
			int numRead = stream.read(buf);
			if(p.waitFor() != 0) {
				return null;
			}			
			stream.close();
			
			if(0 < numRead) {
				s = (new String(buf, 0, numRead)).trim();
				
				if (s.equals(""))
					return null;
				else
					return s;
			}
			else {
				return null;
			}
		}
		catch (Exception e) {
			return null;
		}
	}
	
	private void fatalError(int title, int msg) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        res = getResources();
        
        Log.e("fatalError", (String) res.getText(title));

        alertDialog.setTitle(res.getText(title));
        alertDialog.setMessage(res.getText(msg));
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
        		res.getText(R.string.ok), 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {finish();} });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {finish();} });
        alertDialog.show();		
	}
	
	boolean getModel() {
		String modelName = getProp(modelProp, 64);
		for (int i=0; i<models.length; i++) {
			if (modelName.equals(models[i])) {
				model = i;
				return true;
			}
		}
		return false;
	}
	
	String getFilename() {
		return "/system/board_properties/virtualkeys."+models[model];
	}
	
	boolean loadWidth() {
		try {
			BufferedReader in
			   = new BufferedReader(new FileReader(getFilename()));
			String line = in.readLine();
			line = line.trim();
			Pattern pat;
			if (landscape[model]) {
				pat = Pattern.compile("0x[0-9]+:[0-9]+:[0-9]+:[0-9]+:([0-9]+).*"); 
			}
			else {
				pat = Pattern.compile("0x[0-9]+:[0-9]+:[0-9]+:[0-9]+:[0-9]+:([0-9]+).*");
			}
			Matcher match = pat.matcher(line);
			if (match.find()) {
				width = Integer.parseInt(match.group(1));
				return true;
			}
			else { 
				return false;
			}
		}
		catch (Exception e) {
			return false;
		}
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    	res = getResources();

    	setContentView(R.layout.main);

        if (! getModel() || ! loadWidth()) {
        	fatalError(R.string.incomp_device_title, R.string.incomp_device);
        }

        if (!Root.test()) {
        	fatalError(R.string.need_root_title, R.string.need_root);
        	return;
        }
        
        Log.v("Gen8Resizer", "root set");

        currentValue = (TextView)findViewById(R.id.current_value);
        barControl = (SeekBar)findViewById(R.id.brightness);

        SeekBar.OnSeekBarChangeListener seekbarListener = 
        	new SeekBar.OnSeekBarChangeListener() {
				
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					currentValue.setText(""+progress);
					width = progress;
				}
			};

		barControl.setOnSeekBarChangeListener(seekbarListener);

    	barControl.setProgress(width);
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	root = new Root();
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	root.close();
    }
    
}
