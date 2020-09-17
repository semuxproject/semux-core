#include "org_semux_crypto_Native.h"
#include "ripemd160.h"
#include <sodium/crypto_generichash_blake2b.h>
#include "ed25519-donna/ed25519.h"

#define ed25519_SECRETKEYBYTES 64U
#define ed25519_PUBLICKEYBYTES 32U
#define ed25519_SIGNATUREBYTES 64U

JNIEXPORT jbyteArray JNICALL Java_org_semux_crypto_Native_h256
(JNIEnv *env, jclass cls, jbyteArray msg, jbyteArray salt)
{
    // check inputs
    if (msg == NULL) {
        env->ThrowNew(env->FindClass("org/semux/crypto/CryptoException"), "Input can't be null");
        return NULL;
    }

	// read message bytes
    jsize msg_size = env->GetArrayLength(msg);
    jbyte *msg_buf = (jbyte *)malloc(msg_size);
    env->GetByteArrayRegion(msg, 0, msg_size, msg_buf);

	// read salt bytes
	jsize salt_size = 0;
	jbyte *salt_buf = NULL;

	if (salt != NULL)
	{
		salt_size = env->GetArrayLength(salt);
		salt_buf = (jbyte *)malloc(salt_size);
		env->GetByteArrayRegion(salt, 0, salt_size, salt_buf);
	}

    // compute blake2b hash
    unsigned char hash[crypto_generichash_blake2b_BYTES];
	crypto_generichash_blake2b(hash, sizeof(hash), (const unsigned char *)msg_buf, msg_size, (const unsigned char *)salt_buf, salt_size);

    // release buffer
    free(msg_buf);

	if (salt_buf != NULL)
	{
		free(salt_buf);
	}

    jbyteArray result = env->NewByteArray(sizeof(hash));
    env->SetByteArrayRegion(result, 0, sizeof(hash), (const jbyte*)hash);
    return result;
}

JNIEXPORT jbyteArray JNICALL Java_org_semux_crypto_Native_h160
(JNIEnv *env, jclass cls, jbyteArray msg)
{
    // check inputs
    if (msg == NULL) {
        env->ThrowNew(env->FindClass("org/semux/crypto/CryptoException"), "Input can't be null");
        return NULL;
    }

	// read message bytes
    jsize msg_size = env->GetArrayLength(msg);
    jbyte *msg_buf = (jbyte *)malloc(msg_size);
    env->GetByteArrayRegion(msg, 0, msg_size, msg_buf);

    // compute blake2b hash
    unsigned char hash[crypto_generichash_blake2b_BYTES];
	crypto_generichash_blake2b(hash, sizeof(hash), (const unsigned char *)msg_buf, msg_size, NULL, 0);

    // compute ripemd160 digest
    unsigned char digest[20];
    ripemd160(hash, sizeof(hash), digest);

    // release buffer
    free(msg_buf);

    jbyteArray result = env->NewByteArray(sizeof(digest));
    env->SetByteArrayRegion(result, 0, sizeof(digest), (const jbyte*)digest);
    return result;
}

JNIEXPORT jbyteArray JNICALL Java_org_semux_crypto_Native_sign
(JNIEnv *env, jclass cls, jbyteArray msg, jbyteArray sk)
{
    // check inputs
    if (msg == NULL || sk == NULL || env->GetArrayLength(sk) != ed25519_SECRETKEYBYTES) {
        env->ThrowNew(env->FindClass("org/semux/crypto/CryptoException"), "Input can't be null");
        return NULL;
    }

    // read byte arrays
    jsize msg_size = env->GetArrayLength(msg);
    jbyte *msg_buf = (jbyte *)malloc(msg_size);
    env->GetByteArrayRegion(msg, 0, msg_size, msg_buf);
    jbyte *sk_buf = (jbyte *)malloc(ed25519_SECRETKEYBYTES);
    env->GetByteArrayRegion(sk, 0, ed25519_SECRETKEYBYTES, sk_buf);
    jbyte *pk_buf = (jbyte *)malloc(ed25519_PUBLICKEYBYTES);
    ed25519_publickey((const unsigned char*)sk_buf, (unsigned char*)pk_buf);

    // compute ed25519 signature
    unsigned char sig[ed25519_SIGNATUREBYTES];
    ed25519_sign((const unsigned char *)msg_buf, msg_size, (const unsigned char *)sk_buf, (const unsigned char *)pk_buf, sig);

    // release buffer
    free(pk_buf);
    free(sk_buf);
    free(msg_buf);

    jbyteArray result = env->NewByteArray(sizeof(sig));
    env->SetByteArrayRegion(result, 0, sizeof(sig), (const jbyte*)sig);
    return result;
}

