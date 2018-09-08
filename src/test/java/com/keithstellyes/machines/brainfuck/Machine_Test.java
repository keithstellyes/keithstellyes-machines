package com.keithstellyes.machines.brainfuck;

import com.keithstellyes.machines.shared.MachineUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

public class Machine_Test {
    InputStream in;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Machine machine = new Machine(in, out);

    @Before
    public void setUp() {

    }

    /**
     * Simple hello world w/ no looping
     */
    @Test
    public void simpleHelloWorld() {
        StringBuilder sb = new StringBuilder();
        String hello = "Hello, World!";
        for(int i = 0; i < hello.length(); i++) {
            char c = hello.charAt(i);
            for(int j = 0; j < c; j++) {
                sb.append('+');
            }
            sb.append(".>");
        }
        machine.loadProgram(sb);

        int giveUpCounter = 1000;
        while(giveUpCounter-- > 0 && !machine.isHalted()) {
            machine.parseInstruction(machine.getCurrentInstruction()).apply(machine);
        }

        byte[] actualOut = out.toByteArray();
        for(int i = 0; i < hello.length(); i++) {
            assertEquals(hello.charAt(i), actualOut[i]);
        }
    }

    @Test
    public void slightlyComplexHelloWorld() {
        StringBuilder sb = new StringBuilder();
        String hello = "Hello, World!";
        for(int i = 0; i < hello.length(); i++) {
            char c = hello.charAt(i);
            for(int j = 0; j < c; j++) {
                sb.append('+');
            }
            sb.append('.');
            for(int j = 0; j < c; j++) {
                sb.append('-');
            }
        }
        testProgram(machine, sb, 1000, hello);
    }

    /**
     * Testing the Hello, World! example stolen from Wikipedia.
     */
    @Test
    public void moreComplexHelloWorld() {
        String program = "++++++++[>++++[>++>+++>+++>+<<<<-]>+>+>->>+[<]<-]>>.>---.+++++++..+++.>>.<-.<.+++.------.--------.>>+.>++.\n";
        String hello = "Hello World!\n";
        machine.loadProgram(program);

        testProgram(machine, program, 1000000, hello);
    }

    /**
     * A simple loop, also tests expected overflow behavior.
     */
    @Test
    public void simpleLoop() {
        byte[] expectedOut = new byte[255];
        for(int i = 0; i < 255; i++) {
            expectedOut[i] = (byte) (i + 1);
        }
        testProgram(machine, "+[.+]", 10000, expectedOut);
    }

    private void testProgram(Machine machine, CharSequence program, int giveUpCounter,
                             String expectedOut) {
        try {
            testProgram(machine, program, giveUpCounter, expectedOut.getBytes("US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            fail();
        }
    }

    private void testProgram(Machine machine, CharSequence program, int giveUpCounter,
                             byte[] expectedOut) {
        machine.loadProgram(program);
        boolean didNotGiveUp = MachineUtil.runMachineUntilHalt(machine, giveUpCounter);
        assertTrue(didNotGiveUp);

        byte[] actualOut = out.toByteArray();

        for(int i = 0; i < expectedOut.length; i++) {
            assertEquals(expectedOut[i], actualOut[i]);
        }

        assertEquals(expectedOut.length, actualOut.length);
    }
}
