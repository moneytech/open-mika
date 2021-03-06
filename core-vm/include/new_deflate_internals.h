#ifndef N_DEFLATE_INTERNALS_H
#define N_DEFLATE_INTERNALS_H
/**************************************************************************
* Copyright (c) 2001 by Punch Telematix. All rights reserved.             *
*                                                                         *
* Redistribution and use in source and binary forms, with or without      *
* modification, are permitted provided that the following conditions      *
* are met:                                                                *
* 1. Redistributions of source code must retain the above copyright       *
*    notice, this list of conditions and the following disclaimer.        *
* 2. Redistributions in binary form must reproduce the above copyright    *
*    notice, this list of conditions and the following disclaimer in the  *
*    documentation and/or other materials provided with the distribution. *
* 3. Neither the name of Punch Telematix nor the names of                 *
*    other contributors may be used to endorse or promote products        *
*    derived from this software without specific prior written permission.*
*                                                                         *
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESS OR IMPLIED          *
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF    *
* MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.    *
* IN NO EVENT SHALL PUNCH TELEMATIX OR OTHER CONTRIBUTORS BE LIABLE       *
* FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR            *
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF    *
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR         *
* BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,   *
* WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE    *
* OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN  *
* IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                           *
**************************************************************************/

#include <string.h>

#include "dates.h"
#include "ts-mem.h"
#include "list.h"

typedef w_size                             w_bits;
  
typedef struct w_Zip_Hash *w_zip_hash;
typedef struct w_Zip_Hash {
  w_short next;
} w_Zip_Hash;

typedef struct w_Deflate_QueueElem {
  w_int size;                                   // size of data element
  w_int index;                                   // size of data element
  w_ubyte *data;                                // data block
  w_int release;
  w_void* next;
} w_Deflate_QueueElem;
typedef w_Deflate_QueueElem *w_deflate_queueelem;

typedef struct w_Inflate_Control {
  w_deflate_queueelem last_link_in;
  w_deflate_queueelem first_link_in;
  w_deflate_queueelem last_link_out;
  w_deflate_queueelem first_link_out;

  w_deflate_queueelem par_in, par_out;          // partial

  w_int offset_in, offset_out;                  // read offset in data block for partial reads
  w_bits i_bits;                                // input op bitniveau
  w_bits o_bits, o_mask;                        // output op bitniveau

  w_ubyte output_bekken[32 * 1024 + 1024];      // 32k sliding window en 1024 bytes block voor in queue te steken
  w_int offset_bek_out, size_bek_out;

  w_ubyte input_bekken[32 * 1024 + 512];        // 32k sliding window met 512 bytes lookahead gebruikt door 
                                                // de zipper
  w_int offset_bek_in, lookahead_bek_in;

  w_int nomoreinput;
  //w_short *zip_hash_table;                      // the actual hashtable
  w_zip_hash zip_hash_blocks;                   // a block of allocated hash entries
  w_int zip_hash_first;                         // index to first available hash entry in block

  w_int compression_level;
  w_int no_auto;
  w_int need_more_input;
  w_int processed_size;
  w_int finished;

  volatile w_int stop;
  volatile w_short resets_requested;
  volatile w_short resets_completed;

  w_int dictionary;
} w_Inflate_Control;
typedef w_Inflate_Control *w_inflate_control;

#define END_BLOCK                                256     /* The end of a block code                                                 */
#define LENGTH_CODES                              29     /* The number of length codes, not taking into account the END_BLOCK code. */
#define LITERALS                                 256     /* The number of literal bytes 0 .. 255                                    */
#define D_CODES                                   30     /* The number of distance codes.                                           */
#define MAX_BITS                                  15     /* No code can ever exceed this number of bits.                            */
#define L_CODES                                  (LITERALS + 1 + LENGTH_CODES)
#define BUF_SIZE 16

#define WINF_KEEP_DATA       0x000
#define WINF_CLONE_DATA      0x010
#define WINF_DELETE_DATA     0x100

/*
** A node of a Huffman tree.
*/

typedef struct w_Hnode *                   w_hnode;
typedef struct w_Ztable *                  w_ztable;
typedef struct w_Zdict *                   w_zdict;
typedef struct z_Extra *                   z_extra;

typedef struct z_Extra {
  w_ubyte bits;             /* The extra bits that need to be read in from the stream.  */
  w_short base;             /* The base value against which the extra bits are 'added'. */
} z_Extra;

typedef struct w_Hnode {
  w_short symbol;
  w_short code;
  w_ubyte length;
} w_Hnode;

typedef struct w_Ztable {
  w_ztable previous;
  w_ztable next;
  w_size length;
  w_size offset;
  w_size pivot;
  w_Hnode *table;
} w_Ztable;

typedef struct w_Zdict {
  w_ztable lengths_literals;
  w_ztable distances;
} w_Zdict;

extern const w_byte new_bit_order[];
extern const z_Extra new_length_table[];
extern const z_Extra new_distance_table[];
extern const unsigned short new_bitReverse[];

w_void new_getNewBlock(w_inflate_control l);
w_byte new_readLiteralByte(w_inflate_control bs);
void new_readByteAlign(w_inflate_control bs);

void new_errorFlush(w_inflate_control bs);
void new_bekkenFlush(w_inflate_control bs);
void new_bekkenSendBlock(w_inflate_control bs);

void new_writeByteAlign(w_inflate_control bs);

w_inflate_control allocInflateControl(void);
void releaseInflateControl(w_inflate_control control);
void inflate_control_setInput(w_inflate_control control, w_ubyte* bytes, w_int size, int clonedata);
int inflate_control_inflate(w_inflate_control control);
int inflate_control_getbytes_from_queue(w_inflate_control control, w_ubyte* bytes, w_int size);

// only on aligned stream !!!
static w_int new_writeLiteralByte(w_inflate_control bs, w_word obyte) {
  obyte = (w_byte)(obyte & 0x000000ff);
  
  (*bs).output_bekken[bs->offset_bek_out] = (w_byte)obyte;
  bs->offset_bek_out += 1;
  bs->size_bek_out += 1;

  if (bs->offset_bek_out >= 33*1024) {
    bs->offset_bek_out = 0;
  }
  if (bs->size_bek_out >= 33*1024) {
    new_bekkenSendBlock(bs);
  }

  return 0;  
}

#endif
