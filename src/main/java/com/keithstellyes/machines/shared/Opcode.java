package com.keithstellyes.machines.shared;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Opcode {
    private final String mnemonic;
    private final int opcode;

    public Opcode(String mnemonic, int opcode) {
        this.mnemonic = mnemonic;
        this.opcode = opcode;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public int getOpcode() {
        return opcode;
    }

    /**
     * Deprecating this since now that time as gone on it seems this data structure is worthless
     */
    @Deprecated
    public static class IntToOpcodeMapBuilder {
        private final Map<Integer, Opcode> integerOpcodeMap = new HashMap<>();
        public IntToOpcodeMapBuilder addOpcode(Opcode opcode) {
            integerOpcodeMap.put(opcode.opcode, opcode);

            return this;
        }

        public IntToOpcodeMapBuilder addOpcode(String mnemonic, int opcode) {
            return addOpcode(new Opcode(mnemonic, opcode));
        }

        public Map<Integer, Opcode> build() {
            return Collections.unmodifiableMap(integerOpcodeMap);
        }
    }
}
