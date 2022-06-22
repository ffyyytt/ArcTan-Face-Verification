package com.example.demofaceidapp.mtcnn;
/*
  MTCNN For Android
  by cjf@xmu 20180625
 */
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import java.util.Vector;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class    Utils {
    public static Bitmap copyBitmap(Bitmap bitmap){
        return bitmap.copy(bitmap.getConfig(),true);
    }
    public static void drawRect(Bitmap bitmap,Rect rect,int thick){
        try {
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            int r=255;//(int)(Math.random()*255);
            int g=0;//(int)(Math.random()*255);
            int b=0;//(int)(Math.random()*255);
            paint.setColor(Color.rgb(r, g, b));
            paint.setStrokeWidth(thick);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(rect, paint);
            //Log.i("Util","[*]draw rect");
        }catch (Exception e){
            Log.i("Utils","[*] error"+e);
        }
    }
    public static void drawPoints(Bitmap bitmap, Point[] landmark,int thick){
        for (int i=0;i<landmark.length;i++){
            int x=landmark[i].x;
            int y=landmark[i].y;
            drawRect(bitmap,new Rect(x-1,y-1,x+1,y+1),thick);
        }
    }
    public static void drawBox(Bitmap bitmap,Box box,int thick){
        drawRect(bitmap,box.transform2Rect(),thick);
        drawPoints(bitmap,box.landmark,thick);
    }
    //Flip alone diagonal
    public static void flip_diag(float[]data,int h,int w,int stride){
        float[] tmp=new float[w*h*stride];
        for (int i=0;i<w*h*stride;i++) tmp[i]=data[i];
        for (int y=0;y<h;y++)
            for (int x=0;x<w;x++){
                for (int z=0;z<stride;z++)
                    data[(x*h+y)*stride+z]=tmp[(y*w+x)*stride+z];
            }
    }
    //convert to 2D dst
    public static void expand(float[] src,float[][]dst){
        int idx=0;
        for (int y=0;y<dst.length;y++)
            for (int x=0;x<dst[0].length;x++)
                dst[y][x]=src[idx++];
    }
    //convert to 3D dst
    public static void expand(float[] src,float[][][] dst){
        int idx=0;
        for (int y=0;y<dst.length;y++)
            for (int x=0;x<dst[0].length;x++)
                for (int c=0;c<dst[0][0].length;c++)
                    dst[y][x][c]=src[idx++];

    }
    //dst=src[:,:,1]
    public static void expandProb(float[] src,float[][]dst){
        int idx=0;
        for (int y=0;y<dst.length;y++)
            for (int x=0;x<dst[0].length;x++)
                dst[y][x]=src[idx++*2+1];
    }
    public static Rect[] boxes2rects(Vector<Box> boxes){
        int cnt=0;
        for (int i=0;i<boxes.size();i++) if (!boxes.get(i).deleted) cnt++;
        Rect[] r=new Rect[cnt];
        int idx=0;
        for (int i=0;i<boxes.size();i++)
            if (!boxes.get(i).deleted)
                r[idx++]=boxes.get(i).transform2Rect();
        return r;
    }
    public static Vector<Box> updateBoxes(Vector<Box> boxes){
        Vector<Box> b=new Vector<Box>();
        for (int i=0;i<boxes.size();i++)
            if (!boxes.get(i).deleted)
                b.addElement(boxes.get(i));
        return b;
    }
    public static Bitmap crop(Bitmap bitmap,Rect rect){
        Bitmap cropped=Bitmap.createBitmap(bitmap,rect.left,rect.top,rect.right-rect.left,rect.bottom-rect.top);
        return cropped;
    }
    public static void rectExtend(Bitmap bitmap,Rect rect,int pixels){
        rect.left=max(0,rect.left-pixels);
        rect.right=min(bitmap.getWidth()-1,rect.right+pixels);
        rect.top=max(0,rect.top-pixels);
        rect.bottom=min(bitmap.getHeight()-1,rect.bottom+pixels);
    }

    static public void showPixel(int v){
        Log.i("MainActivity","[*]Pixel:R"+((v>>16)&0xff)+"G:"+((v>>8)&0xff)+ " B:"+(v&0xff));
    }
    static public Bitmap resize(Bitmap _bitmap,int new_width){
        float scale=((float)new_width)/_bitmap.getWidth();
        Matrix matrix=new Matrix();
        matrix.postScale(scale,scale);
        Bitmap bitmap=Bitmap.createBitmap(_bitmap,0,0,_bitmap.getWidth(),_bitmap.getHeight(),matrix,true);
        return bitmap;
    }
}
