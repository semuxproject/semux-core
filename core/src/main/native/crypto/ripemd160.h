#ifndef RIPEMD160_H_
#define RIPEMD160_H_

#include <stdint.h>

#ifdef __cplusplus
extern "C"
{
#endif

typedef struct {
  uint64_t length;
  union {
    uint32_t w[16];
    uint8_t  b[64];
  } buf;
  uint32_t h[5];
  uint8_t bufpos;
} ripemd160_state;

void ripemd160_init(ripemd160_state *md);
void ripemd160_process(ripemd160_state *md, const unsigned char *in, unsigned long inlen);
void ripemd160_done(ripemd160_state *md, unsigned char *out);
void ripemd160(const void *in, unsigned long length, void *out);

#ifdef __cplusplus
}
#endif

#endif