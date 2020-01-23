package the_fireplace.grandexchange.util;

import com.google.common.collect.Maps;
import net.minecraftforge.common.DimensionManager;
import org.apache.commons.lang3.tuple.Pair;
import the_fireplace.grandexchange.market.BuyOffer;
import the_fireplace.grandexchange.market.OfferType;
import the_fireplace.grandexchange.market.SellOffer;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static the_fireplace.grandexchange.market.ExchangeManager.addPayout;
import static the_fireplace.grandexchange.market.ExchangeManager.makeOffer;

@Deprecated
public final class TransactionDatabase implements Serializable {
	private static final long serialVersionUID = 0x42069;

	private static TransactionDatabase instance = null;
	private static final String dataFileName = "grandexchange.dat";
	private static File saveDir = DimensionManager.getCurrentSaveRootDirectory();

	public static TransactionDatabase getInstance() {
		if(instance == null)
			readFromFile();
		return null;
	}

	private HashMap<Pair<String, Integer>, List<BuyOffer>> buyOffers = Maps.newHashMap();
	private HashMap<Pair<String, Integer>, List<SellOffer>> sellOffers = Maps.newHashMap();
	private HashMap<UUID, List<String>> payouts = Maps.newHashMap();

	private static void readFromFile() {
		if (saveDir == null)
			saveDir = DimensionManager.getCurrentSaveRootDirectory();
		if (saveDir == null) {
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
			}
		}
		f.delete();
		if(instance != null) {
			for(List<BuyOffer> buyOfferList: instance.buyOffers.values()) {
				for(BuyOffer offer: buyOfferList) {
					makeOffer(OfferType.BUY, offer.getItemResourceName(), offer.getItemMeta(), offer.getAmount(), offer.getPrice(), offer.getOwner(), offer.getNbt());
				}
			}
			for(List<SellOffer> sellOfferList: instance.sellOffers.values()) {
				for(SellOffer offer: sellOfferList) {
					makeOffer(OfferType.SELL, offer.getItemResourceName(), offer.getItemMeta(), offer.getAmount(), offer.getPrice(), offer.getOwner(), offer.getNbt());
				}
			}
			for(Map.Entry<UUID, List<String>> entry: instance.payouts.entrySet()) {
				for(String serialItemStack: entry.getValue()) {
					addPayout(entry.getKey(), SerializationUtils.stackFromString(serialItemStack));
				}
			}
		}
	}
}
