package jtransc;

import jtransc.annotation.haxe.HaxeAddMembers;
import jtransc.annotation.haxe.HaxeMethodBody;

@HaxeAddMembers({
        "static private var __start = -1.0;",
        "static private function __stamp() { #if sys return Sys.time() * 1000; #else return Date.now().getTime(); #end }"
})
public class JTranscSystem {
    static long start = -1;

    @HaxeMethodBody("if (__start < 0) __start = __stamp(); return Std.int(__stamp() - __start);")
    static public int stamp() {
        if (start < 0) start = System.currentTimeMillis();
        return (int) (System.currentTimeMillis() - start);
    }
}
