package com.example.extkeyboard.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.widget.TextView;

import com.example.extkeyboard.GraphicsUtils;
import com.example.extkeyboard.R;
import com.example.extkeyboard.internal.Constants;

/**
 * Created by vijha on 10/4/2017.
 */

public class ColorLabelView extends AppCompatTextView{

    private Integer colorLabel;
    private Bitmap checkeredBg;

    public ColorLabelView(Context context) {
        super(context);
        BitmapDrawable bgDrawable = (BitmapDrawable) context.getDrawable(R.drawable.checkered);
        checkeredBg = bgDrawable.getBitmap();
    }

    public ColorLabelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorLabelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public Integer getColorLabel() {
        return colorLabel;
    }

    public void setColorLabel(Integer colorLabel) {
        this.colorLabel = colorLabel;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
       if(this.getColorLabel() != null){
           int w = this.getWidth();
           int h = this.getHeight();

           int r = h/4;
           int x = w - h/2 - this.getPaddingEnd();
           int y = h/2;
           Paint paint = new Paint();
           paint.setAntiAlias(true);

           paint.setShadowLayer(5 ,3, 3, 0x88000000);

           if(GraphicsUtils.getBrightness(colorLabel) > 0.4){
               paint.setColor(Constants.COLOR_BLACK);
           }else{
               paint.setColor(Constants.COLOR_WHITE);
           }


           canvas.drawCircle(x, y, r + 3, paint);

           Path path = new Path();
           path.addCircle(x, y, r, Path.Direction.CW);
           canvas.clipPath(path, Region.Op.REPLACE);
           canvas.drawBitmap(checkeredBg, x - r, y - r, paint);


           paint.setColor(colorLabel);
           canvas.drawCircle(x,y,r,paint);
       }

    }
}
