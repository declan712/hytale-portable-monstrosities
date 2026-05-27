package dev.hytalemodding.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Backpack;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Hotbar;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Storage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;

import au.ellie.hyui.builders.GroupBuilder;
import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.builders.HyUIHud;
import au.ellie.hyui.builders.LabelBuilder;
import au.ellie.hyui.uiparser.UIParseResult;
import dev.hytalemodding.components.PkmnCaptureMetadata;

import dev.hytalemodding.ui.PkmnPartySlot;
import dev.hytalemodding.util.PkmnStatUtils;

// public class PkmnHudTestCommand extends AbstractTargetEntityCommand {
public class PkmnHudTestCommand extends AbstractPlayerCommand {

    private static final Map<String, List<PkmnPartySlot>> partyCache = new HashMap<>();

    private static final Map<String, HyUIHud> hudCache = new HashMap<>();
    private static final Map<String, HudBuilder> builderCache = new HashMap<>();

    public static final String HUD_KEY  = "PkmnParty";
    public static final int    MAX_SLOTS = 6;
    private static final String UI_FILE  = "PkmnParty.ui";

    // private static final String SLOT_HEALTHY = "Pokeball.png";
    private static final String SLOT_HEALTHY = "UI/Custom/Pages/Pokeball.png";
    // private static final String SLOT_GREY = "Pokeball_Grey.png";
    // private static final String SLOT_DEAD = "Pokeball_Dead.png";
    // private static final String SLOT_EMPTY = "Pokeball_None.png";
    private static final String SLOT_EMPTY = "UI/Custom/Pages/Pokeball_None.png";

    private static final int ROW_HEIGHT = 48;
    private static final int PADDING = 52;
    private static final int HUD_BASE_HEIGHT = 340;

    /// "*Pokeball[variant]_State_[state]"
    /// 
    /// Variants:
    ///         "Pokeball"
    ///         "Pokeball_Apricorn"
    ///         "Pokeball_Great"
    ///         "Pokeball_Ultra"
    /// 
    /// States:
    ///         "Full"
    ///         "Active"
    ///         "Fainted"
    /// 
    private static final List<String> validItems = List.of(
        "*Pokeball_State_Full",
        "*Pokeball_Apricorn_State_Full",
        "*Pokeball_Great_State_Full",
        "*Pokeball_Ultra_State_Full",

        "*Pokeball_State_Active",
        "*Pokeball_Apricorn_State_Active",
        "*Pokeball_Great_State_Active",
        "*Pokeball_Ultra_State_Active",
        
        "*Pokeball_State_Fainted",
        "*Pokeball_Apricorn_State_Fainted",
        "*Pokeball_Great_State_Fainted",
        "*Pokeball_Ultra_State_Fainted"
    );

    public PkmnHudTestCommand(String name, String description) {
        super(name,description);
    }


