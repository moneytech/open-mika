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
** $Id: BasicPermission.java,v 1.3 2006/02/23 12:32:12 cvs Exp $
*/

package java.security;

import java.io.Serializable;

public abstract class BasicPermission extends Permission implements Serializable {

  public BasicPermission(String name) {
    super(name);
    if (name.equals("")) {
      throw new IllegalArgumentException();
    }
  }

  public BasicPermission(String name, String actions) {
    this(name);
  }

  public boolean equals (Object o) {
    if (o == null)  {
    	return false;
    }
    if (this.getClass().equals(o.getClass())) {
      Permission p = (Permission)o;
      return this.getName().equals(p.getName());
    }
    else return false;
  }

  public int hashCode() {
    return getName().hashCode();
  }

  public String getActions() {
    return "";
  }

  public PermissionCollection newPermissionCollection() {
    return new wonka.security.BasicPermissionCollection();
  }

  public boolean implies (Permission p) {
    if (this.getClass().equals(p.getClass())) {
      String ourname = this.getName();
      if (ourname.equals("*")) {
         return true;
      }
      if (ourname.endsWith(".*")) {
        String name = p.getName();
        return name.length() >= ourname.length() && 
        name.startsWith(ourname.substring(0, ourname.length() - 1));

       }
       return ourname.equals(p.getName());
    }
    return false;
  }
}
