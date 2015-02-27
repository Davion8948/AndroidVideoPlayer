#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavutil/pixfmt.h>
#include <stdio.h>
#include <pthread.h>

#include <jni.h>
#include <android/bitmap.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#define LOG_TAG "ffmpeg"
#define LOGI(...) __android_log_print(4, LOG_TAG, __VA_ARGS__);
#define LOGE(...) __android_log_print(6, LOG_TAG, __VA_ARGS__);
ANativeWindow* 		window;
char 				*videoFileName;
AVFormatContext 	*formatCtx = NULL;//保存要读入的文件格式信息，比如流的个数，以及流数据等。
int 				videoStream, frameCount=0;
AVCodecContext  	*codecCtx = NULL;//保存了流的详细编码信息，视频宽高，编码类型
AVFrame         	*pFrame = NULL;//用于保存数据帧的数据结构
AVFrame         	*pFrameRGB = NULL;
jobject				bitmap;
uint8_t*			buffer;
struct SwsContext   *sws_ctx = NULL;
int 				width;
int 				height;
int					stop;
AVCodec         	*pCodec = NULL;//真正的编码器，其中有编码需要调用的函数
AVDictionary    	*optionsDict = NULL;
int 				screenWidth, screenHeight;//播放视频的长宽
int 				numBytes;
AVPacket        	packet;
int frameFinished;
int stop;//停止视频解析
int den,num;//分母 分子
// BE for Big Endian, LE for Little Endian
int dstFmt = PIX_FMT_RGB565;
struct SwsContext *img_convert_ctx;
//注册支持的文件格式以及编码器
void avRegisterAll(JNIEnv* env, jobject thiz){
	av_register_all();
}
//读取文件头
jboolean avFormatOpenInput(JNIEnv* pEnv, jobject thiz, jstring pFileName){
	videoFileName = (char *)(*pEnv)->GetStringUTFChars(pEnv, pFileName, NULL);
	//LOGI("video file name is %d", i);
	if(avformat_open_input(&formatCtx, videoFileName, NULL, NULL)!=0){//读取文件头信息，不会填充流信息
		return 0; // Couldn't open file
	}else {
		return 1;
	}
}
//读取流的信息
jboolean avFormatFindStreamInfo(JNIEnv* pEnv, jobject thiz){
	if(avformat_find_stream_info(formatCtx, NULL)<0){//读取流信息
		return 0;
	}else{
		//输出文件信息
		 av_dump_format(formatCtx, -1, videoFileName, 0);
		return 1;
	}
}
//读取流
jboolean avFormatFindVideoStream(JNIEnv* pEnv, jobject thiz){
	int i;
	// Dump information about file onto standard error
	av_dump_format(formatCtx, 0, videoFileName, 0);//输出文件信息
	// Find the first video stream
	videoStream=-1;
	for(i=0; i<formatCtx->nb_streams; i++) {
		if(formatCtx->streams[i]->codec->codec_type==AVMEDIA_TYPE_VIDEO) {
			videoStream=i;
			break;
		}
	}
	if(videoStream==-1){
		return 0; // Didn't find a video stream
	}else{
		num = formatCtx->streams[i]->r_frame_rate.num;
		den = formatCtx->streams[i]->r_frame_rate.den;
		return 1;
	}
}
jintArray Java_com_timeslily_videoplayer_FFmpeg_getFPS(JNIEnv* env, jobject thiz){
	jintArray frame_rate;
	frame_rate = (*env)->NewIntArray(env,2);
	jint params[2];
	params[0] = num;
	params[1] = den;
	(*env)->SetIntArrayRegion(env,frame_rate,0,2,params);
	return frame_rate;
}
//在库里面查找支持该格式的解码器
jboolean avCodecFindDecoder( JNIEnv* env, jobject thiz ){
	// Get a pointer to the codec context for the video stream
	codecCtx=formatCtx->streams[videoStream]->codec;
	// Find the decoder for the video stream
	pCodec=avcodec_find_decoder(codecCtx->codec_id);//寻找相应的解码器
	if(pCodec==NULL) {//false
		fprintf(stderr, "Unsupported codec!\n");
		return 0; // Codec not found
	}else{
		return 1;
	}
}
//
jboolean avCodecOpen2(JNIEnv* env, jobject thiz ){
	// Open codec
	if(avcodec_open2(codecCtx, pCodec, &optionsDict)<0){//打开解码器
		return 0; // Could not open codec
	}else{
		return 1;
	}
}

