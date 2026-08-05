package io.github.hello09x.fakeplayer.core.command.impl;

import com.google.inject.Singleton;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.util.DebugColors;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;

/**
 * 假人调试渲染开关: 开启后, 假人周围的方块与实体会以彩色粒子高亮,
 * 颜色与 {@code /fp scan} 输出中的颜色一一对应
 */
@Singleton
public class HighlightCommand extends AbstractCommand {

    /**
     * 渲染间隔 (tick)
     */
    private final static long RENDER_INTERVAL = 10;

    /**
     * 渲染可见范围 (方块)
     */
    private final static double VIEW_RANGE = 48;

    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    public void highlight(@NotNull CommandSender sender, @NotNull CommandArguments args) throws WrapperCommandSyntaxException {
        var fake = super.getFakeplayer(sender, args);
        var id = fake.getUniqueId();

        var task = this.tasks.remove(id);
        if (task != null && !task.isCancelled()) {
            task.cancel();
            sender.sendMessage(translatable("fakeplayer.command.highlight.disabled", text(fake.getName(), GOLD)));
            return;
        }

        var running = new BukkitRunnable() {
            @Override
            public void run() {
                if (!fake.isOnline()) {
                    this.cancel();
                    tasks.remove(id);
                    return;
                }
                render(fake);
            }
        };
        this.tasks.put(id, running.runTaskTimer(Main.getInstance(), 0, RENDER_INTERVAL));
        sender.sendMessage(translatable("fakeplayer.command.highlight.enabled", text(fake.getName(), GOLD)));
    }

    private void render(@NotNull Player fake) {
        var loc = fake.getLocation();
        var viewers = loc.getWorld().getPlayers().stream()
                .filter(p -> p.getLocation().getWorld().equals(loc.getWorld()))
                .filter(p -> p.getLocation().distanceSquared(loc) <= VIEW_RANGE * VIEW_RANGE)
                .toList();
        if (viewers.isEmpty()) {
            return;
        }

        // 假人自身标记
        ring(viewers, loc.getX(), loc.getY() + 0.1, loc.getZ(), 0.6, Color.fromRGB(0xffd700));

        // 周围实体 (与 /fp scan 相同的范围与排序)
        var entities = loc.getWorld().getNearbyEntities(loc, 16, 16, 16).stream()
                .filter(e -> !e.getUniqueId().equals(fake.getUniqueId()))
                .sorted(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(loc)))
                .limit(10)
                .toList();
        for (var entity : entities) {
            var p = entity.getLocation();
            ring(
                    viewers,
                    p.getX(),
                    p.getY() + 0.2,
                    p.getZ(),
                    0.7,
                    DebugColors.of(entity.getType().getKey().toString())
            );
        }

        // 方块 (与 /fp scan 相同的三个方块)
        var lookingAt = fake.getTargetBlockExact(8);
        var feet = loc.getBlock();
        var below = feet.getRelative(BlockFace.DOWN);
        if (lookingAt != null) {
            box(viewers, lookingAt, DebugColors.of(lookingAt.getType().getKey().toString()));
        }
        box(viewers, feet, DebugColors.of(feet.getType().getKey().toString()));
        box(viewers, below, DebugColors.of(below.getType().getKey().toString()));
    }

    private static void ring(@NotNull List<Player> viewers, double x, double y, double z, double radius, @NotNull Color color) {
        var options = new Particle.DustOptions(color, 1.0F);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            var px = x + Math.cos(a) * radius;
            var pz = z + Math.sin(a) * radius;
            for (var viewer : viewers) {
                viewer.spawnParticle(Particle.DUST, px, y, pz, 1, 0, 0, 0, options);
            }
        }
    }

    private static void box(@NotNull List<Player> viewers, @NotNull Block block, @NotNull Color color) {
        var options = new Particle.DustOptions(color, 0.9F);
        double x = block.getX();
        double y = block.getY();
        double z = block.getZ();
        double[][] corners = {
                {0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1},
                {0, 1, 0}, {1, 1, 0}, {1, 1, 1}, {0, 1, 1}
        };
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        for (var edge : edges) {
            var a = corners[edge[0]];
            var b = corners[edge[1]];
            for (double t = 0; t <= 1.0; t += 0.5) {
                var px = x + a[0] + (b[0] - a[0]) * t + 0.02;
                var py = y + a[1] + (b[1] - a[1]) * t + 0.02;
                var pz = z + a[2] + (b[2] - a[2]) * t + 0.02;
                for (var viewer : viewers) {
                    viewer.spawnParticle(Particle.DUST, px, py, pz, 1, 0, 0, 0, options);
                }
            }
        }
    }

}
