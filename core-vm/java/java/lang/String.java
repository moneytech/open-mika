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

/*
** $Id: String.java,v 1.6 2006/04/20 07:28:42 cvs Exp $
*/

package java.lang;

import java.io.UnsupportedEncodingException;
import java.util.Comparator;
import java.util.Locale;

import wonka.decoders.Decoder;

/** Represents an immutable sequence of Unicode characters.
 */
public final class String implements java.io.Serializable,Comparable, CharSequence {

  private static final long serialVersionUID = -6849794470754667710L;

  public static final Comparator CASE_INSENSITIVE_ORDER = new CIComparator();

  /*
  ** Note: this class is initialized "by hand" before the VM is fully
  ** initialized.  Consequently it must not have a static initializer.
  ** (It can have static variables, and even constant initial values
  ** for those variables, but nothing fancier and certainly no static{}
  ** clause.)
  */
  
  /** Create a String identical to another String.
   */
  private native void create_String(String s);

  /** Create a String from the current contents of a StringBuffer.
   */
  private native void create_StringBuffer(StringBuffer b);

  private native void create_char(char[] value, int offset, int count);
  private native void create_byte(byte[] value, int hibyte, int offset, int count);

  public String() {
    this("");
  }

  public String(String value) throws NullPointerException {
    create_String(value);
  }

  public String(StringBuffer value) throws NullPointerException {
    create_StringBuffer(value);
  }

  public String(byte[] value) throws NullPointerException {
    this(value, 0, 0, value.length);
  }

  // DEPRECATED
  public String(byte[] value, int hibyte) throws NullPointerException {
    this(value, hibyte, 0, value.length);
  }

  // DEPRECATED
  public String(byte[] value, int hibyte, int offset, int count) throws NullPointerException, ArrayIndexOutOfBoundsException {
    create_byte(value,hibyte,offset,count);
  }

  public String(byte[] value, int offset, int count) throws NullPointerException, ArrayIndexOutOfBoundsException {
    this(value, 0, offset, count);
  }

  public String(char[] value) throws NullPointerException {
    this(value, 0, value.length);
  }

  public String(byte[] value,int offset, int length, String str) throws UnsupportedEncodingException {
    char[] chars = Decoder.get(str).bToC(value,offset,length);
    create_char(chars, 0,chars.length);
  }

  public String(byte[] value, String str) throws UnsupportedEncodingException {
    this(value, 0, value.length, str);
  }

  public String(char[] value, int offset, int count) throws NullPointerException, ArrayIndexOutOfBoundsException {
    create_char(value, offset,count);
  }

  public String toString() {
    return this;
  }
// adding new Methods implemeneted since jdk 1.2
  public int compareTo(Object obj) throws ClassCastException {
  	return this.compareTo((String)obj);
  }
  public int compareToIgnoreCase(String str) {
   return this.toUpperCase().toLowerCase().compareTo(str.toUpperCase().toLowerCase());
  }

  public native boolean equals(Object obj);

  public native boolean equalsIgnoreCase(String otherString);

  public native int hashCode();

  public native int length();

  public native char charAt(int index) throws StringIndexOutOfBoundsException;

  native public void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin) throws NullPointerException, IndexOutOfBoundsException;

/**
** if utf8 is true it will return utf8 bytes
** else it will return iso-latin bytes
*/
  native private byte[] getNativeBytes(boolean utf8);

  public byte[] getBytes(String enc) throws UnsupportedEncodingException {
    Decoder decoder = Decoder.get(enc);
    if(decoder == Decoder.DEFAULT){
      return getNativeBytes(false);
    }
    char[] chars = toCharArray();
    return decoder.cToB(chars,0,chars.length);
  }

  public byte[] getBytes() {
    Decoder decoder = Decoder.getDefault(GetSystemProperty.FILE_ENCODING);
    /**
    ** if we have the default Decoder we go don't use the it but go straight native.
    */
    if(decoder == Decoder.DEFAULT){
      return getNativeBytes(false);
    }
    char[] chars = toCharArray();
    return decoder.cToB(chars,0,chars.length);
  }

  /**
  ** @deprecated
  */
  public void getBytes(int srcBegin, int srcEnd, byte dst[], int dstBegin)
    throws NullPointerException, ArrayIndexOutOfBoundsException {
    byte[] bytes = getNativeBytes(false);
    System.arraycopy(bytes, srcBegin, dst, dstBegin, srcEnd - srcBegin);
  }

  native public char[] toCharArray();

  public native int compareTo(String anotherString) throws NullPointerException;
  

  native public boolean startsWith(String prefix, int offset) throws NullPointerException;

  public boolean startsWith(String prefix) {
    return startsWith(prefix, 0);
  }

  native public boolean endsWith(String suffix) throws NullPointerException;

  public native String trim();

  public synchronized native String intern();

  public static String valueOf(Object obj) {
    if (obj == null) {
      return "null";
    }
    return obj.toString();
  }

  public static String valueOf(char[] data) throws NullPointerException {
    return new String(data);
  }

  public static String valueOf(char[] data, int offset, int count) throws NullPointerException {
    return new String(data, offset, count);
  }

  public static String valueOf(boolean b) {
    if (b) {
      return new String("true");
    }
    return new String("false");
  }

  native public static String valueOf(char c);
  
  public static String valueOf(int i) {
    return Integer.toString(i);
  }

  public static String valueOf(float f) {
    return Float.toString(f);
  }

  public static String valueOf(long l) {
    return Long.toString(l);
  }

  public static String valueOf(double d) {
    return Double.toString(d);
  }

  public static String copyValueOf(char[] chars, int offset, int length) {
    return new String(chars,offset,length);
  }

  public static String copyValueOf(char[] chars) {
    return new String(chars,0,chars.length);
  }

  native public int indexOf(int ch, int offset);

  public int indexOf(int ch) {
    return indexOf(ch, 0);
  }

  native public int indexOf(String str, int fromIndex) throws NullPointerException;

  public int indexOf(String str) {
    return indexOf(str, 0);
  }

  native public int lastIndexOf(int ch, int offset);

  public int lastIndexOf(int ch) {
    return lastIndexOf(ch, this.length());
  }

  native public int lastIndexOf(String str, int offset) throws NullPointerException;

  public int lastIndexOf(String str) {
    return lastIndexOf(str, this.length());
  }

  public native String substring(int offset, int endIndex) throws IndexOutOfBoundsException;

  public String substring(int offset) {
    return this.substring(offset, this.length());
  }

  native public String concat(String str) throws NullPointerException;

  native public String replace(char oldChar, char newChar);

  native public String toLowerCase(Locale locale);
  
  public String toLowerCase() {
    return this.toLowerCase(Locale.getDefault());
  }
  
  native public String toUpperCase(Locale locale);
  
  public String toUpperCase() {
    return this.toUpperCase(Locale.getDefault());
  }

  native public boolean regionMatches(boolean ignoreCase, int toffset, String other, int ooffset, int len) throws NullPointerException;
  
  public boolean regionMatches(int toffset, String other, int ooffset, int len) throws NullPointerException {
    return regionMatches(false, toffset, other, ooffset, len);
  }

  static final class CIComparator implements Comparator {

    public boolean equals(Object o){
      return (o instanceof CIComparator);
    }

    public int compare(Object f, Object s){
      return ((String)f).compareToIgnoreCase((String)s);
    }
  }
  
  public boolean contentEquals(StringBuffer buf) {   
    return  this.equals(buf.toString());
  }

  public CharSequence subSequence(int start, int end) {    
    return this.substring(start, end);
  }
}
