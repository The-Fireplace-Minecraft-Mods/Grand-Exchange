package the_fireplace.grandexchange.util;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;

import net.minecraft.util.ResourceLocation;
import the_fireplace.grandexchange.market.BuyOffer;
import the_fireplace.grandexchange.market.Offer;
import the_fireplace.grandexchange.market.SellOffer;

public class Utils {
    public static List<String> getListOfStringsMatchingString(String s, Collection<?> possibleCompletions)
    {
        List<String> list = Lists.newArrayList();

        if (!possibleCompletions.isEmpty())
        {
            for (String s1 : possibleCompletions.stream().map(Functions.toStringFunction()).collect(Collectors.toList()))
            {
                if (doesStringStartWith(s, s1))
                {
                    list.add(s1);
                }
            }

            if (list.isEmpty())
            {
                for (Object object : possibleCompletions)
                {
                    if (object instanceof ResourceLocation && doesStringStartWith(s, ((ResourceLocation)object).getPath()))
                    {
                        list.add(String.valueOf(object));
                    }
                }
            }
        }

        return list;
    }

    public static boolean doesStringStartWith(String original, String region)
    {
        return region.regionMatches(true, 0, original, 0, original.length());
    }

    public static List<String> getBuyNames(List<BuyOffer> in){
        List<String> names = Lists.newArrayList();
        for(Offer offer : in){
            names.add(offer.getItemResourceName());
        }
        return names;
    }

    public static List<String> getSellNames(List<SellOffer> in){
        List<String> names = Lists.newArrayList();
        for(Offer offer : in){
            names.add(offer.getItemResourceName());
        }
        return names;
    }
}