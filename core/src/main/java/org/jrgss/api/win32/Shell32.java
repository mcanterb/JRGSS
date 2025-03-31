package org.jrgss.api.win32;

import com.badlogic.gdx.Gdx;
import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class Shell32 {
   @Win32Function(
      dll = "shell32",
      name = "ShellExecute",
      spec = "LPPPPL"
   )
   public static final DLLImpl ShellExecute = new DLLImpl() {
      @Override
      public IRubyObject call(Win32API api, ThreadContext context, IRubyObject[] args) {
         String command = Win32Util.getString(args[1]);
         String urlString = Win32Util.getString(args[2]);
         if (!command.equalsIgnoreCase("open")) {
            Gdx.app.error("Shell32", "ShellExecute implementation does not support anything except open!");
            return Win32Util.rubyNum(0L);
         } else {
            try {
               URI uri = URI.create(urlString);
               Desktop.getDesktop().browse(uri);
            } catch (IllegalArgumentException var9) {
               try {
                  Desktop.getDesktop().open(new File(urlString));
               } catch (Exception var8) {
                  Gdx.app.error("Shell32", "Failed to open " + urlString + "! " + var8);
               }

               return Win32Util.rubyNum(0L);
            } catch (Exception var10) {
               Gdx.app.error("Shell32", "This platform does not seem to support browsing: " + var10);
               return Win32Util.rubyNum(0L);
            }

            Gdx.app.log("Shell32", "Stub ShellExecute behavior for " + command + ", " + urlString);
            return Win32Util.rubyNum(0L);
         }
      }
   };

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Shell32)) {
         return false;
      } else {
         Shell32 other = (Shell32)o;
         return other.canEqual(this);
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof Shell32;
   }

   @Override
   public int hashCode() {
      return 1;
   }

   @Override
   public String toString() {
      return "Shell32()";
   }
}
