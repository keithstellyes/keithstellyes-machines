package com.keithstellyes.machines.mano;

import com.keithstellyes.machines.shared.MachineUtil;
import com.keithstellyes.machines.shared.ShortMemoryBuilder;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static junit.framework.TestCase.assertEquals;

public class Machine_Test {
    InputStream in;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Machine machine = new Machine(in, out);
    ShortMemoryBuilder memoryBuilder = new ShortMemoryBuilder(0, 4095, Machine.MEMORY_COUNT);

    @Test
    public void out1To10() {
        int tenComparator = memoryBuilder.addDatum((0xFFFF - 10) + 1);
        int temp = memoryBuilder.addDatum(0);

        int start = memoryBuilder.addInstruction(Machine.INC);
        memoryBuilder.addInstruction(Machine.OUT);
        memoryBuilder.addInstruction(Machine.STA | temp);
        memoryBuilder.addInstruction(Machine.ADD | tenComparator);
        memoryBuilder.addInstruction(Machine.SZA); // if zero we want to halt, so the next instruction is a jump for the case where we _don't_ halt
        int iJumpToContinue = memoryBuilder.addInstruction(Machine.BUN); // we update this later to jump to the continue section
        memoryBuilder.addInstruction(Machine.HLT);
        int continueSection = memoryBuilder.addInstruction(Machine.CLE); // jumped to when not done yet
        memoryBuilder.addInstruction(Machine.LDA | temp);
        memoryBuilder.addInstruction(Machine.BUN | start);

        memoryBuilder.set(iJumpToContinue, Machine.BUN | continueSection);

        machine.loadProgram(memoryBuilder.getMemory());

        boolean didNotGiveUp = MachineUtil.runMachineUntilHalt(machine, 100);

        byte[] actualResults = out.toByteArray();
        for(int i = 0; i < 10; i++) {
            assertEquals(i + 1, actualResults[i]);
        }
        assertEquals(10, actualResults.length);
        assertEquals(MachineUtil.DID_NOT_GIVE_UP, didNotGiveUp);
    }

    @Test
    public void helloWorld() {
        String hello = "Hello, World!\0";
        int stringLoc = memoryBuilder.addData(hello);
        int stringPtr = memoryBuilder.addDatum(stringLoc);

        int start = memoryBuilder.addInstruction(Machine.INDIRECT | Machine.LDA | stringPtr);
        memoryBuilder.addInstruction(Machine.SZA);
        int iJumpToContinue = memoryBuilder.addInstruction(Machine.BUN);
        memoryBuilder.addInstruction(Machine.HLT);
        int continueSection = memoryBuilder.addInstruction(Machine.OUT);
        memoryBuilder.addInstruction(Machine.LDA | stringPtr);
        memoryBuilder.addInstruction(Machine.INC);
        memoryBuilder.addInstruction(Machine.STA | stringPtr);
        memoryBuilder.addInstruction(Machine.BUN | start);

        memoryBuilder.set(iJumpToContinue, Machine.BUN | continueSection);

        machine.loadProgram(memoryBuilder.getMemory());

        boolean didNotGiveUp = MachineUtil.runMachineUntilHalt(machine, 200);

        byte[] actualOut = out.toByteArray();
        for(int i = 0; i < hello.length() - 1; i++) {
            assertEquals(hello.charAt(i), actualOut[i]);
        }
        assertEquals(hello.length() - 1, actualOut.length);
        assertEquals(MachineUtil.DID_NOT_GIVE_UP, didNotGiveUp);
    }
}
