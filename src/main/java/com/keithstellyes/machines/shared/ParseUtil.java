package com.keithstellyes.machines.shared;

import java.util.HashMap;
import java.util.Map;

public class ParseUtil {
    public static Opcode parseOpcode(int instruction, Map<Integer, Opcode> instructionMap,
                              int opcodeStart, int opcodeEnd) {
        instruction <<= (32 - (opcodeEnd - opcodeStart));
        instruction >>>= (32 - opcodeEnd);

        return instructionMap.get(instruction);
    }

    public static boolean hasFlag(int flags, int flag) {
        return (flags & flag) != 0x0;
    }

    public static int setFlag(int flags, int flag) {
        return flags | flag;
    }
}