jstring getCodecName( JNIEnv* env, jobject thiz ){
	return (*env)->NewStringUTF(env, pCodec->name);
}
//设置播放的视频的长宽
void setVideoScreenSize( JNIEnv* env, jobject thiz, int width, int height){
	screenWidth = width;
	screenHeight = height;
}
jboolean allocateBuffer(JNIEnv* env, jobject thiz){
	stop=0;
	// Allocate video frame
	pFrame=avcodec_alloc_frame();//分配一个帧指针，指向解码后的原始帧
	// Allocate an AVFrame structure
	pFrameRGB=avcodec_alloc_frame();
	if(pFrameRGB==NULL)
		return 0;
	// Determine required buffer size and allocate buffer
	numBytes=avpicture_get_size(dstFmt, screenWidth, screenHeight);
/*
	numBytes=avpicture_get_size(dstFmt, pCodecCtx->width,
			      pCodecCtx->height);
*/
	buffer=(uint8_t *)av_malloc(numBytes * sizeof(uint8_t));
	// Assign appropriate parts of buffer to image planes in pFrameRGB
	// Note that pFrameRGB is an AVFrame, but AVFrame is a superset
	// of AVPicture
	avpicture_fill((AVPicture *)pFrameRGB, buffer, dstFmt, screenWidth, screenHeight);
	return 1;
}
void finish(JNIEnv *pEnv) {
	av_free(buffer);
	// Free the RGB image
	av_free(pFrameRGB);
	// Free the YUV frame
	av_free(pFrame);
	// Close the codec
	avcodec_close(codecCtx);
	// Close the video file
	avformat_close_input(&formatCtx);
}
/* for each decoded frame */
jbyteArray getNextDecodedFrame( JNIEnv* env, jobject thiz ){
	av_free_packet(&packet);
	while(av_read_frame(formatCtx, &packet)>=0&&!stop) {
		if(packet.stream_index==videoStream) {
			avcodec_decode_video2(codecCtx, pFrame, &frameFinished, &packet);
			if(frameFinished) {
				img_convert_ctx = sws_getContext(codecCtx->width, codecCtx->height, codecCtx->pix_fmt, screenWidth, screenHeight, dstFmt, SWS_BICUBIC, NULL, NULL, NULL);
/*
img_convert_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height, pCodecCtx->pix_fmt, pCodecCtx->width, pCodecCtx->height, dstFmt, SWS_BICUBIC, NULL, NULL, NULL);
*/
				sws_scale(img_convert_ctx, (const uint8_t* const*)pFrame->data, pFrame->linesize, 0, codecCtx->height, pFrameRGB->data, pFrameRGB->linesize);
				++frameCount;
				/* uint8_t == unsigned 8 bits == jboolean */
				jbyteArray nativePixels = (*env)->NewByteArray(env, numBytes);
				(*env)->SetByteArrayRegion(env, nativePixels, 0, numBytes, buffer);
				return nativePixels;
			}
		}
	av_free_packet(&packet);
}
	return NULL;
}

/**
 * stop the video playback
 */
void stopVideo(JNIEnv *pEnv, jobject pObj) {
	stop = 1;
	finish(pEnv);
}
//视频宽
jint getWidth(JNIEnv* env, jobject thiz){
//	width = codecCtx->width;
	return codecCtx->width;
}
jint getHeight(JNIEnv* env, jobject thiz){
	//height = codecCtx->height;
	return codecCtx->height;
}

void avFormatCloseInput(JNIEnv* env, jobject thiz){
	avformat_close_input(&formatCtx);
}


/**
 * openVideo() 等同于上面allocateBuffer（）以及其之前的方法
 */
