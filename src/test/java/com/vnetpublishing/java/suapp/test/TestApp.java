package com.vnetpublishing.java.suapp.test;

import static org.junit.Assert.*;

import org.junit.*;

import com.vnetpublishing.java.suapp.*;

public class TestApp implements SuperUserApplication {

    @Test
    public void sudoTest() {
        System.out.println("sudoTest called!");
        SU.daemon = true;
        SU.debug = true;
        int result = SU.run(this, new String[] {});
        assertEquals(0, result);
    }

    public int run(String[] args) {
        System.out.println("*** Admin Process RUN in TEST ***");
        return 0;
    }

}
