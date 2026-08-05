package io.github.hello09x.fakeplayer.core.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * 极简 JSON 输出工具, 用于生成机器可读的命令输出 (例如配合 RCON 使用的机器人)
 */
public final class JsonBuilder {

    private final StringBuilder sb = new StringBuilder("{");
    private boolean first = true;

    private JsonBuilder() {
    }

    public static @NotNull JsonBuilder obj() {
        return new JsonBuilder();
    }

    public @NotNull JsonBuilder key(@NotNull String key) {
        if (!first) {
            sb.append(',');
        }
        first = false;
        sb.append('"').append(escape(key)).append("\":");
        return this;
    }

    public @NotNull JsonBuilder value(@Nullable Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else {
            sb.append('"').append(escape(value.toString())).append('"');
        }
        return this;
    }

    /**
     * 追加一个坐标数组 [x, y, z]
     */
    public @NotNull JsonBuilder pos(double x, double y, double z) {
        sb.append('[')
          .append(Mth.round(x, 2)).append(',')
          .append(Mth.round(y, 2)).append(',')
          .append(Mth.round(z, 2))
          .append(']');
        return this;
    }

    /**
     * 追加一个 JSON 数组
     */
    public @NotNull <T> JsonBuilder array(@NotNull List<T> items, @NotNull Function<T, String> renderer) {
        sb.append('[');
        for (var it = items.iterator(); it.hasNext(); ) {
            var item = it.next();
            sb.append(renderer.apply(item));
            if (it.hasNext()) {
                sb.append(',');
            }
        }
        sb.append(']');
        return this;
    }

    /**
     * 追加原始 JSON 片段
     */
    public @NotNull JsonBuilder raw(@NotNull String json) {
        sb.append(json);
        return this;
    }

    @Override
    public @NotNull String toString() {
        return sb.append('}').toString();
    }

    private static @NotNull String escape(@NotNull String s) {
        var out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

}
