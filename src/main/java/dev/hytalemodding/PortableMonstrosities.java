package dev.hytalemodding;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import dev.hytalemodding.components.PkmnStatsComponent;
import dev.hytalemodding.interactions.CaptureWildCreatureInteraction;
import dev.hytalemodding.interactions.StayCapturedInteraction;
import dev.hytalemodding.interactions.HasOwnerInteration;
import dev.hytalemodding.interactions.CreatureScannerInteration;
import dev.hytalemodding.interactions.FillFluidContainerInteraction;
import dev.hytalemodding.interactions.SetCreatureNameplateInteraction;
import dev.hytalemodding.interactions.UseCaptureOrbInteraction;
import javax.annotation.Nonnull;

public class PortableMonstrosities extends JavaPlugin {

    private static PortableMonstrosities instance = null;

    private ComponentType<EntityStore, PkmnStatsComponent> pkmnStatsComponent;
    // private ComponentType<EntityStore, PkmnCaptureMetadata> pkmnCaptureMetadata;


    public PortableMonstrosities(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        instance = this;
        // -- Commands ----------
        // this.getCommandRegistry().registerCommand(new ExampleCommand("test1", "An example command"));

        // -- Events ----------
        // this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, ExampleEvent::onPlayerReady);

        // -- Codecs (Interactions)  ----------
        this.getCodecRegistry(Interaction.CODEC).register("HasOwner", HasOwnerInteration.class, HasOwnerInteration.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("StayCaptured", StayCapturedInteraction.class, StayCapturedInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("CaptureWildCreature", CaptureWildCreatureInteraction.class, CaptureWildCreatureInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("UseCaptureOrb", UseCaptureOrbInteraction.class, UseCaptureOrbInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("SetCreatureNameplate", SetCreatureNameplateInteraction.class, SetCreatureNameplateInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("CreatureScan", CreatureScannerInteration.class, CreatureScannerInteration.CODEC);
        // this.getCodecRegistry(Interaction.CODEC).register("Bench_Dough_Mixer_Interaction", DoughMixerInteraction.class, DoughMixerInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("FillFluidContainer", FillFluidContainerInteraction.class, FillFluidContainerInteraction.CODEC);


        // -- Components ----------
        this.pkmnStatsComponent = this.getEntityStoreRegistry()
                .registerComponent(PkmnStatsComponent.class,"pkmnStatsComponent", PkmnStatsComponent.CODEC);
        // this.pkmnCaptureMetadata = this.getEntityStoreRegistry()
        //         .registerComponent(PkmnCaptureMetadata.class,"kmnCapture", PkmnCaptureMetadata.CODEC);


    }

    public ComponentType<EntityStore, PkmnStatsComponent> getPkmnStatsComponentType() {
        return this.pkmnStatsComponent;
    }
    // public ComponentType<EntityStore, PokmnCaptureMetadata> getPkmnCaptureMetadataType() {
    //     return this.pkmnCaptureMetadata;
    // }

    public static PortableMonstrosities instance() {
        return instance;
    }

}