JNIEXPORT jboolean JNICALL Java_org_semux_crypto_Native_verify
(JNIEnv *env, jclass cls, jbyteArray msg, jbyteArray sig, jbyteArray pk)
{
    // check inputs
    if (msg == NULL || sig == NULL || env->GetArrayLength(sig) != ed25519_SIGNATUREBYTES
        || pk == NULL || env->GetArrayLength(pk) != ed25519_PUBLICKEYBYTES) {
        return false;
    }

    // read byte arrays
    jsize msg_size = env->GetArrayLength(msg);
    jbyte *msg_buf = (jbyte *)malloc(msg_size);
    env->GetByteArrayRegion(msg, 0, msg_size, msg_buf);
    jbyte *sig_buf = (jbyte *)malloc(ed25519_SIGNATUREBYTES);
    env->GetByteArrayRegion(sig, 0, ed25519_SIGNATUREBYTES, sig_buf);
    jbyte *pk_buf = (jbyte *)malloc(ed25519_PUBLICKEYBYTES);
    env->GetByteArrayRegion(pk, 0, ed25519_PUBLICKEYBYTES, pk_buf);

    // verify ed25519 signature
    jboolean result = ed25519_sign_open(
        (const unsigned char *)msg_buf,
        msg_size,
        (const unsigned char *)pk_buf,
        (const unsigned char *)sig_buf) == 0;

    // release buffer
    free(pk_buf);
    free(sig_buf);
    free(msg_buf);

    return result;
}

JNIEXPORT jboolean JNICALL Java_org_semux_crypto_Native_verifyBatch
(JNIEnv *env, jclass cls, jobjectArray msgs, jobjectArray sigs, jobjectArray pks)
{
    // check null inputs
    if (msgs == NULL || sigs == NULL || pks == NULL) {
        return false;
    }

    // check array lengths
    const jsize msgs_size = env->GetArrayLength(msgs);
    const jsize sigs_size = env->GetArrayLength(sigs);
    const jsize pks_size = env->GetArrayLength(pks);
    if (msgs_size != sigs_size || sigs_size != pks_size) {
        env->ThrowNew(env->FindClass("org/semux/crypto/CryptoException"), "Input arrays must have an identical length");
        return false;
    }

    // read byte arrays into c buffers
    jbyte** msg_bufs = new jbyte*[msgs_size];
    size_t* msg_lens = new size_t[msgs_size];
    jbyte** sig_bufs = new jbyte*[sigs_size];
    jbyte** pk_bufs = new jbyte*[pks_size];
    for (int i = 0;i < msgs_size;i++) {
        jbyteArray msg = (jbyteArray) env->GetObjectArrayElement(msgs, i);
        jbyteArray sig = (jbyteArray) env->GetObjectArrayElement(sigs, i);
        jbyteArray pk = (jbyteArray) env->GetObjectArrayElement(pks, i);
        if (msg == NULL || sig == NULL || env->GetArrayLength(sig) != ed25519_SIGNATUREBYTES
            || pk == NULL || env->GetArrayLength(pk) != ed25519_PUBLICKEYBYTES) {
            return false;
        }

        const jsize msg_size = env->GetArrayLength(msg);
        msg_lens[i] = (size_t) msg_size;
        msg_bufs[i] = new jbyte[msg_size];
        env->GetByteArrayRegion(msg, 0, msg_size, msg_bufs[i]);
        sig_bufs[i] = new jbyte[ed25519_SIGNATUREBYTES];
        env->GetByteArrayRegion(sig, 0, ed25519_SIGNATUREBYTES, sig_bufs[i]);
        pk_bufs[i] = new jbyte[ed25519_PUBLICKEYBYTES];
        env->GetByteArrayRegion(pk, 0, ed25519_PUBLICKEYBYTES, pk_bufs[i]);
    }

    // verify ed25519 signature
    int *valid = new int[msgs_size];
    jboolean result = ed25519_sign_open_batch((const unsigned char **)msg_bufs, msg_lens, (const unsigned char **)pk_bufs, (const unsigned char **)sig_bufs, msgs_size, valid) == 0;

    // release buffers
    for (int i = 0;i < msgs_size;i++) {
        delete[] msg_bufs[i];
        delete[] sig_bufs[i];
        delete[] pk_bufs[i];
    }
    delete[] msg_bufs;
    delete[] msg_lens;
    delete[] sig_bufs;
    delete[] pk_bufs;
    delete[] valid;

    return result;
}