    private void updateSlots(
        @NonNullDecl HudBuilder hud,
        @NonNullDecl List<PkmnPartySlot> party,
        @NonNullDecl List<PkmnPartySlot> oldParty,
        @NonNullDecl String playerKey
    ){
        // HudBuilder hud = builderCache.getOrDefault(playerKey, initialBuilder);
        if(partiesEqual(oldParty, party)) return;
        logParty(party);
        final int partySize = party.size();
        final int oldPartySize = oldParty.size();

        if(partySize != oldPartySize){
            final int resize = PADDING+partySize*ROW_HEIGHT;
            // if (partySize==0) {
            //     hud.editElement(commands -> {commands.set("#Container.Visible",false);});
            //     LOGGER.atInfo().log("Hide");
            //     return;
            // } else if (oldPartySize == 0) {
            //     hud.editElement(commands -> {commands.set("#Container.Visible",true);});
            //     LOGGER.atInfo().log("Unhide");
            // }
            LOGGER.atInfo().log("Party size changed: "+String.valueOf(oldPartySize)+" => "+String.valueOf(partySize));

            LOGGER.atInfo().log("set height to "+String.valueOf(resize));

            hud.editElement(commands -> {
                commands.set("#Container.ResizerSize", HUD_BASE_HEIGHT-resize);
            });
        }

        for (var i = 1; i <= MAX_SLOTS; i++) {
            final int finalI = i;
            final String idPkmn   = "#Pkmn"+String.valueOf(finalI)+".Visible";
            final String idName   = "#Pkmn"+String.valueOf(finalI)+" #PkmnName.Text";
            final String idLevel  = "#Pkmn"+String.valueOf(finalI)+" #PkmnLevel.Text";
            final String idHpBar  = "#Pkmn"+String.valueOf(finalI)+" #HealthBar.Value";
            final String idHpText = "#Pkmn"+String.valueOf(finalI)+" #HealthText.Text";
            final String idIcon   = "#Pkmn"+String.valueOf(finalI)+" #PkmnIcon.AssetPath";
            final String idBall   = "#Balls["+String.valueOf(finalI-1)+"].AssetPath";
            // String idTab    = "#Tabs["+String.valueOf(finalI-1)+"].Icon.TexturePath";

            if (partySize<finalI) {
                if (oldPartySize>=finalI) {
                    hud.editElement(commands  -> {commands.set(idPkmn, false);})
                        .editElement(commands -> {commands.set(idBall, SLOT_EMPTY);});
                }
                continue;
            }else if (oldPartySize<finalI){
                hud.editElement(commands  -> {commands.set(idPkmn, true);})
                    .editElement(commands -> {commands.set(idBall, SLOT_HEALTHY);});
            }

            final PkmnPartySlot pkmn = party.get(finalI-1);

            PkmnPartySlot prev = PkmnPartySlot.of("",0,0,0,"","");
            if(oldPartySize>=finalI){
                prev = oldParty.get(finalI-1);
            }

            if(pkmn.equals(prev)){ continue; }

            LOGGER.atInfo().log("Slot changed: "+String.valueOf(finalI));


            if(!pkmn.name.equals(prev.name)){
                hud.editElement(commands  -> {commands.set(idName, pkmn.name);});
            }

            if (pkmn.level != prev.level) {
                final String level    = "Lv."+String.valueOf(pkmn.level);
                hud.editElement(commands -> {commands.set(idLevel, level);});
            }

            if(pkmn.currentHp != prev.currentHp || pkmn.maxHp != prev.maxHp) {
                final float percentHp = ((float)pkmn.currentHp)/((float)pkmn.maxHp);
                final String hpText   = String.valueOf(pkmn.currentHp)+"/"+String.valueOf(pkmn.maxHp);

                hud.editElement(commands -> {commands.set(idHpBar, percentHp);})
                    .editElement(commands -> {commands.set(idHpText, hpText);});
            }

            if(!pkmn.iconPath.equals(prev.iconPath)) {
                hud.editElement(commands -> {commands.set(idIcon, pkmn.iconPath);});
            }
        }
        partyCache.put(playerKey, party);
        // builderCache.put(playerKey,hud);
        return;
    }

