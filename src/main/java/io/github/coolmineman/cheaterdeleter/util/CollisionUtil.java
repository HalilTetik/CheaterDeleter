package io.github.coolmineman.cheaterdeleter.util;

import java.util.function.BiPredicate;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableDouble;

import io.github.coolmineman.cheaterdeleter.objects.CDPlayer;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlimeBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

public class CollisionUtil {
    private CollisionUtil() { }

    public static final BiPredicate<World, BlockPos> NON_SOLID_COLLISION = (world, pos) -> {
        BlockState state = world.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) return true;
        if (state.getMaterial().isLiquid()) return true;
        if (state.getBlock() instanceof SlimeBlock) return true;
        if (state.getBlock() instanceof BedBlock) return true;
        return false;
    };

    public static BiPredicate<World, BlockPos> steppablePredicate(float stepheight) {
        return (world, pos) -> {
            VoxelShape shape = world.getBlockState(pos).getCollisionShape(world, pos);
            MutableBoolean steppable = new MutableBoolean(false);
            shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                if (maxY <= stepheight) steppable.setTrue();
            });
            return steppable.booleanValue();
        };
    }

    //TODO: Step Heights > 1
    public static BiPredicate<World, BlockPos> touchingNonSteppablePredicate(float stepheight, Box box, double oldY, double newY) {
        return (world, pos) -> {
            VoxelShape shape = world.getBlockState(pos).getCollisionShape(world, pos).offset(pos.getX(), pos.getY(), pos.getZ());
            if (newY <= oldY) return intersects(shape, box);
            if (intersects(shape, box)) {
                MutableDouble stepHeight = new MutableDouble(Double.MAX_VALUE);
                shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                    if (maxY < stepHeight.doubleValue() && box.intersects(minX, minY, minZ, maxX, maxY, maxZ)) {
                        stepHeight.setValue(maxY);
                    }
                });
                double stepDelta = stepHeight.doubleValue() - oldY;
                if (stepDelta > stepheight) {
                    return true;
                }
                Box translatedBox = new Box(box.minX, box.minY + stepDelta, box.minZ, box.maxX, box.maxY + stepDelta, box.maxZ).expand(-0.1);
                return intersects(shape, translatedBox);
            }
            return false;
        };
    }

    public static BiPredicate<World, BlockPos> touchingPredicate(Box box) {
        return (world, pos) -> {
            VoxelShape shape = world.getBlockState(pos).getCollisionShape(world, pos).offset(pos.getX(), pos.getY(), pos.getZ());
            return intersects(shape, box);
        };
    }

    public static boolean intersects(VoxelShape shape, Box box) {
        if (shape.isEmpty()) return false;
        MutableBoolean result = new MutableBoolean(false);
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            if (box.intersects(minX, minY, minZ, maxX, maxY, maxZ)) {
                result.setTrue();
            }
        });
        return result.booleanValue();
    }
    
    public static Box getBoxForPosition(CDPlayer player, double posx, double posy, double posz) {
        return player.mcPlayer.getDimensions(player.mcPlayer.getPose()).method_30231(posx, posy, posz); // method_30231 -> withPos
    }

    public static boolean isNearby(CDPlayer player, double expandHorizontal, double expandVertical, BiPredicate<World, BlockPos> predicate) {
        return isTouching(player.mcPlayer.getBoundingBox().expand(expandHorizontal, expandVertical, expandHorizontal), player.getWorld(), predicate);
    }

    public static boolean isNearby(CDPlayer player, double posx, double posy, double posz, double expandHorizontal, double expandVertical, BiPredicate<World, BlockPos> predicate) {
        return isTouching(getBoxForPosition(player, posx, posy, posz).expand(expandHorizontal, expandVertical, expandHorizontal), player.getWorld(), predicate);
    }

    public static boolean isTouching(Box box, World world, BiPredicate<World, BlockPos> predicate) {
        return BlockPos.stream(box).anyMatch(pos -> predicate.test(world, pos));
    }
}
