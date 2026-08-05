package io.github.hello09x.fakeplayer.core.command.impl;

import com.google.inject.Singleton;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.CommandExecutor;
import io.github.hello09x.fakeplayer.core.Main;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Singleton
public class MoveCommand extends AbstractCommand {

    private final Map<UUID, BukkitTask> stopTasks = new HashMap<>();

    /**
     * 假人移动 (默认距离: 行走 1 秒, 冲刺 2 秒)
     */
    public CommandExecutor move(@Range(from = 0, to = 1) float forward, @Range(from = 0, to = 1) float strafing) {
        return (sender, args) -> this.move(sender, args, forward, strafing, null);
    }

    /**
     * 假人移动指定格数 (水平位移达到后自动停止)
     */
    public CommandExecutor moveBlocks(@Range(from = 0, to = 1) float forward, @Range(from = 0, to = 1) float strafing) {
        return (sender, args) -> {
            var blocks = args.getOptional("blocks");
            this.move(sender, args, forward, strafing, blocks.isPresent() ? (Integer) blocks.get() : null);
        };
    }

    private void move(@NotNull CommandSender sender, @NotNull CommandArguments args, float forward, float strafing, Integer blocks) throws WrapperCommandSyntaxException {
        var fake = getFakeplayer(sender, args);
        this.cancelMove(fake);

        var handle = bridge.fromPlayer(fake);
        float vel = fake.isSneaking() ? 0.3F : 1.0F;
        var fakeId = fake.getUniqueId();

        if (blocks == null) {
            // 默认行为: 朝指定方向移动一段时间后自动停止
            if (forward != 0.0F) {
                handle.setZza(vel * forward);
            }
            if (strafing != 0.0F) {
                handle.setXxa(vel * strafing);
            }

            var stopping = new BukkitRunnable() {
                @Override
                public void run() {
                    handle.setXxa(0);
                    handle.setZza(0);
                    var self = stopTasks.get(fakeId);
                    if (self != null && self.getTaskId() == this.getTaskId()) {
                        stopTasks.remove(fakeId);
                    }
                }
            };
            this.stopTasks.put(fakeId, stopping.runTaskLater(Main.getInstance(), fake.isSprinting() ? 40 : 20));
            return;
        }

        // 精确移动: 水平位移达到指定格数后停止
        var start = fake.getLocation().clone();
        var moving = new BukkitRunnable() {
            @Override
            public void run() {
                if (!fake.isOnline() || fake.isDead()) {
                    this.stopMoving();
                    return;
                }
                handle.setZza(forward * vel);
                handle.setXxa(strafing * vel);
                var loc = fake.getLocation();
                double dx = loc.getX() - start.getX();
                double dz = loc.getZ() - start.getZ();
                if (Math.sqrt(dx * dx + dz * dz) >= blocks) {
                    this.stopMoving();
                }
            }

            private void stopMoving() {
                handle.setXxa(0);
                handle.setZza(0);
                this.cancel();
                var self = stopTasks.get(fakeId);
                if (self != null && self.getTaskId() == this.getTaskId()) {
                    stopTasks.remove(fakeId);
                }
            }
        };
        this.stopTasks.put(fakeId, moving.runTaskTimer(Main.getInstance(), 0, 1));
    }

    /**
     * 取消假人正在执行的移动任务
     */
    public void cancelMove(@NotNull Player fake) {
        var task = this.stopTasks.remove(fake.getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
            var handle = bridge.fromPlayer(fake);
            handle.setXxa(0);
            handle.setZza(0);
        }
    }

}
