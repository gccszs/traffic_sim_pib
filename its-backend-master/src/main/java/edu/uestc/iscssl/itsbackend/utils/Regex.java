package edu.uestc.iscssl.itsbackend.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regex {
    public static final String GET_STEP_NUMBER="\"step\":([0-9]+),";
    private Pattern p;
    private Matcher m;
    public Regex(String regex) {
        p=Pattern.compile(regex);
    }

    public String getStepNumOfLine(String str){
        m=p.matcher(str);
        if (m.find())
            return m.group(1);
        return null;
    }

}
