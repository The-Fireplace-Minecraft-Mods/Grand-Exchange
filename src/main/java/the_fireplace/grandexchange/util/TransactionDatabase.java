package the_fireplace.grandexchange.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.registries.ForgeRegistries;
import the_fireplace.grandeconomy.TextStyles;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.GrandExchange;
import the_fireplace.grandexchange.market.BuyOffer;
import the_fireplace.grandexchange.market.Offer;
import the_fireplace.grandexchange.market.SellOffer;

import java.io.*;
import java.util.*;

public final class TransactionDatabase implements Serializable {
	private static final long serialVersionUID = 0x5EED1355;

	private static TransactionDatabase instance = null;
	private static final String dataFileName = "grandexchange.dat";
	private static File saveDir = GrandExchange.saveRoot;

	public static TransactionDatabase getInstance() {
		if(instance == null)
			readFromFile();
		return instance;
	}

	public static HashMap<String, List<BuyOffer>> getBuyOffers() {
		getInstance().sortOffers();
		return getInstance().buyOffers;
	}

	public static HashMap<String, List<SellOffer>> getSellOffers() {
		getInstance().sortOffers();
		return getInstance().sellOffers;
	}

	@SuppressWarnings("WeakerAccess")
	public static HashMap<UUID, List<String>> getPayouts() {
		return getInstance().payouts;
	}

	private HashMap<String, List<BuyOffer>> buyOffers = Maps.newHashMap();
	private HashMap<String, List<SellOffer>> sellOffers = Maps.newHashMap();
	private HashMap<UUID, List<String>> payouts = Maps.newHashMap();

	public static boolean hasPayout(UUID player){
		return getPayouts().containsKey(player) && !getPayouts().get(player).isEmpty();
	}

	@SuppressWarnings("WeakerAccess")
	public void addPayout(UUID player, ItemStack payout){
		if(!payouts.containsKey(player))
			payouts.put(player, Lists.newArrayList());
		payouts.get(player).add(SerializationUtils.stackToString(payout));
		saveToFile();
	}

	public static List<String> getPayout(UUID player){
		return hasPayout(player) ? getInstance().payouts.get(player) : Lists.newArrayList();
	}

	public void removePayouts(UUID player, Collection<String> toRemove){
		payouts.get(player).removeAll(toRemove);
	}

