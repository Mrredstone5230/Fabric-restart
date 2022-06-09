package ru.aiefu.fabricrestart;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ModCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher){
        dispatcher.register(CommandManager.literal("restart").requires(stack -> stack.hasPermissionLevel(4)).executes(context -> restart(context.getSource())));

        dispatcher.register(CommandManager.literal("restart-when").executes(context -> getTimeUntilRestart(context.getSource())));
        dispatcher.register(CommandManager.literal("memory-stat").requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.argument("type", StringArgumentType.string()).executes(context -> memoryStat(context.getSource(), StringArgumentType.getString(context,"type")))));
        dispatcher.register(CommandManager.literal("getTPS").executes(context -> getTPS(context.getSource())));
    }

    private static int restart(ServerCommandSource source){
        if (FabricRestart.rdata == null || !FabricRestart.rdata.isScriptEnabled()) {
            FabricRestart.registerShutdownHook();
        }
        source.getServer().stop(false);
        return 0;
    }

    private static int getTimeUntilRestart(ServerCommandSource source){
        if(FabricRestart.rdata != null) {
            source.sendFeedback(Text.literal("Restart time: " + LocalDateTime.
                    ofEpochSecond(FabricRestart.rdata.getRestartTime() / 1000, 0, OffsetDateTime.
                            now().getOffset()).format(DateTimeFormatter.ofPattern("HH:mm"))
            ), false);
            System.out.println(FabricRestart.rdata.getRestartTime());
        }
        else source.sendFeedback(Text.literal("Auto-restart is disabled"), false);
        return 0;
    }

    private static int memoryStat(ServerCommandSource source, String arg){
        MemoryMXBean mxMem = ManagementFactory.getMemoryMXBean();
        switch (arg) {
            case "offheap" -> {
                MemoryUsage memoryUsage = mxMem.getNonHeapMemoryUsage();
                long used = memoryUsage.getUsed();
                long committed = memoryUsage.getCommitted();
                com.sun.management.OperatingSystemMXBean sys = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                source.sendFeedback(Text.literal("Offheap Usage: "), false);
                source.sendFeedback(Text.literal("Used: " + formatBytesToReadable(used)), false);
                source.sendFeedback(Text.literal("Reserved by JVM: " + formatBytesToReadable(committed)), false);
                source.sendFeedback(Text.literal("Total memory usage exclude heap: " + formatBytesToReadable(sys.getCommittedVirtualMemorySize() - mxMem.getHeapMemoryUsage().getCommitted())), false);
            }
            case "heap" -> {
                MemoryUsage memoryUsage = mxMem.getHeapMemoryUsage();
                long used = memoryUsage.getUsed();
                long committed = memoryUsage.getCommitted();
                long max = memoryUsage.getMax();
                com.sun.management.OperatingSystemMXBean sys = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                source.sendFeedback(Text.literal("Heap Usage: "), false);
                source.sendFeedback(Text.literal("Used: " + formatBytesToReadable(used)), false);
                source.sendFeedback(Text.literal("Reserved by JVM: " + formatBytesToReadable(committed)), false);
                source.sendFeedback(Text.literal("Maximum Possible: " + formatBytesToReadable(max)), false);
                source.sendFeedback(Text.literal("Total JVM Process Consumption: " + formatBytesToReadable(sys.getCommittedVirtualMemorySize())), false);
            }
            case "system" -> {
                com.sun.management.OperatingSystemMXBean sys = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                long totalMemory = sys.getTotalMemorySize();
                long freeMemory = sys.getFreeMemorySize();
                source.sendFeedback(Text.literal("Memory: " + formatBytesToReadable(totalMemory - freeMemory) + "/" + formatBytesToReadable(totalMemory)), false);
                source.sendFeedback(Text.literal("Free Memory: " + formatBytesToReadable(freeMemory)), false);
            }
            default -> source.sendError(Text.literal("Wrong argument, available arguments are: heap, offheap, system"));
        }
        return 0;
    }

    private static int getTPS(ServerCommandSource source){
        String formatted = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(((ITPS)source.getServer()).getAverageTPS());
        source.sendFeedback(Text.literal("TPS: " + formatted), false);
        return 0;
    }

    private static final long K = 1024;
    private static final long M = K * K;
    private static final long G = M * K;

    private static String formatBytesToReadable(long bytes){
        final long[] dividers = new long[] {G, M, K, 1 };
        final String[] units = new String[] {"GB", "MB", "KB", "B" };
        if(bytes < 1){
            return "Unable to parse bytes to text";
        }
        for(int i = 0; i < dividers.length; i++){
            final long divider = dividers[i];
            if(bytes >= divider){
                return new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format((double) bytes / divider) + " " + units[i];
            }
        }
        return "Unable to parse bytes to text";
    }
}
