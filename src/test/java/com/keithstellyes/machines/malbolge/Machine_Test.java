package com.keithstellyes.machines.malbolge;

import com.keithstellyes.machines.shared.MachineUtil;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class Machine_Test {
    InputStream in;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Machine machine = new Machine(in, out);

    @Test
    public void helloWorld() {
        String program = " (=<`#9]~6ZY32Vx/4Rs+0No-&Jk)\"Fh}|Bcy?`=*z]Kw%oG4UUS0/@-ejc(:'8dc\n";
        String hello = "HELLO WORLD!";
        machine.loadProgram(program);

        assertTrue(MachineUtil.runMachineUntilHalt(machine, Integer.MAX_VALUE));
        byte[] actualOut = out.toByteArray();
        for(int i = 0; i < hello.length(); i++) {
            assertEquals(hello.charAt(i), actualOut[i]);
        }
    }

    @Test
    public void encrypt() {
        int[] inputs = {0, 1, 2, 20, 67, 93};
        int[] outputs = {57, 109, 60, 125, 85, 98};
        assert inputs.length == outputs.length;

        for(int i = 0; i < inputs.length; i++) {
            assertEquals(outputs[i], Machine.encrypt(inputs[i]));
        }
    }


    @Test
    public void toTernary() {
        int[] expect = {0, 0, 0, 0, 0, 0, 0, 1, 1, 0};
        byte[] actual = Machine.toTernary(12);
        assertEquals(Arrays.toString(expect), Arrays.toString(actual));

        expect = new int[]{1, 0, 0, 1, 0, 2, 2, 2, 1, 1};
        actual = Machine.toTernary(20_650);
        assertEquals(Arrays.toString(expect), Arrays.toString(actual));
    }

    @Test
    public void crazy() {
        assertEquals(20_650, Machine.crazy(1_131, 11_355));
    }

    @Test
    public void rotate() {
        assertEquals(39_973, Machine.ternaryRotate(1_823));
    }
}
