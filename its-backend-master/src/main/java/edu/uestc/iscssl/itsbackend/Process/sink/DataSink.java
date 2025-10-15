package edu.uestc.iscssl.itsbackend.Process.sink;

import edu.uestc.iscssl.itsbackend.utils.DataInfo;


public interface DataSink {
    public void sink(String data);
    public void sink2(DataInfo dataInfo);
}
