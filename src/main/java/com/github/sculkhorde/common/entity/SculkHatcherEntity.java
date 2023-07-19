package com.github.sculkhorde.common.entity;

import com.github.sculkhorde.common.entity.goal.*;
import com.github.sculkhorde.core.EntityRegistry;
import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.util.TargetParameters;
import com.github.sculkhorde.util.TickUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The Sculk Hatcher.
 * @see com.github.sculkhorde.client.renderer.entity.SculkHatcherRenderer
 * @see com.github.sculkhorde.client.model.enitity.SculkHatcherModel
 */
public class SculkHatcherEntity extends Monster implements GeoEntity, ISculkSmartEntity {

    /**
     * In order to create a mob, the following java files were created/edited.<br>
     * Edited {@link EntityRegistry}<br>
     * Edited {@link com.github.sculkhorde.util.ModEventSubscriber}<br>
     * Edited {@link com.github.sculkhorde.client.ClientModEventSubscriber}<br>
     * Edited {@link com.github.sculkhorde.common.world.ModWorldEvents} (this might not be necessary)<br>
     * Edited {@link com.github.sculkhorde.common.world.gen.ModEntityGen}<br>
     * Added {@link SculkHatcherEntity}<br>
     * Added {@link com.github.sculkhorde.client.model.enitity.SculkHatcherModel} <br>
     * Added {@link com.github.sculkhorde.client.renderer.entity.SculkHatcherRenderer}
     */

    //The Health
    public static final float MAX_HEALTH = 20F;
    //The armor of the mob
    public static final float ARMOR = 4F;
    //ATTACK_DAMAGE determines How much damage it's melee attacks do
    public static final float ATTACK_DAMAGE = 3F;
    //ATTACK_KNOCKBACK determines the knockback a mob will take
    public static final float ATTACK_KNOCKBACK = 1F;
    //FOLLOW_RANGE determines how far away this mob can see and chase enemies
    public static final float FOLLOW_RANGE = 25F;
    //MOVEMENT_SPEED determines how far away this mob can see other mobs
    public static final float MOVEMENT_SPEED = 0.25F;

    // Controls what types of entities this mob can target
    private TargetParameters TARGET_PARAMETERS = new TargetParameters(this).enableTargetHostiles().ignoreTargetBelow50PercentHealth().enableMustReachTarget();

    //factory The animation factory used for animations
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /**
     * The Constructor
     * @param type The Mob Type
     * @param worldIn The world to initialize this mob in
     */
    public SculkHatcherEntity(EntityType<? extends SculkHatcherEntity> type, Level worldIn) {
        super(type, worldIn);
        this.setPathfindingMalus(BlockPathTypes.UNPASSABLE_RAIL, 0.0F);
    }

    /**
     * An Easier Constructor where you do not have to specify the Mob Type
     * @param worldIn  The world to initialize this mob in
     */
    public SculkHatcherEntity(Level worldIn) {super(EntityRegistry.SCULK_HATCHER.get(), worldIn);}

