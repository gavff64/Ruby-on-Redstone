package io.github.hello09x.fakeplayer.core.command.impl;

import com.google.inject.Singleton;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.util.DebugColors;
import io.github.hello09x.fakeplayer.core.util.JsonBuilder;
import lombok.Lombok;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static net.kyori.adventure.text.Component.text;

/**
 * 输出假人周围的信息 (JSON), 供外部程序 (例如 RCON 机器人) 解析
 * <p>包含以假人为中心的立方体内的所有方块 (含空气) 坐标, 以及最近的实体列表.
 * 每个方块/实体带有一个确定性生成的 {@code color} 字段 (由方块/实体 id 哈希得出).
 * 完整 JSON 始终写入 {@code plugins/fakeplayer/scans/latest.json} (RCON 响应有 4096
 * 字符上限), 响应中返回摘要与文件路径</p>
 */
@Singleton
public class ScanCommand extends AbstractCommand {

    /**
     * 视线检测距离
     */
    private final static int LOOKING_AT_RANGE = 8;

    /**
     * 默认立方体半径 (半边长): 5x5x5 = 125 个方块
     */
    private final static int DEFAULT_RADIUS = 2;

    /**
     * 最大立方体半径: 13x13x13 = 2197 个方块
     */
    public final static int MAX_RADIUS = 6;

    public void scan(@NotNull CommandSender sender, @NotNull CommandArguments args) throws WrapperCommandSyntaxException {
        var fake = super.getFakeplayer(sender, args);
        int radius = (int) args.getOptional("radius").orElse(DEFAULT_RADIUS);
        int limit = (int) args.getOptional("limit").orElse(10);

        var loc = fake.getLocation();
        var center = loc.getBlock();

        // 实体: 立方体内的实体, 按距离取最近的 limit 个
        var all = fake.getWorld().getNearbyEntities(loc, radius, radius, radius);
        var entities = all.stream()
                .filter(e -> !e.getUniqueId().equals(fake.getUniqueId()))
                .sorted(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(loc)))
                .limit(limit)
                .toList();

        // 方块网格: (2r+1)^3, 确定性顺序 (y 从下到上, 每层 x 再 z)
        var grid = new ArrayList<Block>((2 * radius + 1) * (2 * radius + 1) * (2 * radius + 1));
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    grid.add(center.getRelative(dx, dy, dz));
                }
            }
        }

        var lookingAt = fake.getTargetBlockExact(LOOKING_AT_RANGE);
        var feet = loc.getBlock();
        var below = feet.getRelative(BlockFace.DOWN);

        // 完整 JSON 始终写入文件 (RCON 响应有 4096 字符上限, 且文件对机器人是稳定可靠的读取方式),
        // 响应中返回摘要 + 文件路径
        var full = head(fake, loc, radius, lookingAt, feet, below, all, entities, grid.size())
                .key("blocks").array(grid, this::blockJson)
                .toString();
        var file = writeScanFile(full);
        var response = head(fake, loc, radius, lookingAt, feet, below, all, entities, grid.size())
                .key("file").value(file.getAbsolutePath())
                .toString();
        sender.sendMessage(text(response));
    }

    private @NotNull JsonBuilder head(
            @NotNull org.bukkit.entity.Player fake,
            @NotNull Location loc,
            int radius,
            @org.jetbrains.annotations.Nullable Block lookingAt,
            @NotNull Block feet,
            @NotNull Block below,
            @NotNull java.util.Collection<Entity> all,
            @NotNull List<Entity> entities,
            int blocksTotal
    ) {
        return JsonBuilder.obj()
                .key("name").value(fake.getName())
                .key("pos").pos(loc.getX(), loc.getY(), loc.getZ())
                .key("dimension").value(loc.getWorld().getKey().toString())
                .key("radius").value(radius)
                .key("looking_at").raw(lookingAt == null ? "null" : blockJson(lookingAt))
                .key("block_at_feet").raw(blockJson(feet))
                .key("block_below").raw(blockJson(below))
                .key("entities_total").value(Math.max(0, all.size() - 1))
                .key("entities").array(entities, e -> entityJson(e, loc))
                .key("blocks_total").value(blocksTotal);
    }

    private @NotNull File writeScanFile(@NotNull String json) {
        var folder = new File(Main.getInstance().getDataFolder(), "scans");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Failed to create scan folder: " + folder);
        }
        var file = new File(folder, "latest.json");
        try {
            Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw Lombok.sneakyThrow(e);
        }
        return file;
    }

    private @NotNull String entityJson(@NotNull Entity e, @NotNull Location origin) {
        var pos = e.getLocation();
        var name = e.customName() == null
                ? e.getName()
                : PlainTextComponentSerializer.plainText().serialize(e.customName());
        Color color = DebugColors.of(e.getType().getKey().toString());
        return JsonBuilder.obj()
                .key("type").value(e.getType().getKey().toString())
                .key("name").value(name)
                .key("pos").pos(pos.getX(), pos.getY(), pos.getZ())
                .key("distance").value(Math.round(Math.sqrt(pos.distanceSquared(origin)) * 100) / 100.0)
                .key("color").value(DebugColors.hex(color))
                .toString();
    }

    private @NotNull String blockJson(@NotNull Block b) {
        Color color = DebugColors.of(b.getType().getKey().toString());
        return JsonBuilder.obj()
                .key("block").value(b.getType().getKey().toString())
                .key("pos").pos(b.getX(), b.getY(), b.getZ())
                .key("color").value(DebugColors.hex(color))
                .toString();
    }

}
