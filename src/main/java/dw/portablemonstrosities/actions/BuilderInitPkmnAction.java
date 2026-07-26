package dw.portablemonstrosities.actions;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.AssetHolder;
import com.hypixel.hytale.server.npc.asset.builder.holder.BooleanHolder;
import com.hypixel.hytale.server.npc.asset.builder.holder.IntHolder;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.IntValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import javax.annotation.Nonnull;

public class BuilderInitPkmnAction extends BuilderActionBase {
   protected final AssetHolder   entityEffect   = new AssetHolder();
   protected final BooleanHolder useTarget      = new BooleanHolder();
   protected final IntHolder     level          = new IntHolder();
   protected final StringHolder  type1          = new StringHolder();
   protected final StringHolder  type2          = new StringHolder();
   protected final IntHolder     hp             = new IntHolder();
   protected final IntHolder     atk            = new IntHolder();
   protected final IntHolder     def            = new IntHolder();
   protected final IntHolder     spAtk          = new IntHolder();
   protected final IntHolder     spDef          = new IntHolder();
   protected final IntHolder     spd            = new IntHolder();
   protected final BooleanHolder shiny          = new BooleanHolder();

   public BuilderInitPkmnAction() {
   }

   @Nonnull
   public InitPkmnAction build(@Nonnull BuilderSupport builderSupport) {
      return new InitPkmnAction(this, builderSupport);
   }

   @Nonnull
   public String getShortDescription() {
      return "Initialise PkmnStats";
   }

   @Nonnull
   public String getLongDescription() {
      return this.getShortDescription();
   }

   @Nonnull
   public BuilderDescriptorState getBuilderDescriptorState() {
      return BuilderDescriptorState.Stable;
   }




   @Nonnull
   public BuilderInitPkmnAction readConfig(@Nonnull JsonElement data) {
      // this.requireAsset(data, "EntityEffect", this.entityEffect, EntityEffectExistsValidator.required(), BuilderDescriptorState.Stable, "The entity effect to apply", (String)null);

      // this.getBoolean(data, "UseTarget", this.useTarget, true, BuilderDescriptorState.Stable, "Use the sensor-provided target for the action, self otherwise", (String)null);

      // this.requireFeatureIf(this.useTarget, true, Feature.LiveEntity);
      
      this.getInt(data,    "Level",    this.level, 5,             new TestIntValidator(),     BuilderDescriptorState.Stable, "Pokemon level",                   (String)null);
      this.getString(data, "Type1",    this.type1, "NormalType",  new TestStringValidator(),  BuilderDescriptorState.Stable, "First Pokemon type",              (String)null);
      this.getString(data, "Type2",    this.type2, null,          new TestStringValidator(),  BuilderDescriptorState.Stable, "Second Pokemon type (optional)",  (String)null);
      this.getInt(data,    "Hp",       this.hp,    40,            new TestIntValidator(),     BuilderDescriptorState.Stable, "HP stat",                         (String)null);
      this.getInt(data,    "Atk",      this.atk,   40,            new TestIntValidator(),     BuilderDescriptorState.Stable, "Attack stat",                     (String)null);
      this.getInt(data,    "Def",      this.def,   40,            new TestIntValidator(),     BuilderDescriptorState.Stable, "Defense stat",                    (String)null);
      this.getInt(data,    "SpAtk",    this.spAtk, 40,            new TestIntValidator(),     BuilderDescriptorState.Stable, "Special Attack stat",             (String)null);
      this.getInt(data,    "SpDef",    this.spDef, 40,            new TestIntValidator(),     BuilderDescriptorState.Stable, "Special Defense stat",            (String)null);
      this.getInt(data,    "Spd",      this.spd,   40,            new TestIntValidator(),     BuilderDescriptorState.Stable, "Speed stat",                      (String)null);
      this.getBoolean(data, "Shiny",   this.shiny, false,                                     BuilderDescriptorState.Stable, "Whether the Pokemon is shiny",    (String)null);

      return this;
   }

   // public int getEntityEffect(@Nonnull BuilderSupport support) {
   //    return EntityEffect.getAssetMap().getIndex(this.entityEffect.get(support.getExecutionContext()));
   // }

   // public boolean isUseTarget(@Nonnull BuilderSupport support) {
   //    return this.useTarget.get(support.getExecutionContext());
   // }

   public int getLevel(@Nonnull BuilderSupport support) {
      return this.level.get(support.getExecutionContext());
   }

   public String getType1(@Nonnull BuilderSupport support) {
      return this.type1.get(support.getExecutionContext());
   }

   public String getType2(@Nonnull BuilderSupport support) {
      return this.type2.get(support.getExecutionContext());
   }

   public int getHp(@Nonnull BuilderSupport support) {
      return this.hp.get(support.getExecutionContext());
   }

   public int getAtk(@Nonnull BuilderSupport support) {
      return this.atk.get(support.getExecutionContext());
   }

   public int getDef(@Nonnull BuilderSupport support) {
      return this.def.get(support.getExecutionContext());
   }

   public int getSpAtk(@Nonnull BuilderSupport support) {
      return this.spAtk.get(support.getExecutionContext());
   }

   public int getSpDef(@Nonnull BuilderSupport support) {
      return this.spDef.get(support.getExecutionContext());
   }

   public int getSpd(@Nonnull BuilderSupport support) {
      return this.spd.get(support.getExecutionContext());
   }

   public boolean isShiny(@Nonnull BuilderSupport support) {
      return this.shiny.get(support.getExecutionContext());
   }
}


class TestIntValidator extends IntValidator {

   public TestIntValidator(){}

   @Override
   public String errorMessage(int arg0) {
      return "TestIntValidatorError: "+String.valueOf(arg0);
   }

   @Override
   public String errorMessage(int arg0, String arg1) {
      return "TestIntValidatorError2: "+String.valueOf(arg0)+", "+arg1;
   }

   @Override
   public boolean test(int arg0) {
      return arg0>0;
   }

}

class TestStringValidator extends StringValidator {

   public TestStringValidator(){}

   @Override
   public String errorMessage(String arg0) {
      return "TestStringValidatorError: "+arg0;
   }

   @Override
   public String errorMessage(String arg0, String arg1) {
      return "TestStringValidatorError2: ("+arg0+", "+arg1+")";
   }

   @Override
   public boolean test(String arg0) {
      return true;
   }

}