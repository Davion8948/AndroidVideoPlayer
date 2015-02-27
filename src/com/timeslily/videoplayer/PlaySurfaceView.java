package com.timeslily.videoplayer;

import java.nio.ByteBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import com.timeslily.PlayActivity_New;

public class PlaySurfaceView extends SurfaceView implements Callback {
    private SurfaceHolder holder;
    private BitmapThread thread;
    private FFmpeg ffmpeg;
    private Bitmap bitmap;
    private boolean isPlaying = true;
    public boolean isRun;
    public PlaySurfaceView(Context context) {
        super(context);
    }

    public PlaySurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public PlaySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.holder = getHolder();
        holder.addCallback(this);
        thread = new BitmapThread(holder);
    }

    public void playVideo() {
        isPlaying = true;
        isRun = true;
    }

    public void pauseVideo() {
        isPlaying = false;
    }

    public void stopVideo() {
        isRun = false;
        FFmpeg.stopVideo();
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i("ccc", "surfaceCreated");
        ffmpeg = FFmpeg.getInstance();
        int height = PlayActivity_New.displayHeight;
        int width = PlayActivity_New.displayWidth;
        if (height != 0 && width != 0) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            thread.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i("ccc", "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i("ccc", "surfaceCreated");
    }

    class BitmapThread extends Thread {
        private SurfaceHolder holder;


        public BitmapThread(SurfaceHolder holder) {
            this.holder = holder;
            isRun = true;
        }

        @Override
        public void run() {
            Paint paint = new Paint();
            while (isRun) {
                Canvas canvas = null;
                if (isPlaying) {
                    try {
                        synchronized (holder) {
                            canvas = holder.lockCanvas();
                            byte[] nativePixels = ffmpeg.getNextDecodedFrame();
                            if (nativePixels != null) {
                                ByteBuffer byteBuffer = ByteBuffer.wrap(nativePixels);
                                bitmap.copyPixelsFromBuffer(byteBuffer);
                            }
                            canvas.drawBitmap(bitmap, 0, 0, paint);
                        }
                        Thread.sleep(FFmpeg.frameRate);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        holder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
    }
}
