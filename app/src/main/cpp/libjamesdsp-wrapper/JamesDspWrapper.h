#ifndef DSPHOST_H
#define DSPHOST_H

#include <jni.h>

typedef struct
{
    void* dsp;
    JavaVM* jvm;
    jobject callbackInterface;
    jmethodID callbackOnLiveprogOutput;
    jmethodID callbackOnLiveprogExec;
    jmethodID callbackOnLiveprogResult;
    jmethodID callbackOnVdcParseError;
} JamesDspWrapper;

static void receiveLiveprogStdOut(const char* buffer, void* userData);

#endif

