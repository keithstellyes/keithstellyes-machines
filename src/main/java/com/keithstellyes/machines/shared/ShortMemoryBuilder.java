package com.keithstellyes.machines.shared;

/**
 * A handy builder for setting up memory for programmatically generating programs.
 *
 * This will be useful for things like assemblers, or testing of classes.
 *
 * Currently, this assumes that data can be conveniently placed at the very end (as marked
 * by maxValue)
 */
public class ShortMemoryBuilder {
    private final int minValue;
    private final int maxValue;
    private final short[] memory;
    private int programCounter;
    private int datumCounter;

    /**
     * A simple constructor where program is at beginning and data at very end.
     * @param minValue
     * @param maxValue
     */
    public ShortMemoryBuilder(int minValue, int maxValue, int memorySize) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        memory = new short[memorySize];
        programCounter = minValue;
        datumCounter = maxValue;
    }

    public int addInstruction(short instruction) {
        memory[programCounter++] = instruction;

        return programCounter - 1;
    }

    public int addInstruction(int instruction) {
        return addInstruction((short) instruction);
    }

    public int addDatum(short datum) {
        memory[datumCounter--] = datum;

        return datumCounter + 1;
    }

    public int addDatum(int datum) {
        return addDatum((short) datum);
    }

    public int addData(short[] data) {
        for(int i = data.length - 1; i >= 0; i--) {
            memory[datumCounter--] = data[i];
        }

        return datumCounter + 1;
    }

    public int addData(char[] data) {
        for(int i = data.length - 1; i >= 0; i--) {
            memory[datumCounter--] = (short) data[i];
        }

        return datumCounter + 1;
    }

    public int addData(String data) {
        return addData(data.toCharArray());
    }

    public int set(int address, short value) {
        memory[address] = value;

        return address;
    }

    public int set(int address, int value) {
        return set(address, (short) value);
    }

    public short[] getMemory() {
        return memory;
    }
}
