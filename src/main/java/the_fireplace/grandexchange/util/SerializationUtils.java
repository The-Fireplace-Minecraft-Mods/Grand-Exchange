package the_fireplace.grandexchange.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;

public class SerializationUtils {
    public static String stackToString(ItemStack stack) {
        return stack.writeToNBT(new NBTTagCompound()).toString();
    }

    public static ItemStack stackFromString(String stackString) {
        try {
            return new ItemStack(JsonToNBT.getTagFromJson(stackString));
        } catch (NBTException e) {
            return null;
        }
    }

    public static boolean isValidNBT(String nbt) {
        try{
            JsonToNBT.getTagFromJson(nbt);
            return true;
        } catch(NBTException e) {
            return false;
        }
    }

    public static NBTTagCompound getNbt(String nbt) {
        try{
            return JsonToNBT.getTagFromJson(nbt);
        } catch(NBTException e) {
            return null;
        }
    }
}
