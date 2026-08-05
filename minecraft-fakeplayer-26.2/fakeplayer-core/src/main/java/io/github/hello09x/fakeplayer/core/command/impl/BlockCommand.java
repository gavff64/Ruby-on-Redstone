package io.github.hello09x.fakeplayer.core.command.impl;

import com.google.inject.Singleton;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.fakeplayer.core.util.JsonBuilder;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static net.kyori.adventure.text.Component.text;

/**
 * 查询单个方块的类型 (JSON), 供外部程序 (例如 RCON 机器人) 解析
 */
@Singleton
public class BlockCommand extends AbstractCommand {

    public void block(@NotNull CommandSender sender, @NotNull CommandArguments args) throws WrapperCommandSyntaxException {
        var fake = super.getFakeplayer(sender, args);
        var pos = Objects.requireNonNull((Location) args.get("location"));
        var block = fake.getWorld().getBlockAt(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
        var json = JsonBuilder.obj()
                .key("block").value(block.getType().getKey().toString())
                .key("pos").pos(block.getX(), block.getY(), block.getZ())
                .key("world").value(block.getWorld().getKey().toString())
                .toString();
        sender.sendMessage(text(json));
    }

}
