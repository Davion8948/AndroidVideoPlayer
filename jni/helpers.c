#include <jni.h>
#include "helpers.h"

jfieldID java_get_field(JNIEnv *env, char * class_name, JavaField field){
	jclass clazz = (*env)->FindClass(env, class_name);
	jfieldID jField = (*env)->GetFieldID(env, clazz, field.name, field.signature);
	(*env)->DeleteLocalRef(env, clazz);
	return jField;
}
jmethodID java_get_method(JNIEnv *env, jclass class, JavaMethod method){
	return (*env)->GetMethodID(env, class, method.name, method.signature);
}
