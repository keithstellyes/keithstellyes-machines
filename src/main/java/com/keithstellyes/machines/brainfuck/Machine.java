package com.keithstellyes.machines.brainfuck;

import com.keithstellyes.machines.shared.Location;
import com.keithstellyes.machines.shared.ParseUtil;
import com.keithstellyes.machines.shared.exception.NotImplementedException;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * A brainfuck virtual machine, internally it has its own instruction format that
 * looks like the normal language it is coded in.
 */
public class Machine extends com.keithstellyes.machines.shared.Machine {
    public final static int MEMORY_COUNT = 30000;
    public final static int REG_COUNT = 2;
    public final static int PROGRAM_COUNTER_REG = 0;
    public final static int DATA_PTR_REG = 1;

    /**
     * An internal instruction is 3 bytes:
     *
     * byte[0] = opcode same as the language it's coded in.
     * byte[2] byte[1] = the argument (loc to jump to, value to add/sub, etc.)
     * byte[3] is the DO_HALT
     */
    public final int INC_DATA_PTR = '>';
    public final int DEC_DATA_PTR = '<';
    public final int INC_VALUE = '+';
    public final int DEC_VALUE = '-';
    public final int OUT = '.';
    public final int IN = ',';
    public final int BRZ = '[';
    public final int BRNZ = ']';
    public final int HALT = 'H';

    private byte[] memory = new byte[MEMORY_COUNT];
    private int[] registers = new int[REG_COUNT];
    private boolean isHalted = false;
    private int[] program;

    public Machine(InputStream in, OutputStream out) {
        super(in, out);
    }

    public Machine() {
        super();
    }

    @Override
    public String getName() {
        return "Brainfuck Virtual Machine";
    }

    @Override
    public int getMemoryValue(int address) throws NotImplementedException {
        return memory[address];
    }

    @Override
    public void setMemoryValue(int address, int value) {
        memory[address] = (byte) value;
    }

    @Override
    public int getRegister(int address) {
        return registers[address];
    }

    @Override
    public void setRegister(int address, int value) {
        registers[address] = value;
    }

    @Override
    public int getValue(Location location) throws NotImplementedException {
        return location.isRegister()
                ? getRegister(location.getAddress()) : getMemoryValue(location.getAddress());
    }

    @Override
    public void setValue(Location location, int value) {
        int addr = location.getAddress();
        if(location.isRegister()) setRegister(addr, value);
        else setMemoryValue(addr, value);
    }

    /**
     * Note this parses an instruction in this machine's own internal format!
     * @param instruction
     * @return
     */
    @Override
    public Delta parseInstruction(int instruction) {
        Delta.Builder builder = new Delta.Builder();

        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(instruction);
        // java chars are two-bytes long
        char opcode = (char) byteBuffer.get(0);
        short argument = byteBuffer.getShort(1);
        int pc = registers[PROGRAM_COUNTER_REG];
        int newPc = pc + 1;
        int dataPtr = registers[DATA_PTR_REG];
        int newDataPtr = dataPtr;
        int dataAtDataPtr = memory[dataPtr];
        int sum;

        switch (opcode) {
            case DEC_DATA_PTR:
                argument *= -1;
            case INC_DATA_PTR:
                newDataPtr = (dataPtr + argument) % MEMORY_COUNT;
                break;
            case DEC_VALUE:
                argument *= -1;
            case INC_VALUE:
                sum = (dataAtDataPtr + argument) % 256;
                builder.setMemory(dataPtr, dataAtDataPtr, sum);
                break;
            case BRZ:
                if(dataAtDataPtr == 0) newPc = argument;
                break;
            case BRNZ:
                if(dataAtDataPtr != 0) newPc = argument;
                break;
            case IN:
                while(argument --> 0)
                builder.input(Location.newMemoryLocation(dataPtr));
                break;
            case OUT:
                while(argument --> 0)
                builder.output(memory[dataPtr]);
                break;
            case HALT:
                builder.halt();
                break;
        }

        builder.setRegister(PROGRAM_COUNTER_REG, pc, newPc);
        builder.setRegister(DATA_PTR_REG, dataPtr, newDataPtr);
        return builder.build();
    }

    @Override
    public void reset() {
        memory = new byte[MEMORY_COUNT];
        registers = new int[REG_COUNT];
        isHalted = false;
        program = null;
    }

    @Override
    public void halt() {
        isHalted = true;
    }

    @Override
    public void unhalt() {
        isHalted = false;
    }

    @Override
    public boolean isHalted() {
        return isHalted;
    }

    @Override
    public int getCurrentInstruction() {
        return program[registers[PROGRAM_COUNTER_REG]];
    }

    public void loadProgram(CharSequence charSequence) {
        Stack<Integer> returnStack = new Stack<>();
        program = new int[charSequence.length() + 1];
        int programIndex = 0;

        for(int i = 0; i < charSequence.length(); i++) {
            char c = charSequence.charAt(i);
            switch (c) {
                case INC_DATA_PTR:
                case DEC_DATA_PTR:
                case INC_VALUE:
                case DEC_VALUE:
                case IN:
                case OUT:
                    int newIndex = ParseUtil.greedyConsumeEqualChars(charSequence, i);
                    int argument = newIndex - i;
                    if(c == INC_VALUE || c == DEC_VALUE) argument %= 256;
                    i = newIndex - 1;
                    program[programIndex++] = buildInstruction(c, argument);
                    break;
                case BRZ: // [
                    returnStack.push(programIndex++);
                    break;
                case BRNZ: // ]
                    int iJumpTo = returnStack.pop();
                    program[programIndex++] = buildInstruction(c, iJumpTo);
                    program[iJumpTo] = buildInstruction(BRZ, programIndex - 1);
                    break;
                default: // ignore
                    break;
            }
        }

        program[programIndex] = buildInstruction(HALT, 0);
    }

    public static int buildInstruction(int opcode, int argument) {
        ByteBuffer buff = ByteBuffer.allocate(4);
        buff.put((byte)opcode);
        buff.putShort((short) argument);

        return buff.getInt(0);
    }
}
