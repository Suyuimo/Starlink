package de.weinschenk.starlink.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/**
 * Rezept-Typ für die Satellite Workbench.
 * Delegiert alle Matching-/Assembly-Logik an ein ShapedRecipe,
 * überschreibt nur getType() und getSerializer().
 */
public class SatelliteCraftingRecipe implements CraftingRecipe {

    private final ShapedRecipe delegate;

    public SatelliteCraftingRecipe(ShapedRecipe delegate) {
        this.delegate = delegate;
    }

    public ShapedRecipe getDelegate() { return delegate; }

    @Override public boolean matches(CraftingContainer container, Level level) { return delegate.matches(container, level); }
    @Override public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) { return delegate.assemble(container, registryAccess); }
    @Override public boolean canCraftInDimensions(int w, int h) { return delegate.canCraftInDimensions(w, h); }
    @Override public ItemStack getResultItem(RegistryAccess registryAccess) { return delegate.getResultItem(registryAccess); }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) { return delegate.getRemainingItems(container); }
    @Override public NonNullList<Ingredient> getIngredients() { return delegate.getIngredients(); }
    @Override public String getGroup() { return delegate.getGroup(); }
    @Override public CraftingBookCategory category() { return delegate.category(); }
    @Override public boolean isSpecial() { return delegate.isSpecial(); }
    @Override public @NotNull ResourceLocation getId() { return delegate.getId(); }

    @Override
    public RecipeType<?> getType() { return ModRecipeTypes.SATELLITE_CRAFTING.get(); }

    @Override
    public RecipeSerializer<?> getSerializer() { return ModRecipeTypes.SATELLITE_CRAFTING_SERIALIZER.get(); }
}
