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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;

/**
 * 假人调试渲染开关: 开启后, 假人周围立方体内的所有非空气方块会以与
 * {@code /fp scan} 输出颜色一致的彩色粒子方框高亮, 实体以彩色圆环标记
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

    /**
     * 绘制完整方框的方块数量上限, 超出部分仅绘制中心点
     */
    private final static int BOX_LIMIT = 64;

    /**
     * 默认立方体半径 (与 /fp scan 一致)
     */
    private final static int DEFAULT_RADIUS = 2;

    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    private final Map<UUID, Integer> radii = new HashMap<>();

    /**
     * 切换高亮渲染
     */
    public void highlight(@NotNull CommandSender sender, @NotNull CommandArguments args) throws WrapperCommandSyntaxException {
        var fake = super.getFakeplayer(sender, args);
        this.toggle(fake, null, sender);
    }

    /**
     * 以指定半径开启高亮渲染
     */
    public void highlightWithRadius(@NotNull CommandSender sender, @NotNull CommandArguments args) throws WrapperCommandSyntaxException {
        var fake = super.getFakeplayer(sender, args);
        var radius = (Integer) args.get("radius");
        if (radius < 1 || radius > ScanCommand.MAX_RADIUS) {
            throw dev.jorel.commandapi.CommandAPI.failWithString(
                    "Radius must be between 1 and " + ScanCommand.MAX_RADIUS + " (got " + radius + ")"
            );
        }
        this.toggle(fake, radius, sender);
    }

    private void toggle(@NotNull Player fake, @Nullable Integer radius, @NotNull CommandSender sender) {
        var id = fake.getUniqueId();
        var task = this.tasks.remove(id);
        if (task != null && !task.isCancelled()) {
            if (radius == null) {
                // 已开启且未指定半径: 关闭
                task.cancel();
                sendDisabled(fake, sender);
                return;
            }
            // 已开启时指定半径: 使用新半径重新开启
            task.cancel();
        }

        if (radius != null) {
            this.radii.put(id, radius);
        }
        int renderRadius = this.radii.getOrDefault(id, DEFAULT_RADIUS);

        var running = new BukkitRunnable() {
            @Override
            public void run() {
                if (!fake.isOnline()) {
                    this.cancel();
                    tasks.remove(id);
                    return;
                }
                render(fake, renderRadius);
            }
        };
        this.tasks.put(id, running.runTaskTimer(Main.getInstance(), 0, RENDER_INTERVAL));
        sendEnabled(fake, renderRadius, sender);
    }

    private void sendEnabled(@NotNull Player fake, int radius, @NotNull CommandSender sender) {
        sender.sendMessage(translatable(
                "fakeplayer.command.highlight.enabled",
                text(fake.getName(), GOLD),
                text(radius, GOLD)
        ));
    }

    private void sendDisabled(@NotNull Player fake, @NotNull CommandSender sender) {
        sender.sendMessage(translatable(
                "fakeplayer.command.highlight.disabled",
                text(fake.getName(), GOLD)
        ));
    }

    private void render(@NotNull Player fake, int radius) {
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

        // 周围实体 (与 /fp scan 相同的半径与排序)
        var entities = loc.getWorld().getNearbyEntities(loc, radius, radius, radius).stream()
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

        // 三个特殊方块 (与 /fp scan 一致), 始终绘制完整方框
        var lookingAt = fake.getTargetBlockExact(8);
        var feet = loc.getBlock();
        var below = feet.getRelative(BlockFace.DOWN);
        if (lookingAt != null) {
            box(viewers, lookingAt, DebugColors.of(lookingAt.getType().getKey().toString()), 3);
        }
        box(viewers, feet, DebugColors.of(feet.getType().getKey().toString()), 3);
        box(viewers, below, DebugColors.of(below.getType().getKey().toString()), 3);

        // 立方体内所有非空气方块: 前 BOX_LIMIT 个绘制方框, 其余绘制中心点
        var center = loc.getBlock();
        var nonAir = new ArrayList<Block>((2 * radius + 1) * (2 * radius + 1) * (2 * radius + 1));
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    var b = center.getRelative(dx, dy, dz);
                    if (!b.getType().isAir()) {
                        nonAir.add(b);
                    }
                }
            }
        }
        nonAir.sort(Comparator.comparingDouble(b -> b.getLocation().distanceSquared(loc)));
        int boxes = Math.min(nonAir.size(), BOX_LIMIT);
        for (int i = 0; i < nonAir.size(); i++) {
            var b = nonAir.get(i);
            var color = DebugColors.of(b.getType().getKey().toString());
            if (i < boxes) {
                box(viewers, b, color, 2);
            } else {
                dot(viewers, b, color);
            }
        }
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

    /**
     * 方块边缘方框
     *
     * @param stepsPerEdge 每条边粒子数 (2 = 仅顶点, 3 = 顶点+中点)
     */
    private static void box(@NotNull List<Player> viewers, @NotNull Block block, @NotNull Color color, int stepsPerEdge) {
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
            for (int s = 0; s < stepsPerEdge; s++) {
                double t = (double) s / (stepsPerEdge - 1);
                var px = x + a[0] + (b[0] - a[0]) * t + 0.02;
                var py = y + a[1] + (b[1] - a[1]) * t + 0.02;
                var pz = z + a[2] + (b[2] - a[2]) * t + 0.02;
                for (var viewer : viewers) {
                    viewer.spawnParticle(Particle.DUST, px, py, pz, 1, 0, 0, 0, options);
                }
            }
        }
    }

    private static void dot(@NotNull List<Player> viewers, @NotNull Block block, @NotNull Color color) {
        var options = new Particle.DustOptions(color, 0.5F);
        var p = block.getLocation().add(0.5, 0.5, 0.5);
        for (var viewer : viewers) {
            viewer.spawnParticle(Particle.DUST, p.getX(), p.getY(), p.getZ(), 1, 0, 0, 0, options);
        }
    }

}
