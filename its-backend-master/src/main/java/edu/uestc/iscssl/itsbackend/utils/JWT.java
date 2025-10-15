package edu.uestc.iscssl.itsbackend.utils;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Random;

/**
 *
 * This is a reference implementation of the XJWT specification: https://github.com/softtouchit/xjwt
 *
 *  Single-threaded only.
 *
 * To support multiple threads, use ThreadLocal.
 *
 * An XJWT has three parts: header, payload, and signature separated by two '.'.
 *
 * The header
 *
 * The header has the following raw structure:
 *
 * [expiry:long][version:byte][issue id:long]
 *
 *
 * The payload
 *
 * The payload was prepared from a JSON document as the following:
 *
 * aes256(random long + json + aes padding, aes key)
 *
 * where aes256 is the AES256 encryption function, the random long is a randomly
 * generated 8 byte long, json is a UTF8 encoded JSON document, aes padding are
 * additional characters for AES padding purpose.
 *
 * The raw structure is then base64 encoded
 *
 * The signature
 *
 * The signature is computed as the following:
 *
 * base64(HmacSHA256(based64(raw header) +'.' + base64 (raw payload), secret
 * key))
 *
 *
 * Verification and decryption The signature is verified with the shared secret
 * key, if not valid, the token is discarded. The header is base64 decoded and
 * the expire time is compared with the current time. If it has elapsed, then
 * the token is discarded. The header version is read and if it is not
 * supported, then the token is invalid. The payload is based64 decoded and
 * decrypted using the shared aes key. The first 8 bytes of the decrypted
 * messages and the paddings are discarded. The remaining data is returned.
 *
 *
 */
