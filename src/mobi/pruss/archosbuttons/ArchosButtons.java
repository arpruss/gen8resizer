package mobi.pruss.archosbuttons;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class ArchosButtons extends Activity {
	Resources res;
	
	private static final int MAX_BUTTONS=8;
	private static final String models[] = 
	{ "A101IT", "A43", "A70H", "A70H2", "A70S", "A70S2" };
	
	private static final boolean defaultBottom[] = {
		false, true, false, false, false, false
	};
	
	private static final int defaultWidth[] = {
		44, 54, 40, 44, 40, 44 };
	
	private static final int screenWidth[] = {
		980+44, 480, 760+40, 980+44, 760+40, 980+44
	};
	
	private static final int screenHeight[] = {
		600, 800+54, 480, 600, 480, 600
	};
	private static final int defaultKeys[][] = {
		{ 217, 102, 229, 158, 0,0,0,0 },
		{ 158, 229, 102, 217, 0,0,0,0 },
		{ 217, 102, 229, 158, 0,0,0,0 },
		{ 217, 102, 229, 158, 0,0,0,0 },
		{ 217, 102, 229, 158, 0,0,0,0 },
		{ 217, 102, 229, 158, 0,0,0,0 }
	};
	private static final String originals[] = {
		"0x12:217:980:0:44:600:0:0:1:.25:0x12:102:980:0:44:600:0:.25:1:.5:0x12:229:980:0:44:600:0:.5:1:.75:0x12:158:980:0:44:600:0:.75:1:1",
		"0x12:158:0:800:480:54:0:0:.25:1:0x12:229:0:800:480:54:.25:0:.5:1:0x12:102:0:800:480:54:.5:0:.75:1:0x12:217:0:800:480:54:.75:0:1:1",
		"0x12:217:760:0:40:480:0:0:1:.25:0x12:102:760:0:40:480:0:.25:1:.5:0x12:229:760:0:40:480:0:.5:1:.75:0x12:158:760:0:40:480:0:.75:1:1",
		"0x12:217:980:0:44:600:0:0:1:.25:0x12:102:980:0:44:600:0:.25:1:.5:0x12:229:980:0:44:600:0:.5:1:.75:0x12:158:980:0:44:600:0:.75:1:1",
		"0x12:217:760:0:40:480:0:0:1:.25:0x12:102:760:0:40:480:0:.25:1:.5:0x12:229:760:0:40:480:0:.5:1:.75:0x12:158:760:0:40:480:0:.75:1:1",
		"0x12:217:980:0:44:600:0:0:1:.25:0x12:102:980:0:44:600:0:.25:1:.5:0x12:229:980:0:44:600:0:.5:1:.75:0x12:158:980:0:44:600:0:.75:1:1"		
	};
	private int width;
	private int keys[];
	private int model;
	private boolean bottom;
	private static final String modelProp="ro.product.model";
	private int[] keyIds = { R.id.key1,R.id.key2,R.id.key3,R.id.key4,
			R.id.key5,R.id.key6,R.id.key7,R.id.key8 };
	
	private Root root;
	
	private SeekBar barControl;
	private TextView currentValue;
	
	private int findKeyIndex(int keyNumber) {
		String[] keyNumbers = res.getStringArray(R.array.key_numbers);
		for (int i=0; i<keyNumbers.length; i++) {
			if (Integer.parseInt(keyNumbers[i]) == keyNumber) {
				return i;
			}
		}
		
		return -1;
	}
	
	private String format(double x) {
		NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
		nf.setMaximumFractionDigits(3);

		String s = nf.format(x);
		
		if (s.startsWith("0.") && s.length()>2 && x != 0) {
			/* trim zero */
			return s.substring(1);
		}
		
		return s;
	}
	
	private String makeKeyLine() {
		String newBar;
		int myWidth;
		int[] myKeys;
		
		int keyCount = 0;
		for(int i=0; i<MAX_BUTTONS; i++) {
			if (keys[i] != 0)
				keyCount ++;
		}
		
		if (keyCount == 0) {
			myWidth = 0;
			myKeys = defaultKeys[model].clone();
			keyCount = 4;
		}
		else {
			myWidth = width;
			myKeys = keys;
		}
		
		if (bottom) {
			int top = screenHeight[model]-myWidth;
			newBar = "0:" + top + ":" + screenWidth[model] + ":" + myWidth;
		}
		else {
			int left = screenWidth[model]-myWidth;
			newBar = left + ":0:" + myWidth + ":" + screenHeight[model]; 
		}
		
		String line = "";
		int pos = 0;
		for (int i=0; i<MAX_BUTTONS; i++) {
			if (myKeys[i] != 0) {
				line += "0x12:"+myKeys[i]+":"+newBar+":";
				if (bottom) {
					line += format((double)pos/keyCount) + ":0:" + 
						format((double)(pos+1)/keyCount) + ":1";
				}
				else {
					line += "0:" + format((double)pos/keyCount) + ":1:" + 
						format((double)(pos+1)/keyCount);
				}
				if (pos + 1 < keyCount)
					line += ":";
				pos++;
			}
		}
		
		return line;		
	}
	
	public void doSetAndReboot(String key) {
		root.exec("sed -i \"1s/.*/" + key + "/\" \"" + getFilename() + "\"");
		root.exec("killall zygote");
		root.close();
	}
	
	public void setAndReboot(View w) {
		doSetAndReboot(makeKeyLine());
	}
	
	public void restore(View w) {
		doSetAndReboot(originals[model]);
	}
	
	public void showSettings() {
		barControl.setProgress(width);
		currentValue.setText(""+width);
		for (int i=0; i<keyIds.length; i++) 
			((Spinner)findViewById(keyIds[i])).setSelection(findKeyIndex(keys[i]));		
	}
	
	public void setDefaults(View v) {
		width = defaultWidth[model];
		bottom = defaultBottom[model];
		keys = defaultKeys[model].clone();
		showSettings();
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
	
	boolean loadSettings() {
		try {
			BufferedReader in
			   = new BufferedReader(new FileReader(getFilename()));
			String line = in.readLine();
			line = line.trim();
			
			if (! line.startsWith("0x12:")) {
				return false;
			}
			
			bottom = defaultBottom[model];
			
			if (bottom != line.matches("^0x12:[0-9]+:0:.*")) {
				return false;
			}

			Pattern pat;
			
			if (bottom) {
				pat = Pattern.compile("^0x12:[0-9]+:[0-9]+:[0-9]+:[0-9]+:([0-9]+).*");
			}
			else {
				pat = Pattern.compile("^0x12:[0-9]+:[0-9]+:[0-9]+:([0-9]+).*"); 
			}
			Matcher match = pat.matcher(line);

			if (match.find()) 
				width = Integer.parseInt(match.group(1));
			else 
				return false;
			
			Log.e("width", ""+width);

			String data[] = line.split(":");			
			
			if (data.length % 10 != 0 || data.length / 10 > MAX_BUTTONS || data.length / 10 < 1) {
				keys = defaultKeys[model].clone();
				width = defaultWidth[model];
				bottom = defaultBottom[model]; 
				return true;
			}
			
			keys = new int[MAX_BUTTONS];
			
			for (int i=0; i<MAX_BUTTONS; i++) {
				if (10*i+1 < data.length) { 
					keys[i] = Integer.parseInt(data[10*i+1]);
					if (findKeyIndex(keys[i])<0) {
						Log.e("ArchosButtons", "not found "+keys[i]);
						keys[i] = defaultKeys[model][i];
					}				
				}
				else {
					keys[i] = defaultKeys[model][i];
				}
			}
						
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}
	
	private void setSpinner(int id, final int keyNum) {
        final Spinner spinner = (Spinner)findViewById(id);
        
        ArrayAdapter<CharSequence> spinAdapter = ArrayAdapter.createFromResource(
        		this, R.array.key_names, android.R.layout.simple_spinner_item);        
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinAdapter);
        OnItemSelectedListener spinListen = new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				keys[keyNum] = 
					Integer.parseInt(
					res.getStringArray(R.array.key_numbers)[spinner.getSelectedItemPosition()]
					                                        );
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
        };
        spinner.setOnItemSelectedListener(spinListen);
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    	res = getResources();

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    	setContentView(R.layout.main);

        if (! getModel() || ! loadSettings()) {
        	fatalError(R.string.incomp_device_title, R.string.incomp_device);
        }

        if (!Root.test()) {
        	fatalError(R.string.need_root_title, R.string.need_root);
        	return;
        }
        
        Log.v("ArchosButtons", "root set");

        currentValue = (TextView)findViewById(R.id.current_value);
        barControl = (SeekBar)findViewById(R.id.width);

        for (int i=0; i<MAX_BUTTONS; i++) 
        	setSpinner(keyIds[i], i);
	
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

		showSettings();
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
