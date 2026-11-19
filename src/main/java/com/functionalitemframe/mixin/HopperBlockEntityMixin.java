package com.functionalitemframe.mixin;

import com.functionalitemframe.ItemFrameSeeker;
import net.minecraft.block.Blocks;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private void functionalitemframe$insertIntoItemFrame(CallbackInfoReturnable<Boolean> callbackInfo) {
        HopperBlockEntity hopper = (HopperBlockEntity) (Object) this;
        World world = hopper.getWorld();
        if (!(world instanceof ServerWorld)) {
            return;
        }

        BlockPos hopperPos = hopper.getPos();
        if (world.getBlockState(hopperPos).getBlock() != Blocks.HOPPER
                || !world.getBlockState(hopperPos).get(HopperBlock.ENABLED)) {
            return;
        }

        Direction facing = world.getBlockState(hopperPos).get(HopperBlock.FACING);
        BlockPos targetPos = hopperPos.offset(facing);
        ItemStack stack = ItemStack.EMPTY;
        int slot = -1;
        for (int index = 0; index < hopper.size(); index++) {
            if (!hopper.getStack(index).isEmpty()) {
                slot = index;
                stack = hopper.getStack(index);
                break;
            }
        }

        if (slot < 0) {
            return;
        }

        for (ItemFrameEntity frame : ItemFrameSeeker.findItemFrames((ServerWorld) world, targetPos)) {
            if (frame.getHeldItemStack().isEmpty()) {
                frame.setHeldItemStack(stack.split(1));
                hopper.setStack(slot, stack);
                hopper.markDirty();
                callbackInfo.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = "extract(Lnet/minecraft/block/entity/Hopper;)Z", at = @At("TAIL"), cancellable = true)
    private static void functionalitemframe$extractFromItemFrame(
            Hopper hopper, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (callbackInfo.getReturnValueZ() || !(hopper instanceof HopperBlockEntity)) {
            return;
        }

        HopperBlockEntity hopperBlockEntity = (HopperBlockEntity) hopper;
        World world = hopperBlockEntity.getWorld();
        if (!(world instanceof ServerWorld)) {
            return;
        }

        BlockPos hopperPos = hopperBlockEntity.getPos();
        if (world.getBlockState(hopperPos).getBlock() != Blocks.HOPPER
                || !world.getBlockState(hopperPos).get(HopperBlock.ENABLED)) {
            return;
        }

        BlockPos supportPos = hopperPos.up();
        for (ItemFrameEntity frame : ItemFrameSeeker.findItemFrames((ServerWorld) world, supportPos)) {
            ItemStack frameStack = frame.getHeldItemStack();
            if (frameStack.isEmpty()) {
                continue;
            }

            int slot = -1;
            for (int index = 0; index < hopperBlockEntity.size(); index++) {
                ItemStack hopperStack = hopperBlockEntity.getStack(index);
                if (hopperStack.isEmpty()
                        || (ItemStack.areItemsEqual(hopperStack, frameStack)
                        && ItemStack.areTagsEqual(hopperStack, frameStack)
                        && hopperStack.getCount() < Math.min(hopperStack.getMaxCount(),
                        hopperBlockEntity.getMaxCountPerStack()))) {
                    slot = index;
                    break;
                }
            }

            if (slot < 0) {
                return;
            }

            ItemStack hopperStack = hopperBlockEntity.getStack(slot);
            ItemStack extractedStack = frameStack.copy();
            extractedStack.setCount(1);
            ItemStack remainingFrameStack = frameStack.copy();
            remainingFrameStack.decrement(1);

            if (hopperStack.isEmpty()) {
                hopperBlockEntity.setStack(slot, extractedStack);
            } else {
                hopperStack.increment(1);
            }
            frame.setHeldItemStack(remainingFrameStack);
            hopperBlockEntity.markDirty();
            callbackInfo.setReturnValue(true);
            return;
        }
    }
}
