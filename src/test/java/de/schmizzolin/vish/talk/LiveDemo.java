package de.schmizzolin.vish.talk;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class LiveDemo {
    private static final Linker LINKER = Linker.nativeLinker();

    public static void main(String[] args) throws Throwable {
    }

    //-- helper methods

    private static FunctionDescriptor desc(ValueLayout result, ValueLayout... args) {
        return result == null ? FunctionDescriptor.ofVoid(args) : FunctionDescriptor.of(result, args);
    }

}

