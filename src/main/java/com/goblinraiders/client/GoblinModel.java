package com.goblinraiders.client;

import com.goblinraiders.entity.AbstractGoblin;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

/** The player model (skins are regular 64x64 player skins) with mob arm poses. */
public class GoblinModel<T extends AbstractGoblin> extends PlayerModel<T> {
    public GoblinModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
        this.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        this.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        ItemStack held = entity.getMainHandItem();
        if (!held.isEmpty()) {
            // Drawing the bow while fighting, otherwise just holding the item.
            HumanoidModel.ArmPose pose = held.getItem() instanceof BowItem && entity.isAggressive()
                    ? HumanoidModel.ArmPose.BOW_AND_ARROW : HumanoidModel.ArmPose.ITEM;
            if (entity.getMainArm() == HumanoidArm.RIGHT) {
                this.rightArmPose = pose;
            } else {
                this.leftArmPose = pose;
            }
        }
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entity.isAggressive() && entity.getMainHandItem().isEmpty()) {
            // Bare-handed goblins reach for their target like zombies do.
            AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, true, this.attackTime, ageInTicks);
            this.leftSleeve.copyFrom(this.leftArm);
            this.rightSleeve.copyFrom(this.rightArm);
        }
    }
}
