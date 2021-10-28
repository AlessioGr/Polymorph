package top.theillusivec4.polymorph.common.integration.toms_storage;

import com.tom.storagemod.tile.TileEntityCraftingTerminal;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import top.theillusivec4.polymorph.common.capability.AbstractTileEntityRecipeData;

public class CraftingTerminalRecipeData
    extends AbstractTileEntityRecipeData<TileEntityCraftingTerminal> {

  public CraftingTerminalRecipeData(TileEntityCraftingTerminal pOwner) {
    super(pOwner);
  }

  @Override
  protected NonNullList<ItemStack> getInput() {
    CraftingInventory craftingInventory = this.getOwner().getCraftingInv();

    if (craftingInventory != null) {
      NonNullList<ItemStack> stacks =
          NonNullList.withSize(craftingInventory.getSizeInventory(), ItemStack.EMPTY);

      for (int i = 0; i < craftingInventory.getSizeInventory(); i++) {
        stacks.set(i, craftingInventory.getStackInSlot(i));
      }
      return stacks;
    }
    return NonNullList.create();
  }
}
