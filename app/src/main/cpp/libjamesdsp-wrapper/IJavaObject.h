
#ifndef ROOTLESSJAMESDSP_IJAVAOBJECT_H
#define ROOTLESSJAMESDSP_IJAVAOBJECT_H

#include <jni.h>

class IJavaObject {
public:
    IJavaObject(JNIEnv* env) : _env(env) {};
    virtual bool isValid() const = 0;
    virtual jobject getJavaReference() = 0;

protected:
    JNIEnv* _env;
};

#endif
