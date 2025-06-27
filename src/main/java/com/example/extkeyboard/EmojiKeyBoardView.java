package com.example.extkeyboard;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.inputmethodservice.KeyboardView;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.example.extkeyboard.internal.Configuration;
import com.example.extkeyboard.internal.Constants;
import com.example.extkeyboard.serde.JSON;
import com.example.extkeyboard.views.KeyPopup;

public class EmojiKeyBoardView extends View{
	
	private static final int GAP = 20;
	
	private GestureDetector mGestureDetector;
	private boolean mScrolled;
	private int mTargetScrollX = 0/*, mTargetScrollY*/;
	private int mTotalWidth, mTotalHeight;
	private int mTouchX = Constants.OUT_OF_BOUNDS, mTouchY = Constants.OUT_OF_BOUNDS;
	private Resources r;
	private Rect padding;
	
	private Rect rect;
	private RectF rectF;
	
	
	private Paint mPaint;
	private Context context;

    protected KeyPopup keyPopup;
    protected EmojiKey pressedKey;

    protected KeyMessageHandler messageHandler;

    protected  long keyDownTime, keyIndex = Constants.OUT_OF_BOUNDS;

    protected Bitmap tabSymbols;
	protected boolean tabRedraw = true;
	/*private String[][] emojis = {
			{"\uD83D\uDE00","\uD83D\uDE01","\uD83D\uDE02","\uD83D\uDE03","\uD83D\uDE04","\uD83D\uDE05", "\uD83D\uDE05"},
			{"\uD83D\uDE06","\uD83D\uDE07","\uD83D\uDE08","\uD83D\uDE09","\uD83D\uDE0A", "\uD83D\uDE0B"},
			{"\uD83D\uDE0E","\uD83D\uDE0D","\uD83D\uDE18","\uD83D\uDE17","\uD83D\uDE16", "\uD83D\uDE19"},
	};
	*/
	//private int nRows = emojis.length;
	private int keyHeight = 0, keyWidth = 0, keyPerPage;
	
	private String nextKeyBoardSymbol = "ABC";
	
	private int selectX = -1, selectY = -1, preSelectX = -1, preSelectY = -1;
	
	private KeyboardView.OnKeyboardActionListener listener;
	
	private List<EmojiPage> emojiPages = new ArrayList<>();
	private EmojiPage recentEmojiPage;
	private List<String> recentEmojis = new LinkedList<>();
	private int currentPageIndex;
	private Configuration configuration;

    private JsonEmojiPage[] jsonEmojiPages;

	public EmojiKeyBoardView(Context context)  {
		super(context);
		this.context = context;
		
		configuration = Configuration.getInstance();
        keyPopup = new KeyPopup(context);
        messageHandler = new KeyMessageHandler();
		
		rect = new Rect();
		rectF = new RectF();


		
		mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                    float distanceX, float distanceY) {
                mScrolled = true;

                if(keyPopup.isShowing()){
                    keyPopup.dismiss();
                }
                //int sx = getScrollX();
                int sx = mTargetScrollX;
                sx += distanceX;
                int visibleWidth = getWidth();
                if (sx < 0) {
                	if(distanceX < -10){
                		showPrevious();
                	}else{
                		sx = 0;
                	}
                }else if (sx + visibleWidth > mTotalWidth) {
                	if(distanceX > 10){
                		showNext();
                	}else{
                		
                	}
                }else{
                	mTargetScrollX = sx;
                	invalidate();
                }
                //scrollTo(sx, getScrollY());
                return true;
            }
            
