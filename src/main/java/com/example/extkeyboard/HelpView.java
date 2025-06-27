package com.example.extkeyboard;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import android.content.res.Resources;
import android.inputmethodservice.InputMethodService;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.example.extkeyboard.internal.Configuration;
import com.example.extkeyboard.views.PopupActivity;

public class HelpView extends PopupActivity {

	private Context context;
	private Resources reosurces;
	private final LinearLayout layoutContainer;
	
	private final WebView webView;
	private final TextView header;
	
	private OnPopCloseListener popCloseListener;
	private Map<String, String> helpFiles = new LinkedHashMap<>();
	
	private View parent;
	
	private JSInterface jsInterface = new JSInterface();
	
	private String onloadjs = "";
	private String helpcss	= "";
	private String footer	= "";
	
	public HelpView(final Context context) {
		super(context);
		this.context = context;
		this.reosurces = context.getResources();
		layoutContainer = (LinearLayout) ((InputMethodService) context).getLayoutInflater().inflate(R.layout.keyboard_help, null);
		
		header = (TextView) layoutContainer.findViewById(R.id.help_header_lablel);
		
		webView = (WebView) layoutContainer.findViewById(R.id.help_webview); 
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setLoadsImagesAutomatically(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.getSettings().setDomStorageEnabled(true);
		
		webView.addJavascriptInterface(jsInterface, "jsInterface");
		final GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){
			
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
	                float velocityY) {
	            if(Math.abs(velocityX) >  1000){
	            	if(velocityX > 0){
	            		webView.evaluateJavascript("showPreviousPage()", null);
	            	}else{
	            		webView.evaluateJavascript("showNextPage()", null);
	            	}
	            	return true;
	            }else{
	            	return false;
	            }
	        }
			
		});
		
		
		webView.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		});
		
		TextView backButton = (TextView) layoutContainer.findViewById(R.id.help_header_back);
		backButton.setClickable(true);
		backButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dismiss();
				
			}
		});
	   	
		
		setOnDismissListener(new PopupWindow.OnDismissListener() {
   			
   			@Override
   			public void onDismiss() {
   				parent.setMinimumHeight(0);
   				parent.setVisibility(View.VISIBLE);
   				
   				if(popCloseListener != null){
   					popCloseListener.onPopupClose();
   				}
   				
   			}
   		});
		
		try {
			onloadjs = Util.readStream(reosurces.getAssets().open("help/basic/snippet/onload.js"));
			helpcss  = Util.readStream(reosurces.getAssets().open("help/basic/snippet/help.css"));
			footer   = Util.readStream(reosurces.getAssets().open("help/basic/snippet/footer.html"));
		} catch (IOException e) {
			e.printStackTrace();
		}
   		
	}
	
	public void showAtLocation(View parent, int gravity, int x, int y){
		this.parent = parent;
		show();
		super.showAtLocation(parent, gravity, x, y);
        load();
	}
	
	public void show(){
		
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
	   	 Display display = wm.getDefaultDisplay();
	   	 DisplayMetrics metrics = new DisplayMetrics();
	   	 display.getMetrics(metrics);
	   	 setWidth(metrics.widthPixels);
	   	 setHeight(metrics.heightPixels - Configuration.getInstance().getStatusBarHeight());
	   	 setContentView(layoutContainer);
	   	 
	 	parent.setVisibility(View.INVISIBLE);
	   	 parent.setMinimumHeight(metrics.heightPixels);
		

	}

	private void load(){
        InputStream is = null;
        String source = "Help could not be loaded.";
        try {
            is = context.getAssets().open("help/basic/index.html");
            if(is != null){
                source = processHTML(Util.readStream(is));
            }
        } catch (Exception e) {
            return;
        }

        webView.loadDataWithBaseURL("file:///android_asset/help/basic/", source, "text/html", "UTF-8", null);
    }
	
	public void setOnPopCloseListener(OnPopCloseListener popCloseListener){
		this.popCloseListener = popCloseListener;
	}
	
	private String processHTML(String source) throws Exception {
		
		StringBuilder sb = new StringBuilder(source);
		
		//clear region marked for clear
				{
					int regionStart = sb.indexOf("<!-- @cleanStart@ -->");
					while(regionStart > -1){
						int regionEnd = sb.indexOf("<!-- @cleanEnd@ -->");
						if(regionEnd > -1){
							sb.replace(regionStart, regionEnd + "<!-- @cleanEnd@ -->".length() , "");
						}else{
							return "<!-- @cleanStart@ --> and <!-- @cleanEnd@ --> do not match.";
						}
						regionStart = sb.indexOf("<!-- @cleanStart@ -->");
					}
				}
		
				
		//Remove javascript
		boolean tagPresent = true;
		String tag = "script";
		
		while(tagPresent){
			
			int tagStart = sb.indexOf("<" + tag);
			int tagEnd = -1;
			int tagSartEnd = sb.indexOf(">", tagStart + tag.length());
			
			if(tagStart > -1){
				if(tagSartEnd > -1 && sb.charAt(tagSartEnd - 1) == '>'){
					sb.replace(tagStart, tagSartEnd, "");
				}else{
					tagEnd = sb.indexOf("</" + tag, tagStart + tag.length());
					if(tagEnd > -1){
						tagEnd = sb.indexOf(">", tagEnd + 1);
					}
					if(tagEnd > -1){
						sb.replace(tagStart, tagEnd + 1, "");
					}
				}
			}else {
				tagPresent = false;
			}
			
		}
		
		
		//Inject javascript and css
		{
			int headEnd = sb.indexOf("</head>");
			if(headEnd == -1){
				return "closing </head> tag not found.";
			}
			sb.insert(headEnd, "<script type=\"text/javascript\">" + onloadjs + "</script>");
			sb.insert(headEnd, "<style type=\"text/css\">" + helpcss + "</style>");
			
		}
		
		//inject footer
		{
			int bodyEnd = sb.indexOf("</body>");
			if(bodyEnd == -1){
				return "closing </body> tag not found.";
			}
			
			sb.insert(bodyEnd, footer);
			
		}
		
		source = sb.toString();
		int titleStart = source.indexOf("<title>") + "<title>".length();
		int titleEnd = source.indexOf("</title>");
		
		if(titleStart > -1 && titleEnd > -1){
			String title = source.substring(titleStart, titleEnd);
			if(title.length() > 20){
				title = title.substring(0,17) + "...";
			}
			header.setText(title);
		}
		source = source.replace("/*@label-color@*/", "color: #009688;");
		
		return source;
	}

	/*********************************************************************************
	 * Inner classes and interfaces 
	 *********************************************************************************/
	public interface OnPopCloseListener{
		public void onPopupClose();
	}
	
	class JSInterface{
		
		private int numberOfPages;
		
		@JavascriptInterface
		public void setNumberOfPages(int n){
			numberOfPages = n;
		}

		public int getNumberOfPages() {
			return numberOfPages;
		}
		
	}
	
	
	
}
