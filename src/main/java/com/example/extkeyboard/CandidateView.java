/*
 * Copyright (C) 2008-2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.extkeyboard;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.example.extkeyboard.internal.Configuration;
import com.example.extkeyboard.internal.Constants;

public class CandidateView extends View {

    
    private ExtensibleKeyboard mService;
    private List<String> mSuggestions;
    private int mSelectedIndex;
    private int mTouchX = Constants.OUT_OF_BOUNDS;
    private Drawable mSelectionHighlight;
    private boolean mTypedWordValid;
    private boolean showSeparator = true;    
    private Rect mBgPadding;
    
    private Resources r;

    private static final int SCROLL_PIXELS = 20;
    
    //private int[] mWordWidth = new int[Constants.DICT_MAX_SUGGESTIONS];
    //private int[] mWordX = new int[Constants.DICT_MAX_SUGGESTIONS];

    private static final int X_GAP = 60;
    
    private static final List<String> EMPTY_LIST = new ArrayList<String>();

    private int mVerticalPadding;
    private Paint mPaint;
    private boolean mScrolled;
    private boolean scrollable = false;
    private int mTargetScrollX;
    
    private int mTotalWidth;
    private GestureDetector mGestureDetector;
    
    private List<String> boardSymbols = new ArrayList<>();
    private int currentBoardIndex = 0;
    private long lastTapTime = -1;
    private long lastTapIndex = -1;
    
    private Configuration configuration;
    private ESuggestionTypes suggestionType;
    
    /**
     * Construct a CandidateView for showing suggested words for completion.
     * @param context
     */
    public CandidateView(Context context) {
        super(context);
        mSelectionHighlight = context.getResources().getDrawable(
                android.R.drawable.list_selector_background);
        mSelectionHighlight.setState(new int[] {
                android.R.attr.state_enabled,
                android.R.attr.state_focused,
                android.R.attr.state_window_focused,
                android.R.attr.state_pressed
        });
        
        configuration = Configuration.getInstance();
        
        this.setPadding(0, 10, 0, 10);

        r = context.getResources();
        
        setBackgroundColor(configuration.getKeyBoardHeaderColor());
        
        mVerticalPadding = r.getDimensionPixelSize(R.dimen.candidate_vertical_padding);
        
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(r.getDimensionPixelSize(R.dimen.candidate_font_height));
        mPaint.setStrokeWidth(0);
        
        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                    float distanceX, float distanceY) {
            	
            	if(!scrollable){
            		return false;
            	}
                mScrolled = true;
                int sx = getScrollX();
                sx += distanceX;
                if (sx < 0) {
                    sx = 0;
                }
                if (sx + getWidth() > mTotalWidth) {                    
                    sx -= distanceX;
                }
                mTargetScrollX = sx;
                scrollTo(sx, getScrollY());
                invalidate();
                return true;
            }
        });
        //setHorizontalFadingEdgeEnabled(true);
        setWillNotDraw(false);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
    }
    
    /**
     * A connection back to the service to communicate with the text field
     * @param listener
     */
    public void setService(ExtensibleKeyboard listener) {
        mService = listener;
    }
    
    @Override
    public int computeHorizontalScrollRange() {
        return mTotalWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = resolveSize(50, widthMeasureSpec);
        
        // Get the desired height of the icon menu view (last row of items does
        // not have a divider below)
        Rect padding = new Rect();
        mSelectionHighlight.getPadding(padding);
        final int desiredHeight = ((int)mPaint.getTextSize()) + mVerticalPadding
                + padding.top + padding.bottom + 7;
        
        // Maximum possible width and desired height
        setMeasuredDimension(measuredWidth,
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    /**
     * If the canvas is null, then only touch calculations are performed to pick the target
     * candidate.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas != null) {
            super.onDraw(canvas);
            if (mSuggestions == null || mSuggestions.isEmpty()){
                showOptionsView(canvas);
            }else{
                switch (suggestionType){
                    case autoCompletion:
                    case setting:
                        scrollable = false;
                        showCandidates(canvas);
                        break;
                    case controlKeys:
                        scrollable = false;
                        showOptionsView(canvas);
                        break;
                    case keySuggestions:
                        showKeyExpansion(canvas);
                        break;

                }
            }


        }
    }

    private void showKeyExpansion(Canvas canvas) {
        {

            mTotalWidth = 0;
            if (mBgPadding == null) {
                mBgPadding = new Rect(0, 10, 0, 10);
            }

            int x = 0;
            final int viewWidth = getWidth();
            final int count = mSuggestions.size();

            final int height = getHeight();
            final Rect bgPadding = mBgPadding;
            final Paint paint = mPaint;
            final int touchX = mTouchX;
            final int scrollX = getScrollX();
            final boolean scrolled = mScrolled;
            final int y = (int) (((height - mPaint.getTextSize()) / 2) - mPaint.ascent());
            final int tabWidth = viewWidth / count >= 100 ? viewWidth / count : 100;


            for (int i = 0; i < mSuggestions.size(); i++) {
                String suggestion = mSuggestions.get(i);

                float textWidth = paint.measureText(suggestion);

                paint.setColor(configuration.getKeyTextColor());
                paint.setFakeBoldText(false);
                if (touchX + scrollX >= x && touchX + scrollX < x + tabWidth && !scrolled) {
                    if (canvas != null) {
                        canvas.translate(x, 0);
                        highLightRect(0, bgPadding.top / 2, tabWidth, height - bgPadding.bottom / 2, canvas, paint);
                        canvas.translate(-x, 0);
                        /*
                        mSelectionHighlight.setBounds(0, 0, wordWidth, height);
                        mSelectionHighlight.draw(canvas);
                        canvas.translate(-x, 0);*/
                    }
                    mSelectedIndex = i;
                }

                    if (canvas != null) {
                        canvas.drawText(suggestion, x + (tabWidth - textWidth) / 2, y, paint);

                    }
                    x += tabWidth;
            }

            if (x > getWidth()) {
                scrollable = true;
            } else {
                scrollable = false;
            }
            mTotalWidth = x;
            if (mTargetScrollX != getScrollX()) {
                scrollToTarget();
            }
        }

    }
    
    private void showCandidates(Canvas canvas){
    	
        mTotalWidth = 0;
        if (mBgPadding == null) {
            mBgPadding = new Rect(0, 10, 0, 10);
        }

        mTargetScrollX = 0;
        scrollTo(mTargetScrollX, getScrollY());

        final int separatorColor = 0x44ffffff &
                GraphicsUtils.getComplementaryShade(configuration.getKeyBoardHeaderColor());

        int count = 0;


        final int height = getHeight();
        final int viewWidth = getWidth() - 40;
        final Rect bgPadding = mBgPadding;
        final Paint paint = mPaint;
        paint.setTextSize(r.getDimensionPixelSize(R.dimen.candidate_font_height));
        final int touchX = mTouchX;
        final int y = (int) (((height - mPaint.getTextSize()) / 2) - mPaint.ascent());
        int gap = X_GAP;


        float  width = 0;

        for( ; count < mSuggestions.size() && count < Constants.DICT_MAX_SUGGESTIONS; count++){
            float wordWidth = paint.measureText(mSuggestions.get(count));
            width += gap + wordWidth;
            if(width > viewWidth || wordWidth * count > viewWidth){
                break;
            }
        }

        if(mSuggestions.size() == 1 && count == 0){
            count = 1;
        }else  if(mSuggestions.size() >= 3 && count < 3){
            count = 3;
        }else if(count > 5){
            count = 5;
        }


        int tabWidth = viewWidth /count;
        float defaultXScale = paint.getTextScaleX();
        paint.setFakeBoldText(false);

        int x = 20;
        boolean scaled = false;

        for (int i = 0; i < count; i++) {
            String suggestion = mSuggestions.get(i);
            //New word for addition
            boolean newWord = false;
            if(suggestion.charAt(0) == '^'){
                suggestion = suggestion.substring(1);
                newWord = true;
            }
            //Set back to default scaling for next measurement
            float textWidth = paint.measureText(suggestion);
            int leftGap;
            if(textWidth > tabWidth - gap){
                float xScaling = (tabWidth - gap)/textWidth;
                paint.setTextScaleX(xScaling);
                scaled = true;
                leftGap = gap/2;
            }else{
                leftGap = (int) ((tabWidth - textWidth)/2);
            }

            paint.setColor(configuration.getKeyTextColor());
            if (touchX >= x && touchX < x + tabWidth) {
                if (canvas != null) {
                    highLightRect(x, bgPadding.top/2, x + tabWidth,height - bgPadding.bottom/2, canvas, paint );
                    /*canvas.translate(x, 0);
                    mSelectionHighlight.setBounds(0, 0, tabWidth, height);
                    mSelectionHighlight.draw(canvas);
                    canvas.translate(-x, 0);*/
                }
                mSelectedIndex = i;
            }
            if (canvas != null) {
                
                canvas.drawText(suggestion, x + leftGap, y, paint);
                if( i < count - 1){
                	paint.setColor(separatorColor);
                	canvas.drawLine(x + tabWidth, bgPadding.top,
                			x + tabWidth, height - bgPadding.bottom, paint);
                }
                if(newWord){
                    String clickToAdd = "Touch to add";
                    float preSize = paint.getTextSize();
                    paint.setTextSize(20);
                    textWidth = paint.measureText(clickToAdd);
                    int start = (int) (x + (tabWidth - textWidth)/2);
                    if(tabWidth < textWidth){
                        paint.setTextScaleX(tabWidth/textWidth);
                        start = x;
                        scaled = true;
                    }
                    paint.setColor(configuration.getKeyTextColor());
                    canvas.drawText(clickToAdd, start, 25, paint);
                    paint.setTextSize(preSize);
                }
            }

            if(scaled){
                paint.setTextScaleX(defaultXScale);
                scaled = false;
            }
            x += tabWidth;
        }

        paint.setTextScaleX(defaultXScale);

       /* mTotalWidth = x;
        if (mTargetScrollX != getScrollX()) {
            scrollToTarget();
        }*/
    }
    
    private void scrollToTarget() {
        int sx = getScrollX();
        if (mTargetScrollX > sx) {
            sx += SCROLL_PIXELS;
            if (sx >= mTargetScrollX) {
                sx = mTargetScrollX;
                requestLayout();
            }
        } else {
            sx -= SCROLL_PIXELS;
            if (sx <= mTargetScrollX) {
                sx = mTargetScrollX;
                requestLayout();
            }
        }
        scrollTo(sx, getScrollY());
        invalidate();
    }
    
    public void setSuggestions(List<String> suggestions, boolean completions,
            boolean typedWordValid, ESuggestionTypes suggestionType) {
        this.suggestionType = suggestionType;
        lastTapIndex = -1;
        clear();
        if (suggestions != null) {
            mSuggestions = new ArrayList<String>(suggestions);
        }
        mTypedWordValid = typedWordValid;
        scrollTo(0, 0);
        mTargetScrollX = 0;
        // Compute the total width
        onDraw(null);
        invalidate();
        requestLayout();
    }

    public void clear() {
        mSuggestions = EMPTY_LIST;
        mTouchX = Constants.OUT_OF_BOUNDS;
        mSelectedIndex = -1;
        invalidate();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent me) {

        if (mGestureDetector.onTouchEvent(me)) {
            return true;
        }

        int action = me.getAction();
        int x = (int) me.getX();
        int y = (int) me.getY();
        mTouchX = x;

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mScrolled = false;
            invalidate();
            break;
        case MotionEvent.ACTION_MOVE:
            if (y <= 0) {
                // Fling up!?
            	if (mSelectedIndex >= 0) {
            		mService.pickSuggestionManually(mSelectedIndex);
	            	/*if(mSuggestions.size() > 0){
	            	}else{
	            		mService.onText(boardSymbols.get(mSelectedIndex));
	            	}*/
            	mSelectedIndex = -1;
            	}
            }
            invalidate();
            break;
        case MotionEvent.ACTION_UP:
            if (!mScrolled) {
            	if (mSelectedIndex >= 0) {

                    if(lastTapIndex > Constants.OUT_OF_BOUNDS && lastTapIndex == mSelectedIndex
                            && lastTapTime > Constants.OUT_OF_BOUNDS && lastTapTime + 800 >  me.getEventTime()){
                        ((ExtensibleKeyboard)mService).onHeaderDoubleTap(mSelectedIndex);
                        //mService.
                    }else{
                        lastTapIndex = mSelectedIndex;
                        lastTapTime = me.getEventTime();
                        mService.pickSuggestionManually(mSelectedIndex);
                    }

            	}
            }
            mSelectedIndex = -1;
            removeHighlight();
            requestLayout();
            break;
        }
        return true;
    }
    
    /**
     * For flick through from keyboard, call this method with the x coordinate of the flick 
     * gesture.
     * @param x
     */
    public void takeSuggestionAt(float x) {
        mTouchX = (int) x;
        // To detect candidate
        onDraw(null);
        if (mSelectedIndex >= 0) {
            mService.pickSuggestionManually(mSelectedIndex);
        }
        invalidate();
    }

    private void removeHighlight() {
        mTouchX = Constants.OUT_OF_BOUNDS;
        invalidate();
    }

	public void setKeyboards(List<InputProvider> providers) {
		this.boardSymbols.clear();
		this.boardSymbols.add(Constants.STR_SETTINGS_SYMBOL);
		for(InputProvider provider : providers){
			this.boardSymbols.add(provider.getSymbol());
		}
		this.boardSymbols.add(Constants.STR_HELP_SYMBOL);
		invalidate();
		
	}
	
	private void showOptionsView(Canvas canvas){
		
		//reset all scrolls
		mTargetScrollX = 0;
		scrollTo(mTargetScrollX, getScrollY());
		
		//
		int width = Configuration.getInstance().getDisplayWidth();
		final int height = getHeight();
		final int y = (int) (((height - mPaint.getTextSize()) / 2) - mPaint.ascent());
		int x = 0, xGap = 20;
		
		float textWidth = mPaint.measureText("ABC");
		int minTabWidth = (int) (textWidth) + 20;
		
		final int touchX = mTouchX;
        final int scrollX = getScrollX();
		
        float symbolWidth = mPaint.measureText(Constants.STR_SETTINGS_SYMBOL);
        int settingsWidth = (int) (symbolWidth + 2*xGap);
		int tabWidth = (int) (width - 2*(settingsWidth))/ (boardSymbols.size() - 2);
		if(tabWidth < minTabWidth){
			tabWidth = minTabWidth;
		}

        mPaint.setShadowLayer(0, 0, 0, Constants.COLOR_WHITE);

		mPaint.setFakeBoldText(true);
		mPaint.setTextSize(r.getDimensionPixelSize(R.dimen.multichar_text_height));

		/*
		 *Draw from second to second-last symbols.
		 * First and last are setting and help symbol respectively.
		 * */
		x += settingsWidth;
		for(int i = 1 ; i < boardSymbols.size() - 1; i++){
			if(currentBoardIndex == i - 1){
				mPaint.setColor(configuration.getKeyboardBgColor());
				canvas.drawRoundRect(x, 5, x + tabWidth, height + 10, 10, 10, mPaint);
			}

			boolean xScaled = false;

			mPaint.setColor(configuration.getKeyTextColor());
			String symbol = boardSymbols.get(i);
			textWidth = mPaint.measureText(symbol);
            if(textWidth > tabWidth - 20){
                mPaint.setTextScaleX((tabWidth - 20)/textWidth);
                xScaled = true;
                textWidth = mPaint.measureText(symbol);
            }
			if(Util.isRTL(symbol)){
				canvas.drawText(symbol, x + (tabWidth - textWidth)/2 , y, mPaint);
			}else{
				canvas.drawText(symbol, x + (tabWidth - textWidth)/2 , y, mPaint);
			}

			if(xScaled){
                mPaint.setTextScaleX(1);
            }
			
			
			if (touchX + scrollX >= x && touchX + scrollX < x + tabWidth) {
	            mSelectedIndex = i;
	        }
			
			x += tabWidth;
		}
		
		mPaint.setColor(configuration.getKeyTextColor());
        //mPaint.setShadowLayer(5, 1, 5, (configuration.getKeyTextColor() & 0x00ffffff) | 0x88000000);
		//Draw control buttons
		mPaint.setTextSize(r.getDimensionPixelSize(R.dimen.candidate_font_height));
		// Draw settings icon
		x = 0;
        mPaint.setShadowLayer(2, 1, 1, GraphicsUtils.getComplementaryShade(configuration.getKeyBoardHeaderColor()) & 0x88ffffff);
		canvas.drawText(Constants.STR_SETTINGS_SYMBOL, x + xGap, y, mPaint);
		if (touchX + scrollX >= x && touchX + scrollX < x + settingsWidth) {
            mSelectedIndex = 0;
        }
		// Draw help icon

        setLayerType(LAYER_TYPE_SOFTWARE, mPaint);
        mPaint.setColor(configuration.getKeyTextColor() | 0xcc000000);
        mPaint.setShadowLayer(5, 1, 3, (configuration.getKeyTextColor() & 0x00ffffff) | 0x88000000);
        canvas.drawCircle(width - (symbolWidth)/2 - xGap, (int)(getHeight())/2, (int)(getHeight() - 30)/2 ,mPaint );

        mPaint.setColor(configuration.getKeyBoardHeaderColor());
       // mPaint.setShadowLayer(5, 1, 5, (configuration.getKeyBoardHeaderColor() & 0x00ffffff) | 0x88000000);
        mPaint.setShadowLayer(0, 0, 0, Constants.COLOR_WHITE);
        mPaint.setTextSize((int)(r.getDimensionPixelSize(R.dimen.candidate_font_height)/1.2));
		canvas.drawText(Constants.STR_HELP_SYMBOL, (int)(width - symbolWidth - 1.3 * xGap), y, mPaint);
		if (touchX + scrollX >= width - settingsWidth && touchX + scrollX < width) {
            mSelectedIndex = boardSymbols.size() - 1;
        }
        mPaint.setTextSize(r.getDimensionPixelSize(R.dimen.candidate_font_height));
		
	}

	public void highLightRect(int left, int top, int right, int bottom, Canvas canvas, Paint paint){
        int preColor = paint.getColor();
        paint.setColor(GraphicsUtils.getComplementaryShade(configuration.getKeyBoardHeaderColor()) & 0x22ffffff);
        RectF rF = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(rF, 10, 10, paint);
        paint.setColor(preColor);
    }


	public void setCurrentBoardIndex(int currentBoardIndex) {
		this.currentBoardIndex = currentBoardIndex;
	}

	public List<String> getBoardSymbols() {
		return boardSymbols;
	}
}