public class JWT {
    public static final byte DOT = '.';
    public static final int SIG_LENGTH = 32;
    public static final int BASE64_SIG_LENGTH = 44;
    public static final int AES_KEY_LENGTH = 44;
    public static final int EXPIRES_OFFSET = 0;
    public static final int TYPE_OFFSET = EXPIRES_OFFSET + 8;
    public static final int ISSUEID_OFFSET = TYPE_OFFSET + 1;
    public static final int HEADER_LENGTH = ISSUEID_OFFSET + 8;
    public final ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH).order(ByteOrder.BIG_ENDIAN);
    private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5','6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    private final Mac sha256_HMAC;
    private final SecretKeySpec secret_key;
    private byte[] sig = new byte[SIG_LENGTH];
    private byte[] aesKey;
    private final Random rand;
    private final Cipher cipher;
    private final Cipher decipher;
    private final SecretKeySpec keySpec;
    private final IvParameterSpec iv;
    final Base64 Base64 = new Base64();

    private final static String SECRET = "dp1cat";
    private final static String AES_KEY = "/LcTU0sAYB7yJFljldMxaQ+DbcUrBRwifMNy1Is2m4g=";
    private final static Long ISSUE_ID=100916L;
    //private final static String SECRET = "16jmp2";
    //private final static String AES_KEY = "SbYymvfZ8UjEmShxRAB0b1Dtaa0uGjDOOJa/f0Mbuo4=";
    //private final static Long ISSUE_ID=100400L;

    public interface Type {
        byte RESERVED = 0;
        byte JSON = 1;
        byte SYS = 2;
    }
    public static boolean isValidType(byte t) {
        switch (t) {
            case Type.RESERVED:
            case Type.JSON:
            case Type.SYS:
                return true;
            default:
                return false;
        }
    }
    private ByteBuffer encrypted = ByteBuffer.allocate(1024 * 64).order(ByteOrder.BIG_ENDIAN);
    public JWT(){
        this(SECRET, AES_KEY, System.nanoTime());
    }
    /**
     *
     * @param secret
     *  shared secret between parties
     * @param aesKey
     *  shared aesKey between parties
     * @param seed
     *  a random seed
     */
    public JWT(String secret, String aesKey, long seed) {
        try {
            if (aesKey.length() != AES_KEY_LENGTH) {
                throw new IllegalArgumentException("Aes key length must be 44");
            }
            sha256_HMAC = Mac.getInstance("HmacSHA256");
            secret_key = new javax.crypto.spec.SecretKeySpec(secret.getBytes("UTF8"), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            this.aesKey = Base64.decode(aesKey.getBytes());

            cipher = Cipher.getInstance("AES/CBC/NoPadding");
            keySpec = new SecretKeySpec(this.aesKey, "AES");
            iv = new IvParameterSpec(this.aesKey, 0, 16);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);

            decipher = Cipher.getInstance("AES/CBC/NoPadding");

            decipher.init(Cipher.DECRYPT_MODE, keySpec, iv);

            rand = new Random(seed);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int absolutePosition(ByteBuffer bb) {
        return bb.position() + bb.arrayOffset();
    }

    /**
     * Sign the payload. The output buffer is cleared and flipped.
     *
     * @param payload
     *            the payload
     * @param out
     *            the output buffer, it will be cleared
     * @param expires
     *         expire time in milliseconds
     * @return the output buffer
     */
    public ByteBuffer sign(byte type, ByteBuffer payload, ByteBuffer out, long expires) {
        out.clear();
        int pos = absolutePosition(out);
        header.clear();
        header.putLong(EXPIRES_OFFSET, expires);
        header.put(TYPE_OFFSET, type);
        header.putLong(ISSUEID_OFFSET, ISSUE_ID);
        try {
            byte[] dstHeader = new byte[header.remaining()];
            header.get(dstHeader);

            out.put(Base64.encode(dstHeader));
            out.put(DOT);

            byte[] dstPayload =new byte[payload.remaining()];
            payload.get(dstPayload);
            out.put(Base64.encode(dstPayload));

        } catch (Exception e1) {
            e1.printStackTrace();
        }
        sha256_HMAC.update(out.array(), pos, absolutePosition(out) - pos);
        out.put(DOT);
        try {
            sha256_HMAC.doFinal(sig, 0);
        } catch (ShortBufferException | IllegalStateException e) {
            throw new RuntimeException(e);
        }
        out.put(Base64.encode(sig));
        out.flip();

        //System.out.println(out.toString());

        return out;
    }

    /**
     * Encrypt and then sign the payload. The output buffer is cleared and flipped.
     *
     * According to the current research, this is the best practice
     *
     * @param type
     *            the type of the payload
     * @param payload
     *            the payload data
     * @param out
     *            encrypted and signed output
     * @param expires
     *         expire time in milliseconds
     * @return the output byte buffer, same as out
     */
    public ByteBuffer encryptAndSign(byte type, ByteBuffer payload, ByteBuffer out, long expires) {
        if (!isValidType(type)) {
            throw new IllegalArgumentException("Unknown type:" + type);
        }
        out.clear();
        int pos = out.position();
        out.putLong(rand.nextLong());
        out.put(payload);

        byte padding = (byte)  ((16 - ((out.position() + 1) & 0xF)) & 0xF);

        for (int i = 0; i < padding + 1; ++i) {
            out.put(padding);
        }

        try {
            int len = cipher.doFinal(out.array(), pos + out.arrayOffset(), out.position() - pos, encrypted.array(), 0);
            encrypted.limit(len);
            out.position(pos);
            return sign(type, encrypted, out, expires);

        } catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verify a signed data
     *
     * @param data
     * @param out
     * @return the type of the message
     * @param now
     *         current time in milliseconds
     */
    public byte verify(String data, ByteBuffer out, long now) {
        if (data == null) {
            throw new RuntimeException("Input data is null");
        }
        int i = data.length() - BASE64_SIG_LENGTH - 1;
        if (i < 0 || data.charAt(i) != DOT) {
            throw new RuntimeException("Invalid token");
        }

        byte[] da = data.getBytes();

        sha256_HMAC.update(da, 0, i);
        try {
            sha256_HMAC.doFinal(sig, 0);
        } catch (ShortBufferException | IllegalStateException e) {
            try {
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
            } catch (InvalidKeyException | InvalidAlgorithmParameterException e1) {
                throw new RuntimeException(e);
            }
            throw new RuntimeException(e);
        }

        byte[] tmp = data.substring(data.lastIndexOf(DOT) + 1).getBytes();
        byte[] _sig = Base64.decode(tmp);

        for (int j = 0; j < _sig.length; ++j) {
            if (_sig[j] != sig[j]) {
                throw new RuntimeException("Invalid token");
            }
        }

        int s = data.indexOf(DOT);

        byte[] header = Base64.decode(data.substring(0, s).getBytes());

        ByteBuffer bb = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN);

        long expires = bb.getLong(EXPIRES_OFFSET);
        if (expires < now) {
            throw new RuntimeException("Invalid token, expired");
        }

        out.clear();
        out.put(Base64.decode(data.substring(s + 1, i).getBytes()));
        out.flip();
        byte type = header[TYPE_OFFSET];
        if (!isValidType(type)) {
            throw new RuntimeException("Invalid token, unknown type:" + type);
        }
        return type;
    }

    /**
     * Verify and decrypt a signed data
     *
     * @param data
     * @param out
     * @return the type of the message
     * @param now
     *         current time in milliseconds
     */

    public byte verifyAndDecrypt(String data, ByteBuffer out, long now) {
        byte type = verify(data, encrypted, now);
        out.clear();
        int len;
        try {
            len = decipher.doFinal(encrypted.array(), absolutePosition(encrypted), encrypted.remaining(), out.array(), absolutePosition(out));
            len -= 1 + out.array()[out.position() + out.arrayOffset() + len - 1];
            if (len < 0 || len > out.remaining()) {
                throw new RuntimeException("Incorrect AES key or outout buffer too small");
            }
            out.position(8).limit(len);
            return type;
        } catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
            try {
                decipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
            } catch (InvalidKeyException | InvalidAlgorithmParameterException e1) {
                throw new RuntimeException(e);
            }
            throw new RuntimeException(e);
        }

    }

    /**
     * Encrypt and sign the given payload as type JSON
     *
     * @param payload
     *            the payload to be encrypted and signed
     * @param token
     *            the output token
     * @param expires
     *            the expire time in milliseconds
     * @return the token
     *
     */
    public ByteBuffer encryptAndSign(ByteBuffer payload, ByteBuffer token, long expires) {
        return encryptAndSign(Type.JSON, payload, token, expires);
    }

    public String verifyAndDecrypt(String data, long now) {
        ByteBuffer out = ByteBuffer.allocate(1024 * 8).order(ByteOrder.BIG_ENDIAN);
        this.verifyAndDecrypt(data, out, now);
        return new String(out.array(), out.arrayOffset() + out.position(), out.remaining());
    }
    //生成随机字符串
    public static String getRandomChar(int length) {
        char[] chr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        Random random = new Random();
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < length; i++) {
            buffer.append(chr[random.nextInt(16)]);
        }
        return buffer.toString();
    }
    public String encrypt(String password) throws NoSuchAlgorithmException {
/*        int pos = out.position();
        sha256_HMAC.update(out.array(), pos, absolutePosition(out) - pos);
        try {
            sha256_HMAC.doFinal(sig, 0);
        } catch (ShortBufferException | IllegalStateException e) {
            throw new RuntimeException(e);
        }*/
        if(password == null ){
            return null;
        }
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.reset();
        messageDigest.update(password.getBytes());
        return getFormattedText(messageDigest.digest());
    }
    private static String getFormattedText(byte[] bytes){
        int len = bytes.length;
        StringBuilder buf = new StringBuilder(len * 2);
        for (int j = 0; j < len; j++) {
            buf.append(HEX_DIGITS[(bytes[j] >> 4) & 0x0f]);
            buf.append(HEX_DIGITS[bytes[j] & 0x0f]);
        }
        return buf.toString();
    }

    public static String  dencrty(String token) throws UnsupportedEncodingException {
        //获取当前时间
        long now=new Date().getTime();
        //创建JWT实例
        JWT jwt=new JWT(SECRET,AES_KEY,System.nanoTime());
        token= URLDecoder.decode(token,"UTF-8");
        //调用解密方法解密token
        String resultJson=jwt.verifyAndDecrypt(token,now);
        return resultJson;
    }

    //加密数据
    public static String  encrty(String json) throws UnsupportedEncodingException {
        //获取当前时间
        long now=System.currentTimeMillis();
        //创建JWT实例
        JWT jwt=new JWT(SECRET,AES_KEY,System.nanoTime());
        //创建payload用来装参数
        ByteBuffer payload = ByteBuffer.allocate(1024).order(ByteOrder.BIG_ENDIAN);
        payload.put(json.getBytes("UTF-8")).flip();
        //创建out对象
        ByteBuffer out = ByteBuffer.allocate(1024);
        //调用加密方法，加密参数
        jwt.encryptAndSign(JWT.Type.SYS,payload,out,now+60*60*1000);
        String xjwt = new String(out.array(),out.arrayOffset(),out.remaining());
        return URLEncoder.encode(xjwt,"UTF-8");
    }
}