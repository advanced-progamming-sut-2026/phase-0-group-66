package view;

import java.util.List;

public class ShopView {
    public void showItems(List<String> items) {
        items.forEach(System.out::println);
    }

    public void showMessage(String message) {
        System.out.println(message);
    }
}
