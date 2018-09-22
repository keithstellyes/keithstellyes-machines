package com.keithstellyes.machines.malbolge;

import com.keithstellyes.machines.shared.Location;
import com.keithstellyes.machines.shared.exception.NotImplementedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Machine extends com.keithstellyes.machines.shared.Machine {
    private final static int MEMORY_COUNT  = 59_049;
    private final static int REG_COUNT = 3;
    private final static int POW9 = new Double(Math.pow(3, 9)).intValue();
    public final static int TRIT_COUNT = 10;

    public final static int PROGRAM_COUNTER_REG = 0;
    public final static int ACC_REG = 1;
    public final static int DATA_PTR_REG = 2;

    public final static int OP_JMP = 4;
    public final static int OP_OUT = 5;
    public final static int OP_IN = 23;
    public final static int OP_ROTR = 39;
    public final static int OP_MOVDD = 40;
    public final static int OP_CRAZY = 62;
    public final static int OP_NOP = 68;
    public final static int OP_HALT = 81;

    public final static int EOF = MEMORY_COUNT - 1;
    public final static int TO_NL = '\r';

    private int[] memory = new int[MEMORY_COUNT];
    private int[] registers = new int[REG_COUNT];
    private boolean isHalted = false;

    public Machine(InputStream in, OutputStream out) {
        super(in, out);
    }

    public Machine() {
        super();
    }

    @Override
    public int readMemoryValue(int address) {
        return memory[address];
    }

    @Override
    public void writeMemory(int address, int value) {
        memory[address] = value;
    }

    @Override
    public int readRegister(int address) {
        return registers[address];
    }

    @Override
    public void writeRegister(int address, int value) {
        registers[address] = value;
    }

    @Override
    public void writeValue(Location location, int value) {
        if(location.isRegister()) {
            registers[location.getAddress()] = value;
        } else {
            memory[location.getAddress()] = value;
        }
    }

    @Override
    public String getName() {
        return "Malbolge Virtual Machine";
    }

    @Override
    public void input(Location location) {
        int read;
        try {
            read = in.read();
        } catch(IOException e) {
            halt();
            return;
        }
        if(read == EOF) {
            halt();
        } else if(read == TO_NL) {
            read = '\n';
        }

        if(location.isRegister()) {
            registers[location.getAddress()] = read;
        } else {
            memory[location.getAddress()] = read;
        }
    }

    /**
     * Hahaha....
     * @param instruction
     * @return
     */
    @Override
    public Delta parseInstruction(int instruction) {
        if(isHalted()) return Delta.EMPTY_DELTA;

        int newDataPtr = (registers[DATA_PTR_REG] + 1) % MEMORY_COUNT;
        int newPc = registers[PROGRAM_COUNTER_REG] + 1;
        instruction = (instruction + memory[instruction]) % 94;
        Delta.Builder builder = new Delta.Builder();
        int dataAtDataPtr = memory[registers[DATA_PTR_REG]];
        int instructionToEncrypt = registers[PROGRAM_COUNTER_REG];

        switch (instruction) {
            case OP_JMP:
                newPc = dataAtDataPtr;
                instructionToEncrypt = newPc - 1;
                break;
            case OP_OUT:
                builder.output(registers[ACC_REG]);
                break;
            case OP_IN:
                builder.input(Location.newRegisterLocation(ACC_REG));
                break;
            case OP_ROTR:
                int rotatedValue = ternaryRotate(dataAtDataPtr);
                builder.writeMemory(registers[DATA_PTR_REG],
                        dataAtDataPtr,
                        rotatedValue);
                builder.writeRegister(ACC_REG, registers[ACC_REG], rotatedValue);
                break;
            case OP_MOVDD:
                // (after every instruction, dataptr increments by one)
                newDataPtr = (dataAtDataPtr + 1) % MEMORY_COUNT;
                break;
            case OP_CRAZY:
                int result = crazy(dataAtDataPtr, registers[ACC_REG]);
                builder.writeMemory(registers[dataAtDataPtr], memory[registers[DATA_PTR_REG]],
                        result);
                builder.writeRegister(ACC_REG, registers[ACC_REG], result);
                break;
            case OP_HALT:
                builder.halt();
                break;

        }

        newPc %= MEMORY_COUNT;

        builder.writeRegister(DATA_PTR_REG, registers[DATA_PTR_REG], newDataPtr);
        builder.writeRegister(PROGRAM_COUNTER_REG, registers[PROGRAM_COUNTER_REG], newPc);
        builder.writeMemory(instructionToEncrypt, memory[instructionToEncrypt],
                encrypt(memory[instructionToEncrypt]));

        return builder.build();
    }

    public static byte[] toTernary(int n) {
        if(n == 0) {
            return new byte[TRIT_COUNT];
        }

        byte[] outputDigits = new byte[TRIT_COUNT];
        List<Byte> tempDigits = new ArrayList<>();

        int digitIndex = TRIT_COUNT - 1;

        while(n > 0) {
            int r = n % 3;
            n = n / 3;
            tempDigits.add((byte)r);
        }

        Collections.reverse(tempDigits);
        while(tempDigits.size() < 10) tempDigits.add(0, (byte)0);
        for(int i = 0; i < tempDigits.size(); i++)
            outputDigits[i] = tempDigits.get(i);

        return outputDigits;
    }

    public static int ternaryRotate(int n) {
        return POW9*(n%3) + n / 3;
    }

    public static int crazy(int a, int b) {
        byte[] aDigits = toTernary(a);
        byte[] bDigits = toTernary(b);
        byte[][] CRAZY_TABLE = {{1, 0, 0},
                                {1, 0, 2},
                                {2, 2, 1}};
        int result = 0;
        int multiplier = 1;

        for(int i = 0; i < TRIT_COUNT; i++) {
            result += CRAZY_TABLE[aDigits[i]][bDigits[i]] * multiplier;
            multiplier *= 3;
        }

        return result;
    }

    public static int encrypt(int n) {
        final String ENCRYPTION_TABLE = "9m<.TVac`uY*MK'X~xDl}REokN:#?G\"i@5z]&gqtyfr$(we4{WP)H-Zn,[%\\3dL+Q;>U!pJS72FhOA1CB6v^=I_0/8|jsb";
        return ENCRYPTION_TABLE.charAt(n % 94);
    }

    public void loadProgram(String program) {
        reset();
        int memIndex = 0;

        for(int i = 0; i < program.length(); i++) {
            memory[memIndex++] = program.charAt(i);
        }

        for(int i = memIndex; i < memory.length; i++) {
            memory[i] = crazy(memory[i - 2], memory[i - 1]);
        }
    }

    @Override
    public int getCurrentInstruction() {
        return memory[registers[PROGRAM_COUNTER_REG]];
    }

    @Override
    public void reset() {
        memory = new int[MEMORY_COUNT];
        registers = new int[REG_COUNT];
        isHalted = false;
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
}
