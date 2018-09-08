package com.keithstellyes.machines.shared;

public final class MachineUtil {
    public static final int NEVER_GIVE_UP = 0;
    public static final boolean DID_NOT_GIVE_UP = true;
    public static final boolean DID_GIVE_UP = false;

    /**
     * true if it did not give up, false otherwise
     * @param machine
     * @param giveUpCounter
     * @return
     */
    public static boolean runMachineUntilHalt(Machine machine, int giveUpCounter) {
        if(giveUpCounter == NEVER_GIVE_UP) {
            while (!machine.isHalted()) {
                applyCurrentInstruction(machine);
            }

            return DID_NOT_GIVE_UP;
        } else {
            while(true) {
                if(machine.isHalted()) {
                    return DID_NOT_GIVE_UP;
                } else if(giveUpCounter-- == 0) {
                    return DID_GIVE_UP;
                } else {
                    applyCurrentInstruction(machine);
                }
            }
        }
    }

    public static void applyCurrentInstruction(Machine machine) {
        int instruction = machine.getCurrentInstruction();
        Machine.Delta delta = machine.parseInstruction(instruction);
        delta.apply(machine);
    }
}
