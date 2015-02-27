#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <stdlib.h>
//包含ffmpeg库头文件
#include <ffmpeg-2.0.1/libavcodec/avcodec.h>
#include <ffmpeg-2.0.1/libavformat/avformat.h>
#include <ffmpeg-2.0.1/libswscale/swscale.h>

//-------------定义Android logtag---------------
#define  LOG_TAG    "FFMPEGSample"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

//全局对象
AVFormatContext *pFormatCtx;
AVCodecContext *pCodecCtx;
AVFrame *pFrame;
AVFrame *pFrameRGB;
int videoStream;
//------------------------------------
//定义的静态方法，将某帧AVFrame在Android的Bitmap中绘制
static void fill_bitmap(AndroidBitmapInfo*  info, void *pixels, AVFrame *pFrame)
{
    uint8_t *frameLine;

    int  yy;
    for (yy = 0; yy < info->height; yy++) {
        uint8_t*  line = (uint8_t*)pixels;
        frameLine = (uint8_t *)pFrame->data[0] + (yy * pFrame->linesize[0]);

        int xx;
        for (xx = 0; xx < info->width; xx++) {
            int out_offset = xx * 4;
            int in_offset = xx * 3;

            line[out_offset] = frameLine[in_offset];
            line[out_offset+1] = frameLine[in_offset+1];
            line[out_offset+2] = frameLine[in_offset+2];
            line[out_offset+3] = 0;
        }
        pixels = (char*)pixels + info->stride;
    }
}
//-------------------------------------------

//---------stringFromJNI native 方法的实现------------------
jstring
Java_com_example_hellojni_HelloJni_stringFromJNI( JNIEnv* env,
                                                        jobject thiz )
{
     char str[25];
     sprintf(str, "%d", avcodec_version());
     return (*env)->NewStringUTF(env, str);
}
//---------------end--------------------------

//---定义java函数，为了Java中的openFile方法调用-----
void Java_com_example_hellojni_HelloJni_openFile(JNIEnv * env, jobject this)
{
    int ret;
    int err;
    int i;
    AVCodec *pCodec;
    uint8_t *buffer;
    int numBytes;
    //注册所有的函数
    av_register_all();
    LOGE("Registered formats");
    //打开sdcard中的vid.3gp文件
    err = avformat_open_input(&pFormatCtx, "file:/sdcard/videos/IOT.avi", NULL, NULL);
    LOGE("Called open file");
    if(err!=0) {
        LOGE("Couldn't open file");
        return;
    }
    LOGE("Opened file");

    if(avformat_find_stream_info(pFormatCtx,NULL)<0) {
        LOGE("Unable to get stream info");
        return;
    }

    videoStream = -1;
    //定义设置videoStream
    for (i=0; i<pFormatCtx->nb_streams; i++) {
        if(pFormatCtx->streams[i]->codec->codec_type==AVMEDIA_TYPE_VIDEO) {
            videoStream = i;
            break;
        }
    }
    if(videoStream==-1) {
        LOGE("Unable to find video stream");
        return;
    }

    LOGI("Video stream is [%d]", videoStream);
    //定义编码类型
    pCodecCtx=pFormatCtx->streams[videoStream]->codec;
    //获取解码器
    pCodec=avcodec_find_decoder(pCodecCtx->codec_id);
    if(pCodec==NULL) {
        LOGE("Unsupported codec");
        return;
    }
    //使用特定的解码器打开
    if(avcodec_open2(pCodecCtx, pCodec,NULL)<0) {
        LOGE("Unable to open codec");
        return;
    }
    //分配帧空间
    pFrame=avcodec_alloc_frame();
    //分配RGB帧空间
    pFrameRGB=avcodec_alloc_frame();
    LOGI("Video size is [%d x %d]", pCodecCtx->width, pCodecCtx->height);
    //获取大小
    numBytes=avpicture_get_size(PIX_FMT_RGB24, pCodecCtx->width, pCodecCtx->height);
    //分配空间
    buffer=(uint8_t *)av_malloc(numBytes*sizeof(uint8_t));

    avpicture_fill((AVPicture *)pFrameRGB, buffer, PIX_FMT_RGB24,
                            pCodecCtx->width, pCodecCtx->height);
}
//-----------------------end--------------------