    private void setSlots(
        @NonNullDecl HudBuilder hud,
        @NonNullDecl List<PkmnPartySlot> party
    ) {

        for (var i = 1; i <= MAX_SLOTS; i++) {
            int finalI = i;

            final String idPkmn   = "#Pkmn"+String.valueOf(finalI)+".Visible";
            final String idName   = "#Pkmn"+String.valueOf(finalI)+" #PkmnName.Text";
            final String idLevel  = "#Pkmn"+String.valueOf(finalI)+" #PkmnLevel.Text";
            final String idHpBar  = "#Pkmn"+String.valueOf(finalI)+" #HealthBar.Value";
            final String idHpText = "#Pkmn"+String.valueOf(finalI)+" #HealthText.Text";
            final String idIcon   = "#Pkmn"+String.valueOf(finalI)+" #PkmnIcon.AssetPath";
            final String idBall   = "#Balls["+String.valueOf(finalI-1)+"].AssetPath";
            // String idTab    = "#Tabs["+String.valueOf(finalI-1)+"].Icon.TexturePath";

            if (party.size()<finalI) {
                hud.editElement(commands  -> {commands.set(idPkmn, false);})
                    .editElement(commands -> {commands.set(idBall, SLOT_EMPTY);});
                continue;
            }else{
                hud.editElement(commands  -> {commands.set(idPkmn, true);})
                    .editElement(commands -> {commands.set(idBall, SLOT_HEALTHY);});
            }

            PkmnPartySlot pkmn = party.get(finalI-1);

            final String name     = pkmn.name;
            final String level    = "Lv."+String.valueOf(pkmn.level);
            final int currentHP   = pkmn.currentHp;
            final int maxHp       = pkmn.maxHp;
            final float percentHp = Math.max(0f, Math.min(1f, ((float)currentHP)/((float)maxHp) ));
            final String hpText   = String.valueOf(currentHP)+"/"+String.valueOf(maxHp);
            final String icon     = pkmn.iconPath;

            hud.editElement(commands  -> {commands.set(idName, name);})
                .editElement(commands -> {commands.set(idLevel, level);})
                .editElement(commands -> {commands.set(idHpBar, percentHp);})
                .editElement(commands -> {commands.set(idHpText, hpText);})
                .editElement(commands -> {commands.set(idIcon, icon);});
            
            // Hide the whole slot panel when not in use
            // cmd.set("#PkmnSlot" + i + ".Visibility", hasSlot ? "Visible" : "Hidden");

            // float  hpRatio  = slot.maxHp > 0 ? (float) slot.currentHp / slot.maxHp : 0f;
            // // Clamp just in case
            // hpRatio = Math.max(0f, Math.min(1f, hpRatio));
        }
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext context, 
            @NonNullDecl Store<EntityStore> store, 
            @NonNullDecl Ref<EntityStore> ref, 
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world
        ) {
            Ref<EntityStore> sender = context.senderAsPlayerRef();
            if (sender == null || !sender.isValid()) return;

            final String playerKey = playerRef.toString();

            Player player = store.getComponent(sender,Player.getComponentType());
            if(player==null) return;
            HudManager hudManager = player.getHudManager();
            var customHuds = hudManager.getCustomHuds();
            if(customHuds != null){
                LOGGER.atInfo().log("CustomHuds: ");
                for (CustomUIHud customHud : customHuds.values()) {
                    LOGGER.atInfo().log("    - "+customHud.getKey());
                }

            }
            

            HyUIHud existingHud = hudCache.getOrDefault(playerKey,null);
            if(existingHud!=null){
                existingHud.remove();
                hudCache.put(playerKey,null);
                return;
            }

            Hotbar   hotbar   = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
            Storage  storage  = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
            Backpack backpack = store.getComponent(ref, InventoryComponent.Backpack.getComponentType());
            List<PkmnPartySlot> initialParty = buildPartyFromInventory(store,ref,world,hotbar,storage,backpack);
            
            // HudBuilder hud = HudBuilder.detachedHud()
            HudBuilder hud = HudBuilder.hudForPlayer(playerRef)
                .fromUIFile("Pages/"+UI_FILE)
                .enablePersistentElementEdits(true)
                .withRefreshRate(2000);

            setSlots(hud,initialParty);

            hud.onRefresh(hyUIHud->{
                // var time = world.getTick();
                // hyUIHud.editById("#Pkmn1 #PkmnName",LabelBuilder.class, e -> {
                //     e.withText("counter: "+String.valueOf(time));
                // }).updatePage(true);


                world.execute(()->{
                    List<PkmnPartySlot> oldParty = partyCache.getOrDefault(playerKey, new ArrayList<PkmnPartySlot>());
                    List<PkmnPartySlot> newParty = buildPartyFromInventory(store,ref,world,hotbar,storage,backpack);
                    updateSlots(hud, newParty, oldParty, playerKey);

                    hyUIHud.update(hud);
                    hyUIHud.refreshOrRerender(true,false);
                    
                    // hud.updateExisting(hyUIHud);
                    // context.sendMessage(Message.raw("Try to update: "+hyUIHud.getKey()));
                    // hyUIHud.
                    // context.sendMessage(Message.raw("slots updated"));
                    // context.sendMessage(Message.raw("."));
                });
                hyUIHud.update(hud);
                hyUIHud.refreshOrRerender(true,false);

                // context.sendMessage(Message.raw("hud updated"));
            });
            builderCache.put(playerKey, hud);
            HyUIHud huh = hud.show();
            // .show(playerRef);
            context.sendMessage(Message.raw("HUD Key = "+huh.getKey()));
            hudCache.put(playerKey,huh);
            // context.sendMessage(Message.raw(huh.name));
    }


    private static List<PkmnPartySlot> buildPartyFromInventory(
        @NonNullDecl Store<EntityStore> store,
        @NonNullDecl Ref<EntityStore> playerRef,
        @NonNullDecl World world,
        InventoryComponent.Hotbar hotbar,
        InventoryComponent.Storage storage,
        InventoryComponent.Backpack backpack
    ) {
        List<PkmnPartySlot> party = new ArrayList<PkmnPartySlot>();

        if(playerRef == null) {
            LOGGER.atInfo().log("Error: player ref is NULL");
            return party;
        }
        if (hotbar   != null                    ) collectFromContainer(hotbar.getInventory(),   party, world);
        if (storage  != null && party.size() < 6) collectFromContainer(storage.getInventory(),  party, world);
        if (backpack != null && party.size() < 6) collectFromContainer(backpack.getInventory(), party, world);
        return party;
    }


