package dev.hytalemodding;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import dev.hytalemodding.commands.PkmnHudTestCommand;
import dev.hytalemodding.components.FaintedPkmnComponent;
import dev.hytalemodding.components.PkmnStatsComponent;
import dev.hytalemodding.components.PkmnStorageComponent;
import dev.hytalemodding.interactions.CaptureWildCreatureInteraction;
import dev.hytalemodding.interactions.StayCapturedInteraction;
import dev.hytalemodding.interactions.HasOwnerInteraction;
import dev.hytalemodding.interactions.ReturnActivePkmnInteraction;
import dev.hytalemodding.interactions.ReturnFaintedPkmnInteraction;
import dev.hytalemodding.interactions.SpawnPkmnTombstoneInteration;
import dev.hytalemodding.interactions.CreatureScannerInteraction;
import dev.hytalemodding.interactions.DamagePkmnEntityInteraction;
import dev.hytalemodding.interactions.FillFluidContainerInteraction;
import dev.hytalemodding.interactions.SetCreatureNameplateInteraction;
import dev.hytalemodding.interactions.UseCaptureOrbInteraction;
import dev.hytalemodding.systems.AddNetworkIdToMyEntitySystem;

import javax.annotation.Nonnull;

public class PortableMonstrosities extends JavaPlugin {

    private static PortableMonstrosities instance = null;

    private ComponentType<EntityStore, PkmnStatsComponent> pkmnStatsComponent;
    private ComponentType<EntityStore, FaintedPkmnComponent> faintedPkmnComponent;
    private ComponentType<EntityStore, PkmnStorageComponent> pkmnStorageComponent;
    // private ComponentType<EntityStore, PkmnCaptureMetadata> pkmnCaptureMetadata;


    public PortableMonstrosities(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        instance = this;
        // -- Commands ----------
        // this.getCommandRegistry().registerCommand(new ExampleCommand("test1", "An example command"));
        this.getCommandRegistry().registerCommand(new PkmnHudTestCommand("pkmnhud", "shows a test hud using multiplehud mod"));

        // -- Events ----------
        // this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, ExampleEvent::onPlayerReady);

        // -- Codecs (Interactions)  ----------
        this.getCodecRegistry(Interaction.CODEC).register("DamagePkmnEntity", DamagePkmnEntityInteraction.class, DamagePkmnEntityInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("HasOwner", HasOwnerInteraction.class, HasOwnerInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("StayCaptured", StayCapturedInteraction.class, StayCapturedInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("CaptureWildCreature", CaptureWildCreatureInteraction.class, CaptureWildCreatureInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("UseCaptureOrb", UseCaptureOrbInteraction.class, UseCaptureOrbInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("ReturnPkmn", ReturnActivePkmnInteraction.class, ReturnActivePkmnInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("ReturnFaintedPkmn", ReturnFaintedPkmnInteraction.class, ReturnFaintedPkmnInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("SpawnPkmnTombstone", SpawnPkmnTombstoneInteration.class, SpawnPkmnTombstoneInteration.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("SetCreatureNameplate", SetCreatureNameplateInteraction.class, SetCreatureNameplateInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("CreatureScan", CreatureScannerInteraction.class, CreatureScannerInteraction.CODEC);
        // this.getCodecRegistry(Interaction.CODEC).register("Bench_Dough_Mixer_Interaction", DoughMixerInteraction.class, DoughMixerInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("FillFluidContainer", FillFluidContainerInteraction.class, FillFluidContainerInteraction.CODEC);


        // -- Components ----------
        this.pkmnStatsComponent = this.getEntityStoreRegistry()
                .registerComponent(PkmnStatsComponent.class,"pkmnStatsComponent", PkmnStatsComponent.CODEC);
        this.faintedPkmnComponent = this.getEntityStoreRegistry()
                .registerComponent(FaintedPkmnComponent.class,"faintedPkmnComponent", FaintedPkmnComponent.CODEC);
        this.pkmnStorageComponent = this.getEntityStoreRegistry()
                .registerComponent(PkmnStorageComponent.class,"pkmnStorageComponent", PkmnStorageComponent.CODEC);
        // this.pkmnCaptureMetadata = this.getEntityStoreRegistry()
        //         .registerComponent(PkmnCaptureMetadata.class,"kmnCapture", PkmnCaptureMetadata.CODEC);


        // -- Systems ----------
        this.getEntityStoreRegistry().registerSystem(new AddNetworkIdToMyEntitySystem());
    }

    public ComponentType<EntityStore, PkmnStatsComponent> getPkmnStatsComponentType() {
        return this.pkmnStatsComponent;
    }
    public ComponentType<EntityStore, FaintedPkmnComponent> getFaintedPkmnComponent() {
        return this.faintedPkmnComponent;
    }
    public ComponentType<EntityStore, PkmnStorageComponent> getPkmnStorageComponentType() {
        return this.pkmnStorageComponent;
    }
    // public ComponentType<EntityStore, PokmnCaptureMetadata> getPkmnCaptureMetadataType() {
    //     return this.pkmnCaptureMetadata;
    // }

    public static PortableMonstrosities instance() {
        return instance;
    }

}

