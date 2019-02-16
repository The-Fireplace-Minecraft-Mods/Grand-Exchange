package the_fireplace.grandexchange.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;

public class SerializationUtils {
    public static String stackToString(ItemStack stack) {
        return stack.write(new NBTTagCompound()).toString();
    }

    public static ItemStack stackFromString(String stackString) {
        try {
            return ItemStack.read(JsonToNBT.getTagFromJson(stackString));
        } catch (CommandSyntaxException e) {
            return null;
        }
    }

    public static boolean isValidNBT(String nbt) {
        try{
            JsonToNBT.getTagFromJson(nbt);
            return true;
        } catch(CommandSyntaxException e) {
            return false;
        }
    }

    public static NBTTagCompound getNbt(String nbt) {
        try{
            return JsonToNBT.getTagFromJson(nbt);
        } catch(CommandSyntaxException e) {
            return null;
        }
    }
}
