package com.keithstellyes.machines.mano;

import com.keithstellyes.machines.shared.Location;
import com.keithstellyes.machines.shared.ParseUtil;
import com.keithstellyes.machines.shared.exception.NotImplementedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Machine extends com.keithstellyes.machines.shared.Machine {
    public static final int PROGRAM_COUNTER_REG = 0;
    public static final int ACC_REG = 1;
    public static final int FLAG_REG = 2;

    public static final int REG_COUNT = 3;
    private int[] registers = new int[REG_COUNT];
    private static final int CARRY_FLAG = 1;
    private static final int ENABLE_INTERRUPTS_FLAG = 2;

    public static final int MEMORY_COUNT = 4096;
    private short[] memory = new short[MEMORY_COUNT];

    public static final int CLA = 0b0111001100100000;
    public static final int CLE = 0b0111000110010000;
    public static final int CMA = 0b0111000011001000;
    public static final int CME = 0b0111000001100100;
    public static final int CIR = 0b0111000001010000;
    public static final int CIL = 0b0111000000101000;
    public static final int INC = 0b0111000000010100;
    public static final int SPA = 0b0111000000001010;
    public static final int SNA = 0b0111000000001000;
    public static final int SZA = 0b0111000000000100;
    public static final int SZE = 0b0111000000000010;
    public static final int HLT = 0b0111000000000001;
    public static final short INP = (short) 0b1111001100100000;
    public static final short OUT = (short) 0b1111000110010000;

    public static final int AND = 0 << 12;
    public static final int ADD = 1 << 12;
    public static final int LDA = 2 << 12;
    public static final int STA = 3 << 12;
    public static final int BUN = 4 << 12;
    public static final int BSA = 5 << 12;
    public static final int ISZ = 6 << 12;
    public static final short INDIRECT = (short) (1 << 15);

    private boolean isHalted = false;

    public Machine(InputStream in, OutputStream out) {
        super(in, out);
    }

    public Machine() {
        super();
    }

    @Override
    public String getName() {
        return "Mano Machine";
    }

    @Override
    public int readMemoryValue(int address) {
        return memory[address];
    }

    @Override
    public void writeMemory(int address, int value) {
        memory[address] = (short) value;
    }

    @Override
    public int readRegister(int address) {
        return registers[address];
    }

    @Override
    public void writeRegister(int address, int value) {
        registers[address] = (short) value;
    }

    @Override
    public void writeValue(Location location, int value) {
        if(location.isRegister()) {
            writeRegister(location.getAddress(), value);
        } else {
            writeMemory(location.getAddress(), value);
        }
    }

    @Override
    public void output(int value) {
        try {
            out.write(value);
        } catch (IOException e) {

        }
    }

    @Override
    public void input(Location location) {
        try {
            writeValue(location, in.read());
        } catch (IOException e) { }
    }

    @Override
    public Delta parseInstruction(int instruction) {
        if(isHalted) return Delta.EMPTY_DELTA;
        int bit15 = INDIRECT & instruction;
        int bits14To12 = ((0b111 << 12) & instruction) >> 12;
        int address = 0b111111111111 & instruction;
        int accValue = readRegister(ACC_REG);
        int newAccValue = accValue;
        int flagsValue = readRegister(FLAG_REG);
        int oldPc = readRegister(PROGRAM_COUNTER_REG);
        int newPc = oldPc + 1;
        int accBit15 = accValue & (1 << 15);

        boolean carry = false;
        Delta.Builder builder = new Delta.Builder();

        if(bits14To12 == 7) {
            switch (instruction) {
                case CLA:
                    builder.writeRegister(ACC_REG, accValue, 0);
                    break;
                case CLE:
                    builder.writeRegister(FLAG_REG, flagsValue, ParseUtil.unsetFlag(flagsValue, CARRY_FLAG));
                    break;
                case CMA:
                    newAccValue = ~accValue;
                    break;
                case CME:
                    builder.writeRegister(FLAG_REG, flagsValue, hasCarryFlag() ? ParseUtil.unsetFlag(flagsValue, CARRY_FLAG) : ParseUtil.setFlag(flagsValue, CARRY_FLAG));
                    break;
                case CIR:
                    if((accValue & 1) != 0) {
                        builder.writeRegister(FLAG_REG, flagsValue, ParseUtil.setFlag(flagsValue, CARRY_FLAG));
                    }
                    newAccValue = accValue >> 1;
                    break;
                case CIL:
                    if(accBit15 != 0) {
                        builder.writeRegister(FLAG_REG, flagsValue, ParseUtil.setFlag(flagsValue, CARRY_FLAG));
                    }
                    newAccValue = accValue << 1;
                    break;
                case INC:
                    newAccValue = accValue + 1;
                    break;
                case SPA:
                    if(accBit15 == 0) {
                        newPc++;
                    }
                    break;
                case SNA:
                    if(accBit15 != 0) {
                        newPc++;
                    }
                    break;
                case SZA: // SZA
                    if(accValue == 0) {
                        newPc++;
                    }
                    break;
                case SZE: // SZE
                    if(!hasCarryFlag()) {
                        newPc++;
                    }
                    break;
                case HLT: // HLT
                    builder.halt();
                    break;
                case INP: // INP
                    builder.input(Location.newRegisterLocation(ACC_REG));
                    break;
                case OUT: // OUT
                    builder.output(accValue);
                    break;
                default:
                    break;
            }
        } else {
            // instructions dealing with memory
            if(bit15 != 0) {
                // set address to indirect
                address = readMemoryValue(address);
            }
            switch (bits14To12 << 12) {
                case AND:
                    newAccValue = accValue & readMemoryValue(address);
                    break;
                case ADD:
                    newAccValue = accValue + readMemoryValue(address);
                    if((newAccValue & 0x10000) != 0) {
                        builder.writeRegister(FLAG_REG, flagsValue, ParseUtil.setFlag(flagsValue, CARRY_FLAG));
                    }
                    break;
                case LDA:
                    newAccValue = readMemoryValue(address);
                    break;
                case STA:
                    builder.writeMemory(address, readMemoryValue(address), accValue);
                    break;
                case BSA: // SWITCH-CASE FALLTHROUGH INTENTIONAL
                    builder.writeMemory(address, readMemoryValue(address), newPc);
                case BUN:
                    newPc = address;
                    break;
                case ISZ:
                    newAccValue = accValue + 1;
                    if((newAccValue & 0xFFFF) == 0) {
                        newPc = address;
                    }
                    break;
            }
        }

        builder.writeRegister(PROGRAM_COUNTER_REG, oldPc, newPc);
        builder.writeRegister(ACC_REG, accValue, newAccValue & 0xFFFF);
        return builder.build();
    }

    @Override
    public void halt() {
        isHalted = true;
    }

    @Override
    public boolean isHalted() {
        return isHalted;
    }

    @Override
    public int getCurrentInstruction() {
        return memory[registers[PROGRAM_COUNTER_REG]];
    }

    public boolean hasCarryFlag() {
        return ParseUtil.hasFlag(readRegister(FLAG_REG), CARRY_FLAG);
    }

    public void loadProgram(short[] program) {
        for(int i = 0; i < MEMORY_COUNT; i++) {
            memory[i] = program[i];
        }
    }
}
