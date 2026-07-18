package dw.portablemonstrosities.ui;

import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.events.DynamicPageData;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;

public class RawEventPageBuilder extends PageBuilder {

    private CustomPageLifetime myLifetime = CustomPageLifetime.CanDismiss;
    private BiConsumer<DynamicPageData, HyUIPage> rawEventHandler;

    public RawEventPageBuilder(PlayerRef playerRef) { super(playerRef); }
    public RawEventPageBuilder() { super(); }
    public static RawEventPageBuilder detached() { return new RawEventPageBuilder(); }

    @Override
    public RawEventPageBuilder withLifetime(CustomPageLifetime lifetime) {
        this.myLifetime = lifetime;
        return this;
    }

    /** Fires for every event the client sends, regardless of parsed element registry. */
    public RawEventPageBuilder onRawDataEvent(BiConsumer<DynamicPageData, HyUIPage> handler) {
        this.rawEventHandler = handler;
        return this;
    }

    @Override
    public HyUIPage open(@Nonnull PlayerRef playerRefParam, Store<EntityStore> store) {
        Player playerComponent = store.getComponent(playerRefParam.getReference(), Player.getComponentType());
        PageManager pageManager = playerComponent.getPageManager();

        HyUIPage page = new HyUIPage(
                playerRefParam,
                myLifetime,
                uiFile,                 // protected, inherited from InterfaceBuilder
                getTopLevelElements(),  // public
                editCallbacks,          // protected, inherited — your editElement(...) calls
                templateHtml,           // protected
                templateProcessor,      // protected
                runtimeTemplateUpdatesEnabled, // protected
                null,
                this) {
            @Override
            public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull DynamicPageData data) {
                super.handleDataEvent(ref, store, data);
                if (rawEventHandler != null) rawEventHandler.accept(data, this);
            }
        };

        pageManager.openCustomPage(playerRefParam.getReference(), store, page);
        return page;
    }
}