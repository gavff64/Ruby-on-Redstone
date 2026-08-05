package io.github.hello09x.fakeplayer.core.util;

import org.bukkit.Color;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * 调试渲染与扫描输出的颜色映射.
 * <p>颜色由方块/实体的 key 确定性生成, 因此扫描文本中的颜色与世界中高亮的颜色始终一致</p>
 */
public final class DebugColors {

    private static final List<Color> PALETTE = List.of(
            Color.fromRGB(0xe74c3c), // red
            Color.fromRGB(0xe67e22), // orange
            Color.fromRGB(0xf1c40f), // yellow
            Color.fromRGB(0x2ecc71), // green
            Color.fromRGB(0x3498db), // blue
            Color.fromRGB(0x9b59b6), // purple
            Color.fromRGB(0x1abc9c), // teal
            Color.fromRGB(0xe84393), // pink
            Color.fromRGB(0x95a5a6), // gray
            Color.fromRGB(0xd35400), // burnt orange
            Color.fromRGB(0x27ae60), // dark green
            Color.fromRGB(0x2980b9), // dark blue
            Color.fromRGB(0x8e44ad), // dark purple
            Color.fromRGB(0x16a085), // dark teal
            Color.fromRGB(0xc0392b), // dark red
            Color.fromRGB(0xf39c12)  // amber
    );

    private DebugColors() {
    }

    /**
     * 根据 key 获取确定性颜色
     *
     * @param key 方块或实体的 key, 例如 {@code minecraft:sand}
     * @return 颜色
     */
    public static @NotNull Color of(@NotNull String key) {
        int hash = key.hashCode() & 0x7fffffff;
        return PALETTE.get(hash % PALETTE.size());
    }

    /**
     * 颜色转 hex 字符串, 例如 {@code #e74c3c}
     */
    public static @NotNull String hex(@NotNull Color color) {
        return String.format(Locale.ROOT, "#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

}