            public void onLongPress(MotionEvent e){
            }
        });
		
		r = context.getResources();
		/*colorNormal  = r.getColor(R.color.candidate_normal);
		colorBg  = r.getColor(R.color.colorWhiteGrey);
		colorPressed = r.getColor(R.color.colorWhiteGreyD);*/
		
		mPaint = new Paint();
        mPaint.setColor(configuration.getKeyTextColor());
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(r.getDimensionPixelSize(R.dimen.candidate_font_height));
        mPaint.setStrokeWidth(0);
        
        padding = new Rect(20, 10, 30, 10);
        keyHeight = r.getDimensionPixelSize(R.dimen.key_height);
        float textWidth = (int) (padding.left + mPaint.measureText(Constants.STR_EMOJI_SYMBOL) + padding.right) + GAP;

        int width = context.getResources().getDisplayMetrics().widthPixels;
        keyPerPage = (int)(width/textWidth);
        keyWidth = width/keyPerPage;
        
        mTotalWidth = width;

        try {
            String jsonData = Util.readStream(context.getAssets().open("kb/emoji.json"));
            jsonEmojiPages = (JsonEmojiPage[]) new JSON(jsonData, JsonEmojiPage[].class).toObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        try {
			createKeys();
		} catch (XmlPullParserException | IOException e) {
			e.printStackTrace();
		}



        currentPageIndex = 0;
	}
	
	public void setOnKeyboardActionListener(KeyboardView.OnKeyboardActionListener listener){
		this.listener = listener;
	}
	
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		mTotalWidth = emojiPages.get(currentPageIndex).getMaxWidth() * (keyWidth);
        int measuredWidth = resolveSize(mTotalWidth, widthMeasureSpec);
        
        // Get the desired height of the icon menu view (last row of items does
        // not have a divider below)
        //mSelectionHighlight.getPadding(padding);
        final int desiredHeight = r.getDimensionPixelSize(R.dimen.key_height);
        mTotalHeight = desiredHeight * 4 + ((int)mPaint.getTextSize()) + r.getDimensionPixelSize(R.dimen.candidate_vertical_padding);
        
        // Maximum possible width and desired height
        setMeasuredDimension(measuredWidth,
               resolveSize(mTotalHeight, heightMeasureSpec));
    }
	
	
	@Override
	public void onDraw(Canvas canvas){
		super.onDraw(canvas);

        preSelectX = selectX;
        preSelectY = selectY;

        int width = ((ExtensibleKeyboard)getContext()).getMaxWidth();



		int y = 0;
		int x = 0;

		int i = 0;
		Typeface defaultFont = mPaint.getTypeface();
		Typeface symbolFont = Configuration.getInstance().getSymbolFont();
		mPaint.setTypeface(symbolFont);
		
		int pageTabMargin = 20;
		int pageKeyWidth = (int) ((width - pageTabMargin)/emojiPages.size());
		int pageKeyHeight = (int) (keyHeight * 0.7);
		
		y = pageKeyHeight;
		x += pageTabMargin/2;


        if(tabSymbols == null){
            tabSymbols = Bitmap.createBitmap(getWidth(), pageKeyHeight, Bitmap.Config.ARGB_8888);
            tabRedraw = true;
        }


		mPaint.setColor(configuration.getKeyBoardHeaderColor());
		rect.set(0, 0, width, pageKeyHeight);
        canvas.clipRect(rect, Region.Op.REPLACE);
		canvas.drawRect(rect, mPaint);

		EmojiPage page = emojiPages.get(currentPageIndex);
		mPaint.setTextSize(r.getDimensionPixelSize(R.dimen.emoji_text_height));
		
		/*****************************************************************************
		 * Draw tabs
		 *****************************************************************************/

        if(tabRedraw){
            Canvas c = new Canvas(tabSymbols);
            c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC);
            mPaint.setColor(configuration.getKeyTextColor());

            for(i = 0; i < emojiPages.size() ; i++){
                String text = emojiPages.get(i).getSymbol();
                int textWidth = (int) mPaint.measureText(text);
                c.drawText(text, x + (pageKeyWidth - textWidth)/2 , y - pageKeyHeight/3, mPaint);
                x += pageKeyWidth;
            }

            tabRedraw = false;

            x = 0;
        }

		for(i = 0; i < emojiPages.size() ; i++){
			if (mScrolled == false && mTouchX >= x && mTouchX < x + pageKeyWidth &&
					mTouchY <= y && mTouchY >= y - keyHeight) {
				selectY = 0;
				selectX = i;
			}
			
			if(currentPageIndex == i || (selectX == i && selectY == 0)){
				mPaint.setColor(configuration.getKeyboardBgColor());
				rectF.set(x, y - pageKeyHeight + 5, x + pageKeyWidth, y + 10);
				canvas.drawRoundRect(rectF, 10, 10, mPaint);
			}

			x += pageKeyWidth;
		}

        rect.set(0, 0, getWidth(),pageKeyHeight);
		canvas.drawBitmap(tabSymbols, rect, rect, mPaint);

		//Draw emojis pane

		mPaint.setColor(configuration.getKeyboardBgColor());
		rect.set(0, pageKeyHeight, width, getHeight());
        canvas.clipRect(rect, Region.Op.REPLACE);
		canvas.drawRect(rect, mPaint);


		mPaint.setTypeface(defaultFont);
        mPaint.setTextSize(r.getDimensionPixelSize(R.dimen.emoji_text_height));
		
		y = pageKeyHeight + keyHeight;

        // Draw backgournd
		for(i = 0; i < page.keys.length; i++){
			x = -mTargetScrollX;
			EmojiKey[] row = page.keys[i];
			for(int j = 0 ; j < row.length ; j++){
				EmojiKey key = row[j];
				String text = key.getLabel();

				if (mScrolled == false && mTouchX >= x && mTouchX < x + keyWidth &&
						mTouchY <= y && mTouchY >= y - keyHeight) {
					selectY = i + 1;
					selectX = j;
					highlightKey(x, y, canvas);

                    rect.set(x, y , x + keyWidth, y + keyHeight );
                    showPopup( rect, text );

				}

				x += keyWidth;
			}

			y += keyHeight;

		}

		// draw key text
        y = pageKeyHeight + keyHeight;
        mPaint.setColor(configuration.getKeyTextColor());
        for(i = 0; i < page.keys.length; i++){
            x = -mTargetScrollX;
            EmojiKey[] row = page.keys[i];
            for(int j = 0 ; j < row.length ; j++){
                EmojiKey key = row[j];
                String text = key.getLabel();
                float textWidth = mPaint.measureText(text);

                canvas.drawText(text, x  + (keyWidth - textWidth)/2 , y - keyHeight/3, mPaint);

                x += keyWidth;
            }

            y += keyHeight;

        }
		
		
		x = 0;
		y = getHeight();
		
		mPaint.setStrokeWidth(0);
		mPaint.setColor(r.getColor(R.color.colorWhiteGreyC));
		canvas.drawLine(10, y - keyHeight, width - 10, y - keyHeight , mPaint);
		mPaint.setColor(r.getColor(R.color.teal));
		mPaint.setStrokeWidth(4);
		if(mTotalWidth <= width){
			canvas.drawLine(10, y - keyHeight, width - 10, y - keyHeight , mPaint);
		}else{
			int barStart = (int) (10 + (width - 10.0)/mTotalWidth * mTargetScrollX),
					barEnd = (int) (10 + (width - 10.0)/mTotalWidth * (mTargetScrollX + width));
			canvas.drawLine(barStart, y - keyHeight, barEnd, y - keyHeight , mPaint);
			
		}
		
		/*****************************************************************************
		 * Draw Control keys
		 *****************************************************************************/
		mPaint.setColor(configuration.getKeyTextColor());
		if (mTouchX >= x && mTouchX < x + keyWidth &&
				mTouchY <= y && mTouchY >= y - keyHeight) {
			selectY = i + 1;
			selectX = 0;
			highlightKey(x, y, canvas);

            rect.set(x, y , x + keyWidth, y + keyHeight );
            showPopup( rect, nextKeyBoardSymbol );

		}
		mPaint.setFakeBoldText(true);
		drawKey(x, y - keyHeight/3, nextKeyBoardSymbol, canvas);
		mPaint.setFakeBoldText(false);
		
		x = width - keyWidth;
		
		
		if (mTouchX >= x && mTouchX < x + keyWidth &&
				mTouchY <= y && mTouchY >= y - keyHeight) {
			selectY = i + 1;
			selectX = 1;
			highlightKey(x, y, canvas);
            if(pressedKey == null){
                pressedKey = new EmojiKey(Constants.KEYCODE_DELETE);
                pressedKey.keyCodes = new int[1];
                pressedKey.keyCodes[0] = Constants.KEYCODE_DELETE;
                pressedKey.repeatable = true;
                Message m = new Message();
                m.obj = keyDownTime = SystemClock.uptimeMillis();
                keyIndex = selectX >> 16 | selectY;
                messageHandler.sendMessageDelayed(m ,50);
            }

            rect.set(x, y , x + keyWidth, y + keyHeight );
            showPopup( rect, "\u232B" );

		}
		drawKey(x - padding.right, y - keyHeight/3 , "\u232B", canvas);
		
	}

	private void showPopup(Rect r, String text){
        if(keyPopup.isShowing() && (preSelectX != selectX || preSelectY != selectY)){
            keyPopup.updatePosition(r, text);
        }

        if(!keyPopup.isShowing()){
            keyPopup.show(this, rect, text);
        }
    }

	private int drawKey(int x, int y, String text, Canvas canvas){
		float textWidth = mPaint.measureText(text);
		int wordWidth;
		mPaint.setColor(configuration.getKeyTextColor());
		if(text.length() > 1){
			mPaint.setTextSize(r.getDimensionPixelSize(R.dimen.multichar_text_height));
			wordWidth = (int) (padding.left/2 + textWidth + padding.right/2);
			canvas.drawText(text, padding.left/2 + x + padding.right/2 , y, mPaint);

		}else{
			mPaint.setTextSize(r.getDimensionPixelSize(R.dimen.key_text_size_default));
			wordWidth = (int) (padding.left + textWidth + padding.right);
			canvas.drawText(text, padding.left + x + padding.right , y, mPaint);
		}
		return wordWidth;
	}
	
	private void highlightKey(int x, int y, Canvas canvas){
		if (mScrolled == false && mTouchX >= x && mTouchX < x + keyWidth &&
				mTouchY <= y && mTouchY >= y - keyHeight) {
			mPaint.setColor(configuration.getTouchBgColor());
			canvas.drawRoundRect(new RectF(x, y - keyHeight, x + keyWidth, y ), 10, 10, mPaint);
		}
	}
	
	@Override
    public boolean onTouchEvent(MotionEvent me) {

		performClick();

        if (mGestureDetector.onTouchEvent(me)) {
            return true;
        }
        
        EmojiPage emojiPage = emojiPages.get(currentPageIndex);

        int action = me.getAction();
        int x = (int) me.getX();
        int y = (int) me.getY();
        mTouchX = x;
        mTouchY = y;

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mScrolled = false;
            invalidate();

            break;
        case MotionEvent.ACTION_MOVE:

            if (y <= 0) {
                /*// Fling up!?
                if (mSelectedIndex >= 0) {
                    mService.pickSuggestionManually(mSelectedIndex);
                    mSelectedIndex = -1;
                }*/
            }
            invalidate();
            break;
        case MotionEvent.ACTION_UP:
            if(keyPopup.isShowing()) {
                keyPopup.dismiss();
            }

            if (!mScrolled) {
                if(selectX > -1 && selectY > -1){
                	int[] codes = new int[1];
                	if(selectY == 0){
                		setPageIndex(selectX);
                		mTargetScrollX = 0;
                	}else if(selectY > 0 && selectY <= emojiPage.keys.length ){
                		EmojiKey key = emojiPage.keys[selectY-1][selectX];
                		if(key.keyCodes != null && key.keyCodes.length > 0){
                			codes[0] = key.keyCodes[0];
                    		
                    		if(recentEmojis.contains(codes[0] + "")){
                    			recentEmojis.remove(codes[0] + "");
                    		}
                    		recentEmojis.add(0, codes[0] + "");
                    		listener.onKey(codes[0], codes);
                		}else if(key.label != null){
                			if(recentEmojis.contains(key.label)){
                    			recentEmojis.remove(key.label);
                    		}
                			recentEmojis.add(0, key.label);
                			listener.onText(key.label);
                		}
                		
                	}else if(selectY == emojiPage.keys.length + 1){
                		switch (selectX) {
						case 0:
							codes[0] = -104;
							try {
								saveRecentEmojis();
							} catch (IOException e) {
								e.printStackTrace();
							}
	                		listener.onKey(codes[0], codes);
							break;

						case 1:
							codes[0] = -5;
	                		listener.onKey(codes[0], codes);
							break;
						}
                		
                	}
                	selectX = selectY = Constants.OUT_OF_BOUNDS;
                	mTouchX = mTouchY = Constants.OUT_OF_BOUNDS;
                    pressedKey = null;
                }
            }
            invalidate();
            /*mSelectedIndex = -1;
            removeHighlight();
            requestLayout();*/
            break;
        }
        return true;
    }
	
	public boolean performClick(){
		return super.performClick();
	}

	public String getNextKeyBoardSymbol() {
		return nextKeyBoardSymbol;
	}

	public void setNextKeyBoardSymbol(String nextKeyBoardSymbol) {
		this.nextKeyBoardSymbol = nextKeyBoardSymbol;
	}
	
	private void showNext(){
		if(currentPageIndex < emojiPages.size() - 1){
			setPageIndex(currentPageIndex + 1);
			mTargetScrollX = 0;
			invalidate();
		}
	}
	
	private void showPrevious(){
		if(currentPageIndex > 0){
			setPageIndex(currentPageIndex - 1);
			int pageWidth = emojiPages.get(currentPageIndex).maxWidth * (keyWidth);
			if(pageWidth > getWidth()){
				mTargetScrollX = pageWidth - getWidth();
			}else{
				mTargetScrollX = 0;
			}
			invalidate();
		}
	}
	
	private void setPageIndex(int index){
		currentPageIndex = index;
		if(index == 0){
			populateEmojiPage(recentEmojiPage, recentEmojis, keyPerPage );
		}
		requestLayout();
	}
	
	public void createKeys() throws XmlPullParserException, IOException{

        EmojiPage emojiPage = null;

        List<List<EmojiKey>> page = null;
        List<EmojiKey> row = null;

        emojiPage = new EmojiPage(new String(Character.toChars(Constants.SYMBOL_RECENT_EMOJI)));
        emojiPages.add(emojiPage);
        loadRecentEmojis();
        populateEmojiPage(emojiPage, recentEmojis, keyPerPage);
        recentEmojiPage = emojiPage;

        for(JsonEmojiPage jsonEmojiPage : jsonEmojiPages){
            emojiPage = new EmojiPage(jsonEmojiPage.getSymbol());
            emojiPages.add(emojiPage);
            populateEmojiPage(emojiPage, jsonEmojiPage.getEmojis(), keyPerPage);
            emojiPage.computerMaxWidth();
        }

		/*XmlResourceParser parser = getResources()
				.getXml(R.xml.emoji);

		

		//Add pages from xml
		int eventType = parser.getEventType();
		while (eventType != XmlResourceParser.END_DOCUMENT) {
			if (eventType == XmlResourceParser.START_DOCUMENT) {
				
			} else if (eventType == XmlResourceParser.START_TAG) {
				if(parser.getName().equals("EmojiBoard")){
					//Do nothing
				}else if(parser.getName().equals("Page")){
					String pageSymbol = parser.getAttributeValue(null, "symbol");
					emojiPage = new EmojiPage(pageSymbol);
					emojiPages.add(emojiPage);
					page = new ArrayList<>();
				}else if(parser.getName().equals("Row")){
					row = new ArrayList<>();
					page.add(row);
				}else if(parser.getName().equals("Key")){
					EmojiKey key = new EmojiKey(parser);
					row.add(key);
				}
			} else if (eventType == XmlResourceParser.END_TAG) {
				if(parser.getName().equals("Page")){
					emojiPage.populateBlock(page);
				}
			} else if (eventType == XmlResourceParser.TEXT) {
			}
			eventType = parser.next();
		}*/
	}
	
	private void loadRecentEmojis(){
		
		try {
			InputStream is = context.openFileInput(Constants.STR_RECENT_EMOJI_FILENAME);
			String data = Util.readStream(is);
			is.close();
            if(!data.isEmpty()){
                recentEmojis.addAll(Arrays.asList(data.split(",")));
            }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void populateEmojiPage(EmojiPage emojiPage, List<String> emojis, int nKeys){

        List<List<EmojiKey>> emojiKeyRows = new ArrayList<>(3);
		emojiPage.keys = new EmojiKey[3][];
        int i = 0;
		for(; i < 3 ; i++){
            List<EmojiKey> emojiRow = new ArrayList<>();
			int rowLength = 0; 
			if(emojis.size() < i * nKeys){
				rowLength = 0;
			}else if(emojis.size() < (i + 1) * nKeys){
				rowLength = emojis.size() - i * nKeys;
			}else{
				rowLength = nKeys;
			}
			for(int j = 0; j < rowLength ; j++){
				String value = emojis.get(i * nKeys + j);
				if(value.matches("\\d+") || value.matches("0x[\\dA-Fa-f]+") ){
					int code = Integer.parseInt(value);
                    emojiRow.add(new EmojiKey(code));
				}else if(value.length() == Character.charCount(value.codePointAt(0))){
                    emojiRow.add(new EmojiKey(value.codePointAt(0)));
				}else{
                    emojiRow.add(new EmojiKey(value));
                }
			}
			emojiKeyRows.add(emojiRow);
		}
		if(emojis.size() > nKeys * 3){
            for(i = nKeys * 3; i < emojis.size(); i++){
                EmojiKey emojiKey;
                String value = emojis.get(i);
                if(value.matches("\\d+") || value.matches("0x[\\dA-Fa-f]+") ){
                    int code = Integer.parseInt(value);
                    emojiKey = new EmojiKey(code);
                }else{
                    emojiKey = new EmojiKey(value);
                }
                emojiKeyRows.get(i%3).add(emojiKey);

            }
        }

        emojiPage.keys[0] = emojiKeyRows.get(0).toArray(new EmojiKey[]{});
        emojiPage.keys[1] = emojiKeyRows.get(1).toArray(new EmojiKey[]{});
        emojiPage.keys[2] = emojiKeyRows.get(2).toArray(new EmojiKey[]{});

	}
	
	public void saveRecentEmojis() throws IOException{
		OutputStream os = context.openFileOutput(Constants.STR_RECENT_EMOJI_FILENAME, Context.MODE_PRIVATE);
		StringBuilder sb = new StringBuilder();
		for(String str : recentEmojis){
			if(sb.length() > 0){
				sb.append(',');
			}
			sb.append(str);
		}
		
		os.write(sb.toString().getBytes());
		os.close();
	}
	
	private static class EmojiPage{
		private String symbol;
		public EmojiKey[][] keys = null;
		private int maxWidth = 0;
		
		public EmojiPage(String symbol) {
			this.symbol = symbol;
		}
		
		public void populateBlock(List<List<EmojiKey>> page){
			keys = new EmojiKey[page.size()][];
			for(int i = 0 ;i < page.size(); i++){
				List<EmojiKey> row = page.get(i);
				keys[i] = new EmojiKey[row.size()];
				
				if(row.size() > maxWidth){
					maxWidth = row.size();
				}
				
				for(int j = 0 ; j < row.size() ; j++){
					keys[i][j] = row.get(j);
				}
			}
		}

		public void computerMaxWidth(){
            int maxKeys = 0;
            for(int i = 0 ;i < keys.length; i++){
                if(keys[i].length > maxKeys){
                    maxKeys = keys[i].length;
                }
            }
            maxWidth = maxKeys;
        }

		public int getMaxWidth() {
			return maxWidth;
		}

		public String getSymbol() {
			return symbol;
		}
	}
	
	public static class EmojiKey{
		public int[] keyCodes;
		public boolean repeatable = false;
        private String label;

		
		public EmojiKey(int keyCode){
			keyCodes = new int[1];
			keyCodes[0] = keyCode;
            if(Character.isValidCodePoint(keyCode)){
                label = new String(Character.toChars(keyCodes[0]));
            }else{
                label = "";
            }

		}
		
		public EmojiKey(String label){
			this.label = label;
		}
		
		public EmojiKey(XmlResourceParser parser) {
			String codesValue = parser.getAttributeValue(null, "codes");
			if(codesValue != null){
				String[] codes = codesValue.split(",");
				keyCodes = new int[codes.length];
				for(int i = 0 ; i < codes.length; i++){
					String string = codes[i].trim();
					if(string.startsWith("0x")){
						keyCodes[i] = Integer.parseInt(string.substring(2), 16);
					}else{
						keyCodes[i] = Integer.parseInt(string);
					}
				}
			}
			label = parser.getAttributeValue(null, "label");
			if(label == null){
				label = new String(Character.toChars(keyCodes[0]));
			}
		}
		
		public String getLabel(){
			return this.label;
		}
	}

    private static class JsonEmojiPage{
        private String name;
        private String symbol;
        public JsonEmojiKey[] keys = null;
        public String[] codes = null;
        private int maxWidth = 0;

        public JsonEmojiPage(String symbol) {
            this.symbol = symbol;
        }

        public int getMaxWidth() {
            return maxWidth;
        }

        public String getSymbol() {
            return String.valueOf(Character.toChars(Util.parserInt(symbol)));
        }

        public List<String> getEmojis() {
            List<String> emojis = new ArrayList<>();

            StringBuilder emoji = new StringBuilder();
            for(String code : codes){
                emoji.setLength(0);

                String[] parts = code.split("0x");
                for(String part : parts){
                    if(part.length() > 0){
                        int unicode = Util.parserInt("0x" + part);
                        emoji.append(String.valueOf(Character.toChars(unicode)));
                    }
                }
                emojis.add(emoji.toString());
            }

            return  emojis;
        }
    }

    public static class JsonEmojiKey{
        public int[] keyCodes;
        private String label;

        public JsonEmojiKey(int keyCode){
            keyCodes = new int[1];
            keyCodes[0] = keyCode;
            label = new String(Character.toChars(keyCodes[0]));
        }

        public JsonEmojiKey(String label){
            this.label = label;
        }

        public JsonEmojiKey(XmlResourceParser parser) {
            String codesValue = parser.getAttributeValue(null, "codes");
            if(codesValue != null){
                String[] codes = codesValue.split(",");
                keyCodes = new int[codes.length];
                for(int i = 0 ; i < codes.length; i++){
                    String string = codes[i].trim();
                    if(string.startsWith("0x")){
                        keyCodes[i] = Integer.parseInt(string.substring(2), 16);
                    }else{
                        keyCodes[i] = Integer.parseInt(string);
                    }
                }
            }
            label = parser.getAttributeValue(null, "label");
            if(label == null){
                label = new String(Character.toChars(keyCodes[0]));
            }
        }

        public String getLabel(){
            return this.label;
        }
    }


    protected class KeyMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            long msgTime = (long) msg.obj;
            EmojiKey key = pressedKey;
            if (pressedKey != null && key.repeatable &&
                    keyDownTime == msgTime && keyIndex == (selectX >> 16 | selectY)) {

                listener.onKey(key.keyCodes[0], key.keyCodes);
                Message m = new Message();
                m.obj = msgTime;
                m.arg2 = msg.arg2;
                this.sendMessageDelayed(m, 50);
            }

        }
    }
	

}
