package com.keithstellyes.machines.shared;

import com.keithstellyes.machines.shared.exception.NotImplementedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * A primitive von Neumann machine, designed for machines with a series of registers
 * and a series of cells in memory.
 *
 * This should suit well as a parent class for varied machines like LC-3, Little Man Computer,
 * MIX, MMIX, among many others. These are the machines in-mind with the design of
 * this class.
 *
 * Brainfuck I am still thinking how best to implement it.
 *
 * As I see patterns between Machine implementations, I'll try to carry them up to this
 * parent class.
 *
 * In addition, it should be fairly trivial to get a Turing Machine to extend this well,
 * or even machines with multiple I/O's.
 *
 * It does not have its own arrays or other data structures as what makes sense
 * will vary depending on the machine, some may have varying bitwidths and sizes.
 *
 */
public abstract class Machine {
    protected final InputStream in;
    protected final OutputStream out;

    public Machine(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    public Machine() {
        this.in = System.in;
        this.out = System.out;
    }

    public int getMemoryValue(int address) throws NotImplementedException {
        return 0;
    }

    public void setMemoryValue(int address, int value) { }

    public int getRegister(int address) {
        return 0;
    }

    public void setRegister(int address, int value) { }

    public int getValue(Location location) throws NotImplementedException {
        throw new NotImplementedException();
    }

    public void setValue(Location location, int value) { }

    public void output(int value) {
        try {
            out.write(value);
        } catch (IOException e) { }
    }

    public void input(Location location) {
        try {
            setValue(location, in.read());
        } catch(IOException e) { }
    }

    public String getName() {
        return "Unimplemented";
    }

    /**
     * Creates a Delta that can then
     * be applied t othis machine based off of a parsed instruction. This will usually
     * be based on the current state of this machine.
     *
     * Stepping through an
     * @return
     */
    public Delta parseInstruction(int instruction) {
        return null;
    }

    public int getCurrentInstruction() {
        return 0;
    }

    public void reset() { }
    public void halt() { }
    public void unhalt() { }
    public boolean isHalted() { return false; }

    /**
     * A delta is a series of actions that can be rolled back.
     * This is done as an atomic transaction, and useful for stepping forwards and
     * backwards.
     *
     * Currently, deltas are not cached, and in the future this would be nice to have.
     *
     * This is also returned by the InstructionParser.
     */
    public static class Delta {
        private final List<Consumer<Machine>> applyFunctions;
        private final List<Consumer<Machine>> unapplyFunctions;

        public final static Delta EMPTY_DELTA = new Delta(Collections.emptyList(),
                Collections.emptyList());

        private Delta(List<Consumer<Machine>> applyFunctions,
                      List<Consumer<Machine>> unapplyFunctions) {
            this.applyFunctions = applyFunctions;
            this.unapplyFunctions = unapplyFunctions;
        }

        public void apply(Machine machine) {
            for(Consumer<Machine> action : applyFunctions) {
                action.accept(machine);
            }
        }

        public void unapply(Machine machine) {
            for(Consumer<Machine> action : unapplyFunctions) {
                action.accept(machine);
            }
        }

        public static class Builder {
            private final static Consumer<Machine> noop = (m) -> {};

            private final List<Consumer<Machine>> applyFunctions = new ArrayList<>();
            private final List<Consumer<Machine>> unapplyFunctions = new ArrayList<>();

            public Builder setValue(Location location, int oldValue, int newValue) {
                if(oldValue == newValue) return this;
                applyFunctions.add((m) -> m.setValue(location, newValue));
                unapplyFunctions.add((m) -> m.setValue(location, oldValue));

                return this;
            }

            public Builder setMemory(int address, int oldValue, int newValue) {
                if(oldValue == newValue) return this;
                applyFunctions.add((m) -> m.setMemoryValue(address, newValue));
                unapplyFunctions.add((m) -> m.setMemoryValue(address, oldValue));

                return this;
            }

            public Builder setRegister(int register, int oldValue, int newValue) {
                if(oldValue == newValue) return this;
                applyFunctions.add((m) -> m.setRegister(register, newValue));
                unapplyFunctions.add((m) -> m.setRegister(register, oldValue));

                return this;
            }

            /**
             * Currently, there is no undo of I/O¸and redoing will
             * merely once again either write, or read again.
             */
            public Builder output(int value) {
                applyFunctions.add((m) -> m.output(value));
                unapplyFunctions.add(noop);

                return this;
            }

            public Builder input(Location location) {
                applyFunctions.add((m) -> m.input(location));
                unapplyFunctions.add(noop);

                return this;
            }

            public Builder halt() {
                applyFunctions.add(Machine::halt);
                unapplyFunctions.add(Machine::unhalt);

                return this;
            }

            public Delta build() {
                return new Delta(applyFunctions, unapplyFunctions);
            }
        }
    }
}