void openVideo(JNIEnv *pEnv, jstring filePath){
	int ret;
	int err;
	int i;
	AVCodec *pCodec;
	uint8_t *buffer;
	int numBytes;
//	av_register_all();//通常在FFmpeg对象实例化的时候注册过
	LOGI("Regisitered formats");
	videoFileName = (char *)(*pEnv)->GetStringUTFChars(pEnv, filePath, NULL);
	if(avformat_open_input(&formatCtx, videoFileName, NULL, NULL)!=0){
		LOGE("Couldn't open file");
        return;
	}else{
		LOGE("open file");
	}
    if(avformat_find_stream_info(formatCtx, NULL)<0){
    	LOGE("Unable to get stream info");
    	return;
    }
    videoStream = -1;
    for(i=0;i<formatCtx->nb_streams;i++){
    	if(formatCtx->streams[i]->codec->codec_type==AVMEDIA_TYPE_VIDEO){
    		videoStream = i;
    		break;
    	}
    }
    if(videoStream==-1){
    	LOGE("Unable to find video stream");
    	return;
    }
    LOGI("Video stream is [%d]", videoStream);
    codecCtx = formatCtx->streams[videoStream]->codec;
    pCodec = avcodec_find_decoder(codecCtx->codec_id);
    if(pCodec==NULL){
    	LOGE("Unsupported codec");
    	return;
    }
    if(avcodec_open2(codecCtx, pCodec, NULL)<0){
    	LOGE("Unable to open codec");
    	return;
    }
    pFrame = avcodec_alloc_frame();
    pFrameRGB = avcodec_alloc_frame();
    LOGI("Video size is [%d x %d]", codecCtx->width, codecCtx->height);
    numBytes = avpicture_get_size(PIX_FMT_RGB24, codecCtx->width, codecCtx->height);
    buffer = (uint8_t *)av_malloc(numBytes*sizeof(uint8_t));
    avpicture_fill((AVPicture *)pFrameRGB, buffer, PIX_FMT_RGB24, codecCtx->width, codecCtx->height);
}
//定义的静态方法，将某帧AVFrame在Android的Bitmap中绘制
static void fill_bitmap(AndroidBitmapInfo* info, void *pixels, AVFrame *pFrame){
	uint8_t *frameLine;
	int yy;
	for( yy=0;yy<info->height;yy++){
		uint8_t * line = (uint8_t*)pixels;
		frameLine = (uint8_t *)pFrame->data[0]+(yy*pFrame->linesize[0]);
		int xx;
		for(xx=0;xx<info->width;xx++){
			int out_offset=xx*4;
			int in_offset=xx*3;
			 line[out_offset] = frameLine[in_offset];
			 line[out_offset+1] = frameLine[in_offset+1];
			 line[out_offset+2] = frameLine[in_offset+2];
			 line[out_offset+3] = 0;
		}
		pixels = (char*)pixels + info->stride;
	}
}
//内部调用函数，用来查找帧
int seek_frame(int tsms){
	int64_t frame;
	frame = av_rescale(tsms, formatCtx->streams[videoStream]->time_base.den, formatCtx->streams[videoStream]->time_base.num);
	frame/=1000;
	if(avformat_seek_file(formatCtx, videoStream, 0,frame, frame, AVSEEK_FLAG_FRAME)<0){
		return 0;
	}
    avcodec_flush_buffers(codecCtx);
    return 1;
}
void getBitmap(JNIEnv *env,jstring bitmap, int isFirst, jint second){
	AndroidBitmapInfo info;
		void* pixels;
		int ret;
		int err;
		int i=0;
		int frameFinished = 0;
		AVPacket packet;
		static struct SwsContext *img_convert_ctx;
		int64_t seek_target;
		if((ret=AndroidBitmap_getInfo(env, bitmap, &info))<0){
			LOGE("AndroidBitmap_getInfo() failed! error=%d", ret);
			return;
		}
	    LOGI("Checked on the bitmap");
	    if((ret=AndroidBitmap_lockPixels(env, bitmap, &pixels))<0){
	    	LOGE("AndroidBitmap_lockPixels() failed !error=%d", ret);
	    }
	    LOGI("Grabbed the pixels");
	    if(!isFirst){
	    	seek_frame(second * 1000);
	    }
	    while((i==0)&&(av_read_frame(formatCtx, &packet)>=0)){
	    	if(packet.stream_index==videoStream){
	    		avcodec_decode_video2(codecCtx, pFrame, &frameFinished, &packet);
	    		if(frameFinished){
	    			LOGE("packet pts &llu", packet.pts);
	    		    // This is much different than the tutorial, sws_scale
	    		    // replaces img_convert, but it's not a complete drop in.
	    		    // This version keeps the image the same size but swaps to
	    		    // RGB24 format, which works perfect for PPM output.
	    			int target_width=320;
	    			int target_height=240;
	    			img_convert_ctx = sws_getContext(codecCtx->width,codecCtx->height,codecCtx->pix_fmt,
	    					target_width, target_height, PIX_FMT_RGB24, SWS_BICUBIC, NULL, NULL, NULL);
	    			if(img_convert_ctx==NULL){
	    				LOGE("could not initialize conversion context\n");
	    				return;
	    			}
	    			sws_scale(img_convert_ctx, (const uint8_t* const*)pFrame->data,pFrame->linesize,
	    					0, codecCtx->height,pFrameRGB->data,pFrameRGB->linesize);
	    			fill_bitmap(&info, pixels, pFrameRGB);
	    			i=1;
	    		}
	    	}
	        av_free_packet(&packet);
	    }
	    AndroidBitmap_unlockPixels(env, bitmap);
}
/**
 * 获取第一帧
 */
