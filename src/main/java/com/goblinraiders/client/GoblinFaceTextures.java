package com.goblinraiders.client;

import com.goblinraiders.GoblinRaiders;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Goblin skins with an expression baked in: the skin with the eye and mouth overlays ({@code face/<skin>_<state>.png},
 * drawn by tools/textures/skins.py) painted over its face, made the first time a goblin pulls that face. The goblin is
 * then drawn once with that texture. Drawing the overlay as a second pass over the same head does not survive
 * Sodium and Iris, which batch entity rendering and let the plain face cover it.
 */
public final class GoblinFaceTextures {
    /** The head's front face in a 64x64 skin. */
    private static final int FACE_X = 8, FACE_Y = 8, FACE_SIZE = 8;
    private static final Map<String, ResourceLocation> BAKED = new HashMap<>();

    public static ResourceLocation skin(String skin) {
        return GoblinRaiders.id("textures/entity/goblin/" + skin + ".png");
    }

    /** The skin wearing the given expression (null parts keep the skin's own face). */
    public static ResourceLocation get(String skin, @Nullable String eyes, @Nullable String mouth) {
        if (eyes == null && mouth == null) {
            return skin(skin);
        }
        String key = skin + "/" + (eyes == null ? "plain" : eyes) + "_" + (mouth == null ? "plain" : mouth);
        return BAKED.computeIfAbsent(key, k -> bake(skin, eyes, mouth, k));
    }

    private static ResourceLocation bake(String skin, @Nullable String eyes, @Nullable String mouth, String key) {
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        try {
            NativeImage image = read(resources, skin(skin));
            for (String state : new String[] {eyes, mouth}) {
                if (state != null) {
                    try (NativeImage overlay = read(resources, GoblinRaiders.id("textures/entity/goblin/face/" + skin + "_" + state + ".png"))) {
                        for (int y = FACE_Y; y < FACE_Y + FACE_SIZE; y++) {
                            for (int x = FACE_X; x < FACE_X + FACE_SIZE; x++) {
                                int pixel = overlay.getPixelRGBA(x, y);
                                if (pixel >>> 24 != 0) {
                                    image.setPixelRGBA(x, y, pixel);
                                }
                            }
                        }
                    }
                }
            }
            ResourceLocation id = GoblinRaiders.id("goblin_face/" + key);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
            return id;
        } catch (IOException e) {
            GoblinRaiders.LOGGER.error("Cannot make the goblin face {}", key, e);
            return skin(skin);
        }
    }

    private static NativeImage read(ResourceManager resources, ResourceLocation location) throws IOException {
        try (InputStream in = resources.open(location)) {
            return NativeImage.read(in);
        }
    }

    /** Resource packs changed: the skins may have too, so the faces are baked anew. */
    public static void clear() {
        BAKED.values().forEach(Minecraft.getInstance().getTextureManager()::release);
        BAKED.clear();
    }

    private GoblinFaceTextures() {
    }
}
