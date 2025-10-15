package edu.uestc.iscssl.itsengine.Class2Structure;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class SignalStructure extends Structure {
   public  Integer crossID;
   public  Integer cycleTime;
   public  Integer ewStraight;
   public Integer ewLeft;
   public Integer snStraight;
   public Integer snLeft;

   @Override
   protected List getFieldOrder() {
      return Arrays.asList(new String[]{"crossID", "cycleTime","ewStraight","ewLeft","snStraight","snLeft"});
   }


}
