package com.keithstellyes.machines.shared;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class Location {
    private final int register;
    private final int memory;
    private final boolean isRegister;
    private final static Map<Integer, Location> registerInstances = new HashMap<>();

    private Location(int register, int memory, boolean isRegister) {
        if(isRegister) {
            this.register = register;
            this.memory = 0;
        } else {
            this.register = 0;
            this.memory = memory;
        }

        this.isRegister = isRegister;
    }

    public static Location newRegisterLocation(int register) {
        if(!registerInstances.containsKey(register)) {
            registerInstances.put(register, new Location(register, 0, true));
        }

        return registerInstances.get(register);
    }

    public static Location newMemoryLocation(int memory) {
        return new Location(0, memory, false);
    }

    public boolean isRegister() {
        return isRegister;
    }

    public boolean isMemory() {
        return !isRegister;
    }

    public int getAddress() {
        if(isRegister) {
            return memory;
        }

        return register;
    }
}
