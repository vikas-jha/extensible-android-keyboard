package com.example.extkeyboard;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.example.extkeyboard.internal.Configuration;
import com.example.extkeyboard.internal.Constants;
import com.example.extkeyboard.serde.JSON;
import com.example.extkeyboard.serde.JsonSettings;
import com.example.extkeyboard.serde.SettingType;
import com.example.extkeyboard.views.ColorLabelView;
import com.example.extkeyboard.views.PopupActivity;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.slider.AlphaSlider;
import com.flask.colorpicker.slider.LightnessSlider;

import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsView extends PopupActivity {

	private Context context;
	private Resources reosurces;

	private LinearLayout container;
	//private final LinearLayout kbListContainer;
    private final TextView headerLabel;

	private OnPopCloseListener popCloseListener;
	
	private KeyBoardsManager km;
	private Configuration configuration;
	
	private View parent;
	
	private WindowManager wm;

    private int fontSize, padding;

    private JsonSettings pageSettings = null, settings;

    private static FrameLayout.LayoutParams defaultLayoutParams =
            new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);

    private Map<String, SettingsViewPopulator> customDrawMap = new HashMap<>();

    private ColorPickerView colorPickerView;
    private KeyMessageHandler handler = new KeyMessageHandler();
	
	public SettingsView(final Context context) {
		super(context);
		this.context = context;
		this.reosurces = context.getResources();
        fontSize = reosurces.getDimensionPixelSize(R.dimen.kbs_default_font_size);
        padding = reosurces.getDimensionPixelSize(R.dimen.kbs_switch_padding);
		
		km = KeyBoardsManager.getInstance();
		configuration = Configuration.getInstance();
		
		
		wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);

		container = (LinearLayout) ((InputMethodService) context).getLayoutInflater().inflate(R.layout.keyboard_settings, (ViewGroup) parent);
		
		//kbListContainer = (LinearLayout) container.findViewById(R.id.settings_kb_list);

        headerLabel = (TextView) container.findViewById(R.id.settings_lablel);

		TextView backTV = ((TextView) container.findViewById(R.id.settings_header_back));
		backTV.setClickable(true);
		backTV.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				SettingsView.this.onBack();
				
			}
		});

        customDrawMap.put("keyBoardPopulator", new KeyboardViewPopulator());
		
		setOnDismissListener(new PopupWindow.OnDismissListener() {
   			
   			@Override
   			public void onDismiss() {

   				try {
   					FileOutputStream fos = context.openFileOutput(Constants.STR_SETTINGS_FILENAME, Context.MODE_PRIVATE);
   					fos.write(new JSON(Configuration.getInstance().getSettings()).toString().getBytes());
   					fos.close();
   				} catch (IOException e) {
   					e.printStackTrace();
   				}

   				Configuration.load(context);
   				
   				if(popCloseListener != null){
   					popCloseListener.onPopupClose();
   				}
   				
   			}
   		});
   		
	}
	
	public void showAtLocation(View parent, int gravity, int x, int y) {
		this.parent = parent;

		
		Display display = wm.getDefaultDisplay();
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		setWidth(metrics.widthPixels);
        setHeight(metrics.heightPixels - configuration.getStatusBarHeight());
		parent.setMinimumHeight(metrics.heightPixels);
		parent.setVisibility(View.INVISIBLE);

		container.findViewById(R.id.kbs_scroll_view).scrollTo(0,0);

		show();
		super.showAtLocation(parent, gravity, x, y);

	}
	
	public void dismiss(){
		parent.setMinimumHeight(0);
		parent.setVisibility(View.VISIBLE);
		super.dismiss();
	}
	
	public void show() {

        setContentView(container);
        JsonSettings settings = Configuration.getInstance().getSettings();
        this.settings = settings;

        populateCurrentView(settings);
	}

    @Override
    public boolean onBack() {
        if(this.pageSettings != null && this.pageSettings.getParent() != null){
            if(this.pageSettings.getSettingType() != null &&
                    this.pageSettings.getSettingType().equals(SettingType.COLOR)){
                String value = String.format("0x%08x",colorPickerView.getSelectedColor());
                if(this.pageSettings.getValue().length() == 8){
                    value = "0x" + value.substring(4);
                }
                this.pageSettings.setValue(value);
            }
            populateCurrentView(this.pageSettings.getParent());
            return true;
        }else{
            return super.onBack();
        }

    }

    private void populateCurrentView(JsonSettings settings) {
        LinearLayout parent = (LinearLayout) container.findViewById(R.id.kbs_scroll_child);
        parent.removeAllViews();

        this.pageSettings = settings;
        this.headerLabel.setText(settings.getLabel());

        if(settings.getSettingType() != null && settings.getSettingType().equals(SettingType.COLOR)){
            showColorSelector(settings, parent);
        }else if (settings.getRenderer() != null) {
            customDrawMap.get(settings.getRenderer()).populateView(settings, parent);
        }else{
            populateView(settings, parent);
        }
    }

    private void populateView(JsonSettings settings, LinearLayout parent){

        for(JsonSettings js : settings.getSettings()){
            final JsonSettings fjs = js;
            js.setParent(settings);
            if(js.getSettings() != null){
                TextView tv = createHeader(js);
                tv.setClickable(true);
                tv.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        populateCurrentView(fjs);
                    }
                });
                parent.addView(tv);

            }else if(js.getSettingType() != null){
                View v = createControl(js);
                if(v != null){
                    parent.addView(v);
                }

            }else{
                if(js.getOn() != null){
                    parent.addView(createSwitch(js));
                }
            }

        }
    }



    private View createControl(final JsonSettings settings){
        switch (settings.getSettingType()){
            case SettingType.HEADER:
                return createHeader(settings);
            case SettingType.COLOR:
                TextView tv = createColorLabel(settings);
                tv.setClickable(true);
                tv.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        populateCurrentView(settings);
                    }
                });
                return tv;
        }
        return null;
    }

    private TextView createHeader(JsonSettings settings){
        TextView tv = createLabel(settings);
        tv.setTextColor(reosurces.getColor(R.color.teal));
        if(settings.getSettings() != null){
            tv.setBackground(reosurces.getDrawable(R.drawable.settings_border));
        }
        return tv;
    }

    private TextView createLabel(JsonSettings settings){
        TextView tv = new TextView(context);
        tv.setText(settings.getLabel());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);
        tv.setPadding(padding ,padding ,padding, padding);
        tv.setLayoutParams(defaultLayoutParams);
        return tv;
    }

    private TextView createColorLabel(JsonSettings settings){
        ColorLabelView tv = new ColorLabelView(context);
        tv.setColorLabel(Util.getColorValue(settings.getValue()));
        tv.setText(settings.getLabel());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);
        tv.setPadding(padding ,padding ,padding, padding);
        tv.setLayoutParams(defaultLayoutParams);
        tv.setBackground(reosurces.getDrawable(R.drawable.settings_border));
        return tv;
    }

    private Switch createSwitch(JsonSettings js){
        final JsonSettings fjs = js;
        Switch ts = new Switch(context);
        ts.setText(js.getLabel());
        ts.setTag(js.getId());
        ts.setChecked(js.getOn());
        ts.setBackground(reosurces.getDrawable(R.drawable.settings_border));
        ts.getTrackDrawable().setColorFilter(configuration.getControlBgColor(), PorterDuff.Mode.SRC_IN);
        ts.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);
        ts.setPadding(padding ,padding ,padding, padding);
        if(Util.isRTL(js.getLabel())){
            ts.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }
        ts.setLayoutParams(defaultLayoutParams);

        ts.setOnCheckedChangeListener(new OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                fjs.setOn(isChecked);
            }
        });
        return  ts;
    }

    private void showColorSelector(JsonSettings settings, LinearLayout parent){

        String value = settings.getValue();
        long longCode = Util.parserLong(value);
        int alpha = (int) ((longCode & 0xff000000 ) >> 24);
        int rgb = (int)(longCode & 0x00ffffff );
        int color = (alpha << 24) | rgb ;
        if(value.length()  < 10){
            color = color | 0xff000000;
        }

        LinearLayout layout = (LinearLayout) ((InputMethodService) context).getLayoutInflater().
                inflate(R.layout.color_selector, (ViewGroup) parent);
        layout.setVisibility(View.INVISIBLE);

        colorPickerView = (ColorPickerView) layout.findViewById(R.id.colorpicker_color);


        LightnessSlider lightnessSlider = (LightnessSlider) layout.findViewById(R.id.colorpicker_lightness);

        AlphaSlider alphaSlider = (AlphaSlider) layout.findViewById(R.id.colorpicker_alpha);
        if(value.length()  < 10){
            colorPickerView.setAlphaSlider(null);
            alphaSlider.setVisibility(View.GONE);
        }

       if( getWidth() > getHeight()){
            colorPickerView.getLayoutParams().width = getHeight();
            lightnessSlider.getLayoutParams().width = getHeight();
            alphaSlider.getLayoutParams().width = getHeight();
        }else{
           colorPickerView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
       }

        //colorPickerView.setInitialColor(color, true);
        //colorPickerView.setColor(color, true);
        //lightnessSlider.setColor(color);

        Message m = new Message();
        m.arg2 = color;
        m.obj = layout;
        handler.sendMessage(m);

    }

   // private synchronized setInitialColor


	
	public void setOnPopCloseListener(OnPopCloseListener popCloseListener){
		this.popCloseListener = popCloseListener;
	}

	class SettingsListener implements OnCheckedChangeListener {
    	LinearLayout container;
    	int nEnabledBoards = 0;
    	SettingsListener(LinearLayout view){
    		this.container = view;
    	}
		
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if(isChecked){
				nEnabledBoards ++;
			}else{
				nEnabledBoards--;
			}

			SettingsView.this.pageSettings.getSubSetting((String) buttonView.getTag()).setOn(isChecked);
			setSwitchStates();
		}
		public void validate(){
			nEnabledBoards = 0;
			for(int i = 0; i < container.getChildCount(); i++){
				View view = container.getChildAt(i);
				if(view instanceof Switch){
					if(((Switch) view).isChecked()){
						nEnabledBoards++;
					}
				}
			}
			
			setSwitchStates();
		}
		
		private void setSwitchStates(){
			if(nEnabledBoards <= Constants.NUM_MIN_KEYBOARDS){
				for(int i = 0; i < container.getChildCount(); i++){
					View view = container.getChildAt(i);
					if(view instanceof Switch && ((Switch)view).isChecked()){
						view.setEnabled(false);
					}
				}
				Toast.makeText(context, "At least " + Constants.NUM_MIN_KEYBOARDS + " boards must be selected.", Toast.LENGTH_SHORT).show();
			}else if(nEnabledBoards >= Constants.NUM_MAX_KEYBOARDS && 
					km.getAllProviders().size() > Constants.NUM_MAX_KEYBOARDS){
				Toast.makeText(context, "Only upto " + Constants.NUM_MAX_KEYBOARDS + " keyboars can be selected.", Toast.LENGTH_SHORT).show();
				for(int i = 0; i < container.getChildCount(); i++){
					View view = container.getChildAt(i);
					if(view instanceof Switch && !((Switch)view).isChecked()){
						view.setEnabled(false);
					}
				}
			}else{
				for(int i = 0; i < container.getChildCount(); i++){
					View view = container.getChildAt(i);
					if(view instanceof Switch && !((Switch)view).isEnabled()){
						view.setEnabled(true);
					}
				}
			}
			
			
		}
	}
	
	public interface OnPopCloseListener{
		public void onPopupClose();
	}

	public interface SettingsViewPopulator{
        public LinearLayout populateView(JsonSettings settings, LinearLayout parent);
    }

    public class KeyboardViewPopulator implements SettingsViewPopulator{

        @Override
        public LinearLayout populateView(JsonSettings settings, LinearLayout parent) {
            LinearLayout layout = parent;//new LinearLayout(context);
            layout.setLayoutParams(defaultLayoutParams);

            SettingsListener settingsListener = new SettingsListener(layout);

            for(JsonSettings js : settings.getSettings()){
                Switch ts = createSwitch(js);
                ts.setOnCheckedChangeListener(settingsListener);
                parent.addView(ts);
            }


            /*for(InputProvider ip : Configuration.getInstance().getProviders()){

                String name = ip.getName();
                Switch ts = new Switch(context);
                ts.setText(name);

                ts.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);
                ts.setTextColor(Color.DKGRAY);

                ts.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
                int padding = reosurces.getDimensionPixelSize(R.dimen.kbs_switch_padding);
                ts.setPadding(padding ,padding ,padding, padding);
                ts.setBackground(reosurces.getDrawable(R.drawable.settings_border));
                ts.getTrackDrawable().setColorFilter(configuration.getControlBgColor(), PorterDuff.Mode.SRC_IN);

                layout.addView(ts);

                if(Util.isRTL(name)){
                    ts.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                }

                String value = (String) properties.get("kb." + name.replace(" ", "_"));
                ts.setChecked(value!= null && value.equals("true"));

                ts.setOnCheckedChangeListener(settingsListener);

            }*/

            settingsListener.validate();
            return layout;
        }
    }

    protected class KeyMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int color = msg.arg2;

            LinearLayout layout = (LinearLayout) msg.obj;
            ColorPickerView cpv = (ColorPickerView) layout.findViewById(R.id.colorpicker_color);
            cpv.setColor(color, true);
            layout.setVisibility(View.VISIBLE);
        }
    }
}