    /**
     * Determines & registers the attributes of the mob.
     * @return The Attributes
     */
    public static AttributeSupplier.Builder createAttributes()
    {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.ATTACK_KNOCKBACK, ATTACK_KNOCKBACK)
                .add(Attributes.FOLLOW_RANGE,FOLLOW_RANGE)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED);
    }

    @Override
    public void checkDespawn() {}

    public boolean isIdle() {
        return getTarget() == null;
    }

    protected SoundEvent getAmbientSound() {
        return SoundEvents.COW_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.COW_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.COW_DEATH;
    }

    protected void playStepSound(BlockPos pPos, BlockState pBlockIn) {
        this.playSound(SoundEvents.COW_STEP, 0.15F, 1.0F);
    }

    /**
     * Registers Goals with the entity. The goals determine how an AI behaves ingame.
     * Each goal has a priority with 0 being the highest and as the value increases, the priority is lower.
     * You can manually add in goals in this function, however, I made an automatic system for this.
     */
    @Override
    public void registerGoals() {

        Goal[] goalSelectorPayload = goalSelectorPayload();
        for(int priority = 0; priority < goalSelectorPayload.length; priority++)
        {
            this.goalSelector.addGoal(priority, goalSelectorPayload[priority]);
        }

        Goal[] targetSelectorPayload = targetSelectorPayload();
        for(int priority = 0; priority < targetSelectorPayload.length; priority++)
        {
            this.targetSelector.addGoal(priority, targetSelectorPayload[priority]);
        }

    }

    /**
     * Prepares an array of goals to give to registerGoals() for the goalSelector.<br>
     * The purpose was to make registering goals simpler by automatically determining priority
     * based on the order of the items in the array. First element is of priority 0, which
     * represents highest priority. Priority value then increases by 1, making each element
     * less of a priority than the last.
     * @return Returns an array of goals ordered from highest to lowest piority
     */
    public Goal[] goalSelectorPayload()
    {
        Goal[] goals =
                {
                        new DespawnAfterTime(this, TickUnits.convertMinutesToTicks(15)),
                        new DespawnWhenIdle(this, 30),
                        //SwimGoal(mob)
                        new FloatGoal(this),
                        //MeleeAttackGoal(mob, speedModifier, followingTargetEvenIfNotSeen)
                        new SculkHatcherAttackGoal(this, 1.0D, true),
                        new PathFindToRaidLocation<>(this),
                        //MoveTowardsTargetGoal(mob, speedModifier, within) THIS IS FOR NON-ATTACKING GOALS
                        new MoveTowardsTargetGoal(this, 0.8F, 20F),
                        //WaterAvoidingRandomWalkingGoal(mob, speedModifier)
                        new ImprovedRandomStrollGoal(this, 1.0D).setToAvoidWater(true),
                        //new RangedAttackGoal(this, new AcidAttack(this), 20),
                        //LookAtGoal(mob, targetType, lookDistance)
                        new LookAtPlayerGoal(this, Pig.class, 8.0F),
                        //LookRandomlyGoal(mob)
                        new RandomLookAroundGoal(this),
                        new OpenDoorGoal(this, true)
                };
        return goals;
    }

    /**
     * Prepares an array of goals to give to registerGoals() for the targetSelector.<br>
     * The purpose was to make registering goals simpler by automatically determining priority
     * based on the order of the items in the array. First element is of priority 0, which
     * represents highest priority. Priority value then increases by 1, making each element
     * less of a priority than the last.
     * @return Returns an array of goals ordered from highest to lowest piority
     */
    public Goal[] targetSelectorPayload()
    {
        Goal[] goals =
                {
                        new InvalidateTargetGoal(this),
                        new TargetAttacker(this).setAlertAllies(),
                        new NearestLivingEntityTargetGoal<>(this, true, true)
                };
        return goals;
    }

    /** ~~~~~~~~ ANIMATION ~~~~~~~~ **/

    private static final RawAnimation BODY_IDLE_ANIMATION = RawAnimation.begin().thenPlay("body.idle");
    private static final RawAnimation BODY_WALK_ANIMATION = RawAnimation.begin().thenPlay("body.walk");
    private static final RawAnimation LEGS_IDLE_ANIMATION = RawAnimation.begin().thenPlay("legs.idle");
    private static final RawAnimation LEGS_WALK_ANIMATION = RawAnimation.begin().thenLoop("legs.walk");
    private static final RawAnimation HEAD_ATTACK_ANIMATION = RawAnimation.begin().thenPlay("head.attack");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(this, "Legs", 5, this::poseLegs),
                new AnimationController<>(this, "Body", 5, this::poseBody),
                DefaultAnimations.genericAttackAnimation(this, HEAD_ATTACK_ANIMATION)
        );
    }

    // Create the animation handler for the leg segment
    protected PlayState poseLegs(AnimationState<SculkHatcherEntity> state)
    {
        if(state.isMoving())
        {
            state.setAnimation(LEGS_WALK_ANIMATION);
        }
        else
        {
            //state.setAnimation(LEGS_IDLE_ANIMATION);
            state.setAnimation(LEGS_IDLE_ANIMATION);
        }

        return PlayState.CONTINUE;
    }

    // Create the animation handler for the body segment
    protected PlayState poseBody(AnimationState<SculkHatcherEntity> state)
    {
        if(state.isMoving())
        {
            state.setAnimation(BODY_WALK_ANIMATION);
        }
        else
        {
            state.setAnimation(BODY_IDLE_ANIMATION);
        }

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private boolean isParticipatingInRaid = false;

    @Override
    public boolean isParticipatingInRaid() {
        return isParticipatingInRaid;
    }

    @Override
    public void setParticipatingInRaid(boolean isParticipatingInRaidIn) {
        isParticipatingInRaid = isParticipatingInRaidIn;
    }

    @Override
    public TargetParameters getTargetParameters() {
        return TARGET_PARAMETERS;
    }

    /** ~~~~~~~~ CLASSES ~~~~~~~~ **/

    public class SculkHatcherAttackGoal extends MeleeAttackGoal
    {

        private final SculkHatcherEntity thisMob;
        private long tickCooldownForSpawn = 20 * 1;
        private long ticksInCooldown = 0;

        /**
         * The Constructor
         * @param mob The mob that called this
         * @param speedModifier How fast can they attack?
         * @param followTargetIfNotSeen Should the mob follow their target if they cant see them.
         */
        public SculkHatcherAttackGoal(SculkHatcherEntity mob, double speedModifier, boolean followTargetIfNotSeen) {
            super(mob, speedModifier, followTargetIfNotSeen);
            this.thisMob = mob;
        }

        @Override
        public boolean canUse()
        {
            boolean canWeUse = ((ISculkSmartEntity)this.mob).getTargetParameters().isEntityValidTarget(this.mob.getTarget(), true);
            // If the mob is already targeting something valid, don't bother
            return canWeUse;
        }

        @Override
        public boolean canContinueToUse()
        {
            return canUse();
        }


        /**
         * Gets called every tick the attack is active<br>
         * We shouldn't have to check if the target is null since
         * the super class does this. However, something funky is going on that
         * causes a null pointer exception if we dont check this here. This is
         * absolutely some sort of bug that I was unable to figure out. For the
         * time being (assuming I ever fix this), this will have to do.
         */
        public void tick()
        {
            if(this.thisMob.getTarget() == null)
            {
                stop();
            }
            else
            {
                super.tick();
                if(ticksInCooldown >= tickCooldownForSpawn && thisMob.getTarget() != null && thisMob.getHealth() > SculkMiteEntity.MAX_HEALTH)
                {
                    ticksInCooldown = 0;
                    BlockPos spawnPos = new BlockPos(thisMob.blockPosition());
                    EntityRegistry.SCULK_MITE.get().spawn((ServerLevel) thisMob.level(), spawnPos, MobSpawnType.SPAWNER);
                    thisMob.hurt(damageSources().generic(), SculkMiteEntity.MAX_HEALTH);
                }
                else
                {
                    ticksInCooldown++;
                }
            }
        }
    }

    public boolean dampensVibrations() {
        return true;
    }


    /**
     * If a sculk living entity despawns, refund it's current health to the sculk hoard
     */
    @Override
    public void onRemovedFromWorld() {
        SculkHorde.savedData.addSculkAccumulatedMass((int) this.getHealth());
        super.onRemovedFromWorld();
    }
}
