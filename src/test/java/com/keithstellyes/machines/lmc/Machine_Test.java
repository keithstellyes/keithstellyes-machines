package com.keithstellyes.machines.lmc;

import com.keithstellyes.machines.shared.MachineUtil;
import com.keithstellyes.machines.shared.ShortMemoryBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static com.keithstellyes.machines.shared.Machine.Delta;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Machine_Test {
    private Machine machine;
    private ByteArrayInputStream in;
    private ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ShortMemoryBuilder memoryBuilder = new ShortMemoryBuilder(Machine.MIN_VALUE,
            99, 100);

    @Before
    public void setUp() {
        machine = new Machine(in, out);
    }

    @Test
    public void add() {
        machine.setMemoryValue(0, 1);
        machine.parseInstruction(100).apply(machine);
        assertEquals(1, machine.getMemoryValue(0));
        assertEquals(1, machine.getRegister(Machine.PROGRAM_COUNTER_REG));
        assertEquals(1, machine.getRegister(Machine.ACCUMULATOR_REG));
    }

    @Test
    public void halt() {
        machine.setMemoryValue(0, Machine.buildInstruction(Machine.HLT, 0));
        machine.parseInstruction(machine.getCurrentInstruction()).apply(machine);
        assertTrue(machine.isHalted());
    }

    /**
     * Tests a simple program with a count-down to 0, each
     * iteration of a loop adding the ASCII '0' to output
     */
    @Test
    public void basicLoopTest() {
        //// make the program ////
        // data //
        int loopCountLoc = addDatum(9);
        int printableOffsetLoc = addDatum('0');
        int constOneLoc = addDatum(1);

        addInstruction(Machine.LDA, loopCountLoc);
        // we'll come back and over-write this again
        int loopBegin = addInstruction(Machine.HLT, 0);

        // print the value //
        addInstruction(Machine.ADD, printableOffsetLoc);
        addInstruction(Machine.IO, Machine.IO_ARG_OUT);

        // reload our counter, decrement, then jump back to top //
        addInstruction(Machine.LDA, loopCountLoc);
        addInstruction(Machine.SUB, constOneLoc);
        addInstruction(Machine.STA, loopCountLoc);
        addInstruction(Machine.BRA, loopBegin);
        int allDone = addInstruction(Machine.HLT, 0);
        setMemoryInstruction(loopBegin, Machine.BRZ, allDone);
        // we're done programmaically making the program for the LMC to run //

        machine.loadProgram(memoryBuilder.getMemory());
        MachineUtil.runMachineUntilHalt(machine, 100);

        byte[] actualOut = out.toByteArray();
        for(int i = 9; i >= 1; i--) {
            assertEquals(i + '0', actualOut[9 - i]);
        }
    }

    /**
     * Prints a NUL-terminated string. The only way to support array-indexing is to
     * do arithmetic against a load instruction.
     */
    @Test
    public void helloWorld() {
        final String helloWorld = "Hello, World!\0";
        // looping, printing a NUL-terminated string.
        int stringLoc = memoryBuilder.addData(helloWorld.toCharArray());
        int constOneLoc = memoryBuilder.addDatum((short) 1);

        // not data this time...
        int haltLoc = memoryBuilder.addDatum((short) Machine.HLT);

        // load string index
        // this instruction will actually be _incremented_
        int loopBegin = addInstruction(Machine.LDA, stringLoc);
        addInstruction(Machine.BRZ, haltLoc);
        addInstruction(Machine.IO, Machine.IO_ARG_OUT);

        //increment our index-load-instruction
        addInstruction(Machine.LDA, loopBegin);
        addInstruction(Machine.ADD, constOneLoc);
        addInstruction(Machine.STA, loopBegin);
        int finalInstruction = addInstruction(Machine.BRA, loopBegin);

        machine.loadProgram(memoryBuilder.getMemory());
        MachineUtil.runMachineUntilHalt(machine, 500);

        final byte[] actualOut = out.toByteArray();
        // the last char of hello world isn't OUTed
        for(int i = 0; i < helloWorld.length() - 1; i++) {
            char expect = helloWorld.charAt(i);
            char actual = (char) actualOut[i];
            assertEquals(expect, actual);
        }

        // make sure the memory values between our code and data sections are still 0.
        for(int i = finalInstruction + 1; i < haltLoc; i++) {
            assertEquals(0, machine.getMemoryValue(i));
        }
    }

    private int addInstruction(int opcode, int argument) {
        return memoryBuilder.addInstruction(Machine.buildInstruction(opcode, argument));
    }

    private int addDatum(int datum) {
        return memoryBuilder.addDatum((short) datum);
    }

    private int setMemoryInstruction(int address, int opcode, int argument) {
        memoryBuilder.set(address, Machine.buildInstruction(opcode, argument));
        return address;
    }
}
