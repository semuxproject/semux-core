#include "org_semux_crypto_Native.h"
#include <sodium.h>

JNIEXPORT jbyteArray JNICALL Java_org_semux_crypto_Native_blake2b
(JNIEnv *env, jclass cls, jbyteArray msg)
{
    if (msg == NULL) {
        return env->NewByteArray(0);
    }

    // compute blake2b hash
    jsize in_size = env->GetArrayLength(msg);
    jbyte *in = env->GetByteArrayElements(msg, NULL);
    unsigned char hash[32];
    crypto_generichash_blake2b(hash, 32, (const unsigned char *) in, in_size, NULL, 0);
    env->ReleaseByteArrayElements(msg, in, 0);

    // return results
    jbyteArray result = env->NewByteArray(32);
    env->SetByteArrayRegion(result, 0, 32, (const jbyte*) hash);
    return result;
}

JNIEXPORT jbyteArray JNICALL Java_org_semux_crypto_Native_ed25519_1sign
(JNIEnv *env, jclass, jbyteArray, jbyteArray)
{
    return NULL;
}

JNIEXPORT jboolean JNICALL Java_org_semux_crypto_Native_ed25519_1verify
(JNIEnv *, jclass, jbyteArray, jbyteArray)
{
    return NULL;
}