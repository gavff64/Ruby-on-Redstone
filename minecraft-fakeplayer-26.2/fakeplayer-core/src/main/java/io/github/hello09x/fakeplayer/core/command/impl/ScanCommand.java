package io.github.hello09x.fakeplayer.core.command.impl;

import com.google.inject.Singleton;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.fakeplayer.core.util.DebugColors;
import io.github.hello09x.fakeplayer.core.util.JsonBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

/**
 * 输出假人周围有限数量的实体与方块信息 (JSON), 供外部程序 (例如 RCON 机器人) 解析
 * <p>输出是有上限的, 不会一次性输出所有方块/实体的坐标.
 * 每个方块/实体带有一个 {@code color} 字段, 与 {@code /fp debug} 的粒子渲染颜色一一对应.
 * 玩家执行时还会额外收到一份彩色文本摘要</p>
 */
@Singleton
public class ScanCommand extends AbstractCommand {

    /**
     * 视线检测距离
     */
    private final static int LOOKING_AT_RANGE = 8;

    public void scan(@NotNull CommandSender sender, @NotNull CommandArguments args) throws WrapperCommandSyntaxException {
        var fake = super.getFakeplayer(sender, args);
        int radius = (int) args.getOptional("radius").orElse(16);
        int limit = (int) args.getOptional("limit").orElse(10);

        var loc = fake.getLocation();

        var all = fake.getWorld().getNearbyEntities(loc, radius, radius, radius);
        var entities = all.stream()
                .filter(e -> !e.getUniqueId().equals(fake.getUniqueId()))
                .sorted(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(loc)))
                .limit(limit)
                .toList();

        var lookingAt = fake.getTargetBlockExact(LOOKING_AT_RANGE);
        var feet = loc.getBlock();
        var below = feet.getRelative(BlockFace.DOWN);

        var json = JsonBuilder.obj()
                .key("pos").pos(loc.getX(), loc.getY(), loc.getZ())
                .key("dimension").value(loc.getWorld().getKey().toString())
                .key("looking_at").raw(lookingAt == null ? "null" : blockJson(lookingAt))
                .key("block_at_feet").raw(blockJson(feet))
                .key("block_below").raw(blockJson(below))
                .key("entities_total").value(Math.max(0, all.size() - 1))
                .key("entities").array(entities, e -> entityJson(e, loc))
                .toString();
        sender.sendMessage(text(json));

        // 玩家执行时, 额外发送一份彩色摘要, 颜色与 /fp debug 的渲染一致
        if (sender instanceof Player) {
            var summary = Component.empty()
                    .append(text("looking: ", GRAY))
                    .append(lookingAt == null ? text("null", GRAY) : coloredBlock(lookingAt))
                    .append(text("  feet: ", GRAY))
                    .append(coloredBlock(feet))
                    .append(text("  below: ", GRAY))
                    .append(coloredBlock(below));
            sender.sendMessage(summary);

            if (!entities.isEmpty()) {
                var entityLine = Component.empty().append(text("entities: ", GRAY));
                for (var it = entities.iterator(); it.hasNext(); ) {
                    var e = it.next();
                    var color = DebugColors.of(e.getType().getKey().toString());
                    var distance = Math.round(Math.sqrt(e.getLocation().distanceSquared(loc)) * 100) / 100.0;
                    entityLine = entityLine.append(text(e.getType().getKey().getKey(), TextColor.color(color.asRGB())))
                                           .append(text(" " + distance + "m", WHITE));
                    if (it.hasNext()) {
                        entityLine = entityLine.append(text(", ", GRAY));
                    }
                }
                sender.sendMessage(entityLine);
            }
        }
    }

    private @NotNull Component coloredBlock(@NotNull Block b) {
        var color = DebugColors.of(b.getType().getKey().toString());
        return text(b.getType().getKey().getKey(), TextColor.color(color.asRGB()));
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
