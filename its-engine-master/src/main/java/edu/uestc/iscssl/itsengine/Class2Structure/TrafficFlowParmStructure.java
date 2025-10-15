package edu.uestc.iscssl.itsengine.Class2Structure;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class TrafficFlowParmStructure extends Structure {
   public Integer roadID;
   public Integer policy;
   public Integer demand;

   @Override
   protected List getFieldOrder() {
      return Arrays.asList(new String[]{"roadID", "policy", "demand"});
   }


}
