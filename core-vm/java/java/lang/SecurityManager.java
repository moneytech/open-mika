/**************************************************************************
* Parts copyright (c) 2001 by Punch Telematix. All rights reserved.       *
* Parts copyright (c) 2009 by /k/ Embedded Java Solutions.                *
* All rights reserved.                                                    *
*                                                                         *
* Redistribution and use in source and binary forms, with or without      *
* modification, are permitted provided that the following conditions      *
* are met:                                                                *
* 1. Redistributions of source code must retain the above copyright       *
*    notice, this list of conditions and the following disclaimer.        *
* 2. Redistributions in binary form must reproduce the above copyright    *
*    notice, this list of conditions and the following disclaimer in the  *
*    documentation and/or other materials provided with the distribution. *
* 3. Neither the name of Punch Telematix or of /k/ Embedded Java Solutions*
*    nor the names of other contributors may be used to endorse or promote*
*    products derived from this software without specific prior written   *
*    permission.                                                          *
*                                                                         *
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESS OR IMPLIED          *
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF    *
* MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.    *
* IN NO EVENT SHALL PUNCH TELEMATIX, /K/ EMBEDDED JAVA SOLUTIONS OR OTHER *
* CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,   *
* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,     *
* PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR      *
* PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF  *
* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING    *
* NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS      *
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.            *
**************************************************************************/

package java.lang;

import java.io.FileDescriptor; 
import java.io.FilePermission; 
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.SocketPermission;
import java.security.AccessControlContext;
import java.security.Permission;
import java.security.SecurityPermission;
import java.util.PropertyPermission;

/** The default SecurityManager for Mika.
 ** If USE_SECURITY_MANAGER is set in wonka.vm.SecurityConfiguration
 ** then this is a real security manager which uses the AccessController
 ** to implement all checks: otherwise it is a null security manager which
 ** allows everyone to do everything.
 */
public class SecurityManager {

  /** Field inCheck is set true iff a security check is in progress
   ** AND we call another method of SecurityManager (which might 
   ** have been overridden).  Otherwise we don't bother.  
   ** Note that use of this variable is deprecated.
   */
  protected boolean inCheck = false;

  /**
   ** Tnis boolean is set when permission to create a class loader is granted.
   ** Until then we can be sure that no classes created by such a class loader
   ** can be on the stack, and therefore we can skip some checks (and avoid
   ** some nasty recursion).
   */  
  private boolean haveUserDefinedClassLoaders;

  /** This is what one might call ``a workaround''.
  ** The problem is that we may not have an AWT, and in that case
  ** there will be no class java.awt.AWTPermission.  So in our
  ** static initializer we try to load that class, and if the attempt
  ** fails then all calls to checkPrintJobAccess, checkSystemClipboardAccess,
  ** checkAwtEventQueueAccess, and checkTopLevelWindow will fail.
  */
  static private Class awtPermission;

  /** The constructor of awtPermission which takes a single string as argument.
   */
  static private Constructor awtpCon;

