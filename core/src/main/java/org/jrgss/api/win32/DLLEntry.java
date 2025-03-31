package org.jrgss.api.win32;

import java.beans.ConstructorProperties;

class DLLEntry {
   final String dllName;
   final String funcName;
   final String spec;

   @ConstructorProperties({"dllName", "funcName", "spec"})
   public DLLEntry(String dllName, String funcName, String spec) {
      this.dllName = dllName;
      this.funcName = funcName;
      this.spec = spec;
   }

   public String getDllName() {
      return this.dllName;
   }

   public String getFuncName() {
      return this.funcName;
   }

   public String getSpec() {
      return this.spec;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof DLLEntry)) {
         return false;
      } else {
         DLLEntry other = (DLLEntry)o;
         if (!other.canEqual(this)) {
            return false;
         } else {
            Object this$dllName = this.getDllName();
            Object other$dllName = other.getDllName();
            if (this$dllName == null ? other$dllName == null : this$dllName.equals(other$dllName)) {
               Object this$funcName = this.getFuncName();
               Object other$funcName = other.getFuncName();
               if (this$funcName == null ? other$funcName == null : this$funcName.equals(other$funcName)) {
                  Object this$spec = this.getSpec();
                  Object other$spec = other.getSpec();
                  return this$spec == null ? other$spec == null : this$spec.equals(other$spec);
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof DLLEntry;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      Object $dllName = this.getDllName();
      result = result * 59 + ($dllName == null ? 43 : $dllName.hashCode());
      Object $funcName = this.getFuncName();
      result = result * 59 + ($funcName == null ? 43 : $funcName.hashCode());
      Object $spec = this.getSpec();
      return result * 59 + ($spec == null ? 43 : $spec.hashCode());
   }

   @Override
   public String toString() {
      return "DLLEntry(dllName=" + this.getDllName() + ", funcName=" + this.getFuncName() + ", spec=" + this.getSpec() + ")";
   }
}
