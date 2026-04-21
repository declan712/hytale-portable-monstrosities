package dev.hytalemodding.components;

import java.util.HashMap;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;

import dev.hytalemodding.PortableMonstrosities;



/// when putting pkmn in PC, store the metadata in the player instead
/// when opening another PC, populate it with the players data
/// {
///     Boxes:[
///         [0]{
///             name: "Box 1"
///             contents: {
///                 0:(PkmnCaptureMetadata,CapturedNPCMetadata),
///             }
///         },
///         [1]{
///             ...
///         },
///         ...
///         [15]{
///             ...
///         }
///     ]
/// }
/// 
public class PkmnStorageComponent implements Component<EntityStore> {
    public  static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private HashMap<Integer,Box> boxes;
    private Integer maxBoxes = 16;
    private Integer slotsPerBox = 30;

    private class BoxSlot {
        public String name;
        public PkmnCaptureMetadata pkmnMetadata;
        public CapturedNPCMetadata npcMetadata;
        public String slotIcon;
    }

    private class Box {
        public String name;
        public HashMap<Integer,BoxSlot> slots;
    }

    public HashMap<Integer,BoxSlot> getBoxContents(Integer boxNumber){
        if(boxNumber>maxBoxes || boxNumber<=0){
            LOGGER.atInfo().log("Invalid box number: "+String.valueOf(boxNumber));
            return null;
        }
        Box box = boxes.get(boxNumber);
        if(box == null) {
            LOGGER.atInfo().log("Box is missing?? "+String.valueOf(boxNumber));
            return null;
        }
        return box.slots;
    }



    public static ComponentType<EntityStore, PkmnStorageComponent> getComponentType() {
        return PortableMonstrosities.instance().getPkmnStorageComponentType();
    }

    public PkmnStorageComponent() {
    }

    public PkmnStorageComponent(PkmnStorageComponent clone) {
        // baseStats = clone.baseStats;
        // evs = clone.evs;
        // ivs = clone.ivs;
        // nature = clone.nature;
        // nickname = clone.nickname;
        // ownerUuid = clone.ownerUuid;
        // level = clone.level;
        // experience = clone.experience;
        // type1 = clone.type1;
        // type2 = clone.type2;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new PkmnStorageComponent(this);
    }


    public static final BuilderCodec<PkmnStorageComponent> CODEC = BuilderCodec
        .builder(PkmnStorageComponent.class, PkmnStorageComponent::new)
        // .append(new KeyedCodec<>("Level", Codec.INTEGER),
        //         (data, value) -> data.level = value,
        //         data -> data.level)
        // .add()
        // .append(new KeyedCodec<>("BaseStats", Codec.INT_ARRAY),
        //         (data, value) -> data.baseStats = value,
        //         data -> data.baseStats)
        // .add()
        // .append(new KeyedCodec<>("Evs", Codec.INT_ARRAY),
        //         (data, value) -> data.evs = value,
        //         data -> data.evs)
        // .add()
        // .append(new KeyedCodec<>("Ivs", Codec.INT_ARRAY),
        //         (data, value) -> data.ivs = value,
        //         data -> data.ivs)
        // .add()
        // .append(new KeyedCodec<>("Nature", Codec.STRING),
        //         (data, value) -> data.nature = value,
        //         data -> data.nature)
        // .add()
        // .append(new KeyedCodec<>("Nickname", Codec.STRING),
        //         (data, value) -> data.nickname = value,
        //         data -> data.nickname)
        // .add()
        // .append(new KeyedCodec<>("Owner", Codec.STRING),
        //         (data, value) -> data.ownerUuid = value,
        //         data -> data.ownerUuid)
        // .add()
        // .append(new KeyedCodec<>("Exp", Codec.LONG),
        //         (data, value) -> data.experience = value,
        //         data -> data.experience)
        // .add()
        // .append(new KeyedCodec<>("Type1", Codec.STRING),
        //         (data, value) -> data.type1 = (value != null && !value.isEmpty()) ? value : "Normal",
        //         data -> data.type1)
        // .add()
        // .append(new KeyedCodec<>("Type2", Codec.STRING),
        //         (data, value) -> data.type2 = (value != null && !value.isEmpty()) ? value : null,
        //         data -> data.type2)
        // .add()
        .build();
}