void Java_com_timeslily_videoplayer_FFmpeg_getFirstFrame(JNIEnv *env, jobject this, jstring bitmap, jstring filePath){
	openVideo(env,filePath);
	getBitmap(env, bitmap,1,0);
}

/**
 * 获取视频时长
 */
jstring Java_com_timeslily_videoplayer_FFmpeg_getDuration(JNIEnv *env, jobject this, jstring filePath){
	int hours, mins, secs;//, us;
	char time[10];
	videoFileName = (char *)(*env)->GetStringUTFChars(env, filePath, NULL);
	LOGI("video file name is %s", videoFileName);
	if(avformat_open_input(&formatCtx, videoFileName, NULL, NULL)!=0){//读取文件头信息，不会填充流信息
		LOGE("Couldn't open file") // Couldn't open file
        return (*env)->NewStringUTF(env,"");
	}
	if(avformat_find_stream_info(formatCtx, NULL)<0){//读取流信息
		LOGE("Couldn't read file");
        return (*env)->NewStringUTF(env,"");
	}else{
		secs = formatCtx->duration / AV_TIME_BASE;
//		us = formatCtx->duration % AV_TIME_BASE;
		mins = secs / 60;
		secs %= 60;
		hours = mins / 60;
		mins %= 60;
		LOGI("%02d:%02d:%02d", hours, mins, secs);//,(10 * us) / AV_TIME_BASE);
		sprintf(time,"%02d:%02d:%02d", hours, mins, secs);
	}
	avformat_close_input(&formatCtx);
	return (*env)->NewStringUTF(env, time);
}
/*
 * 获取指定时间处的图像
 */
void Java_com_timeslily_videoplayer_FFmpeg_getFrameAt(JNIEnv *env, jobject this, jstring bitmap, jint second, jstring filePath){
	openVideo(env,filePath);
	getBitmap(env, bitmap,0,second);
}
jint JNI_OnLoad1(JavaVM* pVm, void* reserved){
	JNIEnv* env;
	if ((*pVm)->GetEnv(pVm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
		return -1;
	}
	JNINativeMethod nm[14];
	nm[0].name="avRegisterAll";
	nm[0].signature="()V";
	nm[0].fnPtr=(void*)avRegisterAll;

	nm[1].name="avFormatOpenInput";
	nm[1].signature="(Ljava/lang/String;)Z";
	nm[1].fnPtr=(void*)avFormatOpenInput;

	nm[2].name="avFormatFindStreamInfo";
	nm[2].signature="()Z";
	nm[2].fnPtr=(void*)avFormatFindStreamInfo;

	nm[3].name="avFormatFindVideoStream";
	nm[3].signature="()Z";
	nm[3].fnPtr=(void*)avFormatFindVideoStream;

	nm[4].name="avCodecFindDecoder";
	nm[4].signature="()Z";
	nm[4].fnPtr=(void*)avCodecFindDecoder;

	nm[5].name="avCodecOpen2";
	nm[5].signature="()Z";
	nm[5].fnPtr=(void*)avCodecOpen2;

	nm[6].name="getCodecName";
	nm[6].signature="()Ljava/lang/String;";
	nm[6].fnPtr=(void*)getCodecName;

	nm[7].name="setVideoScreenSize";
	nm[7].signature="(II)V";
	nm[7].fnPtr=(void*)setVideoScreenSize;

	nm[8].name="allocateBuffer";
	nm[8].signature="()Z";
	nm[8].fnPtr=(void*)allocateBuffer;

	nm[9].name="getNextDecodedFrame";
	nm[9].signature="()[B";
	nm[9].fnPtr=(void*)getNextDecodedFrame;

	nm[10].name = "getWidth";
	nm[10].signature="()I";
	nm[10].fnPtr=(void*)getWidth;

	nm[11].name = "getHeight";
	nm[11].signature="()I";
	nm[11].fnPtr=(void*)getHeight;

	nm[12].name = "stopVideo";
	nm[12].signature="()V";
	nm[12].fnPtr=(void*)stopVideo;

	nm[13].name = "avFormatCloseInput";
	nm[13].signature="()V";
	nm[13].fnPtr=(void*)avFormatCloseInput;

	jclass cls = (*env)->FindClass(env, "com/timeslily/videoplayer/FFmpeg");
	//Register methods with env->RegisterNatives.
	(*env)->RegisterNatives(env, cls, nm, 14);
	return JNI_VERSION_1_6;
}
