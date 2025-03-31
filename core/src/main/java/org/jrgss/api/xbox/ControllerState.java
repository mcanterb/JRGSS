package org.jrgss.api.xbox;

import java.beans.ConstructorProperties;
import java.util.Arrays;

public class ControllerState {
   private final int packet;
   private boolean[] buttonStates;
   private short leftTrigger;
   private short rightTrigger;
   private final short leftThumbX;
   private final short leftThumbY;
   private final short rightThumbX;
   private final short rightThumbY;

   public ControllerState() {
      this.packet = 0;
      this.buttonStates = new boolean[14];
      this.leftTrigger = 0;
      this.rightTrigger = 0;
      this.leftThumbX = 0;
      this.leftThumbY = 0;
      this.rightThumbX = 0;
      this.rightThumbY = 0;
   }

   public ControllerState withNewButtonStatus(int offset, boolean status) {
      boolean[] newButtonStatus = new boolean[this.buttonStates.length];
      System.arraycopy(this.buttonStates, 0, newButtonStatus, 0, this.buttonStates.length);
      newButtonStatus[offset] = status;
      return this.withPacket(this.packet + 1).withButtonStates(newButtonStatus);
   }

   public int getPacket() {
      return this.packet;
   }

   public boolean[] getButtonStates() {
      return this.buttonStates;
   }

   public short getLeftTrigger() {
      return this.leftTrigger;
   }

   public short getRightTrigger() {
      return this.rightTrigger;
   }

   public short getLeftThumbX() {
      return this.leftThumbX;
   }

   public short getLeftThumbY() {
      return this.leftThumbY;
   }

   public short getRightThumbX() {
      return this.rightThumbX;
   }

   public short getRightThumbY() {
      return this.rightThumbY;
   }

   public void setButtonStates(boolean[] buttonStates) {
      this.buttonStates = buttonStates;
   }

   public void setLeftTrigger(short leftTrigger) {
      this.leftTrigger = leftTrigger;
   }

   public void setRightTrigger(short rightTrigger) {
      this.rightTrigger = rightTrigger;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ControllerState)) {
         return false;
      } else {
         ControllerState other = (ControllerState)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getPacket() != other.getPacket()) {
            return false;
         } else if (!Arrays.equals(this.getButtonStates(), other.getButtonStates())) {
            return false;
         } else if (this.getLeftTrigger() != other.getLeftTrigger()) {
            return false;
         } else if (this.getRightTrigger() != other.getRightTrigger()) {
            return false;
         } else if (this.getLeftThumbX() != other.getLeftThumbX()) {
            return false;
         } else if (this.getLeftThumbY() != other.getLeftThumbY()) {
            return false;
         } else {
            return this.getRightThumbX() == other.getRightThumbX() && this.getRightThumbY() == other.getRightThumbY();
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof ControllerState;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getPacket();
      result = result * 59 + Arrays.hashCode(this.getButtonStates());
      result = result * 59 + this.getLeftTrigger();
      result = result * 59 + this.getRightTrigger();
      result = result * 59 + this.getLeftThumbX();
      result = result * 59 + this.getLeftThumbY();
      result = result * 59 + this.getRightThumbX();
      return result * 59 + this.getRightThumbY();
   }

   @Override
   public String toString() {
      return "ControllerState(packet="
         + this.getPacket()
         + ", buttonStates="
         + Arrays.toString(this.getButtonStates())
         + ", leftTrigger="
         + this.getLeftTrigger()
         + ", rightTrigger="
         + this.getRightTrigger()
         + ", leftThumbX="
         + this.getLeftThumbX()
         + ", leftThumbY="
         + this.getLeftThumbY()
         + ", rightThumbX="
         + this.getRightThumbX()
         + ", rightThumbY="
         + this.getRightThumbY()
         + ")";
   }

   @ConstructorProperties({"packet", "buttonStates", "leftTrigger", "rightTrigger", "leftThumbX", "leftThumbY", "rightThumbX", "rightThumbY"})
   public ControllerState(
      int packet, boolean[] buttonStates, short leftTrigger, short rightTrigger, short leftThumbX, short leftThumbY, short rightThumbX, short rightThumbY
   ) {
      this.packet = packet;
      this.buttonStates = buttonStates;
      this.leftTrigger = leftTrigger;
      this.rightTrigger = rightTrigger;
      this.leftThumbX = leftThumbX;
      this.leftThumbY = leftThumbY;
      this.rightThumbX = rightThumbX;
      this.rightThumbY = rightThumbY;
   }

   public ControllerState withPacket(int packet) {
      return this.packet == packet
         ? this
         : new ControllerState(
            packet, this.buttonStates, this.leftTrigger, this.rightTrigger, this.leftThumbX, this.leftThumbY, this.rightThumbX, this.rightThumbY
         );
   }

   public ControllerState withButtonStates(boolean[] buttonStates) {
      return this.buttonStates == buttonStates
         ? this
         : new ControllerState(
            this.packet, buttonStates, this.leftTrigger, this.rightTrigger, this.leftThumbX, this.leftThumbY, this.rightThumbX, this.rightThumbY
         );
   }

   public ControllerState withLeftTrigger(short leftTrigger) {
      return this.leftTrigger == leftTrigger
         ? this
         : new ControllerState(
            this.packet, this.buttonStates, leftTrigger, this.rightTrigger, this.leftThumbX, this.leftThumbY, this.rightThumbX, this.rightThumbY
         );
   }

   public ControllerState withRightTrigger(short rightTrigger) {
      return this.rightTrigger == rightTrigger
         ? this
         : new ControllerState(
            this.packet, this.buttonStates, this.leftTrigger, rightTrigger, this.leftThumbX, this.leftThumbY, this.rightThumbX, this.rightThumbY
         );
   }

   public ControllerState withLeftThumbX(short leftThumbX) {
      return this.leftThumbX == leftThumbX
         ? this
         : new ControllerState(
            this.packet, this.buttonStates, this.leftTrigger, this.rightTrigger, leftThumbX, this.leftThumbY, this.rightThumbX, this.rightThumbY
         );
   }

   public ControllerState withLeftThumbY(short leftThumbY) {
      return this.leftThumbY == leftThumbY
         ? this
         : new ControllerState(
            this.packet, this.buttonStates, this.leftTrigger, this.rightTrigger, this.leftThumbX, leftThumbY, this.rightThumbX, this.rightThumbY
         );
   }

   public ControllerState withRightThumbX(short rightThumbX) {
      return this.rightThumbX == rightThumbX
         ? this
         : new ControllerState(
            this.packet, this.buttonStates, this.leftTrigger, this.rightTrigger, this.leftThumbX, this.leftThumbY, rightThumbX, this.rightThumbY
         );
   }

   public ControllerState withRightThumbY(short rightThumbY) {
      return this.rightThumbY == rightThumbY
         ? this
         : new ControllerState(
            this.packet, this.buttonStates, this.leftTrigger, this.rightTrigger, this.leftThumbX, this.leftThumbY, this.rightThumbX, rightThumbY
         );
   }
}
