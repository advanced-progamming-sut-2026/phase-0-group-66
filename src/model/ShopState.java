package model;

import java.io.Serializable;
import java.time.LocalDate;

public class ShopState implements Serializable {
    private static final long serialVersionUID = 1L;

    private String offerDate;
    private String dailyPlant;
    private boolean dailyPurchased;

    public String getOfferDate() { return offerDate; }
    public String getDailyPlant() { return dailyPlant; }
    public boolean isDailyPurchased() { return dailyPurchased; }

    public void setDailyOffer(LocalDate date, String plantName) {
        offerDate = date.toString();
        dailyPlant = plantName;
        dailyPurchased = false;
    }

    public boolean isCurrent(LocalDate date) {
        return date.toString().equals(offerDate);
    }

    public void markDailyPurchased() {
        dailyPurchased = true;
    }
}