  static {
    try {
      awtPermission = Class.forName("java.awt.AWTPermission");
      Class[] params = new Class[1];
      params[0] = String.class;
      awtpCon = awtPermission.getConstructor(params);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  /** Default constructor, public in Java2.
   ** Performs a security check iff USE_ACCESS_CONTROLLER or USE_SECURITY_MANAGER
   ** is set in wonka.vm.SecurityConfiguration.
   */
  public SecurityManager()
    throws SecurityException
  {
    if (wonka.vm.SecurityConfiguration.ENABLE_SECURITY_CHECKS) {
      java.security.AccessController.checkPermission(new RuntimePermission("createSecurityManager"));
    }
  }

  /** A wrapper for AccessController.checkPermission(perm), or a no-op if
   ** USE_SECURITY_MANAGER is not set in wonka.vm.SecurityConfiguration.
   */
  public void checkPermission(Permission perm) {
    if (wonka.vm.SecurityConfiguration.ENABLE_SECURITY_CHECKS) {
      try {
        inCheck = true;
        java.security.AccessController.getContext().checkPermission(perm);
      } finally {
        inCheck = false;
      }
    }
  }

  /** A wrapper for AccessController.checkPermission(perm).
   */
  public void checkPermission(Permission perm, Object context) {
    if (wonka.vm.SecurityConfiguration.ENABLE_SECURITY_CHECKS) {
      try {
        inCheck = true;
        java.security.AccessControlContext acc = (AccessControlContext)context;
        acc.checkPermission(perm);
      } catch (ClassCastException cce) {
        throw new SecurityException("Not an AccessControlContext: "+context);
      } finally {
        inCheck = false;
      }
    }
  }

  /** Get the security context (i.e., AccessControlContext) currently applicable.
   */
  public Object getSecurityContext() {
    return java.security.AccessController.getContext();
  }

  /** Get the thread group in which new threads should be instantiated.
   ** Used by the constructors of java.lang.Thread which do not specifiy the ThreadGroup.
   ** This implementation just returns the ThreadGroup of the current thread.
   ** Override this if you feel that new threads should be instantiated somewhere else (e.g. in a Realm ...).
   */
  public ThreadGroup getThreadGroup() {
    return Thread.currentThread().getThreadGroup();
  }
 
  /** The Class[] returned contains one element for every method on the stack, starting with the method which called  getClassContext().
   ** Provided for use by user-defined SecurityManager's, we don't use this internally.
   ** Looks at all frames, not just up to the most recent doPrivileged.
   ** Includes native methods, including invoke().
   */
  protected native Class[] getClassContext();
  
  /** The Class[] returned contains one element for every method on the stack, starting with the method which called  getClassContext(), up to the most recent privileged frame.
   ** Includes native methods, including invoke().
   */
  private native Class[] getNonPrivilegedClassContext();
  
  /** Gets the stack depth of the most recent call to a method of the given class.
   ** (But does it include this call? Who cares...).
   ** Returns -1 if not found.
   ** Looks at all frames, not just up to the most recent doPrivileged.
   ** Deprecated.
   */
  protected int classDepth(String name) {
    System.err.println("SecurityManager method classDepth() is deprecated!");
    Class [] classes = getClassContext();
    try {
      Class cl = Class.forName(name);

      for (int i = 0; i < classes.length; ++i) {
        if (classes[i] == cl) {

          return i;

        }
      }
    } catch (ClassNotFoundException cnfe) {
      // Fall through, return -1
    }

    return -1;
  }
  
  /** Get the class loader of the topmost class on the stack that was not loaded by the system class loader or bootstrap class loader.
   ** Only looks at frames up to the most recent doPrivileged, but does include native methods, including invoke().
   ** Always returns null if caller has AllPermission.
   ** Deprecated.
   */
  protected ClassLoader currentClassLoader() {
    Class clc = currentLoadedClass();

    return clc == null ? null : clc.loader;
  }
  
  /** Get the topmost class on the stack that was not loaded by the system class loader or bootstrap class loader.
   ** Only looks at frames up to the most recent doPrivileged, but does include native methods, including invoke().
   ** Always returns null if caller has AllPermission.
   ** Deprecated.
   */
  protected Class currentLoadedClass() {
    if (!haveUserDefinedClassLoaders) {

      return null;

    }

    try {
      checkPermission(new java.security.AllPermission());

      return null;
    }
    catch (SecurityException se) {
    }

    Class [] classes = getNonPrivilegedClassContext();
    for (int i = 0; i < classes.length; ++i) {
      ClassLoader cl = classes[i].loader;
      if (!ClassLoader.isSystemClassLoader(cl)) {

        return classes[i];

      }
    }

    return null;
  }
  
  /** Get the depth on the stack of the topmost class that was not loaded by the system class loader or bootstrap class loader.
   ** Only looks at frames up to the most recent doPrivileged, but does include native methods, including invoke().
   ** Always returns null if caller has AllPermission.
   ** Deprecated.
   */
  protected int classLoaderDepth() {
    if (currentLoadedClass() == null) {

      return -1;

    }

    try {
      checkPermission(new java.security.AllPermission());

      return -1;
    }
    catch (SecurityException se) {
    }

    Class [] classes = getNonPrivilegedClassContext();
    for (int i = 0; i < classes.length; ++i) {
      ClassLoader cl = classes[i].loader;
      if (!ClassLoader.isSystemClassLoader(cl)) {

        return i;

      }
    }

    return -1;
  }
  
  /** See whether the given class is in the stack.
   ** Deprecated.
   */
  protected boolean inClass(String name) {
    return classDepth(name) >= 0;
  }

  /** See whether any non-system class is in the stack.
   ** See currentClassLoader().
   ** Deprecated.
   */
  protected boolean inClassLoader() {
    return currentClassLoader() != null;
  }
  
  /** Get the value of the inCheck field.  Deprecated.
   */
  public synchronized boolean getInCheck()
  {
    return inCheck;
  }
  
  /** Checks whether the caller has permission to create a ClassLoader.
   ** If you override this, you should  include a call to super.checkCreateClassLoader().
   */
  public  void checkCreateClassLoader()
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      checkPermission(new RuntimePermission("createClassLoader"));
    }
    // If we passed the check then user-defined class loaders may be created
    haveUserDefinedClassLoaders = true;
  }
    
