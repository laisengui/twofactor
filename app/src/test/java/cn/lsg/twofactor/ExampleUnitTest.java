package cn.lsg.twofactor;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {

       String s= GoogleAuthenticatorUtils.createSecretKey();
        System.out.println(s);
        String ss=GoogleAuthenticatorUtils.createKeyUri(s,"admin","Google");
        System.out.println(ss);
    }
}