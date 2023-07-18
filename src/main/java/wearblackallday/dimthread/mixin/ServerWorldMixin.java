package wearblackallday.dimthread.mixin;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wearblackallday.dimthread.DimThread;
import wearblackallday.dimthread.util.ServerWorldAccessor;

import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements ServerWorldAccessor {

	protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long biomeAccess, int maxChainedNeighborUpdates) {
		super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
	}

	@Shadow protected abstract void tickTime();

	boolean onMainThread = false;
	boolean timeTickedOnWorldThread = false;

	/**
	 * Time ticking is not thread-safe. We cancel time ticking from the world thread. However, DimThread will tick time on the main thread
	 */
	@Inject(method = "tickTime", at = @At("HEAD"), cancellable = true)
	private void preventTimeTicking(CallbackInfo ci) {
		if (DimThread.MANAGER.isActive(getServer()) && !onMainThread) {
			timeTickedOnWorldThread = true;
			ci.cancel();
		}
	}

	@Override
	public void dimthread_tickTime() {
		if (timeTickedOnWorldThread) {
			onMainThread = true;
			tickTime();
			onMainThread = false;
			timeTickedOnWorldThread = false;
		}
	}
}