  /** Checks whether the caller has permission to modify Thread t.
   ** The check is only applied if t belongs to the system ThreadGroup.
   ** If you override this, you should  include a call to super.checkAccess().
   ** (You should probably also allow access to any thread which has
   ** RuntimePermission("modifyThread"), so that system code can still create threads).
   */
  public void checkAccess(Thread t)
    throws SecurityException
  {
    if (t.getThreadGroup().getParent() != null) {

      return;

    }

    if (haveUserDefinedClassLoaders) {
      checkPermission(new RuntimePermission("modifyThread"));
    }
  }

  /** Checks whether the caller has permission to modify ThreadGroup g.
   ** The check is only applied if t belongs to the system ThreadGroup.
   ** If you override this, you should  include a call to super.checkAccess().
   ** (You should probably also allow access to any thread which has
   ** RuntimePermission("modifyThreadGroup"), so that system code can still create threads).
   */
  public void checkAccess(ThreadGroup g)
    throws SecurityException
  {
    if (g.getParent() != null) {

      return;

    }

    if (haveUserDefinedClassLoaders) {
      checkPermission(new RuntimePermission("modifyThreadGroup"));
    }
  }

  /** Checks whether the caller has access to the declared members of ``cl''.
   ** If you override this, you should  include a call to super.checkMemberAccess().
   */
  public void checkMemberAccess(Class cl, int mtype)
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders && mtype != java.lang.reflect.Member.PUBLIC && cl.loader != currentClassLoader()) {
      checkPermission(new RuntimePermission("accessDeclaredMembers."+cl.getName()));
    }
  }

  /** Checks whether the caller is allowed to exit the VM.
   ** If you override this, you should  include a call to super.checkExit().
   ** The `status' argument is not used.
   */
  public void checkExit(int status)
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      checkPermission(new RuntimePermission("exitVM"));
    }
  }

  /** Checks whether the caller is allowed to execute the specified command.
   ** If you override this, you should  include a call to super.checkExecute().
   ** Note the implicit assumption that all commands are the names of files.
   */
  public void checkExec(String cmd)
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      checkPermission(new FilePermission(cmd, "execute"));
    }
  }

  /** Checks whether the caller has permission to use IP Multicast.
   ** If you override this, you should  include a call to super.checkMulticast().
   */
  public void checkMulticast(InetAddress addr)
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      checkPermission(new SocketPermission(addr.getHostAddress(), "accept,connect"));
    }
  }

  /** Checks whether the caller has permission to use IP Multicast.
   ** If you override this, you should  include a call to super.checkMulticast().
   ** Note: parameter `ttl' is not used (what's it there for?)
   */
  public void checkMulticast(InetAddress addr, byte ttl)
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      checkPermission(new SocketPermission(addr.getHostAddress(), "accept,connect"));
    }
  }

  /** Checks whether the caller has permission to read the given system property.
   ** If you override this, you should  include a call to super.checkPropertyAccess().
   */
  public void checkPropertyAccess(String propname)
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      checkPermission(new PropertyPermission(propname, "read"));
    }
  }

  /** Checks whether the caller has permission to read or modify the system properties as a whole.
   ** If you override this, you should  include a call to super.checkPropertiesAccess().
   */
  public void checkPropertiesAccess()
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      checkPermission(new PropertyPermission("*", "read,write"));
    }
  }

  /** Checks whether the caller has the SecurityPermission called ``target''.
   ** If you override this, you should include a call to super.checkPropertiesAccess().
   */
  public void checkSecurityAccess(String target)
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      checkPermission(new SecurityPermission(target));
    }
  }

  /** Checks whether the caller has permission to load library ``libname''.
   ** If you override this, you should include a call to super.checkLink().
   */
  public void checkLink(String libname)
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      checkPermission(new RuntimePermission("loadLibrary."+libname));
    }
  }

  /** Checks whether the caller has permission to read the file descriptor `fd'.
   ** If you override this, you should include a call to super.checkRead().
   ** Parameter `fd' is not used.
   */
  public void checkRead(FileDescriptor fd)
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      checkPermission(new RuntimePermission("readFileDescriptor"));
    }
  }

  /** Checks whether the caller has permission to read the file ``file''.
   ** If you override this, you should include a call to super.checkRead().
   */
  public void checkRead(String file)
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      checkPermission(new FilePermission(file,"read"));
    }
  }

  /** Checks whether the given context has permission to read the file ``file''.
   */
  public void checkRead(String file, Object context)
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      checkPermission(new FilePermission(file,"read"), context);
    }
  }

  /** Checks whether the caller has permission to write to the file `fd'.
   ** If you override this, you should include a call to super.checkWrite().
   ** Parameter `fd' is not used in the default version.
   */
  public void checkWrite(FileDescriptor fd)
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      checkPermission(new RuntimePermission("writeFileDescriptor"));
    }
  }

  /** Checks whether the caller has permission to write to the file ``file''.
   ** If you override this, you should include a call to super.checkWrite().
   */
  public void checkWrite(String file)
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      checkPermission(new FilePermission(file,"write"));
    }
  }


  public void checkDelete(String file)  
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      checkPermission(new FilePermission(file,"delete"));
    }
  }

  /** Checks whether the caller has permission to connect to the given port.
   ** If you override this, you should include a call to super.checkConnect().
   */
  public void checkConnect(String host, int port)
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      if (port == -1) {
        checkPermission(new SocketPermission(host,"resolve"));
      }
      else {
        checkPermission(new SocketPermission(host+":"+port,"connect"));
      }
    }
  }

  /** Checks whether the given context has permission to connect to the given port.
   ** If you override this, you should include a call to super.checkConnect().
   */
  public void checkConnect(String host, int port, Object context)
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      if (port == -1) {
        checkPermission(new SocketPermission(host,"resolve"), context);
      }
      else {
        checkPermission(new SocketPermission(host+":"+port,"connect"), context);
      }
    }
  }

  /** Checks whether the caller has permission to listen to the given port.
   ** If you override this, you should include a call to super.checkListen().
   */
  public void checkListen(int port)
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      if (port == 0) {
        checkPermission(new SocketPermission("localhost:1024-","listen"));
      }
      else {
        checkPermission(new SocketPermission("localhost:"+port,"listen"));
      }
    }
  }

  /** Checks whether the caller has permission to accept incoming connections.
   ** If you override this, you should include a call to super.checkAccept().
   */
  public void checkAccept(String host, int port)
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      checkPermission(new SocketPermission(host+":"+port,"accept"));
    }
  }

  /** Checks whether the caller has permission to initiate a print job.
   ** If you override this, you should include a call to super.checkPrintJobAccess().
   */
  public void checkPrintJobAccess()
    throws SecurityException
  {
    if (awtPermission != null) {
      Object[] params = new Object[1];
      params[0] = new String("queuePrintJob");
      Permission perm;
      try {
        perm = (Permission)awtpCon.newInstance(params);
      }
      catch (Exception e) {
        e.printStackTrace();
        throw new SecurityException("Failed to create an AWTPermission? "+e);
      }
      checkPermission(perm);
    }
    else {
      throw new SecurityException("No AWT present?");
    }
  }

  /** Checks whether the caller has permission to access the AWT system clipboard.
   ** If you override this, you should include a call to super.checkSystemClipboardAccess().
   */
  public void checkSystemClipboardAccess()
    throws SecurityException
  {
    if (awtPermission != null) {
      Object[] params = new Object[1];
      params[0] = new String("accessClipboard");
      Permission perm;
      try {
        perm = (Permission)awtpCon.newInstance(params);
      }
      catch (Exception e) {
        e.printStackTrace();
        throw new SecurityException("Failed to create an AWTPermission? "+e);
      }
      checkPermission(perm);
    }
    else {
      throw new SecurityException("No AWT present?");
    }
  }

  /** Checks whether the caller has permission to access the AWT event queue.
   ** If you override this, you should include a call to super.checkAwtEventQueueAccess().
   */
  public void checkAwtEventQueueAccess()
    throws SecurityException
  {
    if (awtPermission != null) {
      Object[] params = new Object[1];
      params[0] = new String("accessEventQueue");
      Permission perm;
      try {
        perm = (Permission)awtpCon.newInstance(params);
      }
      catch (Exception e) {
        e.printStackTrace();
        throw new SecurityException("Failed to create an AWTPermission? "+e);
      }
      checkPermission(perm);
    }
    else {
      throw new SecurityException("No AWT present?");
    }
  }

  /** Checks whether the caller has permission to set the socket factories used by the java.net code.
   ** If you override this, you should include a call to super.checkSetFactory().
   */
  public void checkSetFactory()
    throws SecurityException
  {
    if (haveUserDefinedClassLoaders) {
      checkPermission(new RuntimePermission("setFactory"));
    }
  }

  /** Checks whether the caller is trusted to bring up the specified window.
   ** If you override this, you should include a call to super.checkTopLevelWindow().
   ** Note that this method anomolously returns `true' if the caller is
   ** trusted, `false' otherwise.  The parameter (`window') is not used.
   */
  public boolean checkTopLevelWindow(Object window)
    throws SecurityException
  {
    if (awtPermission != null) {
      Object[] params = new Object[1];
      params[0] = new String("topLevelWindowPermission");
      Permission perm;
      try {
        perm = (Permission)awtpCon.newInstance(params);
      }
      catch (Exception e) {
        e.printStackTrace();

        return false;

      }

      try {
        checkPermission(perm);

        return true;

      } catch (SecurityException se) {

        return false;

      }
    }
    else {

      return false;

    }
  }

  /** Checks whether the caller has access to the given package.
   ** If you override this, you should include a call to super.checkPackageAccess().
   */
  public void checkPackageAccess(String packageName)
    throws SecurityException
  {
    String restricteds = java.security.Security.getProperty("package.access");
    if (restricteds == null) {
      restricteds = "wonka,com.acunia.wonka";
    }

    int comma = restricteds.indexOf(',');
    String arestricted;
    while (comma >= 0) {
      arestricted = restricteds.substring(0,comma);
      if (packageName.equals(arestricted) || packageName.startsWith(arestricted) && packageName.charAt(arestricted.length()) == '.') {
        checkPermission(new RuntimePermission("accessClassInPackage."+packageName));
      }
      restricteds = restricteds.substring(comma+1);
      comma = restricteds.indexOf(',');
    }
    arestricted = restricteds;
    if (packageName.equals(arestricted) || packageName.startsWith(arestricted) && packageName.charAt(arestricted.length()) == '.') {
      checkPermission(new RuntimePermission("accessClassInPackage."+packageName));
    }
  }

  /** Checks whether the caller has permission to define classes in the given package.
   ** If you override this, you should include a call to super.checkPackageDefinition().
   */
  public void checkPackageDefinition(String packageName)
    throws SecurityException
  {
    String restricteds = java.security.Security.getProperty("package.definition");
    if (restricteds == null) {
      restricteds = "java,wonka,com.acunia.wonka";
    }

    int comma = restricteds.indexOf(',');
    String arestricted;
    while (comma >= 0) {
      arestricted = restricteds.substring(0,comma);
      if (packageName.equals(arestricted) || packageName.startsWith(arestricted) && packageName.charAt(arestricted.length()) == '.') {
        checkPermission(new RuntimePermission("defineClassInPackage."+packageName));
      }
      restricteds = restricteds.substring(comma+1);
      comma = restricteds.indexOf(',');
    }
    arestricted = restricteds;
    if (packageName.equals(arestricted) || packageName.startsWith(arestricted) && packageName.charAt(arestricted.length()) == '.') {
      checkPermission(new RuntimePermission("defineClassInPackage."+packageName));
    }
  }
}