//定义java回调函数，为了Java中的drawFrame方法调用-----
void Java_com_example_hellojni_HelloJni_drawFrame(JNIEnv * env, jobject this, jstring bitmap)
{
    AndroidBitmapInfo  info;
    void*              pixels;
    int                ret;

    int err;
    int i;
    int frameFinished = 0;
    AVPacket packet;
    static struct SwsContext *img_convert_ctx;
    int64_t seek_target;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }
    LOGE("Checked on the bitmap");

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }
    LOGE("Grabbed the pixels");

    i = 0;
    while((i==0) && (av_read_frame(pFormatCtx, &packet)>=0)) {
        if(packet.stream_index==videoStream) {
            avcodec_decode_video2(pCodecCtx, pFrame, &frameFinished, &packet);

            if(frameFinished) {
                LOGE("packet pts %llu", packet.pts);
                // This is much different than the tutorial, sws_scale
                // replaces img_convert, but it's not a complete drop in.
                // This version keeps the image the same size but swaps to
                // RGB24 format, which works perfect for PPM output.
                int target_width = 320;
                int target_height = 240;
                img_convert_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height,
                       pCodecCtx->pix_fmt,
                       target_width, target_height, PIX_FMT_RGB24, SWS_BICUBIC,
                       NULL, NULL, NULL);
                if(img_convert_ctx == NULL) {
                    LOGE("could not initialize conversion context\n");
                    return;
                }
                sws_scale(img_convert_ctx, (const uint8_t* const*)pFrame->data, pFrame->linesize, 0, pCodecCtx->height, pFrameRGB->data, pFrameRGB->linesize);

                // save_frame(pFrameRGB, target_width, target_height, i);
                fill_bitmap(&info, pixels, pFrameRGB);
                i = 1;
            }
        }
        av_free_packet(&packet);
    }

    AndroidBitmap_unlockPixels(env, bitmap);
}
//-----------------------end--------------------------
//内部调用函数，用来查找帧
int seek_frame(int tsms)
{
    int64_t frame;

    frame = av_rescale(tsms,pFormatCtx->streams[videoStream]->time_base.den,pFormatCtx->streams[videoStream]->time_base.num);
    frame/=1000;

    if(avformat_seek_file(pFormatCtx,videoStream,0,frame,frame,AVSEEK_FLAG_FRAME)<0) {
        return 0;
    }

    avcodec_flush_buffers(pCodecCtx);

    return 1;
}
//--------------------------------------

//定义java回调函数，实现drawFrameAt方法
void Java_com_example_hellojni_HelloJni_drawFrameAt(JNIEnv * env, jobject this, jstring bitmap, jint secs)
{
    AndroidBitmapInfo  info;
    void*              pixels;
    int                ret;

    int err;
    int i;
    int frameFinished = 0;
    AVPacket packet;
    static struct SwsContext *img_convert_ctx;
    int64_t seek_target;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }
    LOGE("Checked on the bitmap");

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }
    LOGE("Grabbed the pixels");

    seek_frame(secs * 1000);

    i = 0;
    while ((i== 0) && (av_read_frame(pFormatCtx, &packet)>=0)) {
        if(packet.stream_index==videoStream) {
            avcodec_decode_video2(pCodecCtx, pFrame, &frameFinished, &packet);

            if(frameFinished) {
                // This is much different than the tutorial, sws_scale
                // replaces img_convert, but it's not a complete drop in.
                // This version keeps the image the same size but swaps to
                // RGB24 format, which works perfect for PPM output.
                int target_width = 320;
                int target_height = 240;
                img_convert_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height,
                       pCodecCtx->pix_fmt,
                       target_width, target_height, PIX_FMT_RGB24, SWS_BICUBIC,
                       NULL, NULL, NULL);
                if(img_convert_ctx == NULL) {
                    LOGE("could not initialize conversion context\n");
                    return;
                }
                sws_scale(img_convert_ctx, (const uint8_t* const*)pFrame->data, pFrame->linesize, 0, pCodecCtx->height, pFrameRGB->data, pFrameRGB->linesize);

                // save_frame(pFrameRGB, target_width, target_height, i);
                fill_bitmap(&info, pixels, pFrameRGB);
                i = 1;
            }
        }
        av_free_packet(&packet);
    }

    AndroidBitmap_unlockPixels(env, bitmap);
}
