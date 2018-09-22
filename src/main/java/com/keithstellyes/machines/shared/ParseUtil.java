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

    public static int unsetFlag(int flags, int flag) {
        return flags & ~flag;
    }

    /**
     * This is a common parsing operation.
     *
     * Given a CharSequence (like a String, we can call this with Strings!),
     * this will start with an index, and then keep incrementing it for as long as it
     * stays the same. It returns the first index of the first next char that isn't
     * the same as the starting. Or, it will return the size of the string if all the
     * rest are the same as the starting char.
     *
     * If the user wants to count the repetitions, merely subtract this result from whatever
     * index it was given initially.
     * @param s
     * @param start
     * @return
     */
    public static int greedyConsumeEqualChars(CharSequence s, int start) {
        char startingChar = s.charAt(start);
        while(start < s.length() && s.charAt(start) == startingChar) {
            start++;
        }

        return start;
    }
}
