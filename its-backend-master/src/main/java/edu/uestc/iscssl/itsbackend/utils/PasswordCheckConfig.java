package edu.uestc.iscssl.itsbackend.utils;

/**
 * 密码检测常量
 */
public class PasswordCheckConfig {

    /**
     * 是否检测密码口令长度
     */
    public static String CHECK_PASSWORD_LENGTH = "enable";
    /**
     * 密码最小长度，默认为8
     */
    public static String MIN_LENGTH = "8";
    /**
     * 密码最大长度，默认为20
     */
    public static String MAX_LENGTH = "20";

    /**
     * 是否检测密码复杂度
     */
    public static String PASSWORD_COMPLEXITY = "enable";

    /**
     * 是否包含数字
     */
    public static String CHECK_CONTAIN_DIGIT = "disable";


    /**
     * 是否包含字母
     */
    public static String CHECK_CONTAIN_CASE = "disable";

    /**
     * 是否区分大小写
     */
    public static String CHECK_DISTINGGUISH_CASE = "disable";


    /**
     * 是否包含小写字母
     */
    public static String CHECK_LOWER_CASE = "disable";


    /**
     * 是否包含大写字母
     */
    public static String CHECK_UPPER_CASE = "disable";


    /**
     * 是否包含特殊符号
     */
    public static String CHECK_CONTAIN_SPECIAL_CHAR = "disable";
    /**
     * 特殊符号集合
     */
    public static String SPECIAL_CHAR = "!\\\"#$%&'()*+,-./:;<=>?@[\\\\]^_`{|}~";


    /**
     * 是否检测键盘按键横向连续
     * disable
     * enable
     */
    public static String CHECK_HORIZONTAL_KEY_SEQUENTIAL = "disable";
    /**
     * 键盘物理位置横向不允许最小的连续个数
     */
    public static String LIMIT_HORIZONTAL_NUM_KEY = "4";


    /**
     * 是否检测键盘按键斜向连续
     */
    public static String CHECK_SLOPE_KEY_SEQUENTIAL = "disable";
    /**
     * 键盘物理位置斜向不允许最小的连续个数
     */
    public static String LIMIT_SLOPE_NUM_KEY = "4";


    /**
     * 是否检测逻辑位置连续
     */
    public static String CHECK_LOGIC_SEQUENTIAL = "disable";
    /**
     * 密码口令中字符在逻辑位置上不允许最小的连续个数
     */
    public static String LIMIT_LOGIC_NUM_CHAR = "4";


    /**
     * 是否检测连续字符相同
     */
    public static String CHECK_SEQUENTIAL_CHAR_SAME = "enable";
    /**
     * 密码口令中相同字符不允许最小的连续个数
     */
    public static String LIMIT_NUM_SAME_CHAR = "4";


    /**
     * 键盘横向方向规则
     */
    public static String[] KEYBOARD_HORIZONTAL_ARR = {"01234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm",};
    /**
     * 键盘斜线方向规则
     */
    public static String[] KEYBOARD_SLOPE_ARR = {"1qaz", "2wsx", "3edc", "4rfv", "5tgb", "6yhn", "7ujm", "8ik,", "9ol.",
            "0p;/", "=[;.", "-pl,", "0okm", "9ijn", "8uhb", "7ygv", "6tfc", "5rdx", "4esz"};


    /**
     * 是否检测常用词库
     */
    public static String CHECK_SIMPLE_WORD = "enable";

    /**
     * 特殊字符
     */
    public static final String SPEC_CHARACTERS = " !\"#$%&'()*+,-./:;<=>?@\\]\\[^_`{|}~";

    /**
     * 纯字母
     */
    public static final String character = "[a-zA-Z]{1,}$";

    /**纯数字
     *
     */
    public static final String numberic = "[0-9]{1,}$";

    /**
     * 字母和数字
     */
    public static final String number_and_character = "((^[a-zA-Z]{1,}[0-9]{1,}[a-zA-Z0-9]*)+)" +
            "|((^[0-9]{1,}[a-zA-Z]{1,}[a-zA-Z0-9]*)+)$";

    /**
     * 字母或数字
     */
    public static final String number_or_character = "[a-zA-Z0-9]+$";

    /**
     * 字母数字下划线
     */
    public static final String ncw = "\\w+$";

    /**
     * 常用词库
     */
    public static String[] SIMPLE_WORDS = {"123456","password","12345","123456789","password1","abc123","12345678","qwerty","111111","1234567","1234","iloveyou","sunshine","monkey","1234567890","123123","princess","baseball","dragon","football","shadow","michael","soccer","unknown","maggie","0","ashley","myspace1","purple","fuckyou","charlie","jordan","hunter","superman","tigger","michelle","buster","pepper","justin","andrew","harley","matthew","bailey","jennifer","samantha","ginger","anthony","qwerty123","qwerty1","peanut","summer","hannah","654321","michael1","cookie","linkedin","madison","joshua","taylor","whatever","mustang","jessica","qwertyuiop","amanda","jasmine","123456a","123abc","brandon","letmein","freedom","basketball","xxx","babygirl","thomas","william","hello","austin","qwe123","123","jackson","fuckyou1","love","family","yellow","trustno1","robert","jesus1","chicken","jordan23","mickey","diamond","scooter","booboo","welcome","george","smokey","cheese","computer","morgan","nicholas","daniel","butterfly","dakota","696969","midnight","princess1","joseph","orange","monkey1","killer","snoopy","heather","qwerty12","patrick","anthony1","melissa","1qaz2wsx","nathan","bandit","nicole","jessica1","sparky","666666","football1","tennis","master","asshole","batman","baseball1","sunshine1","bubbles","friends","1q2w3e4r","chocolate","hockey","yankees","tinkerbell","iloveyou1","abcd1234","elizabeth","flower","121212","passw0rd","pokemon","starwars","softball","iloveyou2","lauren","shopping","love123","a123456","fuckyou2","steelers","danielle","peaches","angels","poohbear","aaaaaa","babygirl1","snowflake","charlie1","sophie","7777777","superman1","loveme","123qwe","zinch","123321","angel1","redsox","cowboys","112233","zxcvbnm","rainbow","dallas","mother","qazwsx","snickers","lakers","money1","ladybug","987654321","olivia","ranger","oliver","asdfghjkl","corvette","asdfgh","aaron431","xbox360","asdf3423","rachel","bigdaddy","blessed1","lovely","asshole1","pussy","muffin","dancer","madison1"};

}
