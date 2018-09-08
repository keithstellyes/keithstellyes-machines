package com.keithstellyes.machines.lmc;

import com.keithstellyes.machines.shared.Location;
import com.keithstellyes.machines.shared.Opcode;
import com.keithstellyes.machines.shared.ParseUtil;
import com.keithstellyes.machines.shared.exception.NotImplementedException;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class Machine extends com.keithstellyes.machines.shared.Machine {
    public final static int HLT = 0;
    public final static int ADD = 1;
    public final static int SUB = 2;
    public final static int STA = 3;
    public final static int LDA = 5;
    public final static int BRA = 6;
    public final static int BRZ = 7;
    public final static int BRP = 8;
    public final static int IO = 9;

    public final static int IO_ARG_IN = 1;
    public final static int IO_ARG_OUT = 2;

    public final static int MAX_VALUE = 999;
    public final static int MIN_VALUE = 0;

    public enum MATH_MODE { OVERFLOW, SATURATION };

    private final static int REGISTER_COUNT = 3;
    private final static int MEMORY_COUNT = 100;
    public final static int PROGRAM_COUNTER_REG = 0;
    public final static int ACCUMULATOR_REG = 1;
    public final static int FLAG_REG = 2;


    private final static int N_FLAG = 0x01;
    private final static int HALT_FLAG = 0x02;

    private MATH_MODE mathMode = MATH_MODE.OVERFLOW;

    private final static Map<Integer, Opcode> OPCODE_MAP;

    static {
        Opcode.IntToOpcodeMapBuilder builder = new Opcode.IntToOpcodeMapBuilder();
        builder.addOpcode("HLT", HLT);
        builder.addOpcode("ADD", ADD);
        builder.addOpcode("SUB", SUB);
        builder.addOpcode("STA", STA);
        builder.addOpcode("LDA", LDA);
        builder.addOpcode("BRA", BRA);
        builder.addOpcode("BRZ", BRZ);
        builder.addOpcode("BRP", BRP);
        builder.addOpcode("IO", IO);

        OPCODE_MAP = builder.build();
    }

    private short[] memory = new short[MEMORY_COUNT];
    private short[] registers = new short[REGISTER_COUNT];
    private boolean isHalted = false;

    public Machine() {
        super();
    }

    public Machine(InputStream in, OutputStream out) {
        super(in, out);
    }

    @Override
    public String getName() {
        return "Little Man Computer";
    }

    @Override
    public int getMemoryValue(int address) {
        return memory[address];
    }

    @Override
    public void setMemoryValue(int address, int value) {
        memory[address] = (short) value;
    }

    @Override
    public int getValue(Location location) {
        if(location.isRegister()) {
            return registers[location.getAddress()];
        }

        return memory[location.getAddress()];
    }

    @Override
    public void setValue(Location location, int value) {
        if(location.isRegister()) {
            registers[location.getAddress()] = (short) value;
        } else {
            memory[location.getAddress()] = (short) value;
        }
    }

    @Override
    public Delta parseInstruction(int instruction) {
        if(isHalted()) return Delta.EMPTY_DELTA;

        int opcode = instruction / 100;
        int argument = instruction % 100;
        int acc = registers[ACCUMULATOR_REG];
        int pc = registers[PROGRAM_COUNTER_REG];
        int newPc = pc + 1;
        boolean doHalt = false;
        if(newPc >= MEMORY_COUNT) {
            doHalt = true;
            newPc = 99;
        }
        int valueAtArgument = memory[argument];
        int newAccValue = acc;
        int flags = registers[FLAG_REG];
        int newFlags = 0;

        Delta.Builder builder = new Delta.Builder();

        switch (opcode) {
            case HLT:
                doHalt = true;
                break;
            case ADD:
                if(acc + valueAtArgument <= MAX_VALUE) {
                    newAccValue = acc + valueAtArgument;
                } else {
                    if(mathMode == MATH_MODE.OVERFLOW) {
                        newAccValue = (acc + valueAtArgument) % (MAX_VALUE + 1);
                        newFlags = ParseUtil.setFlag(newFlags, N_FLAG);
                    } else {
                        newAccValue = MAX_VALUE;
                    }
                }
                break;
            case SUB:
                if(acc - valueAtArgument >= MIN_VALUE) {
                    newAccValue = acc - valueAtArgument;
                } else {
                    if(mathMode == MATH_MODE.OVERFLOW) {
                        newAccValue = MAX_VALUE + 1 + (acc - valueAtArgument);
                        newFlags = ParseUtil.setFlag(newFlags, N_FLAG);
                    } else {
                        newAccValue = MIN_VALUE;
                    }
                }
                break;
            case STA:
                builder.setMemory(argument, getMemoryValue(argument), acc);
                break;
            case LDA:
                builder.setRegister(ACCUMULATOR_REG, acc, getMemoryValue(argument));
                break;
            case BRA:
                newPc = argument;
                break;
            case BRZ:
                if(acc == 0) newPc = argument;
                break;
            case BRP:
                if(mathMode == MATH_MODE.OVERFLOW && ParseUtil.hasFlag(flags, N_FLAG)) newPc = argument;
                break;
            case IO:
                if(argument == IO_ARG_IN) {
                    builder.input(Location.newRegisterLocation(ACCUMULATOR_REG));
                } else {
                    builder.output(acc);
                }
                break;
        }

        builder.setRegister(PROGRAM_COUNTER_REG, pc, newPc);
        builder.setRegister(ACCUMULATOR_REG, acc, newAccValue);
        builder.setRegister(FLAG_REG, flags, newFlags);
        if(doHalt) {
            builder.halt();
        }

        return builder.build();
    }

    public void setMathMode(MATH_MODE mathMode) {
        this.mathMode = mathMode;
    }

    public MATH_MODE getMathMode() {
        return mathMode;
    }

    public static short buildInstruction(int opcode, int argument) {
        return (short) (opcode * 100 + argument);
    }

    public void loadProgram(short[] program) {
        for(int i = 0; i < MEMORY_COUNT; i++) {
            if(i < program.length) {
                memory[i] = program[i];
            } else {
                memory[i] = 0;
            }
        }
    }

    @Override
    public int getCurrentInstruction() {
        return memory[registers[PROGRAM_COUNTER_REG]];
    }

    @Override
    public void reset() {
        registers = new short[REGISTER_COUNT];
        memory = new short[MEMORY_COUNT];
        isHalted = false;
    }

    @Override
    public void halt() {
        registers[FLAG_REG] = (short) ParseUtil.setFlag(registers[FLAG_REG], HALT_FLAG);
    }

    @Override
    public boolean isHalted() {
        return ParseUtil.hasFlag(registers[FLAG_REG], HALT_FLAG);
    }

    @Override
    public int getRegister(int address) {
        return registers[address];
    }

    @Override
    public void setRegister(int address, int value) {
        registers[address] = (short) value;
    }
}
