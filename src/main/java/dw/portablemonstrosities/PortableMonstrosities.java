package dw.portablemonstrosities;

// import com.hypixel.hytale.builtin.adventure.farming.component.CoopResidentComponent;
// import com.hypixel.hytale.builtin.adventure.farming.states.CoopBlock;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import dw.portablemonstrosities.commands.PkmnHudTestCommand;
import dw.portablemonstrosities.commands.PkmnCommand.PkmnCommand;
import dw.portablemonstrosities.components.FaintedPkmnComponent;
import dw.portablemonstrosities.components.PkmnCoopBlock;
import dw.portablemonstrosities.components.PkmnStatsComponent;
import dw.portablemonstrosities.components.PkmnStorageComponent;
import dw.portablemonstrosities.events.InitPlayerStatsEvent;
import dw.portablemonstrosities.interactions.CaptureWildCreatureInteraction;
import dw.portablemonstrosities.interactions.ChanceInteraction;
import dw.portablemonstrosities.interactions.CreatureScannerInteraction;
import dw.portablemonstrosities.interactions.DamagePkmnEntityInteraction;
import dw.portablemonstrosities.interactions.FillFluidContainerInteraction;
import dw.portablemonstrosities.interactions.HasOwnerInteraction;
import dw.portablemonstrosities.interactions.HealAllPkmnInteraction;
import dw.portablemonstrosities.interactions.ReturnActivePkmnInteraction;
import dw.portablemonstrosities.interactions.ReturnFaintedPkmnInteraction;
import dw.portablemonstrosities.interactions.SetCreatureNameplateInteraction;
import dw.portablemonstrosities.interactions.SpawnPkmnTombstoneInteraction;
import dw.portablemonstrosities.interactions.StayCapturedInteraction;
import dw.portablemonstrosities.interactions.TogglePkmnHUDInteraction;
import dw.portablemonstrosities.interactions.UseCaptureOrbInteraction;
import dw.portablemonstrosities.systems.AddNetworkIdToMyEntitySystem;

import javax.annotation.Nonnull;

public class PortableMonstrosities extends JavaPlugin {

    private static PortableMonstrosities instance = null;

    private ComponentType<EntityStore, PkmnStatsComponent> pkmnStatsComponent;
    private ComponentType<EntityStore, FaintedPkmnComponent> faintedPkmnComponent;
    private ComponentType<EntityStore, PkmnStorageComponent> pkmnStorageComponent;
    private ComponentType<ChunkStore, PkmnCoopBlock> pkmnCoopBlockComponent;
    
    // private ComponentType<EntityStore, PkmnCaptureMetadata> pkmnCaptureMetadata;


    public PortableMonstrosities(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        instance = this;
        // -- Commands ----------
        // this.getCommandRegistry().registerCommand(new ExampleCommand("test1", "An example command"));
        this.getCommandRegistry().registerCommand(new PkmnHudTestCommand("pkmnhud", "shows a test hud"));
        // this.getCommandRegistry().registerCommand(new PkmnPartyHudCommand("pkmnparty", "asdf"));
        this.getCommandRegistry().registerCommand(new PkmnCommand());

        // -- Events ----------
        // this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, ExampleEvent::onPlayerReady);
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, InitPlayerStatsEvent::onPlayerReady);

        // -- Codecs (Interactions)  ----------
        this.getCodecRegistry(Interaction.CODEC).register("Chance", ChanceInteraction.class, ChanceInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("DamagePkmnEntity", DamagePkmnEntityInteraction.class, DamagePkmnEntityInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("SetCreatureNameplate", SetCreatureNameplateInteraction.class, SetCreatureNameplateInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("CreatureScan", CreatureScannerInteraction.class, CreatureScannerInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("HasOwner", HasOwnerInteraction.class, HasOwnerInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("TogglePkmnHUD", TogglePkmnHUDInteraction.class, TogglePkmnHUDInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("HealAllPkmn", HealAllPkmnInteraction.class, HealAllPkmnInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("StayCaptured", StayCapturedInteraction.class, StayCapturedInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("CaptureWildCreature", CaptureWildCreatureInteraction.class, CaptureWildCreatureInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("UseCaptureOrb", UseCaptureOrbInteraction.class, UseCaptureOrbInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("ReturnPkmn", ReturnActivePkmnInteraction.class, ReturnActivePkmnInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("ReturnFaintedPkmn", ReturnFaintedPkmnInteraction.class, ReturnFaintedPkmnInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("SpawnPkmnTombstone", SpawnPkmnTombstoneInteraction.class, SpawnPkmnTombstoneInteraction.CODEC);

        // this.getCodecRegistry(Interaction.CODEC).register("Bench_Dough_Mixer_Interaction", DoughMixerInteraction.class, DoughMixerInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("FillFluidContainer", FillFluidContainerInteraction.class, FillFluidContainerInteraction.CODEC);


        // -- Components ----------
        this.pkmnStatsComponent = this.getEntityStoreRegistry()
                .registerComponent(PkmnStatsComponent.class,"pkmnStatsComponent", PkmnStatsComponent.CODEC);
        this.faintedPkmnComponent = this.getEntityStoreRegistry()
                .registerComponent(FaintedPkmnComponent.class,"faintedPkmnComponent", FaintedPkmnComponent.CODEC);
        this.pkmnStorageComponent = this.getEntityStoreRegistry()
                .registerComponent(PkmnStorageComponent.class,"pkmnStorageComponent", PkmnStorageComponent.CODEC);

        this.pkmnCoopBlockComponent = this.getChunkStoreRegistry()
                .registerComponent(PkmnCoopBlock.class,"PkmnCoop", PkmnCoopBlock.CODEC);

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
    public ComponentType<ChunkStore, PkmnCoopBlock> getPkmnCoopBlockComponentType() {
        return this.pkmnCoopBlockComponent;
    }
    // public ComponentType<EntityStore, PokmnCaptureMetadata> getPkmnCaptureMetadataType() {
    //     return this.pkmnCaptureMetadata;
    // }

    public static PortableMonstrosities instance() {
        return instance;
    }

}

