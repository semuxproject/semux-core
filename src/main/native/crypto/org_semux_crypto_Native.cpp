#include "org_semux_crypto_Native.h"
#include <sodium.h>

JNIEXPORT jbyteArray JNICALL Java_org_semux_crypto_Native_blake2b
(JNIEnv *env, jclass cls, jbyteArray msg)
{
    // basic checks
    if (msg == NULL) return NULL;

    // compute blake2b hash
    jsize msg_size = env->GetArrayLength(msg);
    jbyte *msg_ptr = env->GetByteArrayElements(msg, NULL);
    unsigned char hash[crypto_generichash_blake2b_BYTES];
    crypto_generichash_blake2b(hash, sizeof(hash), (const unsigned char *)msg_ptr, msg_size, NULL, 0);
    env->ReleaseByteArrayElements(msg, msg_ptr, 0);

    jbyteArray result = env->NewByteArray(sizeof(hash));
    env->SetByteArrayRegion(result, 0, sizeof(hash), (const jbyte*)hash);
    return result;
}

JNIEXPORT jbyteArray JNICALL Java_org_semux_crypto_Native_ed25519_1sign
(JNIEnv *env, jclass cls, jbyteArray msg, jbyteArray sk)
{
    // basic checks
    if (msg == NULL || sk == NULL || env->GetArrayLength(sk) != crypto_sign_ed25519_SECRETKEYBYTES) {
        return NULL;
    }

    // create ed25519 signature
    jsize msg_size = env->GetArrayLength(msg);
    jbyte *msg_ptr = env->GetByteArrayElements(msg, NULL);
    jbyte *sk_ptr = env->GetByteArrayElements(sk, NULL);
    unsigned char sig[crypto_sign_ed25519_BYTES];
    crypto_sign_ed25519_detached(sig, NULL, (const unsigned char *)msg_ptr, msg_size,
        (const unsigned char *)sk_ptr);
    env->ReleaseByteArrayElements(msg, sk_ptr, 0);
    env->ReleaseByteArrayElements(msg, msg_ptr, 0);

    jbyteArray result = env->NewByteArray(sizeof(sig));
    env->SetByteArrayRegion(result, 0, sizeof(sig), (const jbyte*)sig);
    return result;
}

JNIEXPORT jboolean JNICALL Java_org_semux_crypto_Native_ed25519_1verify
(JNIEnv *env, jclass cls, jbyteArray msg, jbyteArray sig, jbyteArray pk)
{
    // basic checks
    if (msg == NULL || sig == NULL || env->GetArrayLength(sig) != crypto_sign_ed25519_BYTES
        || pk == NULL || env->GetArrayLength(pk) != crypto_sign_ed25519_PUBLICKEYBYTES) {
        return false;
    }

    // verify ed25519 signature
    jsize msg_size = env->GetArrayLength(msg);
    jbyte *msg_ptr = env->GetByteArrayElements(msg, NULL);
    jbyte *sig_ptr = env->GetByteArrayElements(sig, NULL);
    jbyte *pk_ptr = env->GetByteArrayElements(pk, NULL);
    jboolean result = crypto_sign_ed25519_verify_detached((const unsigned char *)sig_ptr,
        (const unsigned char *)msg_ptr, msg_size, (const unsigned char *)pk_ptr) == 0;
    env->ReleaseByteArrayElements(msg, pk_ptr, 0);
    env->ReleaseByteArrayElements(msg, sig_ptr, 0);
    env->ReleaseByteArrayElements(msg, msg_ptr, 0);

    return result;
}