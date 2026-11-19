package com.functionalitemframe.mixin;

import com.functionalitemframe.ItemFrameSeeker;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DispenserBlock.class)
public abstract class DispenserBlockMixin {
    @Inject(method = "dispense", at = @At("HEAD"), cancellable = true)
    private void functionalitemframe$insertIntoItemFrame(ServerWorld world, BlockPos pos, CallbackInfo callbackInfo) {
        Block dispenserBlock = world.getBlockState(pos).getBlock();
        if (dispenserBlock != Blocks.DISPENSER) {
            return;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof DispenserBlockEntity)) {
            return;
        }

        DispenserBlockEntity dispenser = (DispenserBlockEntity) blockEntity;
        int slot = dispenser.chooseNonEmptySlot();
        if (slot < 0) {
            return;
        }

        ItemStack stack = dispenser.getStack(slot);
        if (stack.isEmpty()) {
            return;
        }

        Direction facing = world.getBlockState(pos).get(DispenserBlock.FACING);
        BlockPos targetPos = pos.offset(facing);
        for (ItemFrameEntity frame : ItemFrameSeeker.findItemFrames(world, targetPos)) {
            if (frame.getHeldItemStack().isEmpty()) {
                frame.setHeldItemStack(stack.split(1));
                dispenser.setStack(slot, stack);
                dispenser.markDirty();
                world.syncWorldEvent(1000, pos, 0);
                world.syncWorldEvent(2000, pos, facing.getId());
                callbackInfo.cancel();
                return;
            }
        }
    }
}