	public void cancelOffer(Offer offer){
		if(offer instanceof BuyOffer && buyOffers.get(offer.getItemResourceName()).remove(offer)) {
			GrandEconomyApi.addToBalance(offer.getOwner(), offer.getPrice()*offer.getAmount(), true);
		} else if(offer instanceof SellOffer && sellOffers.get(offer.getItemResourceName()).remove(offer)) {
			ResourceLocation offerRes = new ResourceLocation(offer.getItemResourceName());
			boolean isOfferBlock = ForgeRegistries.BLOCKS.containsKey(offerRes);
			addPayout(offer.getOwner(), isOfferBlock ? new ItemStack(Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(offerRes)), offer.getAmount()) : new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(offerRes)), offer.getAmount()));
		}
		saveToFile();
	}

	public static boolean canTransactItem(ItemStack item){
		return !item.isEmpty();
	}

	public boolean makeOffer(Offer offer){
		boolean offerFulfilled = tryFulfillOffer(offer);
		if(!offerFulfilled){
			if(offer instanceof BuyOffer){
				if(!buyOffers.containsKey(offer.getItemResourceName()))
					buyOffers.put(offer.getItemResourceName(), Lists.newArrayList());
				buyOffers.get(offer.getItemResourceName()).add((BuyOffer)offer);
			} else {
				if(!sellOffers.containsKey(offer.getItemResourceName()))
					sellOffers.put(offer.getItemResourceName(), Lists.newArrayList());
				sellOffers.get(offer.getItemResourceName()).add((SellOffer)offer);
			}
		}
		saveToFile();
		return offerFulfilled;
	}

	@SuppressWarnings("Duplicates")
	private boolean tryFulfillOffer(Offer offer) {
		if(!payouts.containsKey(offer.getOwner()))
			payouts.put(offer.getOwner(), Lists.newArrayList());
		ResourceLocation offerResource = new ResourceLocation(offer.getItemResourceName());
		boolean isOfferBlock = ForgeRegistries.BLOCKS.containsKey(offerResource);
		ItemStack sizeCheckStack = isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), 1) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), 1);
		int maxStackSize = sizeCheckStack.getMaxStackSize();
		sortOffers();
		if(offer instanceof BuyOffer){
			if(sellOffers.containsKey(offer.getItemResourceName()) && !sellOffers.get(offer.getItemResourceName()).isEmpty()){
				List<SellOffer> possibleSellOffers = sellOffers.get(offer.getItemResourceName());
				possibleSellOffers.removeIf(o1 -> o1.getPrice() > offer.getPrice());
				List<SellOffer> removeOffers = Lists.newArrayList();
				boolean offerComplete = false;
				for(SellOffer sellOffer: possibleSellOffers){
					Entity seller = ServerUtils.getEntityFromUUID(sellOffer.getOwner());
					if(offer.getAmount() > sellOffer.getAmount()){
						int givingAmount = sellOffer.getAmount();
						GrandEconomyApi.addToBalance(sellOffer.getOwner(), givingAmount*sellOffer.getPrice(), false);
						if(seller != null)
							seller.sendMessage(new TextComponentTranslation("Sell Offer fulfilled: %s %s %s at %s each.", givingAmount, offer.getItemResourceName(), offer.getPrice()).setStyle(TextStyles.PURPLE));
						while(givingAmount > maxStackSize) {
							payouts.get(offer.getOwner()).add(SerializationUtils.stackToString(isOfferBlock ? new ItemStack(Item.BLOCK_TO_ITEM.get(ForgeRegistries.BLOCKS.getValue(offerResource)), maxStackSize, SerializationUtils.getNbt(offer.getNbt())) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), maxStackSize, SerializationUtils.getNbt(offer.getNbt()))));
							givingAmount -= maxStackSize;
						}
						payouts.get(offer.getOwner()).add(SerializationUtils.stackToString(isOfferBlock ? new ItemStack(Item.BLOCK_TO_ITEM.get(ForgeRegistries.BLOCKS.getValue(offerResource)), givingAmount, SerializationUtils.getNbt(offer.getNbt())) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), givingAmount, SerializationUtils.getNbt(offer.getNbt()))));
						offer.decrementAmount(sellOffer.getAmount());
						removeOffers.add(sellOffer);
					} else {
						int givingAmount = offer.getAmount();
						GrandEconomyApi.addToBalance(sellOffer.getOwner(), givingAmount*sellOffer.getPrice(), false);
						while(givingAmount > maxStackSize) {
							payouts.get(offer.getOwner()).add(SerializationUtils.stackToString(isOfferBlock ? new ItemStack(Item.BLOCK_TO_ITEM.get(ForgeRegistries.BLOCKS.getValue(offerResource)), maxStackSize, SerializationUtils.getNbt(offer.getNbt())) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), maxStackSize, SerializationUtils.getNbt(offer.getNbt()))));
							givingAmount -= maxStackSize;
						}
						payouts.get(offer.getOwner()).add(SerializationUtils.stackToString(isOfferBlock ? new ItemStack(Item.BLOCK_TO_ITEM.get(ForgeRegistries.BLOCKS.getValue(offerResource)), givingAmount, SerializationUtils.getNbt(offer.getNbt())) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), givingAmount, SerializationUtils.getNbt(offer.getNbt()))));
						if(offer.getAmount() == sellOffer.getAmount()) {
							removeOffers.add(sellOffer);
							if(seller != null)
								seller.sendMessage(new TextComponentTranslation("Sell Offer fulfilled: %s %s %s at %s each.", offer.getAmount(), offer.getItemResourceName(), offer.getNbt() != null ? " with NBT "+offer.getNbt() : "", sellOffer.getPrice()).setStyle(TextStyles.PURPLE));
						} else {
							sellOffer.decrementAmount(offer.getAmount());
							if(seller != null)
								seller.sendMessage(new TextComponentTranslation("Sell Offer partially fulfilled: %s %s %s at %s each.", offer.getAmount(), offer.getItemResourceName(), offer.getNbt() != null ? " with NBT "+offer.getNbt() : "", sellOffer.getPrice()).setStyle(TextStyles.PURPLE));
						}
						offerComplete = true;
						break;
					}
				}
				sellOffers.get(offer.getItemResourceName()).removeAll(removeOffers);
				saveToFile();
				return offerComplete;
			}
		} else {
			if(buyOffers.containsKey(offer.getItemResourceName()) && !buyOffers.get(offer.getItemResourceName()).isEmpty()){
				List<BuyOffer> possibleBuyOffers = buyOffers.get(offer.getItemResourceName());
				possibleBuyOffers.removeIf(o1 -> o1.getPrice() < offer.getPrice());
				List<BuyOffer> removeOffers = Lists.newArrayList();
				boolean offerComplete = false;
				for(BuyOffer buyOffer: possibleBuyOffers){
					Entity buyer = ServerUtils.getEntityFromUUID(buyOffer.getOwner());
					if(!payouts.containsKey(buyOffer.getOwner()))
						payouts.put(buyOffer.getOwner(), Lists.newArrayList());
					if(offer.getAmount() > buyOffer.getAmount()){
						int givingAmount = buyOffer.getAmount();
						GrandEconomyApi.addToBalance(offer.getOwner(), givingAmount*buyOffer.getPrice(), false);
						while(givingAmount > maxStackSize) {
							payouts.get(buyOffer.getOwner()).add(SerializationUtils.stackToString(isOfferBlock ? new ItemStack(Item.BLOCK_TO_ITEM.get(ForgeRegistries.BLOCKS.getValue(offerResource)), maxStackSize, SerializationUtils.getNbt(buyOffer.getNbt())) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), maxStackSize, SerializationUtils.getNbt(buyOffer.getNbt()))));
							givingAmount -= maxStackSize;
						}
						payouts.get(buyOffer.getOwner()).add(SerializationUtils.stackToString(isOfferBlock ? new ItemStack(Item.BLOCK_TO_ITEM.get(ForgeRegistries.BLOCKS.getValue(offerResource)), givingAmount, SerializationUtils.getNbt(buyOffer.getNbt())) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), givingAmount, SerializationUtils.getNbt(buyOffer.getNbt()))));
						offer.decrementAmount(buyOffer.getAmount());
						removeOffers.add(buyOffer);
						if(buyer != null)
							buyer.sendMessage(new TextComponentTranslation("Buy Offer fulfilled: %s %s %s at %s each.", buyOffer.getAmount(), buyOffer.getItemResourceName(), buyOffer.getNbt() != null ? " with NBT "+buyOffer.getNbt() : "", buyOffer.getPrice()).setStyle(TextStyles.BLUE));
					} else {
						int givingAmount = offer.getAmount();
						GrandEconomyApi.addToBalance(offer.getOwner(), givingAmount*buyOffer.getPrice(), false);
						while(givingAmount > maxStackSize) {
							payouts.get(buyOffer.getOwner()).add(SerializationUtils.stackToString(isOfferBlock ? new ItemStack(Item.BLOCK_TO_ITEM.get(ForgeRegistries.BLOCKS.getValue(offerResource)), maxStackSize, SerializationUtils.getNbt(buyOffer.getNbt())) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), maxStackSize, SerializationUtils.getNbt(buyOffer.getNbt()))));
							givingAmount -= maxStackSize;
						}
						payouts.get(buyOffer.getOwner()).add(SerializationUtils.stackToString(isOfferBlock ? new ItemStack(Item.BLOCK_TO_ITEM.get(ForgeRegistries.BLOCKS.getValue(offerResource)), givingAmount, SerializationUtils.getNbt(buyOffer.getNbt())) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), givingAmount, SerializationUtils.getNbt(buyOffer.getNbt()))));
						if(offer.getAmount() == buyOffer.getAmount()) {
							removeOffers.add(buyOffer);
							if(buyer != null)
								buyer.sendMessage(new TextComponentTranslation("Buy Offer fulfilled: %s %s %s at %s each.", buyOffer.getAmount(), buyOffer.getItemResourceName(), buyOffer.getNbt() != null ? " with NBT "+buyOffer.getNbt() : "", buyOffer.getPrice()).setStyle(TextStyles.BLUE));
						} else {
							buyOffer.decrementAmount(offer.getAmount());
							if(buyer != null)
								buyer.sendMessage(new TextComponentTranslation("Buy Offer partially fulfilled: %s %s %s at %s each.", offer.getAmount(), buyOffer.getItemResourceName(), buyOffer.getNbt() != null ? " with NBT "+buyOffer.getNbt() : "", buyOffer.getPrice()).setStyle(TextStyles.BLUE));
						}
						offerComplete = true;
						break;
					}
				}
				buyOffers.get(offer.getItemResourceName()).removeAll(removeOffers);
				saveToFile();
				return offerComplete;
			}
		}
		return false;
	}

	private void sortOffers(){
		for(List<BuyOffer> buyOffer : buyOffers.values())
			buyOffer.sort(Comparator.comparing(BuyOffer::getPrice).thenComparing(BuyOffer::getTimestamp));
		for(List<SellOffer> sellOffer : sellOffers.values())
			sellOffer.sort(Comparator.comparing(SellOffer::getTimestamp));
	}

	private static void readFromFile() {
		if (saveDir == null)
			saveDir = GrandExchange.saveRoot;
		if (saveDir == null) {
			instance = new TransactionDatabase();
			return;
		}
		File f = new File(saveDir, dataFileName);
		if (f.exists()) {
			try {
				ObjectInputStream stream = new ObjectInputStream(new FileInputStream(f));
				instance = (TransactionDatabase) stream.readObject();
				stream.close();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				instance = new TransactionDatabase();
				//noinspection ResultOfMethodCallIgnored
				f.delete();
			}
		}
		if (instance == null)
			instance = new TransactionDatabase();
	}

	private static void saveToFile() {
		try {
			if (saveDir == null)
				saveDir = GrandExchange.saveRoot;
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(saveDir, dataFileName)));
			out.writeObject(instance);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
