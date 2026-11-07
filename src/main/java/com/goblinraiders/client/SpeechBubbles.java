package com.goblinraiders.client;

import com.goblinraiders.entity.AbstractGoblin;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

/** Draws what a goblin is saying as wrapped text with a dark background above its head. */
final class SpeechBubbles {
    private static final int MAX_WIDTH = 150;
    private static final double MAX_DISTANCE_SQR = 24.0 * 24.0;

    static void render(AbstractGoblin goblin, PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                       EntityRenderDispatcher dispatcher, Font font) {
        String key = goblin.getSpeech();
        if (key.isEmpty() || dispatcher.distanceToSqr(goblin) > MAX_DISTANCE_SQR) {
            return;
        }
        List<FormattedCharSequence> lines = font.split(Component.translatable(key), MAX_WIDTH);
        double above = goblin.getBbHeight() + 0.55 + (goblin.shouldShowName() ? 0.3 : 0.0);

        poseStack.pushPose();
        poseStack.translate(0.0, above, 0.0);
        poseStack.mulPose(dispatcher.cameraOrientation());
        poseStack.scale(0.025F, -0.025F, 0.025F);
        Matrix4f matrix = poseStack.last().pose();
        int background = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.45F) * 255.0F) << 24;
        int lineHeight = font.lineHeight + 1;
        float top = -lines.size() * lineHeight;
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            float x = -font.width(line) / 2.0F;
            float y = top + i * lineHeight;
            // Like vanilla name tags: background with faint see-through text first, then the solid text on top
            // (one pass with both makes the glyphs z-fight with their own background).
            font.drawInBatch(line, x, y, 0x20FFF4D6, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, background, packedLight);
            font.drawInBatch(line, x, y, 0xFFFFF4D6, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, packedLight);
        }
        poseStack.popPose();
    }

    private SpeechBubbles() {
    }
}
