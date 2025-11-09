package chihalu.building.support.mixin.client;

import java.util.Collection;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CreativeInventoryScreen.class)
public interface CreativeInventoryScreenInvoker {
	@Invoker("setSelectedTab")
	void utility_toolkit$setSelectedTab(ItemGroup group);

	@Invoker("refreshSelectedTab")
	void utility_toolkit$refreshSelectedTab(Collection<ItemStack> stacks);

	@Accessor("selectedTab")
	static ItemGroup utility_toolkit$getSelectedTab() {
		throw new AssertionError();
	}
}
