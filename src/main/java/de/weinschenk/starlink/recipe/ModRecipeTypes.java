package de.weinschenk.starlink.recipe;

import de.weinschenk.starlink.Starlink;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipeTypes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Starlink.MODID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Starlink.MODID);

    // ── Satellite Workbench ──────────────────────────────────────────────────

    public static final RegistryObject<RecipeType<SatelliteCraftingRecipe>> SATELLITE_CRAFTING =
            RECIPE_TYPES.register("satellite_crafting",
                    () -> RecipeType.simple(new ResourceLocation(Starlink.MODID, "satellite_crafting")));

    public static final RegistryObject<RecipeSerializer<SatelliteCraftingRecipe>> SATELLITE_CRAFTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("satellite_crafting", () -> new RecipeSerializer<>() {
                @Override
                public SatelliteCraftingRecipe fromJson(ResourceLocation id, com.google.gson.JsonObject json) {
                    return new SatelliteCraftingRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
                }

                @Override
                public SatelliteCraftingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
                    ShapedRecipe base = RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf);
                    return base == null ? null : new SatelliteCraftingRecipe(base);
                }

                @Override
                public void toNetwork(FriendlyByteBuf buf, SatelliteCraftingRecipe recipe) {
                    RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe.getDelegate());
                }
            });

    // ── Rocket Workbench ─────────────────────────────────────────────────────

    public static final RegistryObject<RecipeType<RocketCraftingRecipe>> ROCKET_CRAFTING =
            RECIPE_TYPES.register("rocket_crafting",
                    () -> RecipeType.simple(new ResourceLocation(Starlink.MODID, "rocket_crafting")));

    public static final RegistryObject<RecipeSerializer<RocketCraftingRecipe>> ROCKET_CRAFTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("rocket_crafting", () -> new RecipeSerializer<>() {
                @Override
                public RocketCraftingRecipe fromJson(ResourceLocation id, com.google.gson.JsonObject json) {
                    return new RocketCraftingRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
                }

                @Override
                public RocketCraftingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
                    ShapedRecipe base = RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf);
                    return base == null ? null : new RocketCraftingRecipe(base);
                }

                @Override
                public void toNetwork(FriendlyByteBuf buf, RocketCraftingRecipe recipe) {
                    RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe.getDelegate());
                }
            });
}
