#ifndef HELPERS_H_
#define HELPERS_H_

typedef struct{
	const char* name;
	const char* signature;
} JavaMethod;

typedef struct{
	char* name;
	char* signature;
} JavaField;

jfieldID java_get_field(JNIEnv *env, char * class_name, JavaField field);
jmethodID java_get_method(JNIEnv *env, jclass class, JavaMethod method);

#endif