    private static void collectFromContainer(
        @NonNullDecl ItemContainer container,
        @NonNullDecl List<PkmnPartySlot> party,
        @NonNullDecl World world
    ) {
        container.forEach((slot, itemStack) -> {
            if(validItems.contains(itemStack.getItemId())){
                PkmnPartySlot partySlot = tryExtractSlot(itemStack, world);
                if (partySlot != null) party.add(partySlot);
                if (party.size() >= 6) return;
            }
        });
    }

    private static PkmnPartySlot tryExtractSlot(
        ItemStack item, 
        @NonNullDecl World world
    ) {
        
        if (item == null) return null;

        final PkmnCaptureMetadata captureMeta = item.getFromMetadataOrNull("PkmnCapture", PkmnCaptureMetadata.CODEC);
        if (captureMeta == null) return null;

        final CapturedNPCMetadata npcMeta = item.getFromMetadataOrNull("CapturedEntity", CapturedNPCMetadata.CODEC);

        // skip empty/uncaptured balls
        final String status = captureMeta.getNpcStatus();
        if (status == null) return null;
        final boolean isActive  = "Active".equals(status);
        final boolean isFainted = "Fainted".equals(status);
        final boolean isHealthy = "Healthy".equals(status);
        if (!isActive && !isFainted && !isHealthy) return null;

        String roleId = null;

        if(isActive) {
            roleId = captureMeta.getRoleId();
        }else{
            if(npcMeta==null) return null;
            roleId = npcMeta.getNpcNameKey();
        }
        if(roleId == null) return null;

        String name   = PkmnStatUtils.displayNameOf(roleId);
        // Prefer nickname if set
        final String nickname = captureMeta.getNickname();
        if (nickname != null && !nickname.isBlank()) name = nickname;

        String wildRole = roleId
            .replace("_Tamed", "")
            .replace("Tamed_", "");
        String icon = "Icons/ModelsGenerated/"+wildRole+".png";

        if (!isActive){
            return PkmnPartySlot.of(
                name,
                captureMeta.getLevel(),
                captureMeta.getCurrentHp(),
                captureMeta.getMaxHp(),
                status,
                icon
            );
        }

        final String npcUuid = captureMeta.getNpcEntityUuid();
        if(npcUuid==null) return null;

        Ref<EntityStore> ref = world.getEntityRef(UUID.fromString(npcUuid));
        if (ref == null || !ref.isValid()) return null;

        Store<EntityStore> store = ref.getStore();
        if(store == null) return null;

        if(!store.isInThread()){
            LOGGER.atInfo().log("Error: Store not in thread");
            return null;
        }

        Map<String, Object>  currentState = PkmnStatUtils.getState(store, ref);
        if (currentState == null) return null;

        var level   = currentState.get("LVL");
        // var exp     = currentState.get("EXP");
        var hp      = currentState.get("HP");
        var maxHp   = currentState.get("HP_MAX");
        // var role    = currentState.get("ROLEID");

        if(level.getClass() == null) level = (int) captureMeta.getLevel();
        if(hp.getClass()    == null) hp = (int) captureMeta.getCurrentHp();
        if(maxHp.getClass() == null) maxHp = (int) captureMeta.getMaxHp();

        return PkmnPartySlot.of(
            name+"*",
            (int) level,
            (int) hp,
            (int) maxHp,
            status,
            icon
        );

    }

    private static boolean partiesEqual(List<PkmnPartySlot> a, List<PkmnPartySlot> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            final PkmnPartySlot pkmna = a.get(i);
            final PkmnPartySlot pkmnb = b.get(i);
            if (!pkmna.equals(pkmnb)) return false;
        }
        // logParty(a);
        // logParty(b);
        return true;
    }

    private static void logParty(List<PkmnPartySlot> party){
        LOGGER.atInfo().log("Party ["+String.valueOf(party.size())+"]:");
        for (int i = 0; i < party.size(); i++) {
            final PkmnPartySlot pkmn = party.get(i);
            LOGGER.atInfo().log("["+String.valueOf(i)+"] - "+pkmn.name);
            LOGGER.atInfo().log("    - Lvl "+String.valueOf(pkmn.level));
            LOGGER.atInfo().log("    - HP:"+String.valueOf(pkmn.currentHp)+"/"+String.valueOf(pkmn.maxHp));
            if(pkmn.fainted){
            LOGGER.atInfo().log("    - [FAINTED]");
            }else{
            LOGGER.atInfo().log("    - [ALIVE]");
            }
        }
    }

}
