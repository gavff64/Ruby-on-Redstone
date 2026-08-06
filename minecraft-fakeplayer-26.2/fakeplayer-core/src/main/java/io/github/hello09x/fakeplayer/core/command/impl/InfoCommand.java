package io.github.hello09x.fakeplayer.core.command.impl;

import com.google.inject.Singleton;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.fakeplayer.core.util.JsonBuilder;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;

/**
 * 输出假人自身状态的 JSON, 供外部程序 (例如 RCON 机器人) 解析
 */
@Singleton
public class InfoCommand extends AbstractCommand {

    public void info(@NotNull CommandSender sender, @NotNull CommandArguments args) throws WrapperCommandSyntaxException {
        var fake = super.getFakeplayer(sender, args);
        var loc = fake.getLocation();
        var velocity = fake.getVelocity();
        var json = JsonBuilder.obj()
                .key("name").value(fake.getName())
                .key("pos").pos(loc.getX(), loc.getY(), loc.getZ())
                .key("velocity").pos(velocity.getX(), velocity.getY(), velocity.getZ())
                .key("dimension").value(loc.getWorld().getKey().toString())
                .key("yaw").value(Math.round(loc.getYaw() * 100) / 100.0)
                .key("pitch").value(Math.round(loc.getPitch() * 100) / 100.0)
                .key("health").value(Math.round(fake.getHealth() * 100) / 100.0)
                .key("hunger").value(fake.getFoodLevel())
                .key("on_ground").value(fake.isOnGround())
                .key("sneaking").value(fake.isSneaking())
                .key("sprinting").value(fake.isSprinting())
                .key("held_item").value(fake.getInventory().getItemInMainHand().getType().getKey().toString())
                .key("tick").value(bridge.fromPlayer(fake).getTickCount())
                .toString();
        sender.sendMessage(text(json));
    }

}
