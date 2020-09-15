package dev.hanetzer.chlorine.common.config;

import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {
    /**
     * Client specific configuration - only loaded clientside from chlorine-client.toml
     */
    public static class Client {
        // quality
        public final ForgeConfigSpec.EnumValue<SodiumGameOptions.GraphicsQuality> cloudQuality;
        public final ForgeConfigSpec.EnumValue<SodiumGameOptions.GraphicsQuality> weatherQuality;
        public final ForgeConfigSpec.BooleanValue enableVignette;
        public final ForgeConfigSpec.BooleanValue enableFog;
        public final ForgeConfigSpec.BooleanValue enableClouds;
        public final ForgeConfigSpec.EnumValue<SodiumGameOptions.LightingQuality> smoothLighting;
        // advanced
        public final ForgeConfigSpec.EnumValue<SodiumGameOptions.ChunkRendererBackendOption> chunkRendererBackend;
        public final ForgeConfigSpec.BooleanValue animateOnlyVisibleTextures;
        public final ForgeConfigSpec.BooleanValue useAdvancedEntityCulling;
        public final ForgeConfigSpec.BooleanValue useParticleCulling;
        public final ForgeConfigSpec.BooleanValue useFogOcclusion;
        public final ForgeConfigSpec.BooleanValue useCompactVertexFormat;
        public final ForgeConfigSpec.BooleanValue useChunkFaceCulling;
        public final ForgeConfigSpec.BooleanValue useMemoryIntrinsics;
        public final ForgeConfigSpec.BooleanValue disableDriverBlacklist;

        Client(ForgeConfigSpec.Builder builder) {
            builder.comment("General Settings").push("quality");

            this.cloudQuality = builder
                    .comment("Controls the quality of rendered clouds in the sky.")
                    .translation("sodium.options.clouds_quality.name")
                    .defineEnum("cloud_quality", SodiumGameOptions.GraphicsQuality.DEFAULT);

            this.weatherQuality = builder
                    .comment("Controls the quality of rain and snow effects.")
                    .translation("sodium.options.weather_quality.name")
                    .defineEnum("weather_quality", SodiumGameOptions.GraphicsQuality.DEFAULT);

            this.enableVignette = builder
                    .comment("If enabled, a vignette effect will be rendered on the player's view. This is very unlikely to make a difference to frame rates unless you are fill-rate limited.")
                    .translation("sodium.options.vignette.name")
                    .define("enable_vignette", true);

            this.enableFog = builder
                    .comment("If enabled, a fog effect will be used for terrain in the distance. Disabling this option will not change fog effects used underwater or in the Nether.")
                    .translation("sodium.options.fog.name")
                    .define("enable_fog", true);

            this.enableClouds = builder
                    .comment("Controls whether or not clouds will be visible.")
                    .translation("options.renderClouds")
                    .define("enable_clouds", true);

            this.smoothLighting = builder
                    .comment("Controls the quality of smooth lighting effects.\n\nOff - No smooth lighting\nLow - Smooth block lighting only\nHigh (new!) - Smooth block and entity lighting")
                    .translation("options.ao")
                    .defineEnum("smooth_lighting", SodiumGameOptions.LightingQuality.HIGH);

            builder.pop();

            builder.comment("Advanced Settings").push("advanced");

            this.chunkRendererBackend = builder
                    .comment("Modern versions of OpenGL provide features which can be used to greatly reduce driver overhead when rendering chunks. You should use the latest feature set allowed by Sodium for optimal performance. If you're experiencing chunk rendering issues or driver crashes, try using the older (and possibly more stable) feature sets.")
                    .translation("sodium.options.chunk_renderer.name")
                    .defineEnum("chunk_renderer_backend", SodiumGameOptions.ChunkRendererBackendOption.GL20);

            this.animateOnlyVisibleTextures = builder
                    .comment("If enabled, only animated textures determined to be visible will be updated. This can provide a significant boost to frame rates on some hardware. If you experience issues with some textures not being animated, disable this option.")
                    .translation("sodium.options.animate_only_visible_textures.name")
                    .define("animate_only_visible_textures", true);

            this.useAdvancedEntityCulling = builder
                    .comment("If enabled, a secondary culling pass will be performed before attempting to render an entity. This additional pass takes into account the current set of visible chunks and removes entities which are not in any visible chunks.")
                    .translation("sodium.options.use_entity_culling.name")
                    .define("use_advanced_entity_culling", true);

            this.useParticleCulling = builder
                    .comment("If enabled, a secondary culling pass will be performed before attempting to render an entity. This additional pass takes into account the current set of visible chunks and removes entities which are not in any visible chunks.")
                    .translation("sodium.options.use_particle_culling.name")
                    .define("use_particle_culling", true);

            this.useFogOcclusion = builder
                    .comment("If enabled, chunks which are determined to be fully hidden by fog effects will be skipped during rendering. This will generally provide a modest improvement to the number of chunks rendered each frame, especially where fog effects are heavier (i.e. while underwater.)")
                    .translation("sodium.options.use_fog_occlusion.name")
                    .define("use_fog_occlusion", true);

            this.useCompactVertexFormat = builder
                    .comment("If enabled, a more compact vertex format will be used for chunk meshes which limits the precision of vertex attributes. This format will reduce graphics memory usage and bandwidth requirements by around 40%, but could cause z-fighting/flickering texture issues in some edge cases.")
                    .translation("sodium.options.use_compact_vertex_format.name")
                    .define("use_compact_vertex_format", true);

            this.useChunkFaceCulling = builder
                    .comment("If enabled, an additional culling pass will be performed on the CPU to determine which planes of a chunk mesh are visible. This can eliminate a large number of block faces very early in the rendering process, saving memory bandwidth and time on the GPU.")
                    .translation("sodium.options.use_chunk_face_culling.name")
                    .define("use_chunk_face_culling", true);

            this.useMemoryIntrinsics = builder
                    .comment("If enabled, special intrinsics will be used to speed up the copying of client memory in certain vertex-limited scenarios, such as particle and text rendering. This option only exists for debugging purposes and should be left enabled unless you know what you are doing.")
                    .translation("sodium.options.use_memory_intrinsics.name")
                    .define("use_memory_intrinsics", true);

            this.disableDriverBlacklist = builder
                    .comment("If selected, Sodium will ignore the built-in driver blacklist and enable options which are known to be broken with your system configuration. This might cause serious problems and should not be used unless you really do know better. The settings screen must be saved, closed, and re-opened after changing this option in order to reveal previously hidden options.")
                    .translation("sodium.options.disable_driver_blacklist.name")
                    .define("disable_driver_blacklist", false);
            builder.pop();
        }
    }

    public static final ForgeConfigSpec clientSpec;
    public static final Client CLIENT;

    static {
        final Pair<Client, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Client::new);
        clientSpec = specPair.getRight();
        CLIENT = specPair.getLeft();
    }
}
