package com.functionalitemframe;

import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class ItemFrameSeeker {
    public static List<ItemFrameEntity> findItemFrames(ServerWorld world, BlockPos targetPos) {
        List<ItemFrameEntity> frames = new ArrayList<>();
        Box searchBox = new Box(targetPos).expand(1.0D);

        for (ItemFrameEntity frame : world.getEntitiesByClass(ItemFrameEntity.class, searchBox, entity -> true)) {
            if (frame.getDecorationBlockPos().equals(targetPos)
                    && frame.getHorizontalFacing().getAxis() != Direction.Axis.Y) {
                frames.add(frame);
            }
        }

        return frames;
    }
}
